package com.guicedee.guicedservlets.rest.test;

import com.guicedee.client.IGuiceContext;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ComprehensiveTest {

    private HttpClient client;

    @BeforeAll
    void setUp() throws Exception {
        // Initialize the Guice context
        System.out.println("Initializing Guice context...");
        IGuiceContext.instance().inject();

        // Wait for the server to start up
        System.out.println("Waiting for server to start...");
        Thread.sleep(2000); // Wait 2 seconds
        System.out.println("Server should be started now");

        // Create an HTTP client
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.of(5, ChronoUnit.SECONDS))
                .build();
    }

    @AfterAll
    void tearDown() {
        // Clean up
        IGuiceContext.get(Vertx.class).close();
    }

    @Test
    void testHelloEndpoint() throws Exception {
        // Test the hello endpoint
        System.out.println("Testing hello endpoint...");
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI("http://localhost:8080/rest/hello/world"))
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/json")
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        System.out.println("Hello endpoint response: " + response.statusCode() + " - " + response.body());
        assertEquals(200, response.statusCode());
        assertEquals("\"Hello world\"", response.body());
    }

    @Test
    void testHelloObjectEndpoint() throws Exception {
        // Test the helloObject endpoint
        System.out.println("Testing helloObject endpoint...");
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI("http://localhost:8080/rest/hello/helloObject/world"))
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        System.out.println("HelloObject endpoint response: " + response.statusCode() + " - " + response.body());
        assertEquals(200, response.statusCode());
        // The response should be a JSON object with a name field
        assertEquals("{\"name\":\"world\"}", response.body());
    }

    @Test
    void testNonExistentEndpoint() throws Exception {
        // Test a non-existent endpoint
        System.out.println("Testing non-existent endpoint...");
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI("http://localhost:8080/rest/nonexistent"))
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        System.out.println("Non-existent endpoint response: " + response.statusCode());
        assertEquals(404, response.statusCode());
    }
}
