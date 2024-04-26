package com.guicedee.guicedservlets.rest.implementations;

import com.guicedee.client.IGuiceContext;
import com.guicedee.vertx.spi.VertxRouterConfigurator;
import com.zandero.rest.RestRouter;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.Path;
import lombok.extern.java.Log;

import java.util.function.Predicate;

@Log
public class VertXRestRouter implements VertxRouterConfigurator
{
    private static final Predicate<ClassInfo> filter = (applicationClass) ->
            applicationClass.isAbstract() || applicationClass.isInterface() || applicationClass.isInnerClass() || applicationClass.isStatic();

    @Override
    public Router builder(Router builder)
    {
        ScanResult scanResult = IGuiceContext
                .instance()
                .getScanResult();
        ClassInfoList resourceClasses = scanResult.getClassesWithAnnotation(Path.class);
        for (ClassInfo resource : resourceClasses)
        {
            if (!filter.test(resource))
            {
                builder = RestRouter.register(builder, IGuiceContext.get(resource.loadClass(false)));
            }
        }
        return builder;
    }

}
