package com.guicedee.guicedservlets.rest.test;

import com.guicedee.client.IGuiceContext;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleRouterTest {

    @Test
    void testSimpleRouter() throws Exception {
        // Create Vertx instance
        Vertx vertx = Vertx.vertx();

        // Create router
        Router router = Router.router(vertx);

        // Add body handler
        router.route().handler(BodyHandler.create());

        // Add a simple route
        router.get("/test").handler(ctx -> {
            System.out.println("Test route accessed");
            ctx.response()
                    .putHeader("content-type", "text/plain")
                    .end("Test route works!");
        });

        // Create HTTP server
        HttpServer server = vertx.createHttpServer();

        // Set router as request handler
        server.requestHandler(router);

        // Start server
        CountDownLatch latch = new CountDownLatch(1);
        server.listen(8081)
                .onSuccess(s -> {
                    System.out.println("Server started on port 8081");
                    latch.countDown();


                    // Create HTTP client
                    HttpClient client = HttpClient.newBuilder()
                            .connectTimeout(Duration.of(5, ChronoUnit.SECONDS))
                            .build();

                    // Test the route
                    System.out.println("Testing route...");
                    HttpResponse<String> response = null;
                    try {
                        response = client.send(HttpRequest.newBuilder()
                                        .GET()
                                        .uri(new URI("http://localhost:8081/test"))
                                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                                        .build(),
                                HttpResponse.BodyHandlers.ofString());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }

                    System.out.println("Response: " + response.statusCode() + " - " + response.body());
                    assertEquals(200, response.statusCode());
                    assertEquals("Test route works!", response.body());


                    // Clean up
                    server.close();
                    vertx.close();

                })
                .onFailure(cause -> {
                    System.err.println("Failed to start server: " + cause.getMessage());
                });

        // Wait for server to start
        //latch.await(5, TimeUnit.SECONDS);


    }
}
