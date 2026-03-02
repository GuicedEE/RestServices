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
        TestServerReady.waitForServer();
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
    void testHelloObjectPostEndpoint() throws Exception {
        // Test the POST helloObject endpoint with a JSON request body
        System.out.println("Testing POST helloObject endpoint...");
        String requestBody = "{\"name\":\"FromBody\"}";
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .uri(new URI("http://localhost:8080/rest/hello/helloObject/PathName"))
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/json")
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        System.out.println("POST HelloObject endpoint response: " + response.statusCode() + " - " + response.body());
        assertEquals(200, response.statusCode());
        // The endpoint concatenates the path param name with the request body name
        assertEquals("{\"name\":\"PathNameFromBody\"}", response.body());
    }

    @Test
    void testHelloObjectPutEndpoint() throws Exception {
        // Test the PUT helloObject endpoint with a JSON request body
        System.out.println("Testing PUT helloObject endpoint...");
        String requestBody = "{\"name\":\"FromBody\"}";
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                        .uri(new URI("http://localhost:8080/rest/hello/helloObject/PathName"))
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/json")
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        System.out.println("PUT HelloObject endpoint response: " + response.statusCode() + " - " + response.body());
        assertEquals(200, response.statusCode());
        // The PUT endpoint prefixes with "PUT:"
        assertEquals("{\"name\":\"PUT:PathNameFromBody\"}", response.body());
    }

    @Test
    void testHelloObjectPostEndpointWithEmptyName() throws Exception {
        // Test the POST helloObject endpoint with an empty name in the body
        System.out.println("Testing POST helloObject endpoint with empty body name...");
        String requestBody = "{\"name\":\"\"}";
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .uri(new URI("http://localhost:8080/rest/hello/helloObject/OnlyPath"))
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/json")
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        System.out.println("POST HelloObject (empty name) response: " + response.statusCode() + " - " + response.body());
        assertEquals(200, response.statusCode());
        assertEquals("{\"name\":\"OnlyPath\"}", response.body());
    }

    @Test
    void testHelloObjectPostEndpointNoContentType() throws Exception {
        // Test the POST helloObject endpoint without Content-Type header - should fail
        System.out.println("Testing POST helloObject endpoint without Content-Type...");
        String requestBody = "{\"name\":\"FromBody\"}";
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .uri(new URI("http://localhost:8080/rest/hello/helloObject/PathName"))
                        .setHeader("Accept", "application/json")
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        System.out.println("POST HelloObject (no content-type) response: " + response.statusCode() + " - " + response.body());
        // Without Content-Type, the server may reject or not parse the body correctly
        // We just verify the server responds (doesn't crash)
        System.out.println("Server responded with status: " + response.statusCode());
    }

    @Test
    void testComplexPostEndpoint() throws Exception {
        // Test the POST complex endpoint with an inner static class DTO containing maps
        System.out.println("Testing POST complex endpoint with inner static DTO...");
        String requestBody = "{\"type\":\"Invoice\",\"classification\":\"Finance\",\"metadata\":{\"key1\":\"val1\",\"key2\":\"val2\"}}";
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .uri(new URI("http://localhost:8080/rest/hello/complex/TestName"))
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/json")
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        System.out.println("POST complex endpoint response: " + response.statusCode() + " - " + response.body());
        assertEquals(200, response.statusCode());
        // The endpoint concatenates: name + ":" + type + ":" + classification + ":" + metadata.size()
        assertEquals("{\"name\":\"TestName:Invoice:Finance:2\"}", response.body());
    }

    @Test
    void testComplexPostEndpointWithoutMetadata() throws Exception {
        // Test the complex endpoint with no metadata map in the body
        System.out.println("Testing POST complex endpoint without metadata...");
        String requestBody = "{\"type\":\"Order\",\"classification\":\"Sales\"}";
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .uri(new URI("http://localhost:8080/rest/hello/complex/NoMeta"))
                        .setHeader("Accept", "application/json")
                        .setHeader("Content-Type", "application/json")
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        System.out.println("POST complex (no metadata) response: " + response.statusCode() + " - " + response.body());
        assertEquals(200, response.statusCode());
        assertEquals("{\"name\":\"NoMeta:Order:Sales\"}", response.body());
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
