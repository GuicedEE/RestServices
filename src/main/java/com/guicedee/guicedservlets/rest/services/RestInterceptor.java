package com.guicedee.guicedservlets.rest.services;

import io.vertx.core.Future;

public interface RestInterceptor
{
    Future<Boolean> onStart();
    Future<Boolean> onEnd();
}
