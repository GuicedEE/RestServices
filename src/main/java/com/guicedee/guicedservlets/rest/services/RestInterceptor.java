package com.guicedee.guicedservlets.rest.services;

import io.vertx.core.Future;

/**
 * Intercepts the lifecycle of a REST call.
 *
 * <p>Implementations can hook into the start and end of a request to perform
 * logging, metrics, or other cross-cutting concerns. Returned futures allow
 * asynchronous setup and teardown.</p>
 */
public interface RestInterceptor
{
    /**
     * Invoked before the resource method is executed.
     *
     * @return A future that completes when startup logic finishes
     */
    Future<Boolean> onStart();

    /**
     * Invoked after the resource method completes.
     *
     * @return A future that completes when cleanup logic finishes
     */
    Future<Boolean> onEnd();
}
