package com.guicedee.guicedservlets.rest.services;

import com.guicedee.guicedinjection.interfaces.IGuicePostStartup;
import lombok.extern.java.Log;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;

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
