package com.guicedee.guicedservlets.rest;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.guicedee.guicedservlets.services.GuiceSiteInjectorModule;
import com.guicedee.guicedservlets.services.IGuiceSiteBinder;
import com.guicedee.logger.LogFactory;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static com.guicedee.guicedservlets.rest.services.JaxRsPreStartup.*;

public class RestModule
		implements IGuiceSiteBinder<GuiceSiteInjectorModule>
{
	private static final Logger log = LogFactory.getLog(RestModule.class);
	public static final Set<String> illegalProviders = new HashSet<>();

	static
	{
		illegalProviders.add("org.apache.cxf.jaxrs.provider.aegis.AegisJSONProvider");
		illegalProviders.add("org.apache.cxf.jaxrs.provider.aegis.AegisElementProvider");
		illegalProviders.add("org.apache.cxf.jaxrs.provider.atom.AtomEntryProvider");
		illegalProviders.add("org.apache.cxf.jaxrs.provider.atom.AtomFeedProvider");
		illegalProviders.add("org.apache.cxf.jaxrs.provider.atom.AtomPojoProvider");
	}

	public RestModule()
	{
		//Nothing Needed
	}

	public static String getPath()
	{
		return RESTContext.baseWSUrl;
	}

	public static void setPath(String path)
	{
		RESTContext.baseWSUrl = path;
	}

	public static String cleanPath(String path)
	{
		if (!path.startsWith("/"))
		{
			path = "/" + path;
		}
		if (!path.endsWith("/") && !"/".equals(path))
		{
			path = path + "/";
		}
		return path;
	}

	@Override
	public void onBind(GuiceSiteInjectorModule module)
	{
		log.config("Binding rest services to path defined in RestModule - " + RESTContext.baseWSUrl);
		module.serve$(cleanPath(RESTContext.baseWSUrl) + "*")
		      .with(GuicedCXFNonSpringJaxrsServlet.class);
	}

	public static boolean validClass(Class<?> clazz)
	{
		boolean valid = false;
		if (illegalProviders.contains(clazz.getCanonicalName()))
		{
			return false;
		}

		try
		{
			for (Constructor<?> declaredConstructor : clazz.getDeclaredConstructors())
			{
				if (declaredConstructor.getParameterCount() == 0
				    || declaredConstructor.isAnnotationPresent(Inject.class)
				    || declaredConstructor.isAnnotationPresent(javax.inject.Inject.class)
				)
				{
					return true;
				}
			}
		}
		catch (NoClassDefFoundError | IllegalAccessError e)
		{
			return false;
		}
		return valid;
	}

}
