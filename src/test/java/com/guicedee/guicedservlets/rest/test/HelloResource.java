package com.guicedee.guicedservlets.rest.test;

import com.google.inject.Inject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@Path("hello")
@Produces("application/json")
public class HelloResource
{
	private final Greeter greeter;

	@Inject
	public HelloResource(final Greeter greeter)
	{
		this.greeter = greeter;
	}

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
