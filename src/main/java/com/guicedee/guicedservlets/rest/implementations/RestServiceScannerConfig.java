package com.guicedee.guicedservlets.rest.implementations;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.guicedee.guicedinjection.GuiceConfig;
import com.guicedee.guicedinjection.interfaces.IGuiceConfigurator;
import com.guicedee.guicedservlets.rest.RESTContext;

public class RestServiceScannerConfig implements IGuiceConfigurator {
	@Override
	public GuiceConfig configure(GuiceConfig config) {

		config.setAnnotationScanning(true);
		config.setMethodInfo(true);
		config.setClasspathScanning(true);
		config.setFieldInfo(true);

		RESTContext.getProviders()
		           .add(JacksonJsonProvider.class.getCanonicalName());

		return config;
	}

}
