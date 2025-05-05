package com.guicedee.guicedservlets.rest.test;

import com.guicedee.client.IGuiceContext;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RestEasyModuleTest
{
    public static void main(String[] args) throws Exception
    {
        new RestEasyModuleTest().configureServlets();
    }

    @Test
    void configureServlets() throws Exception
    {
        // Initialize the Guice context
        IGuiceContext.instance().inject();

        // Wait for the server to start up
        System.out.println("Waiting for server to start...");
        Thread.sleep(2000); // Wait 2 seconds
        System.out.println("Server should be started now");

        // Create an HTTP client
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.of(5, ChronoUnit.SECONDS))
                .build();

        // Test the debug endpoint first to verify the router is working
        System.out.println("Testing debug endpoint...");
        HttpResponse response = client.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI("http://localhost:8080/debug"))
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        System.out.println("Debug endpoint response: " + response.statusCode());

        // Test the hello endpoint
        System.out.println("Testing hello endpoint...");
        response = client.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI("http://localhost:8080/rest/hello/world"))
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/json")
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        System.out.println("Hello endpoint response: " + response.statusCode());
        assertEquals(200, response.statusCode());

        // Test the helloObject endpoint
        System.out.println("Testing helloObject endpoint...");
        response = client.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI("http://localhost:8080/rest/hello/helloObject/world"))
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        System.out.println("HelloObject endpoint response: " + response.statusCode());
        assertEquals(200, response.statusCode());

        // Clean up
        IGuiceContext.get(Vertx.class).close();
    }
}
