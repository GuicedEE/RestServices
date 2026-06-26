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
 * End-to-end verification that DTO fields with digit-suffixed names
 * ({@code image1}, {@code image2}, {@code image3}) render correctly through the
 * REST service serialization path ({@code ResponseHandler} → GuicedEE Jackson 3 mapper).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NumberedFieldRestTest
{
    private HttpClient client;

    @BeforeAll
    void setUp() throws Exception
    {
        LogUtils.addConsoleLogger();
        IGuiceContext.instance().inject();
        TestServerReady.waitForServer();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.of(5, ChronoUnit.SECONDS))
                .build();
    }

    @AfterAll
    void tearDown()
    {
        IGuiceContext.get(Vertx.class).close();
    }

    /**
     * All populated numbered fields must appear in the REST response body.
     */
    @Test
    void numberedFieldsRenderOnRestService() throws Exception
    {
        HttpResponse<String> response = get("/rest/numbered/gallery");
        assertEquals(200, response.statusCode());

        String body = response.body();
        assertAll(
                () -> assertTrue(body.contains("\"image1\""), () -> "image1 missing in REST body: " + body),
                () -> assertTrue(body.contains("\"image2\""), () -> "image2 missing in REST body: " + body),
                () -> assertTrue(body.contains("\"image3\""), () -> "image3 missing in REST body: " + body),
                () -> assertTrue(body.contains("a.png"), () -> "image1 url missing: " + body),
                () -> assertTrue(body.contains("b.png"), () -> "image2 url missing: " + body),
                () -> assertTrue(body.contains("c.png"), () -> "image3 url missing: " + body)
        );
    }

    /**
     * Null numbered fields are omitted by {@code NON_EMPTY} inclusion — the populated
     * one still renders. This is the expected cause of "missing" fields when values are null.
     */
    @Test
    void nullNumberedFieldsAreOmittedOnRestService() throws Exception
    {
        HttpResponse<String> response = get("/rest/numbered/gallery-partial");
        assertEquals(200, response.statusCode());

        String body = response.body();
        assertTrue(body.contains("\"image1\""), () -> "image1 should render: " + body);
        assertFalse(body.contains("\"image2\""), () -> "null image2 should be omitted: " + body);
        assertFalse(body.contains("\"image3\""), () -> "null image3 should be omitted: " + body);
    }

    private HttpResponse<String> get(String path) throws Exception
    {
        return client.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI("http://localhost:8080" + path))
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }
}

