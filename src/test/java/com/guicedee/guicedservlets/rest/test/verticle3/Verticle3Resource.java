package com.guicedee.guicedservlets.rest.test.verticle3;

import com.google.inject.Inject;
import com.guicedee.guicedservlets.rest.test.Greeter;
import com.guicedee.guicedservlets.rest.test.RequestObject;
import com.guicedee.guicedservlets.rest.test.ReturnableObject;
import jakarta.ws.rs.*;

@ApplicationPath("rest")
@Path("verticle3")
@Produces("application/json")
public class Verticle3Resource {

    @Inject
    private Greeter greeter;

    @GET
    @Path("{name}")
    public String hello(@PathParam("name") final String name) {
        return greeter.greet("V3:" + name);
    }

    @GET
    @Path("object/{name}")
    public ReturnableObject helloObject(@PathParam("name") final String name) {
        return new ReturnableObject().setName("V3:" + name);
    }

    @POST
    @Path("object/{name}")
    public ReturnableObject postObject(@PathParam("name") final String name, RequestObject requestObject) {
        return new ReturnableObject().setName("V3:" + name + ":" + requestObject.getName());
    }
}

