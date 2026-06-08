package com.guicedee.rest.test;
import com.guicedee.rest.pathing.SecurityHandler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
/**
 * Verifies the production role-verification behaviour of SecurityHandler - that @RolesAllowed is
 * genuinely enforced against a caller's Vert.x authorizations and role/group claims, that @DenyAll
 * blocks everyone, and that @PermitAll/unannotated methods are open.
 */
public class RoleAuthorizationTest {
    private static Method method(String name) throws NoSuchMethodException {
        return SecuredResource.class.getMethod(name);
    }
    private static User userWithAuthorizationRole(String username, String role) {
        User user = User.create(new JsonObject().put("username", username));
        Set<Authorization> roles = Set.of(RoleBasedAuthorization.create(role));
        user.authorizations().put("test-provider", roles);
        return user;
    }
    private static User userWithRoleClaim(String username, String... roles) {
        JsonArray rolesArray = new JsonArray();
        for (String r : roles) {
            rolesArray.add(r);
        }
        return User.create(new JsonObject().put("username", username).put("roles", rolesArray));
    }
    @Test
    void adminRoleViaAuthorizationIsAllowedOnAdminEndpoint() throws Exception {
        User admin = userWithAuthorizationRole("alice", "admin");
        assertTrue(SecurityHandler.isAuthorized(admin, SecuredResource.class, method("adminEndpoint")),
                "a user holding the admin RoleBasedAuthorization must be authorized for the admin endpoint");
        assertTrue(SecurityHandler.hasAnyRole(admin, Set.of("admin")),
                "hasAnyRole must confirm the admin role");
    }
    @Test
    void wrongRoleIsDeniedOnAdminEndpoint() throws Exception {
        User user = userWithAuthorizationRole("bob", "user");
        assertFalse(SecurityHandler.isAuthorized(user, SecuredResource.class, method("adminEndpoint")),
                "a user holding only the user role must NOT be authorized for the admin endpoint");
        assertTrue(SecurityHandler.isAuthorized(user, SecuredResource.class, method("multiRoleEndpoint")),
                "the user role must be authorized for the admin-or-user endpoint");
    }
    @Test
    void nullUserIsDeniedOnSecuredEndpointButAllowedOnPublic() throws Exception {
        assertFalse(SecurityHandler.isAuthorized((User) null, SecuredResource.class, method("adminEndpoint")),
                "an unauthenticated caller must be denied a @RolesAllowed endpoint");
        assertTrue(SecurityHandler.isAuthorized((User) null, SecuredResource.class, method("publicEndpoint")),
                "@PermitAll endpoints must be open to unauthenticated callers");
    }
    @Test
    void denyAllBlocksEvenAdmins() throws Exception {
        User admin = userWithAuthorizationRole("alice", "admin");
        assertFalse(SecurityHandler.isAuthorized(admin, SecuredResource.class, method("deniedEndpoint")),
                "@DenyAll must block every caller, including admins");
    }
    @Test
    void roleClaimFallbackIsHonoured() throws Exception {
        User claimAdmin = userWithRoleClaim("carol", "admin");
        assertTrue(SecurityHandler.isAuthorized(claimAdmin, SecuredResource.class, method("adminEndpoint")),
                "a user with an admin role claim must be authorized via the claim fallback");
        User claimUser = userWithRoleClaim("dave", "user");
        assertFalse(SecurityHandler.isAuthorized(claimUser, SecuredResource.class, method("adminEndpoint")),
                "a user whose only claim is user must be denied the admin endpoint");
    }
    @Test
    void hasAnyRoleEdgeCases() {
        assertFalse(SecurityHandler.hasAnyRole(null, Set.of("admin")), "null user has no roles");
        User admin = userWithAuthorizationRole("alice", "admin");
        assertFalse(SecurityHandler.hasAnyRole(admin, Set.of()), "empty role set can never match");
        assertFalse(SecurityHandler.hasAnyRole(admin, Set.of("superuser")), "a role the user lacks must not match");
    }
}
