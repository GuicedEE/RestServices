package com.guicedee.rest.test;

import jakarta.ws.rs.*;
import java.util.concurrent.atomic.AtomicInteger;

@Path("/consumes-regression")
@Consumes("application/json")
@Produces("text/plain")
public class ConsumesResource {
    public static final AtomicInteger calls = new AtomicInteger();
    private String invoked() { calls.incrementAndGet(); return "invoked"; }
    @POST @Path("inherited") public String inherited(byte[] body, @QueryParam("number") int number) { return invoked(); }
    @POST @Path("override") @Consumes("text/plain") public String overridden(byte[] body) { return invoked(); }
    @POST @Path("wildcard") @Consumes("application/*") public String wildcard(byte[] body) { return invoked(); }
    @POST @Path("multiple") @Consumes({"text/plain, application/xml", "image/png"}) public String multiple(byte[] body) { return invoked(); }
    @POST @Path("octet") @Consumes("application/octet-stream") public String octet(byte[] body) { return invoked(); }
    @POST @Path("any") @Consumes("*/*") public String any(byte[] body) { return invoked(); }
    @GET @Path("empty") public String empty() { return invoked(); }
}
