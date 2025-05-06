package com.guicedee.guicedservlets.rest.implementations;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import com.guicedee.guicedinjection.interfaces.IGuiceModule;
import jakarta.ws.rs.*;

public class RestModule extends AbstractModule implements IGuiceModule<RestModule>
{

    @Override
    protected void configure()
    {
        RestCallScopeInterceptor interceptor = new RestCallScopeInterceptor();
        requestInjection(interceptor);
        // Intercept methods annotated with HTTP method annotations
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(GET.class), interceptor);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(POST.class), interceptor);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(PUT.class), interceptor);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(DELETE.class), interceptor);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(HEAD.class), interceptor);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(OPTIONS.class), interceptor);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(PATCH.class), interceptor);

        //bindInterceptor(Matchers.any(), Matchers.annotatedWith(Path.class), interceptor);
    }

}
