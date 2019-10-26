package com.guicedee.guicedservlets.rest;

import com.guicedee.guicedservlets.services.GuiceSiteInjectorModule;
import com.guicedee.guicedservlets.services.IGuiceSiteBinder;
import com.guicedee.logger.LogFactory;

import java.util.logging.Logger;

public class RestModule
		implements IGuiceSiteBinder<GuiceSiteInjectorModule> {
	private static final Logger log = LogFactory.getLog(RestModule.class);

	public RestModule() {
		//Nothing Needed
	}

	public static String getPath() {
		return RESTContext.baseWSUrl;
	}

	public static void setPath(String path) {
		RESTContext.baseWSUrl = path;
	}

	public static String cleanPath(String path) {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		if (!path.endsWith("/") && !"/".equals(path)) { path = path + "/"; }
		return path;
	}

	@Override
	public void onBind(GuiceSiteInjectorModule module) {
		log.config("Binding rest services to path defined in RestModule - " + RESTContext.baseWSUrl);
		module.serve$(cleanPath(RESTContext.baseWSUrl) + "*")
			  .with(GuicedCXFNonSpringJaxrsServlet.class);
	}


}
