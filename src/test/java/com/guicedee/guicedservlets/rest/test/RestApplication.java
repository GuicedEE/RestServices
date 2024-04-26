package com.guicedee.guicedservlets.rest.test;

import com.guicedee.client.*;
import jakarta.ws.rs.core.*;

import java.util.*;


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
		singletons.add(IGuiceContext.get(HelloResource.class));
		return singletons;
	}
}
