package com.guicedee.guicedservlets.rest.test;

import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedservlets.rest.implementations.GuicedRestHttpServerConfigurator;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HttpServerConfiguratorTest {

    @Test
    void testHttpServerConfigurator() throws Exception {
        // Create Vertx instance
        Vertx vertx = Vertx.vertx();
        
        // Create router
        Router router = Router.router(vertx);
        
        // Add body handler
        router.route().handler(BodyHandler.create());
        
        // Add a request logger
        router.route().handler(ctx -> {
            System.out.println("Request received: " + ctx.request().method() + " " + ctx.request().path());
            ctx.next();
        });
        
        // Add a direct route for testing
        router.get("/direct").handler(ctx -> {
            System.out.println("Direct route accessed");
            ctx.response()
                    .putHeader("content-type", "text/plain")
                    .end("Direct route works!");
        });
        
        // Create HTTP server
        HttpServer server = vertx.createHttpServer();
        
        // Set router as request handler
        server.requestHandler(router);
        
        // Create and use GuicedRestHttpServerConfigurator
        GuicedRestHttpServerConfigurator configurator = new GuicedRestHttpServerConfigurator();
        
        // Create a promise to pass to the configurator
        Promise<Void> startPromise = Promise.promise();
        
        // Start the configurator
        configurator.start(startPromise, vertx, new AbstractVerticle() {}, "com.guicedee.guicedservlets.rest.test");
        
        // Wait for the configurator to complete
        CountDownLatch latch = new CountDownLatch(1);
        startPromise.future().onComplete(ar -> {
            if (ar.succeeded()) {
                System.out.println("Configurator started successfully");
                latch.countDown();
            } else {
                System.err.println("Failed to start configurator: " + ar.cause().getMessage());
                ar.cause().printStackTrace();
            }
        });
        
        // Wait for the configurator to start
        latch.await(5, TimeUnit.SECONDS);
        
        // Wait a bit more to ensure everything is set up
        Thread.sleep(2000);
        
        // Create HTTP client
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.of(5, ChronoUnit.SECONDS))
                .build();
        
        // Test the direct route
        System.out.println("Testing direct route...");
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI("http://localhost:8080/direct"))
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Direct route response: " + response.statusCode() + " - " + response.body());
        
        // Test the hello route
        System.out.println("Testing hello route...");
        response = client.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI("http://localhost:8080/rest/hello/world"))
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Hello route response: " + response.statusCode() + " - " + response.body());
        
        // Clean up
        vertx.close();
    }
}