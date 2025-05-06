package com.guicedee.guicedservlets.rest.implementations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import com.guicedee.client.CallScopeProperties;
import com.guicedee.client.CallScopeSource;
import com.guicedee.client.CallScoper;
import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedservlets.rest.services.RestInterceptor;
import com.guicedee.services.jsonrepresentation.IJsonRepresentation;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

public class RestCallScopeInterceptor implements MethodInterceptor
{
    private static final Logger log = LoggerFactory.getLogger(RestCallScopeInterceptor.class);
    @Inject
    CallScoper callScoper;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable
    {
        Object result = null;
        try
        {
            callScoper.enter();
            CallScopeProperties csp = IGuiceContext.get(CallScopeProperties.class);
            csp.setSource(CallScopeSource.Rest);
            csp.getProperties().put("rest.method", invocation.getMethod().getName());
            csp.getProperties().put("rest.class", invocation.getMethod().getDeclaringClass().getName());
            try
            {
                csp.getProperties().put("rest.arguments", IJsonRepresentation.getObjectMapper().writeValueAsString(invocation.getArguments()));
            }
            catch (JsonProcessingException e)
            {
                log.warn("Failed to serialize arguments for REST call", e);
            }

            ServiceLoader<RestInterceptor> interceptors = ServiceLoader.load(RestInterceptor.class);
            interceptors.forEach(RestInterceptor::onStart);
            result = invocation.proceed();
            interceptors.forEach(RestInterceptor::onEnd);
        }
        finally
        {
            callScoper.exit();
        }
        return result;
    }
}
