package com.guicedee.guicedservlets.rest;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;
import com.google.inject.Inject;
import com.google.inject.servlet.ServletModule;
import com.guicedee.guicedinjection.GuiceContext;
import com.guicedee.guicedinjection.interfaces.IGuiceModule;
import com.guicedee.guicedservlets.rest.implementations.JAXBMarshaller;
import com.guicedee.guicedservlets.services.IGuiceSiteBinder;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import lombok.extern.java.Log;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

@Log
public class RestModule
				extends ServletModule
				implements IGuiceSiteBinder<RestModule>
{
	
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
	protected void configureServlets()
	{
		log.config("Binding rest services to path defined in RestModule - " + RESTContext.baseWSUrl);
		
		Map<String, String> initParams = new HashMap<>();
		
		//jaxrs services
		ScanResult scanResult = GuiceContext.instance().getScanResult();
		ClassInfoList applicationClasses = scanResult.getSubclasses(Application.class);
		
		StringBuilder applications = new StringBuilder();
		for (ClassInfo applicationClass : applicationClasses)
		{
			if (applicationClass.isAbstract() || applicationClass.isInterface())
				continue;
			
			applications.append(applicationClass.loadClass().getCanonicalName() + ",");
		}
		if (!applicationClasses.isEmpty())
		{
			applications.substring(0, applications.length() - 1);
			log.config("JaxRS Applications : " + applications);
			initParams.put("jakarta.ws.rs.Application", applications.toString());
		}
		
		
		ClassInfoList pathClasses = scanResult.getClassesWithAnnotation(Path.class);
		StringBuilder paths = new StringBuilder();
		for (ClassInfo applicationClass : pathClasses)
		{
			if (applicationClass.isAbstract() || applicationClass.isInterface())
				continue;
			
			paths.append(applicationClass.loadClass().getCanonicalName() + ",");
		}
		if (!applicationClasses.isEmpty())
		{
			paths = new StringBuilder(paths.substring(0, paths.length() - 1));
		}
		log.config("JaxRS Paths : " + paths);
		initParams.put("jaxrs.serviceClasses", paths.toString());
		
		
		ClassInfoList restProviders = scanResult.getClassesWithAnnotation(RestProvider.class);
		StringBuilder providers = new StringBuilder();
		providers.append(JacksonJsonProvider.class.getCanonicalName()).append(",");
		providers.append(JacksonXmlBindJsonProvider.class.getCanonicalName()).append(",");
		providers.append(JAXBMarshaller.class.getCanonicalName()).append(",");
		
		for (ClassInfo applicationClass : restProviders)
		{
			if (applicationClass.isAbstract() || applicationClass.isInterface())
				continue;
			
			RestProvider annotation = applicationClass.loadClass().getAnnotation(RestProvider.class);
			providers.append(annotation.value());
		}
		if (!applicationClasses.isEmpty())
		{
			providers= new StringBuilder(providers.substring(0, providers.length() - 1));
		}
		log.config("JaxRS Providers : " + providers);
		initParams.put("jaxrs.providers", providers.toString());
		
		
		serve(cleanPath(RESTContext.baseWSUrl) + "*")
						.with(GuicedCXFNonSpringJaxrsServlet.class, initParams);
	}
	
	public static boolean validClass(Class<?> clazz)
	{
		boolean valid = false;
		try
		{
			for (Constructor<?> declaredConstructor : clazz.getDeclaredConstructors())
			{
				if (declaredConstructor.getParameterCount() == 0
								|| declaredConstructor.isAnnotationPresent(Inject.class)
								|| declaredConstructor.isAnnotationPresent(jakarta.inject.Inject.class)
				)
				{
					return true;
				}
			}
		} catch (NoClassDefFoundError | IllegalAccessError e)
		{
			return false;
		}
		return valid;
	}
	
}
