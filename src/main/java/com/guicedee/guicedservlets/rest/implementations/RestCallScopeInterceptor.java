package com.guicedee.guicedservlets.rest.implementations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import com.guicedee.client.CallScopeProperties;
import com.guicedee.client.CallScopeSource;
import com.guicedee.client.CallScoper;
import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedservlets.rest.services.RestInterceptor;
import com.guicedee.services.jsonrepresentation.IJsonRepresentation;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

public class RestCallScopeInterceptor implements MethodInterceptor
{
    private static final Logger log = LoggerFactory.getLogger(RestCallScopeInterceptor.class);
    @Inject
    CallScoper callScoper;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable
    {
        Object[] result = new Object[]{null};
        Promise<Object> promiseResult = Promise.promise();

        var context= Vertx.currentContext();

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

        Promise<Boolean> promise = Promise.promise();

        List<Future<Boolean>> futures = new ArrayList<>();
        List<Future<Boolean>> endFutures = new ArrayList<>();
        for (RestInterceptor interceptor : interceptors)
        {
            var restInterceptor = IGuiceContext.get(interceptor.getClass());
            futures.add(restInterceptor.onStart());
        }
        Future.all(futures).onComplete(ar -> {
            if (!ar.succeeded())
            {
                promise.fail(ar.cause());
                callScoper.exit();
                return;
            }
            try
            {
                if (context != null)
                {
                    context.executeBlocking(() -> {
                        try
                        {
                            var res = invocation.proceed();
                            return res;
                        }
                        catch (Throwable e)
                        {
                            throw new RuntimeException(e);
                        }
                    },false)
                            .onSuccess(res->{
                                result[0] = res;
                                promiseResult.complete(res);
                                callScoper.exit();
                                return;
                            })
                            .onFailure(error->{
                                log.error("Error during REST call", error);
                                promiseResult.fail(error);
                            });
                }

                for (RestInterceptor interceptor : interceptors)
                {
                    var restInterceptor = IGuiceContext.get(interceptor.getClass());
                    endFutures.add(restInterceptor.onEnd());
                }
                Future.all(endFutures).onComplete(endAr -> {
                    if (endAr.failed())
                    {
                        promise.fail(endAr.cause());
                        callScoper.exit();
                        return;
                    }
                    callScoper.exit();
                });
            }
            catch (Throwable e)
            {
                promise.fail(e);
                callScoper.exit();
            }
        });
        promise.future().await(1, TimeUnit.MINUTES);
        return result[0];
    }
}
