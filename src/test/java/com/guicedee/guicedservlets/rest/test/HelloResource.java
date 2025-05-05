package com.guicedee.guicedservlets.rest.test;

import com.google.inject.Inject;
import jakarta.ws.rs.*;

@ApplicationPath("rest")
@Path("hello")
@Produces("application/json")
public class HelloResource
{
	//or in constructor
	@Inject
	private Greeter greeter;

	@GET
	@Path("{name}")
	public String hello(@PathParam("name") final String name) {
		return greeter.greet(name);
	}

	@GET
	@Path("helloObject/{name}")
	public ReturnableObject helloObject(@PathParam("name") final String name) {
		return new ReturnableObject().setName(name);
	}

}
