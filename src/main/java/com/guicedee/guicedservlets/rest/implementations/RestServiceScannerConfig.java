package com.guicedee.guicedservlets.rest.implementations;


import com.guicedee.client.services.IGuiceConfig;
import com.guicedee.client.services.lifecycle.IGuiceConfigurator;

/**
 * Enables rich ClassGraph scanning for REST service discovery.
 */
public class RestServiceScannerConfig implements IGuiceConfigurator
{
	/**
	 * Configures the scan settings needed for Jakarta REST discovery.
	 *
	 * @param config The Guice scan configuration to update
	 * @return The updated configuration
	 */
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
