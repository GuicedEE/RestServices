package com.guicedee.rest.pathing;

import com.guicedee.vertx.spi.Verticle;
import com.guicedee.vertx.spi.VerticleBuilder;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.ext.web.RoutingContext;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Executes REST endpoint invocations on the appropriate Vert.x thread.
 *
 * <p>Methods that return reactive or future types are treated as non-blocking
 * and run directly on the event loop. Other methods are executed on a worker
 * thread using {@code executeBlocking} to avoid blocking the event loop.</p>
 *
 * <p>When a resource class belongs to a package annotated with {@code @Verticle},
 * the task is dispatched to the verticle's named worker pool via
 * {@link Vertx#createSharedWorkerExecutor}. Otherwise, the default Vert.x
 * internal worker pool is used.</p>
 *
 * <p>Call scope management is handled at the method invocation level
 * by the REST call scope interceptor and is not duplicated here.</p>
 */
public class EventLoopHandler {

    /** Cache of shared worker executors keyed by worker pool name to avoid re-creation on every request. */
    private static final Map<String, WorkerExecutor> workerExecutors = new ConcurrentHashMap<>();

    /**
     * Determines whether a method should be executed on a worker thread.
     *
     * <p>This is currently a heuristic based on return type: methods that return
     * {@code Future} or {@code Uni} are treated as non-blocking and may run on
     * the event loop. All other methods are treated as blocking.</p>
     *
     * @param method The method to inspect
     * @return {@code true} if the method should be executed on a worker thread
     */
    public static boolean shouldRunOnWorkerThread(Method method) {
        // Check if the method is annotated with a blocking annotation
        // For simplicity, we'll consider methods that don't return Future or Uni as blocking
        Class<?> returnType = method.getReturnType();
        String returnTypeName = returnType.getName();

        return !returnTypeName.contains("Future") && !returnTypeName.contains("Uni");
    }

    /**
     * Executes a runnable task on the appropriate thread and handles failures.
     *
     * <p>If the resource class belongs to a {@code @Verticle}-annotated package
     * with a named worker pool, blocking work is dispatched to that pool.
     * Otherwise, the default Vert.x internal worker pool is used.</p>
     *
     * @param vertx The Vertx instance used to schedule work
     * @param context The routing context for the current request
     * @param task The task to execute
     * @param method The endpoint method being executed
     * @param resourceClass The REST resource class (used for verticle worker pool resolution)
     */
    public static void executeTask(Vertx vertx, RoutingContext context, Runnable task, Method method, Class<?> resourceClass) {
        if (shouldRunOnWorkerThread(method)) {
            // Resolve the worker pool for the resource class's package
            Optional<Verticle> verticleOpt = VerticleBuilder.getVerticleAnnotation(resourceClass);
            if (verticleOpt.isPresent() && verticleOpt.get().value() != null && !verticleOpt.get().value().isEmpty()) {
                Verticle verticle = verticleOpt.get();
                String poolName = verticle.value();
                int poolSize = verticle.workerPoolSize();
                long maxExecTime = verticle.maxWorkerExecuteTime();
                java.util.concurrent.TimeUnit maxExecTimeUnit = verticle.maxWorkerExecuteTimeUnit();

                WorkerExecutor executor = workerExecutors.computeIfAbsent(poolName,
                        name -> vertx.createSharedWorkerExecutor(name, poolSize, maxExecTime, maxExecTimeUnit));

                executor.executeBlocking(() -> {
                    task.run();
                    return null;
                }).onFailure(cause -> ResponseHandler.handleException(context, cause));
            } else {
                // No verticle worker pool defined — use the default Vert.x worker pool
                vertx.executeBlocking(() -> {
                    task.run();
                    return null;
                }).onFailure(cause -> ResponseHandler.handleException(context, cause));
            }
        } else {
            // Execute on event loop
            try {
                task.run();
            } catch (Exception e) {
                ResponseHandler.handleException(context, e);
            }
        }
    }

    /**
     * Backward-compatible overload that uses the default worker pool.
     *
     * @param vertx The Vertx instance used to schedule work
     * @param context The routing context for the current request
     * @param task The task to execute
     * @param method The endpoint method being executed
     */
    public static void executeTask(Vertx vertx, RoutingContext context, Runnable task, Method method) {
        executeTask(vertx, context, task, method, method.getDeclaringClass());
    }

    /**
     * Executes a task that returns a value on the appropriate thread.
     *
     * <p>When executed on a worker thread, the result is awaited before returning.
     * Any exception is routed to {@link ResponseHandler} and {@code null} is returned.</p>
     *
     * <p>If the resource class belongs to a {@code @Verticle}-annotated package
     * with a named worker pool, blocking work is dispatched to that pool.</p>
     *
     * @param vertx The Vertx instance used to schedule work
     * @param context The routing context for the current request
     * @param task The task to execute
     * @param method The endpoint method being executed
     * @param resourceClass The REST resource class (used for verticle worker pool resolution)
     * @param <T> The return type of the task
     * @return The result of the task, or {@code null} if an error occurs
     */
    public static <T> T executeTaskWithResult(Vertx vertx, RoutingContext context, Supplier<T> task, Method method, Class<?> resourceClass) {
        if (shouldRunOnWorkerThread(method)) {
            CompletableFuture<T> future = new CompletableFuture<>();

            // Resolve the worker pool for the resource class's package
            Optional<Verticle> verticleOpt = VerticleBuilder.getVerticleAnnotation(resourceClass);
            io.vertx.core.Future<T> blockingFuture;
            if (verticleOpt.isPresent() && verticleOpt.get().value() != null && !verticleOpt.get().value().isEmpty()) {
                Verticle verticle = verticleOpt.get();
                String poolName = verticle.value();
                int poolSize = verticle.workerPoolSize();
                long maxExecTime = verticle.maxWorkerExecuteTime();
                java.util.concurrent.TimeUnit maxExecTimeUnit = verticle.maxWorkerExecuteTimeUnit();

                WorkerExecutor executor = workerExecutors.computeIfAbsent(poolName,
                        name -> vertx.createSharedWorkerExecutor(name, poolSize, maxExecTime, maxExecTimeUnit));

                blockingFuture = executor.executeBlocking(task::get);
            } else {
                blockingFuture = vertx.executeBlocking(task::get);
            }

            blockingFuture
                    .onSuccess(future::complete)
                    .onFailure(future::completeExceptionally);

            try {
                return future.get();
            } catch (Exception e) {
                ResponseHandler.handleException(context, e);
                return null;
            }
        } else {
            // Execute on event loop
            try {
                return task.get();
            } catch (Exception e) {
                ResponseHandler.handleException(context, e);
                return null;
            }
        }
    }

    /**
     * Backward-compatible overload that uses the default worker pool.
     *
     * @param vertx   The Vertx instance used to schedule work
     * @param context The routing context for the current request
     * @param task    The task to execute
     * @param method  The endpoint method being executed
     * @param <T>     The return type of the task
     * @return The result of the task, or {@code null} if an error occurs
     */
    public static <T> T executeTaskWithResult(Vertx vertx, RoutingContext context, Supplier<T> task, Method method) {
        return executeTaskWithResult(vertx, context, task, method, method.getDeclaringClass());
    }
}
