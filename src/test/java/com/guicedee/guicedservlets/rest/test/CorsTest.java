package com.guicedee.guicedservlets.rest.test;

import com.guicedee.guicedservlets.rest.services.Cors;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CORS annotation functionality.
 */
@Cors(
    allowedOrigins = {"http://localhost:8080", "https://example.com"},
    allowedMethods = {"GET", "POST", "PUT"},
    allowedHeaders = {"X-Custom-Header", "Content-Type"},
    allowCredentials = true,
    maxAgeSeconds = 7200,
    enabled = true
)
public class CorsTest extends AbstractVerticle {

    @Test
    public void testCorsAnnotation() {
        // Verify that the CORS annotation is present on the class
        Cors corsAnnotation = CorsTest.class.getAnnotation(Cors.class);
        assertNotNull(corsAnnotation, "CORS annotation should be present");
        
        // Verify the annotation values
        assertArrayEquals(new String[]{"http://localhost:8080", "https://example.com"}, corsAnnotation.allowedOrigins());
        assertArrayEquals(new String[]{"GET", "POST", "PUT"}, corsAnnotation.allowedMethods());
        assertArrayEquals(new String[]{"X-Custom-Header", "Content-Type"}, corsAnnotation.allowedHeaders());
        assertTrue(corsAnnotation.allowCredentials());
        assertEquals(7200, corsAnnotation.maxAgeSeconds());
        assertTrue(corsAnnotation.enabled());
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        // This method is required by AbstractVerticle but not used in the test
        startPromise.complete();
    }
}