package com.guicedee.guicedservlets.rest.services;

import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.guicedee.guicedinjection.GuiceContext;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import lombok.extern.java.Log;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import com.guicedee.guicedinjection.interfaces.IGuicePostStartup;
import com.guicedee.guicedservlets.rest.RESTContext;

import jakarta.servlet.ServletException;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.guicedee.guicedservlets.rest.RESTContext.*;

@Log
public class JaxRsPostStartup implements IGuicePostStartup<JaxRsPostStartup>
{
	
	@Override
	public void postLoad()
	{
		createBus();
	}
	
	public void createBus()
	{
		log.fine("Creating Jax-RS Bus");
		Bus bus = BusFactory.newInstance().createBus();
		BusFactory.setDefaultBus(bus);
		log.fine("Created Jax-RS Bus");
/*
		GuiceContext.get(ObjectMapper.class)
		            .registerModule(new ParameterNamesModule());*/
	}
	
}
