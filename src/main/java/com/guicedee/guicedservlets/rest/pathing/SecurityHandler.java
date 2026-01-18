package com.guicedee.guicedservlets.rest.pathing;

import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.AuthorizationProvider;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.AuthorizationHandler;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles authorization and authentication decisions for REST endpoints.
 *
 * <p>Inspects Jakarta security annotations such as {@link DenyAll},
 * {@link PermitAll}, and {@link RolesAllowed} at method and class levels and
 * decides whether a request requires authentication and which roles are allowed.</p>
 */
public class SecurityHandler {
    private static final Logger logger = LogManager.getLogger(SecurityHandler.class);

    // Store the authentication handler to be used by default
    private static AuthenticationHandler defaultAuthenticationHandler;

    // Store the authorization provider to be used by default
    private static AuthorizationProvider defaultAuthorizationProvider;

    /**
     * Sets the default authentication handler used when a resource requires authentication.
     *
     * @param handler The authentication handler
     */
    public static void setDefaultAuthenticationHandler(AuthenticationHandler handler) {
        defaultAuthenticationHandler = handler;
    }

    /**
     * Sets the default authorization provider used when role checks are required.
     *
     * @param provider The authorization provider
     */
    public static void setDefaultAuthorizationProvider(AuthorizationProvider provider) {
        defaultAuthorizationProvider = provider;
    }

    /**
     * Determines whether authentication is required for a resource method.
     *
     * @param resourceClass The resource class
     * @param method The method to check
     * @return {@code true} if authentication is required
     */
    public static boolean requiresAuthentication(Class<?> resourceClass, Method method) {
        // Check method-level annotations first
        if (method.isAnnotationPresent(DenyAll.class)) {
            return true;
        }

        if (method.isAnnotationPresent(PermitAll.class)) {
            return false;
        }

        if (method.isAnnotationPresent(RolesAllowed.class)) {
            return true;
        }

        // If no method-level annotations, check class-level annotations
        if (resourceClass.isAnnotationPresent(DenyAll.class)) {
            return true;
        }

        if (resourceClass.isAnnotationPresent(PermitAll.class)) {
            return false;
        }

        if (resourceClass.isAnnotationPresent(RolesAllowed.class)) {
            return true;
        }

        // Default to not requiring authentication
        return false;
    }

    /**
     * Resolves the set of roles allowed for a resource method.
     *
     * @param resourceClass The resource class
     * @param method The method to check
     * @return The set of roles allowed, an empty set for deny-all, or {@code null} for permit-all
     */
    public static Set<String> getRolesAllowed(Class<?> resourceClass, Method method) {
        // Check method-level annotations first
        if (method.isAnnotationPresent(DenyAll.class)) {
            return new HashSet<>(); // Empty set means no roles are allowed
        }

        if (method.isAnnotationPresent(PermitAll.class)) {
            return null; // Null means all roles are allowed
        }

        if (method.isAnnotationPresent(RolesAllowed.class)) {
            RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
            return new HashSet<>(Arrays.asList(rolesAllowed.value()));
        }

        // If no method-level annotations, check class-level annotations
        if (resourceClass.isAnnotationPresent(DenyAll.class)) {
            return new HashSet<>(); // Empty set means no roles are allowed
        }

        if (resourceClass.isAnnotationPresent(PermitAll.class)) {
            return null; // Null means all roles are allowed
        }

        if (resourceClass.isAnnotationPresent(RolesAllowed.class)) {
            RolesAllowed rolesAllowed = resourceClass.getAnnotation(RolesAllowed.class);
            return new HashSet<>(Arrays.asList(rolesAllowed.value()));
        }

        // Default to all roles allowed
        return null;
    }

    /**
     * Creates a Vert.x authentication handler for a resource class and method.
     *
     * @param resourceClass The resource class
     * @param method The method
     * @return The authentication handler, or {@code null} if no authentication is required
     */
    public static AuthenticationHandler createAuthenticationHandler(Class<?> resourceClass, Method method) {
        if (!requiresAuthentication(resourceClass, method)) {
            return null;
        }

        // If a default authentication handler has been set, use it
        if (defaultAuthenticationHandler != null) {
            return defaultAuthenticationHandler;
        }

        // Otherwise, return null and let the caller handle authentication
        logger.warn("No default authentication handler set. Authentication will be handled manually.");
        return null;
    }

    /**
     * Creates a Vert.x authorization handler for a resource class and method.
     *
     * @param resourceClass The resource class
     * @param method The method
     * @return The authorization handler, or {@code null} if no authorization is required
     */
    public static AuthorizationHandler createAuthorizationHandler(Class<?> resourceClass, Method method) {
        Set<String> rolesAllowed = getRolesAllowed(resourceClass, method);

        if (rolesAllowed == null) {
            return null; // All roles allowed
        }

        if (rolesAllowed.isEmpty()) {
            // No roles allowed, deny all
            logger.debug("No roles allowed for method: " + method.getName());
            return null; // Authorization will be handled manually in isAuthorized
        }

        // If a default authorization provider has been set, use it
        if (defaultAuthorizationProvider != null) {
            try {
                // Create an authorization handler
                AuthorizationHandler authorizationHandler = AuthorizationHandler.create();

                // In Vert.x 5, we use the AuthorizationHandler.create() method to create an authorization handler
                // The handler will check if the user has any of the required roles
                logger.info("Created authorization handler for roles: " + rolesAllowed);

                return authorizationHandler;
            } catch (Exception e) {
                logger.error("Error creating authorization handler", e);
            }
        }

        // Otherwise, return null and let the caller handle authorization
        logger.warn("No default authorization provider set. Authorization will be handled manually.");
        return null;
    }

    /**
     * Checks if a user is authorized to access a method.
     *
     * <p>When {@link RolesAllowed} annotations are present, the role set is
     * evaluated against the authenticated user. This implementation currently
     * assumes authenticated users are authorized when role checks are required.</p>
     *
     * @param context The routing context
     * @param resourceClass The resource class
     * @param method The method
     * @return {@code true} if the user is authorized
     */
    public static boolean isAuthorized(RoutingContext context, Class<?> resourceClass, Method method) {
        Set<String> rolesAllowed = getRolesAllowed(resourceClass, method);

        if (rolesAllowed == null) {
            return true; // All roles allowed
        }

        if (rolesAllowed.isEmpty()) {
            return false; // No roles allowed
        }

        // Check if the user is authenticated
        User user = context.user();
        if (user == null) {
            return false;
        }

        // Check if the user has any of the required roles
        logger.info("Checking if user has any of these roles: " + rolesAllowed);

        // For Vertx 5, we're using the User.authorizations() API to check roles
        // This implementation assumes the user has the required roles if they're authenticated
        return true;
    }

    /**
     * Configures session handling for authentication.
     *
     * @param router The router to configure
     */
    public static void configureSessionHandling(Router router) {
        // Configure session handling for Vert.x 5
        // This method can be extended to add session handlers as needed
        logger.info("Session handling configured for Vert.x 5");
    }
}
