package com.guicedee.guicedservlets.rest.implementations;

import com.guicedee.client.Environment;
import com.guicedee.client.IGuiceContext;
import com.guicedee.vertx.spi.VerticleStartup;
import com.guicedee.vertx.web.spi.VertxHttpServerConfigurator;
import com.guicedee.vertx.web.spi.VertxHttpServerOptionsConfigurator;
import com.guicedee.vertx.web.spi.VertxRouterConfigurator;
import com.zandero.rest.RestRouter;
import io.github.classgraph.ClassInfoList;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Path;
import lombok.extern.log4j.Log4j2;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

@Log4j2
public class GuicedRestHttpServerConfigurator implements VerticleStartup<GuicedRestHttpServerConfigurator>
{

    @Override
    public void start(Promise<Void> startPromise, Vertx vertx, AbstractVerticle verticle, String assignedPackage)
    {
        HttpServerOptions serverOptions = new HttpServerOptions();
        serverOptions.setCompressionSupported(true);
        serverOptions.setCompressionLevel(9);
        serverOptions.setTcpKeepAlive(true);
        serverOptions.setMaxHeaderSize(65536);
        serverOptions.setMaxChunkSize(65536);
        serverOptions.setMaxFormAttributeSize(65536);
        serverOptions.setMaxFormFields(-1);

        ServiceLoader<VertxHttpServerOptionsConfigurator> serverOptionsConfigurators = ServiceLoader.load(VertxHttpServerOptionsConfigurator.class);
        serverOptionsConfigurators.stream()
                .map(ServiceLoader.Provider::get)
                .filter(a -> a.getClass().getPackage().getName().startsWith(assignedPackage) ||
                        a.getClass().getPackage().getName().startsWith("com.guicedee.guicedservlets.rest")
                )
                //.flatMap(a -> httpServers.stream()
                //      .map(s -> new AbstractMap.SimpleEntry<>(a, s)))
                .forEachOrdered(entry -> {
                    entry.builder(serverOptions);
                });

        List<HttpServer> httpServers = new ArrayList<>();
        if (Boolean.parseBoolean(Environment.getProperty("REST_HTTP_ENABLED", "true")))
        {
            var server = vertx.createHttpServer(serverOptions);
            serverOptions.setPort(Integer.parseInt(Environment.getSystemPropertyOrEnvironment("REST_HTTP_PORT", "8080")));
            httpServers.add(server);
        }

        if (Boolean.parseBoolean(Environment.getProperty("REST_HTTPS_ENABLED", "false")))
        {
            serverOptions.setSsl(true).setUseAlpn(true);
            serverOptions.setPort(Integer.parseInt(Environment.getSystemPropertyOrEnvironment("REST_HTTPS_PORT", "443")));
            if (Environment.getSystemPropertyOrEnvironment("REST_HTTPS_KEYSTORE", "").toLowerCase().endsWith("pfx") ||
                    Environment.getSystemPropertyOrEnvironment("REST_HTTPS_KEYSTORE", "").toLowerCase().endsWith("p12") ||
                    Environment.getSystemPropertyOrEnvironment("REST_HTTPS_KEYSTORE", "").toLowerCase().endsWith("p8")
            )
            {
                serverOptions
                        .setKeyCertOptions(new PfxOptions()
                                .setPassword(Environment.getSystemPropertyOrEnvironment("REST_HTTPS_KEYSTORE_PASSWORD", ""))
                                .setPath(Environment.getSystemPropertyOrEnvironment("REST_HTTPS_KEYSTORE", "keystore.pfx")));

            } else if (Environment.getSystemPropertyOrEnvironment("REST_HTTPS_KEYSTORE", "").toLowerCase().endsWith("jks"))
            {
                serverOptions
                        .setKeyCertOptions(new JksOptions()
                                .setPassword(Environment.getSystemPropertyOrEnvironment("REST_HTTPS_KEYSTORE_PASSWORD", "changeit"))
                                .setPath(Environment.getSystemPropertyOrEnvironment("REST_HTTPS_KEYSTORE", "keystore.jks")));
            }
            var server = vertx.createHttpServer(serverOptions);
            httpServers.add(server);
        }

        ServiceLoader<VertxHttpServerConfigurator> servers = ServiceLoader.load(VertxHttpServerConfigurator.class);
        servers.stream()
                .map(ServiceLoader.Provider::get)
                .filter(a -> a.getClass().getPackage().getName().startsWith(assignedPackage) ||
                        a.getClass().getPackage().getName().startsWith("com.guicedee.guicedservlets.rest")
                )
                .flatMap(a -> httpServers.stream()
                        .map(s -> new AbstractMap.SimpleEntry<>(a, s)))
                .forEachOrdered(entry -> {
                    IGuiceContext.get(entry.getKey().getClass())
                            .builder(entry.getValue());
                });

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create()
                .setUploadsDirectory("uploads")
                .setDeleteUploadedFilesOnEnd(true));

        ClassInfoList resourceClasses = IGuiceContext.instance().getScanResult().getClassesWithAnnotation(ApplicationPath.class);
        resourceClasses.addAll(IGuiceContext.instance().getScanResult().getClassesWithAnnotation(Path.class));
        resourceClasses.stream()
                .filter(a -> !a.isAbstract() && !a.isInterface() && !a.isInnerClass() && !a.isStatic())
                .filter(a -> a.getClass().getPackage().getName().startsWith(assignedPackage) ||
                        a.getClass().getPackage().getName().startsWith("com.guicedee.guicedservlets.rest")
                )
                .forEach(resource -> RestRouter.register(router, resource.loadClass(false)));

        ServiceLoader<VertxRouterConfigurator> routerConfigurators = ServiceLoader.load(VertxRouterConfigurator.class);
        routerConfigurators.stream()
                .map(ServiceLoader.Provider::get)
                .filter(a -> a.getClass().getPackage().getName().startsWith(assignedPackage) ||
                        a.getClass().getPackage().getName().startsWith("com.guicedee.guicedservlets.rest")
                )
                //.flatMap(a -> httpServers.stream()
                //      .map(s -> new AbstractMap.SimpleEntry<>(a, s)))
                .forEachOrdered(entry -> {
                    IGuiceContext.get(entry.getClass())
                            .builder(router);
                });

        for (var s : httpServers)
        {
            s.requestHandler(router);
        }

        for (var s : httpServers)
        {
            s.listen().onComplete(handler -> {
                if (handler.failed())
                {
                    log.error("Cannot start listener", handler.cause());
                    startPromise.fail(handler.cause());
                }
            });
        }
        startPromise.complete();
    }

}
