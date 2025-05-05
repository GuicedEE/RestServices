package com.guicedee.guicedservlets.rest.test;

import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedservlets.rest.pathing.OperationRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
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

public class OperationRegistryTest {

    @Test
    void testOperationRegistry() throws Exception {
        // Create Vertx instance
        Vertx vertx = Vertx.vertx();
        
        // Create router
        Router router = Router.router(vertx);
        
        // Add a direct route for testing
        router.get("/direct").handler(ctx -> {
            System.out.println("Direct route accessed");
            ctx.response()
                    .putHeader("content-type", "text/plain")
                    .end("Direct route works!");
        });
        
        // Create and use OperationRegistry
        OperationRegistry registry = new OperationRegistry();
        
        // Inject Vertx into the registry
        java.lang.reflect.Field vertxField = OperationRegistry.class.getDeclaredField("vertx");
        vertxField.setAccessible(true);
        vertxField.set(registry, vertx);
        
        // Use the registry to build the router
        router = registry.builder(router);
        
        // Add another direct route after the registry
        router.get("/after").handler(ctx -> {
            System.out.println("After route accessed");
            ctx.response()
                    .putHeader("content-type", "text/plain")
                    .end("After route works!");
        });
        
        // Create HTTP server
        HttpServer server = vertx.createHttpServer();
        
        // Set router as request handler
        server.requestHandler(router);
        
        // Start server
        CountDownLatch latch = new CountDownLatch(1);
        server.listen(8082)
            .onSuccess(s -> {
                System.out.println("Server started on port 8082");
                latch.countDown();
            })
            .onFailure(cause -> {
                System.err.println("Failed to start server: " + cause.getMessage());
            });
        
        // Wait for server to start
        latch.await(5, TimeUnit.SECONDS);
        
        // Create HTTP client
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.of(5, ChronoUnit.SECONDS))
                .build();
        
        // Test the direct route
        System.out.println("Testing direct route...");
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI("http://localhost:8082/direct"))
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Direct route response: " + response.statusCode() + " - " + response.body());
        assertEquals(200, response.statusCode());
        assertEquals("Direct route works!", response.body());
        
        // Test the after route
        System.out.println("Testing after route...");
        response = client.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI("http://localhost:8082/after"))
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        
        System.out.println("After route response: " + response.statusCode() + " - " + response.body());
        assertEquals(200, response.statusCode());
        assertEquals("After route works!", response.body());
        
        // Test the hello route (should be registered by OperationRegistry)
        System.out.println("Testing hello route...");
        response = client.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI("http://localhost:8082/rest/hello/world"))
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Hello route response: " + response.statusCode() + " - " + response.body());
        
        // Clean up
        server.close();
        vertx.close();
    }
}