package com.guicedee.guicedservlets.rest.test;

import com.google.inject.Inject;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.utils.LogUtils;
import jakarta.ws.rs.*;
import org.apache.logging.log4j.Level;

import java.util.Map;
import java.util.UUID;

@ApplicationPath("rest")
@Path("hello")
@Produces("application/json")
public class HelloResource
{
	//or in constructor
	@Inject
	private Greeter greeter;

	static void main() {
		//Configure Logging
		LogUtils.addHighlightedConsoleLogger(Level.DEBUG);
		//Register Slim Classpath Scanning and Modularization
		IGuiceContext.registerModule("com.example.my.module");
		//Start Guice Context
		IGuiceContext.instance().inject();
	}

	/**
	 * Inner static DTO to mirror the ArrangementCreateDTO pattern
	 */
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
	public static class ComplexRequestDTO {
		public String type;
		public String classification;
		public Map<String, String> metadata;
	}

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

	@POST
	@Path("helloObject/{name}")
	public ReturnableObject helloObjectInPost(@PathParam("name") final String name, RequestObject requestObject) {
		return new ReturnableObject().setName(name + requestObject.getName());
	}

	@PUT
	@Path("helloObject/{name}")
	public ReturnableObject helloObjectInPut(@PathParam("name") final String name, RequestObject requestObject) {
		return new ReturnableObject().setName("PUT:" + name + requestObject.getName());
	}

	@POST
	@Path("complex/{name}")
	public ReturnableObject complexPost(@PathParam("name") final String name, ComplexRequestDTO dto) {
		String result = name + ":" + dto.type + ":" + dto.classification;
		if (dto.metadata != null && !dto.metadata.isEmpty()) {
			result += ":" + dto.metadata.size();
		}
		return new ReturnableObject().setName(result);
	}


	@POST
	@Path("complex/{name}/uuid")
	public ReturnableObject complexPostUUID(@PathParam("name") final UUID name, ComplexRequestDTO dto) {
		String result = name + ":" + dto.type + ":" + dto.classification;
		if (dto.metadata != null && !dto.metadata.isEmpty()) {
			result += ":" + dto.metadata.size();
		}
		return new ReturnableObject().setName(result);
	}
}
