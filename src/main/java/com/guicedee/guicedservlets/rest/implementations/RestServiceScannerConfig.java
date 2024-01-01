package com.guicedee.guicedservlets.rest.implementations;


import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.guicedee.guicedinjection.GuiceConfig;
import com.guicedee.guicedinjection.interfaces.IGuiceConfig;
import com.guicedee.guicedinjection.interfaces.IGuiceConfigurator;
import com.guicedee.guicedservlets.rest.RESTContext;

public class RestServiceScannerConfig implements IGuiceConfigurator
{
	@Override
	public IGuiceConfig<?> configure(IGuiceConfig<?> config)
	{
		config.setAnnotationScanning(true);
		config.setMethodInfo(true);
		config.setClasspathScanning(true);
		config.setFieldInfo(true);
		
		RESTContext.providers
						.add(JacksonJsonProvider.class.getCanonicalName());
		
		return config;
	}
	
}
