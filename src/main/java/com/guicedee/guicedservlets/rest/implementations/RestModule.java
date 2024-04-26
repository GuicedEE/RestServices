package com.guicedee.guicedservlets.rest.implementations;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import com.guicedee.guicedinjection.interfaces.IGuiceModule;
import jakarta.ws.rs.Path;

public class RestModule extends AbstractModule implements IGuiceModule<RestModule>
{

    @Override
    protected void configure()
    {
        RestCallScopeInterceptor interceptor = new RestCallScopeInterceptor();
        requestInjection(interceptor);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(Path.class), interceptor);
    }

}
