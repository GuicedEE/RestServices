package com.guicedee.guicedservlets.rest.implementations;


import com.guicedee.guicedinjection.interfaces.IGuiceConfig;
import com.guicedee.guicedinjection.interfaces.IGuiceConfigurator;

public class RestServiceScannerConfig implements IGuiceConfigurator
{
	@Override
	public IGuiceConfig<?> configure(IGuiceConfig<?> config)
	{
		config.setAnnotationScanning(true);
		config.setMethodInfo(true);
		config.setClasspathScanning(true);
		config.setFieldInfo(true);
		
		return config;
	}
	
}
