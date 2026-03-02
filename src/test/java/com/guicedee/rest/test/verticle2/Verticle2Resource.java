package com.guicedee.rest.test.verticle2;

import com.google.inject.Inject;
import com.guicedee.rest.test.Greeter;
import com.guicedee.rest.test.RequestObject;
import com.guicedee.rest.test.ReturnableObject;
import jakarta.ws.rs.*;

@ApplicationPath("rest")
@Path("verticle2")
@Produces("application/json")
public class Verticle2Resource {

    @Inject
    private Greeter greeter;

    @GET
    @Path("{name}")
    public String hello(@PathParam("name") final String name) {
        return greeter.greet("V2:" + name);
    }

    @GET
    @Path("object/{name}")
    public ReturnableObject helloObject(@PathParam("name") final String name) {
        return new ReturnableObject().setName("V2:" + name);
    }

    @POST
    @Path("object/{name}")
    public ReturnableObject postObject(@PathParam("name") final String name, RequestObject requestObject) {
        return new ReturnableObject().setName("V2:" + name + ":" + requestObject.getName());
    }
}

