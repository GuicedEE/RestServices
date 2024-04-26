package com.guicedee.guicedservlets.rest.implementations;

import com.google.inject.Inject;
import com.guicedee.client.CallScoper;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class RestCallScopeInterceptor implements MethodInterceptor
{
    @Inject
    CallScoper callScoper;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable
    {
        Object result = null;
        try
        {
            callScoper.enter();
            result = invocation.proceed();
        }
        finally
        {
            callScoper.exit();
        }
        return result;
    }
}
