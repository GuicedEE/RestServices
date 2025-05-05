package com.guicedee.guicedservlets.rest.test;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@ApplicationPath("rest")
@Path("secured")
@Produces(MediaType.APPLICATION_JSON)
public class SecuredResource {

    @GET
    @Path("public")
    @PermitAll
    public String publicEndpoint() {
        return "This is a public endpoint that anyone can access";
    }

    @GET
    @Path("admin")
    @RolesAllowed("admin")
    public String adminEndpoint() {
        return "This is an admin endpoint that only users with the 'admin' role can access";
    }

    @GET
    @Path("user")
    @RolesAllowed("user")
    public String userEndpoint() {
        return "This is a user endpoint that only users with the 'user' role can access";
    }

    @GET
    @Path("multi-role")
    @RolesAllowed({"admin", "user"})
    public String multiRoleEndpoint() {
        return "This is an endpoint that users with either 'admin' or 'user' role can access";
    }

    @GET
    @Path("denied")
    @DenyAll
    public String deniedEndpoint() {
        return "This endpoint is denied to all users";
    }
}
