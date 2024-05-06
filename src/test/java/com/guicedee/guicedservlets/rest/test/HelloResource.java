package com.guicedee.guicedservlets.rest.test;

import jakarta.ws.rs.*;

@ApplicationPath("rest")
@Path("hello")
@Produces("application/json")
public class HelloResource
{
	//or in constructor
	@jakarta.inject.Inject
	private Greeter greeter;

	@GET
	@Path("{name}")
	public String hello(@PathParam("name") final String name) {
		System.out.println("Reached Hello");
		return greeter.greet(name);
	}

	@GET
	@Path("helloObject/{name}")
	public ReturnableObject helloObject(@PathParam("name") final String name) {
		System.out.println("Reached Hello Object");
		return new ReturnableObject().setName(name);
	}


}
