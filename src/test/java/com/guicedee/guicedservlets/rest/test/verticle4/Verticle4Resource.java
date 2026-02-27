package com.guicedee.guicedservlets.rest.test.verticle4;

import com.google.inject.Inject;
import com.guicedee.guicedservlets.rest.test.Greeter;
import com.guicedee.guicedservlets.rest.test.RequestObject;
import com.guicedee.guicedservlets.rest.test.ReturnableObject;
import jakarta.ws.rs.*;

@ApplicationPath("rest")
@Path("verticle4")
@Produces("application/json")
public class Verticle4Resource {

    @Inject
    private Greeter greeter;

    @GET
    @Path("{name}")
    public String hello(@PathParam("name") final String name) {
        return greeter.greet("V4:" + name);
    }

    @GET
    @Path("object/{name}")
    public ReturnableObject helloObject(@PathParam("name") final String name) {
        return new ReturnableObject().setName("V4:" + name);
    }

    @POST
    @Path("object/{name}")
    public ReturnableObject postObject(@PathParam("name") final String name, RequestObject requestObject) {
        return new ReturnableObject().setName("V4:" + name + ":" + requestObject.getName());
    }
}

