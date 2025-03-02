package com.guicedee.guicedservlets.rest.services;

import com.guicedee.guicedinjection.interfaces.IGuicePreStartup;
import com.guicedee.vertx.spi.VertXPreStartup;
import com.zandero.rest.RestRouter;
import io.vertx.core.Future;

import java.util.List;

public class GuicedRestPreStartup implements IGuicePreStartup<GuicedRestPreStartup>
{
    @Override
    public List<Future<Boolean>> onStartup()
    {
        return List.of(VertXPreStartup.getVertx().executeBlocking(() -> {
            RestRouter.injectWith(new GuiceRestInjectionProvider());
            return true;
        }));
    }
}
