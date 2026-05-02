package com.guicedee.rest.test;

import com.guicedee.client.IGuiceContext;
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
 * Integration test that verifies REST endpoints properly enter and exit
 * the GuicedEE CallScope, ensuring CallScopeProperties (source, etc.)
 * are populated during request handling.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CallScopeIntegrationTest {

    private HttpClient client;

    @BeforeAll
    void setUp() throws Exception {
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
    void restEndpoint_hasActiveCallScope() throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI("http://localhost:8080/rest/callscope/check"))
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Expected 200 OK");
        String body = response.body().replace("\"", ""); // strip JSON quotes
        System.out.println("Call scope check response: " + body);
        assertTrue(body.contains("scopeActive=true"), "Call scope should be active during REST request, got: " + body);
        assertTrue(body.contains("source=Rest"), "Call scope source should be Rest, got: " + body);
    }

    @Test
    void restEndpoint_sourceIsRest() throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI("http://localhost:8080/rest/callscope/source"))
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Expected 200 OK");
        String body = response.body().replace("\"", "");
        System.out.println("Call scope source response: " + body);
        assertEquals("Rest", body, "Call scope source should be Rest");
    }

    @Test
    void multipleRequests_eachGetsFreshScope() throws Exception {
        // Make multiple requests and verify each gets a proper scope
        for (int i = 0; i < 5; i++) {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                            .GET()
                            .uri(new URI("http://localhost:8080/rest/callscope/check"))
                            .timeout(Duration.of(5, ChronoUnit.SECONDS))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode(), "Request " + i + " should succeed");
            String body = response.body().replace("\"", "");
            assertTrue(body.contains("scopeActive=true"),
                    "Request " + i + ": Call scope should be active, got: " + body);
        }
    }
}

