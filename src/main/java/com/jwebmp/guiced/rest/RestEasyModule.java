package com.jwebmp.guiced.rest;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.jwebmp.guiced.rest.implementations.RestEasyPackagesTypeInterceptor;
import com.jwebmp.guicedservlets.services.GuiceSiteInjectorModule;
import com.jwebmp.guicedservlets.services.IGuiceSiteBinder;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.guice.ext.JaxrsModule;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

import java.util.Map;

public class RestEasyModule
		implements IGuiceSiteBinder<GuiceSiteInjectorModule>
{
	private static String path = "/rest";

	public RestEasyModule()
	{
		//Nothing Needed
	}

	@Override
	public void onBind(GuiceSiteInjectorModule module)
	{
		module.install(new JaxrsModule());
		module.bind(GuiceResteasyBootstrapServletContextListener.class)
		      .in(Singleton.class);
		module.bind(HttpServletDispatcher.class)
		      .in(Singleton.class);

		if (path == null)
		{
			module.serve$("/*")
			      .with(HttpServletDispatcher.class);
		}
		else
		{
			final Map<String, String> initParams =
					ImmutableMap.of("resteasy.servlet.mapping.prefix", path);
			module.serve$(path + "/*")
			      .with(HttpServletDispatcher.class, initParams);
		}

		module.bindListener(Matchers.any(), new RestEasyPackagesTypeInterceptor());
	}

	public static String getPath()
	{
		return path;
	}

	public static void setPath(String path)
	{
		RestEasyModule.path = path;
	}
}
