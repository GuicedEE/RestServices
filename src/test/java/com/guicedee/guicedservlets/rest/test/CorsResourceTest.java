package com.guicedee.guicedservlets.rest.test;

import com.guicedee.guicedservlets.rest.services.Cors;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CORS annotation functionality on resource classes and methods.
 */
public class CorsResourceTest {

    @Test
    public void testCorsAnnotationsOnResourceClasses() {
        // Verify that the CORS annotation is present on the TestResource class
        Cors corsAnnotation = TestResource.class.getAnnotation(Cors.class);
        assertNotNull(corsAnnotation, "CORS annotation should be present on TestResource");
        
        // Verify the annotation values
        assertArrayEquals(new String[]{"https://test.example.com"}, corsAnnotation.allowedOrigins());
        assertArrayEquals(new String[]{"GET", "POST"}, corsAnnotation.allowedMethods());
        
        // Verify that the CORS annotation is present on the getWithCors method
        try {
            Cors methodAnnotation = TestResource.class.getMethod("getWithCors").getAnnotation(Cors.class);
            assertNotNull(methodAnnotation, "CORS annotation should be present on getWithCors method");
            
            // Verify the annotation values
            assertArrayEquals(new String[]{"https://method.example.com"}, methodAnnotation.allowedOrigins());
            assertArrayEquals(new String[]{"GET"}, methodAnnotation.allowedMethods());
        } catch (NoSuchMethodException e) {
            fail("Method getWithCors not found", e);
        }
        
        // Verify that the ApplicationPath and Path annotations are present
        ApplicationPath appPath = TestResource.class.getAnnotation(ApplicationPath.class);
        assertNotNull(appPath, "ApplicationPath annotation should be present");
        assertEquals("api", appPath.value());
        
        Path path = TestResource.class.getAnnotation(Path.class);
        assertNotNull(path, "Path annotation should be present");
        assertEquals("test", path.value());
    }
    
    /**
     * Test resource class with CORS annotation.
     */
    @ApplicationPath("api")
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    @Cors(
        allowedOrigins = {"https://test.example.com"},
        allowedMethods = {"GET", "POST"},
        allowedHeaders = {"X-Test-Header", "Content-Type"},
        allowCredentials = true,
        maxAgeSeconds = 3600,
        enabled = true
    )
    public static class TestResource {
        
        @GET
        @Path("simple")
        public String getSimple() {
            return "Simple response";
        }
        
        @GET
        @Path("with-cors")
        @Cors(
            allowedOrigins = {"https://method.example.com"},
            allowedMethods = {"GET"},
            allowedHeaders = {"X-Method-Header", "Content-Type"},
            allowCredentials = false,
            maxAgeSeconds = 1800,
            enabled = true
        )
        public String getWithCors() {
            return "Response with CORS";
        }
        
        @POST
        @Path("post")
        public String post(String body) {
            return "Posted: " + body;
        }
    }
}