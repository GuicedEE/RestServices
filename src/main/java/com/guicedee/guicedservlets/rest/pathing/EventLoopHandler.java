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
 * Handles event loop management for Jakarta WS endpoints.
 */
public class EventLoopHandler {

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
     * Checks if a method should be executed on a worker thread.
     *
     * @param method The method to check
     * @return true if the method should be executed on a worker thread, false otherwise
     */
    public static boolean shouldRunOnWorkerThread(Method method) {
        // Check if the method is annotated with a blocking annotation
        // For simplicity, we'll consider methods that don't return Future or Uni as blocking
        Class<?> returnType = method.getReturnType();
        String returnTypeName = returnType.getName();

        return !returnTypeName.contains("Future") && !returnTypeName.contains("Uni");
    }

    /**
     * Executes a task on the appropriate thread.
     *
     * @param vertx The Vertx instance
     * @param context The routing context
     * @param task The task to execute
     * @param method The method being executed
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
     * @param vertx The Vertx instance
     * @param context The routing context
     * @param task The task to execute
     * @param method The method being executed
     * @param <T> The return type of the task
     * @return The result of the task
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
