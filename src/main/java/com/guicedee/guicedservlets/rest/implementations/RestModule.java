package com.guicedee.guicedservlets.rest.implementations;

import com.google.inject.AbstractModule;
import com.guicedee.client.services.lifecycle.IGuiceModule;

/**
 * Guice module for REST-specific bindings and interceptors.
 *
 * <p>Currently this module does not register interceptors by default, but it
 * serves as the extension point to wire in request-scoped behavior.</p>
 */
public class RestModule extends AbstractModule implements IGuiceModule<RestModule>
{

    /**
     * Configures Guice bindings for REST services.
     */
    @Override
    protected void configure()
    {
     /*   RestCallScopeInterceptor interceptor = new RestCallScopeInterceptor();
        requestInjection(interceptor);
        // Intercept methods annotated with HTTP method annotations
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(GET.class), interceptor);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(POST.class), interceptor);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(PUT.class), interceptor);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(DELETE.class), interceptor);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(HEAD.class), interceptor);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(OPTIONS.class), interceptor);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(PATCH.class), interceptor);
*/
        //bindInterceptor(Matchers.any(), Matchers.annotatedWith(Path.class), interceptor);
    }

}
