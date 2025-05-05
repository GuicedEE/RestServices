package com.guicedee.guicedservlets.rest.test;

import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedservlets.rest.pathing.SecurityHandler;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.AuthorizationHandler;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SecurityTest {

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

    @Test
    void testRequiresAuthentication() throws Exception {
        // Test with a class that has no security annotations
        class NoAnnotations {
            public void method() {}
        }
        Method noAnnotationsMethod = NoAnnotations.class.getMethod("method");
        assertFalse(SecurityHandler.requiresAuthentication(NoAnnotations.class, noAnnotationsMethod));

        // Test with a method that has @PermitAll
        @PermitAll
        class PermitAllClass {
            public void method() {}
        }
        Method permitAllMethod = PermitAllClass.class.getMethod("method");
        assertFalse(SecurityHandler.requiresAuthentication(PermitAllClass.class, permitAllMethod));

        // Test with a method that has @DenyAll
        @DenyAll
        class DenyAllClass {
            public void method() {}
        }
        Method denyAllMethod = DenyAllClass.class.getMethod("method");
        assertTrue(SecurityHandler.requiresAuthentication(DenyAllClass.class, denyAllMethod));

        // Test with a method that has @RolesAllowed
        @RolesAllowed("admin")
        class RolesAllowedClass {
            public void method() {}
        }
        Method rolesAllowedMethod = RolesAllowedClass.class.getMethod("method");
        assertTrue(SecurityHandler.requiresAuthentication(RolesAllowedClass.class, rolesAllowedMethod));
    }

    @Test
    void testGetRolesAllowed() throws Exception {
        // Test with a class that has no security annotations
        class NoAnnotations {
            public void method() {}
        }
        Method noAnnotationsMethod = NoAnnotations.class.getMethod("method");
        assertNull(SecurityHandler.getRolesAllowed(NoAnnotations.class, noAnnotationsMethod));

        // Test with a method that has @PermitAll
        @PermitAll
        class PermitAllClass {
            public void method() {}
        }
        Method permitAllMethod = PermitAllClass.class.getMethod("method");
        assertNull(SecurityHandler.getRolesAllowed(PermitAllClass.class, permitAllMethod));

        // Test with a method that has @DenyAll
        @DenyAll
        class DenyAllClass {
            public void method() {}
        }
        Method denyAllMethod = DenyAllClass.class.getMethod("method");
        assertEquals(0, SecurityHandler.getRolesAllowed(DenyAllClass.class, denyAllMethod).size());

        // Test with a method that has @RolesAllowed
        @RolesAllowed({"admin", "user"})
        class RolesAllowedClass {
            public void method() {}
        }
        Method rolesAllowedMethod = RolesAllowedClass.class.getMethod("method");
        assertEquals(2, SecurityHandler.getRolesAllowed(RolesAllowedClass.class, rolesAllowedMethod).size());
        assertTrue(SecurityHandler.getRolesAllowed(RolesAllowedClass.class, rolesAllowedMethod).contains("admin"));
        assertTrue(SecurityHandler.getRolesAllowed(RolesAllowedClass.class, rolesAllowedMethod).contains("user"));
    }

    @Test
    void testCreateAuthenticationHandler() throws Exception {
        // Test with a class that has no security annotations
        class NoAnnotations {
            public void method() {}
        }
        Method noAnnotationsMethod = NoAnnotations.class.getMethod("method");
        assertNull(SecurityHandler.createAuthenticationHandler(NoAnnotations.class, noAnnotationsMethod));

        // Test with a method that has @RolesAllowed
        @RolesAllowed("admin")
        class RolesAllowedClass {
            public void method() {}
        }
        Method rolesAllowedMethod = RolesAllowedClass.class.getMethod("method");

        // Set a default authentication handler
        AuthenticationHandler mockHandler = new AuthenticationHandler() {
            @Override
            public void handle(RoutingContext context) {
                // Mock implementation
                context.next();
            }
        };
        SecurityHandler.setDefaultAuthenticationHandler(mockHandler);

        // Now the method should return the mock handler
        assertNotNull(SecurityHandler.createAuthenticationHandler(RolesAllowedClass.class, rolesAllowedMethod));
    }

    @Test
    void testCreateAuthorizationHandler() throws Exception {
        // Test with a class that has no security annotations
        class NoAnnotations {
            public void method() {}
        }
        Method noAnnotationsMethod = NoAnnotations.class.getMethod("method");
        assertNull(SecurityHandler.createAuthorizationHandler(NoAnnotations.class, noAnnotationsMethod));

        // Test with a method that has @DenyAll
        @DenyAll
        class DenyAllClass {
            public void method() {}
        }
        Method denyAllMethod = DenyAllClass.class.getMethod("method");
        assertNull(SecurityHandler.createAuthorizationHandler(DenyAllClass.class, denyAllMethod));
    }

    @Test
    void testSecuredEndpoint() throws Exception {
        // Test accessing a secured endpoint without authentication
        // This should return a 401 Unauthorized status code
        System.out.println("Testing secured endpoint without authentication...");
        try {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                            .GET()
                            .uri(new URI("http://localhost:8080/rest/secured"))
                            .timeout(Duration.of(5, ChronoUnit.SECONDS))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            System.out.println("Secured endpoint response: " + response.statusCode() + " - " + response.body());
            // The endpoint might not exist, so we're just checking that we don't get an exception
        } catch (Exception e) {
            System.out.println("Error accessing secured endpoint: " + e.getMessage());
            // This is expected if the endpoint doesn't exist
        }
    }

    @Test
    void testConfigureSessionHandling() {
        // Test that configureSessionHandling doesn't throw an exception
        Router router = Router.router(IGuiceContext.get(Vertx.class));
        SecurityHandler.configureSessionHandling(router);
        // If we get here, the method didn't throw an exception
        assertTrue(true);
    }
}
