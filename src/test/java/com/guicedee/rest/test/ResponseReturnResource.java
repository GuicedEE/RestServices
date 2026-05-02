package com.guicedee.rest.test;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Resource that returns {@link Response} and {@link Uni}&lt;Response&gt; directly.
 * <p>
 * These return types must be handled by our ResponseHandler without triggering
 * a JAX-RS RuntimeDelegate lookup (which would fail since we have no full JAX-RS impl).
 */
@ApplicationPath("rest")
@Path("response-test")
@Produces(MediaType.APPLICATION_JSON)
public class ResponseReturnResource {

    @GET
    @Path("direct")
    public Response directResponse() {
        return Response.ok(new ReturnableObject().setName("direct")).build();
    }

    @GET
    @Path("direct-status")
    public Response directResponseWithStatus() {
        return Response.status(201)
                .entity(new ReturnableObject().setName("created"))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    @GET
    @Path("direct-no-entity")
    public Response directResponseNoEntity() {
        return Response.noContent().build();
    }

    @GET
    @Path("uni")
    public Uni<Response> uniResponse() {
        return Uni.createFrom().item(
                Response.ok(new ReturnableObject().setName("uni-response")).build()
        );
    }

    @GET
    @Path("uni-status")
    public Uni<Response> uniResponseWithStatus() {
        return Uni.createFrom().item(
                Response.status(202)
                        .entity(new ReturnableObject().setName("accepted"))
                        .type(MediaType.APPLICATION_JSON)
                        .build()
        );
    }

    @GET
    @Path("uni-no-entity")
    public Uni<Response> uniResponseNoEntity() {
        return Uni.createFrom().item(Response.noContent().build());
    }

    @GET
    @Path("direct-header")
    public Response directResponseWithCustomHeader() {
        return Response.ok(new ReturnableObject().setName("with-header"))
                .header("X-Custom-Header", "test-value")
                .build();
    }

    @GET
    @Path("uni-header")
    public Uni<Response> uniResponseWithCustomHeader() {
        return Uni.createFrom().item(
                Response.ok(new ReturnableObject().setName("uni-with-header"))
                        .header("X-Custom-Header", "uni-test-value")
                        .build()
        );
    }
}

