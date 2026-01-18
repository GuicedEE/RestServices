package com.guicedee.guicedservlets.rest.pathing;

import com.guicedee.client.IGuiceContext;
import com.guicedee.client.scopes.CallScopeProperties;
import com.guicedee.client.scopes.CallScopeSource;
import com.guicedee.client.scopes.CallScoper;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Executes REST endpoint invocations on the appropriate Vert.x thread.
 *
 * <p>Methods that return reactive or future types are treated as non-blocking
 * and run directly on the event loop. Other methods are executed on a worker
 * thread using {@code executeBlocking} to avoid blocking the event loop.</p>
 *
 * <p>While executing a task, a call scope is established so dependency-scoped
 * resources can be bound to the lifetime of the request.</p>
 */
public class EventLoopHandler {

    /**
     * Runs a task while ensuring a REST call scope is active.
     *
     * @param task The task to execute
     * @param source The call scope source to register
     * @param <T> The task result type
     * @return The task result
     */
    private static <T> T withCallScope(java.util.concurrent.Callable<T> task, CallScopeSource source) {
        CallScoper callScoper = IGuiceContext.get(CallScoper.class);
        boolean startedScope = callScoper.isStartedScope();
        if (!startedScope) {
            callScoper.enter();
        }
        try {
            CallScopeProperties props = IGuiceContext.get(CallScopeProperties.class);
            if (props.getSource() == null || props.getSource() == CallScopeSource.Unknown) {
                props.setSource(source);
            }
            return task.call();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (!startedScope) {
                callScoper.exit();
            }
        }
    }

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
     * @param vertx The Vertx instance used to schedule work
     * @param context The routing context for the current request
     * @param task The task to execute
     * @param method The endpoint method being executed
     */
    public static void executeTask(Vertx vertx, RoutingContext context, Runnable task, Method method) {
        if (shouldRunOnWorkerThread(method)) {
            // Execute on worker thread
            vertx.executeBlocking(() -> withCallScope(() -> {
                try {
                    task.run();
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, CallScopeSource.Rest)).onFailure(cause -> {
                ResponseHandler.handleException(context, cause);
            });
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
     * Executes a task that returns a value on the appropriate thread.
     *
     * <p>When executed on a worker thread, the result is awaited before returning.
     * Any exception is routed to {@link ResponseHandler} and {@code null} is returned.</p>
     *
     * @param vertx The Vertx instance used to schedule work
     * @param context The routing context for the current request
     * @param task The task to execute
     * @param method The endpoint method being executed
     * @param <T> The return type of the task
     * @return The result of the task, or {@code null} if an error occurs
     */
    public static <T> T executeTaskWithResult(Vertx vertx, RoutingContext context, Supplier<T> task, Method method) {
        if (shouldRunOnWorkerThread(method)) {
            // Execute on worker thread
            CompletableFuture<T> future = new CompletableFuture<>();

            vertx.executeBlocking(() -> withCallScope(() -> {
                try {
                    return task.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, CallScopeSource.Rest)).onSuccess(result -> {
                future.complete((T) result);
            }).onFailure(cause -> {
                future.completeExceptionally(cause);
            });

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
}
