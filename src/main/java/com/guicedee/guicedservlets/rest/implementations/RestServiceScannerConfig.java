package com.guicedee.guicedservlets.rest.implementations;


import com.guicedee.client.services.IGuiceConfig;
import com.guicedee.client.services.lifecycle.IGuiceConfigurator;

public class RestServiceScannerConfig implements IGuiceConfigurator
{
	@Override
	public IGuiceConfig<?> configure(IGuiceConfig<?> config)
	{
		config.setAnnotationScanning(true);
		config.setMethodInfo(true);
		config.setClasspathScanning(true);
		config.setFieldInfo(true);
		config.setExcludePackages(true);
		config.setIgnoreMethodVisibility(true);
		config.setIgnoreFieldVisibility(true);
		config.setIgnoreClassVisibility(true);
		return config;
	}
	
}
