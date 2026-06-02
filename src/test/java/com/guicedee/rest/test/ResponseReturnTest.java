package com.guicedee.rest.test;

import com.guicedee.client.IGuiceContext;
import com.guicedee.client.utils.LogUtils;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that {@code Response} and {@code Uni<Response>} return types are handled
 * directly by our ResponseHandler without triggering a JAX-RS RuntimeDelegate lookup.
 * <p>
 * If RuntimeDelegate were invoked, these tests would fail with a
 * {@link LinkageError} or {@link IllegalStateException} since no full JAX-RS
 * implementation is on the classpath.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ResponseReturnTest {

    private HttpClient client;

    @BeforeAll
    void setUp() throws Exception {
        LogUtils.addConsoleLogger();
        IGuiceContext.instance().inject();
        TestServerReady.waitForServer();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.of(5, ChronoUnit.SECONDS))
                .build();
    }

    @AfterAll
    void tearDown() {
        IGuiceContext.get(Vertx.class).close();
    }

    @Test
    void testDirectResponse() throws Exception {
        HttpResponse<String> response = get("/rest/response-test/direct");
        assertEquals(200, response.statusCode());
        assertEquals("{\"name\":\"direct\"}", response.body());
    }

    @Test
    void testDirectResponseWithStatus() throws Exception {
        HttpResponse<String> response = get("/rest/response-test/direct-status");
        assertEquals(201, response.statusCode());
        assertEquals("{\"name\":\"created\"}", response.body());
    }

    @Test
    void testDirectResponseNoEntity() throws Exception {
        HttpResponse<String> response = get("/rest/response-test/direct-no-entity");
        assertEquals(204, response.statusCode());
        assertTrue(response.body() == null || response.body().isEmpty());
    }

    @Test
    void testUniResponse() throws Exception {
        HttpResponse<String> response = get("/rest/response-test/uni");
        assertEquals(200, response.statusCode());
        assertEquals("{\"name\":\"uni-response\"}", response.body());
    }

    @Test
    void testUniResponseWithStatus() throws Exception {
        HttpResponse<String> response = get("/rest/response-test/uni-status");
        assertEquals(202, response.statusCode());
        assertEquals("{\"name\":\"accepted\"}", response.body());
    }

    @Test
    void testUniResponseNoEntity() throws Exception {
        HttpResponse<String> response = get("/rest/response-test/uni-no-entity");
        assertEquals(204, response.statusCode());
        assertTrue(response.body() == null || response.body().isEmpty());
    }

    @Test
    void testDirectResponseWithCustomHeader() throws Exception {
        HttpResponse<String> response = get("/rest/response-test/direct-header");
        assertEquals(200, response.statusCode());
        assertEquals("{\"name\":\"with-header\"}", response.body());
        assertEquals("test-value", response.headers().firstValue("X-Custom-Header").orElse(null));
    }

    @Test
    void testUniResponseWithCustomHeader() throws Exception {
        HttpResponse<String> response = get("/rest/response-test/uni-header");
        assertEquals(200, response.statusCode());
        assertEquals("{\"name\":\"uni-with-header\"}", response.body());
        assertEquals("uni-test-value", response.headers().firstValue("X-Custom-Header").orElse(null));
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI("http://localhost:8080" + path))
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }
}

