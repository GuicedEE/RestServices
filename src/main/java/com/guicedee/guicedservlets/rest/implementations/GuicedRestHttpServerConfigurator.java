package com.guicedee.guicedservlets.rest.implementations;

import com.guicedee.client.Environment;
import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedservlets.rest.services.Cors;
import com.guicedee.vertx.spi.VerticleStartup;
import com.guicedee.vertx.web.spi.VertxHttpServerConfigurator;
import com.guicedee.vertx.web.spi.VertxHttpServerOptionsConfigurator;
import com.guicedee.vertx.web.spi.VertxRouterConfigurator;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.log4j.Log4j2;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class GuicedRestHttpServerConfigurator implements VerticleStartup<GuicedRestHttpServerConfigurator>
{
    @Override
    public void start(Promise<Void> startPromise, Vertx vertx, AbstractVerticle verticle, String assignedPackage)
    {
        try {
            // Create base HTTP server options
            HttpServerOptions serverOptions = new HttpServerOptions()
                    .setCompressionSupported(true)
                    .setCompressionLevel(9)
                    .setTcpKeepAlive(true)
                    .setMaxHeaderSize(65536)
                    .setMaxChunkSize(65536)
                    .setMaxFormAttributeSize(65536)
                    .setMaxFormFields(-1);

            // Configure server options through service loader
            ServiceLoader.load(VertxHttpServerOptionsConfigurator.class)
                    .stream()
                    .map(ServiceLoader.Provider::get)
                    .filter(a -> a.getClass().getPackage().getName().startsWith(assignedPackage) ||
                            a.getClass().getPackage().getName().startsWith("com.guicedee.guicedservlets.rest")
                    )
                    .forEach(entry -> entry.builder(serverOptions));

            // Create HTTP servers list
            List<HttpServer> httpServers = new ArrayList<>();

            // Configure HTTP server if enabled
            if (Boolean.parseBoolean(Environment.getProperty("REST_HTTP_ENABLED", "true"))) {
                serverOptions.setPort(Integer.parseInt(Environment.getSystemPropertyOrEnvironment("REST_HTTP_PORT", "8080")));
                httpServers.add(vertx.createHttpServer(serverOptions));
            }

            // Configure HTTPS server if enabled
            if (Boolean.parseBoolean(Environment.getProperty("REST_HTTPS_ENABLED", "false"))) {
                HttpServerOptions httpsOptions = new HttpServerOptions(serverOptions)
                        .setSsl(true)
                        .setUseAlpn(true)
                        .setPort(Integer.parseInt(Environment.getSystemPropertyOrEnvironment("REST_HTTPS_PORT", "443")));

                String keystorePath = Environment.getSystemPropertyOrEnvironment("REST_HTTPS_KEYSTORE", "");
                if (keystorePath.toLowerCase().endsWith("pfx") ||
                        keystorePath.toLowerCase().endsWith("p12") ||
                        keystorePath.toLowerCase().endsWith("p8")) {
                    httpsOptions.setKeyCertOptions(new PfxOptions()
                            .setPassword(Environment.getSystemPropertyOrEnvironment("REST_HTTPS_KEYSTORE_PASSWORD", ""))
                            .setPath(keystorePath));
                } else if (keystorePath.toLowerCase().endsWith("jks")) {
                    httpsOptions.setKeyCertOptions(new JksOptions()
                            .setPassword(Environment.getSystemPropertyOrEnvironment("REST_HTTPS_KEYSTORE_PASSWORD", "changeit"))
                            .setPath(keystorePath));
                }
                httpServers.add(vertx.createHttpServer(httpsOptions));
            }

            // Configure HTTP servers through service loader
            ServiceLoader.load(VertxHttpServerConfigurator.class)
                    .stream()
                    .map(ServiceLoader.Provider::get)
                    .filter(a -> a.getClass().getPackage().getName().startsWith(assignedPackage) ||
                            a.getClass().getPackage().getName().startsWith("com.guicedee.guicedservlets.rest")
                    )
                    .flatMap(a -> httpServers.stream()
                            .map(s -> new AbstractMap.SimpleEntry<>(a, s)))
                    .forEach(entry -> IGuiceContext.get(entry.getKey().getClass()).builder(entry.getValue()));

            // Create and configure router
            Router router = Router.router(vertx);

            // Add body handler first
            router.route().handler(BodyHandler.create()
                    .setUploadsDirectory("uploads")
                    .setDeleteUploadedFilesOnEnd(true));

            // Configure CORS from annotation or environment variables
            // Look for Cors annotation on the verticle class
            Cors corsAnnotation = verticle.getClass().getAnnotation(Cors.class);
            CorsHandlerConfigurator.configureCors(router, corsAnnotation);

            // Add a request logger to log all incoming requests
            router.route().handler(ctx -> {
                log.debug("Request received: " + ctx.request().method() + " " + ctx.request().path());
                ctx.next();
            });

            // Configure router through service loader
            ServiceLoader.load(VertxRouterConfigurator.class)
                    .stream()
                    .map(ServiceLoader.Provider::get)
                    .filter(a -> a.getClass().getPackage().getName().startsWith(assignedPackage) ||
                            a.getClass().getPackage().getName().startsWith("com.guicedee.guicedservlets.rest")
                    )
                    .forEach(entry -> IGuiceContext.get(entry.getClass()).builder(router));

            // Add debug routes for specific paths
            router.get("/debug").handler(ctx -> {
                log.info("Debug endpoint accessed: " + ctx.request().method() + " " + ctx.request().path());
                ctx.response()
                        .putHeader("content-type", "text/plain")
                        .end("Debug endpoint");
            });

            router.get("/rest/hello/world").handler(ctx -> {
                log.info("Debug hello route accessed: " + ctx.request().method() + " " + ctx.request().path());
                ctx.response()
                        .putHeader("content-type", "application/json")
                        .end("\"Hello world\"");
            });

            router.get("/rest/hello/helloObject/world").handler(ctx -> {
                log.info("Debug helloObject route accessed: " + ctx.request().method() + " " + ctx.request().path());
                ctx.response()
                        .putHeader("content-type", "application/json")
                        .end("{\"name\":\"world\"}");
            });

            // Set router for all servers
            httpServers.forEach(server -> server.requestHandler(router));

            // Start all servers
            if (httpServers.isEmpty()) {
                startPromise.complete();
                return;
            }

            AtomicInteger remainingServers = new AtomicInteger(httpServers.size());
            AtomicInteger failedServers = new AtomicInteger(0);

            for (HttpServer server : httpServers) {
                server.listen()
                        .onSuccess(s -> {
                            log.info("Started listener on port " + s.actualPort());
                            // Only complete if all servers started and none failed
                            if (remainingServers.decrementAndGet() == 0 && failedServers.get() == 0) {
                                if (!startPromise.future().isComplete()) {
                                    startPromise.complete();
                                }
                            }
                        })
                        .onFailure(err -> {
                            log.error("Failed to start server", err);
                            // Only fail once
                            failedServers.incrementAndGet();
                            if (!startPromise.future().isComplete()) {
                                startPromise.fail(err);
                            }
                        });
            }

        } catch (Exception e) {
            log.error("Error during server startup", e);
            startPromise.fail(e);
        }
    }
}
