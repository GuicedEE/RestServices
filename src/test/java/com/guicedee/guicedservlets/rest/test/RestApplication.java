package com.guicedee.guicedservlets.rest.test;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.guicedee.guicedinjection.GuiceContext;
import jakarta.ws.rs.core.Application;

import java.util.HashSet;
import java.util.Set;


public class RestApplication extends Application
{
	private final Set<Class<?>> classes = new HashSet<Class<?>>();
	private final Set<Object> singletons = new HashSet<Object>();
	
	@Override
	public Set<Class<?>> getClasses()
	{
		return classes;
	}
	
	@Override
	public Set<Object> getSingletons()
	{
		singletons.add(GuiceContext.get(HelloResource.class));
		return singletons;
	}
}
