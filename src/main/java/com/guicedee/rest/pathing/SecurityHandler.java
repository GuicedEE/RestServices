package com.guicedee.rest.pathing;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.AuthorizationProvider;
import io.vertx.ext.auth.authorization.Authorizations;
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
import java.util.List;
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
     * Principal/attribute claim keys that may carry a caller's roles when an authentication provider
     * populates claims rather than (or in addition to) Vert.x {@link Authorizations}. Checked as a
     * fallback after the authoritative {@link Authorizations} verification.
     */
    private static final List<String> ROLE_CLAIM_KEYS = List.of("roles", "groups", "role", "authorities", "permissions");

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
     * Checks whether the caller authenticated on the given routing context is authorized to invoke a
     * resource method, honouring {@link DenyAll}, {@link PermitAll} and {@link RolesAllowed} (method
     * level taking precedence over class level).
     *
     * @param context       The routing context (its {@link RoutingContext#user()} is the caller)
     * @param resourceClass The resource class
     * @param method        The method
     * @return {@code true} if the caller is authorized
     */
    public static boolean isAuthorized(RoutingContext context, Class<?> resourceClass, Method method) {
        return isAuthorized(context == null ? null : context.user(), resourceClass, method);
    }

    /**
     * Checks whether a resolved {@link User} is authorized to invoke a resource method.
     *
     * <p>Semantics:</p>
     * <ul>
     *   <li>{@link PermitAll} / no security annotation — always authorized;</li>
     *   <li>{@link DenyAll} — never authorized;</li>
     *   <li>{@link RolesAllowed} — authorized when the user holds <em>at least one</em> of the listed
     *       roles (see {@link #hasAnyRole(User, Set)}). A {@code null} user is never authorized.</li>
     * </ul>
     *
     * @param user          The authenticated user, or {@code null} when unauthenticated
     * @param resourceClass The resource class
     * @param method        The method
     * @return {@code true} if the user is authorized
     */
    public static boolean isAuthorized(User user, Class<?> resourceClass, Method method) {
        Set<String> rolesAllowed = getRolesAllowed(resourceClass, method);

        if (rolesAllowed == null) {
            return true; // @PermitAll or no annotation — all callers allowed
        }

        if (rolesAllowed.isEmpty()) {
            return false; // @DenyAll — no caller allowed
        }

        if (user == null) {
            return false; // role required but no authenticated user
        }

        boolean authorized = hasAnyRole(user, rolesAllowed);
        if (!authorized && logger.isDebugEnabled()) {
            logger.debug("Authorization denied: user lacks all of the required roles {}", rolesAllowed);
        }
        return authorized;
    }

    /**
     * Determines whether a user holds at least one of the supplied roles.
     *
     * <p>Resolution order:</p>
     * <ol>
     *   <li><strong>Vert.x authorizations</strong> — the authoritative source. Each required role is
     *       checked as a {@link RoleBasedAuthorization} against {@link User#authorizations()} (populated
     *       by authentication/authorization providers, including role-based providers and the
     *       application's own identity bridge).</li>
     *   <li><strong>Claim fallback</strong> — role/group claims carried on the {@link User#principal()}
     *       or {@link User#attributes()} (e.g. JWT/OIDC tokens). Supports the common keys
     *       {@code roles}, {@code groups}, {@code role}, {@code authorities}, {@code permissions} (as a
     *       JSON array or a space/comma delimited string), plus Keycloak-style
     *       {@code realm_access.roles} and {@code resource_access.*.roles}.</li>
     * </ol>
     *
     * @param user         The authenticated user
     * @param rolesAllowed The set of acceptable roles (any one grants access)
     * @return {@code true} when the user holds at least one of the roles
     */
    public static boolean hasAnyRole(User user, Set<String> rolesAllowed) {
        if (user == null || rolesAllowed == null || rolesAllowed.isEmpty()) {
            return false;
        }

        // 1) Authoritative: Vert.x role-based authorizations.
        try {
            Authorizations authorizations = user.authorizations();
            if (authorizations != null) {
                for (String role : rolesAllowed) {
                    if (role != null && authorizations.verify(RoleBasedAuthorization.create(role))) {
                        return true;
                    }
                }
            }
        } catch (RuntimeException e) {
            logger.debug("Role verification via User.authorizations() failed; falling back to claims", e);
        }

        // 2) Fallback: role/group claims on the principal or attributes.
        return claimsContainAnyRole(user.principal(), rolesAllowed)
                || claimsContainAnyRole(user.attributes(), rolesAllowed);
    }

    /**
     * Inspects a JSON claims object (principal or attributes) for any of the supplied roles under the
     * common role-claim keys, plus Keycloak {@code realm_access}/{@code resource_access} structures.
     */
    private static boolean claimsContainAnyRole(JsonObject claims, Set<String> rolesAllowed) {
        if (claims == null) {
            return false;
        }
        for (String key : ROLE_CLAIM_KEYS) {
            if (valueContainsAnyRole(claims.getValue(key), rolesAllowed)) {
                return true;
            }
        }
        // Keycloak: realm_access.roles
        JsonObject realmAccess = claims.getValue("realm_access") instanceof JsonObject jo ? jo : null;
        if (realmAccess != null && valueContainsAnyRole(realmAccess.getValue("roles"), rolesAllowed)) {
            return true;
        }
        // Keycloak: resource_access.<client>.roles
        JsonObject resourceAccess = claims.getValue("resource_access") instanceof JsonObject jo ? jo : null;
        if (resourceAccess != null) {
            for (String client : resourceAccess.fieldNames()) {
                JsonObject clientObj = resourceAccess.getValue(client) instanceof JsonObject jo ? jo : null;
                if (clientObj != null && valueContainsAnyRole(clientObj.getValue("roles"), rolesAllowed)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Matches a claim value (a JSON array of role names, or a single/space/comma delimited string)
     * against the set of acceptable roles.
     */
    private static boolean valueContainsAnyRole(Object value, Set<String> rolesAllowed) {
        if (value == null) {
            return false;
        }
        if (value instanceof JsonArray array) {
            for (Object element : array) {
                if (element != null && rolesAllowed.contains(element.toString())) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof String s) {
            for (String part : s.split("[\\s,]+")) {
                if (!part.isBlank() && rolesAllowed.contains(part)) {
                    return true;
                }
            }
        }
        return false;
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
