package com.guicedee.guicedservlets.rest.implementations;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;
import com.google.inject.Inject;
import com.google.inject.servlet.ServletModule;
import com.guicedee.guicedinjection.GuiceContext;
import com.guicedee.guicedservlets.rest.RESTContext;
import com.guicedee.guicedservlets.rest.annotations.*;

import com.guicedee.guicedservlets.servlets.services.IGuiceSiteBinder;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import lombok.extern.java.Log;
import org.apache.cxf.jaxrs.provider.MultipartProvider;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

@Log
public class RestModule
				extends ServletModule
				implements IGuiceSiteBinder<RestModule>
{
	
	public RestModule()
	{
		//Nothing Needed
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
	
	private final Predicate<ClassInfo> filter = (applicationClass)
					->applicationClass.isAbstract() || applicationClass.isInterface() || applicationClass.isInnerClass() || applicationClass.isStatic() ||
							applicationClass.getPackageName().startsWith("org.apache.cxf.jaxrs.openapi") || 
							applicationClass.getPackageName().startsWith("io.swagger.v3.jaxrs2.integration.resources") ||
							applicationClass.getPackageName().startsWith("org.apache.cxf.jaxrs.swagger.ui")
					;
	
	@Override
	protected void configureServlets()
	{
		log.info("Binding rest services to path defined in RestModule - " + RESTContext.baseWSUrl);
		
		Map<String, String> initParams = new HashMap<>();
		
		//jaxrs services
		ScanResult scanResult = GuiceContext.instance().getScanResult();
		
		ClassInfoList applicationClasses = scanResult.getSubclasses(Application.class);
		applicationClasses.removeIf(filter);
		StringBuilder applications = new StringBuilder();
		for (ClassInfo applicationClass : applicationClasses)
		{
			applications.append(applicationClass.loadClass().getCanonicalName()).append(",");
		}
		if (!applicationClasses.isEmpty())
		{
			applications = new StringBuilder(applications.substring(0, applications.length() - 1));
			log.config("JaxRS Applications : " + applications);
			initParams.put("jakarta.ws.rs.Application", applications.toString());
		}
		
		ClassInfoList pathClasses = scanResult.getClassesWithAnnotation(Path.class);
		pathClasses.removeIf(filter);
		StringBuilder paths = new StringBuilder();
		for (ClassInfo applicationClass : pathClasses)
		{
			paths.append(applicationClass.loadClass().getCanonicalName()).append(",");
		}
		if (!applicationClasses.isEmpty())
		{
			paths = new StringBuilder(paths.substring(0, paths.length() - 1));
		}
		log.config("JaxRS Paths : " + paths);
		initParams.put("jaxrs.serviceClasses", paths.toString());
		
		
		ClassInfoList restProviders = scanResult.getClassesWithAnnotation(RestProvider.class);
		restProviders.removeIf(filter);
		StringBuilder providers = new StringBuilder();
		providers.append(JacksonJsonProvider.class.getCanonicalName()).append(",");
		providers.append(MultipartProvider.class.getCanonicalName()).append(",");
		providers.append(JacksonXmlBindJsonProvider.class.getCanonicalName()).append(",");
		providers.append(JAXBMarshaller.class.getCanonicalName()).append(",");
		
		for (ClassInfo applicationClass : restProviders)
		{
			RestProvider annotation = applicationClass.loadClass().getAnnotation(RestProvider.class);
			providers.append(annotation.value());
		}
		if (!restProviders.isEmpty())
		{
			providers = new StringBuilder(providers.substring(0, providers.length() - 1));
		}
		log.config("JaxRS Providers : " + providers);
		initParams.put("jaxrs.providers", providers.toString());
		
		
		ClassInfoList featureClasses = scanResult.getClassesWithAnnotation(RestFeature.class);
		featureClasses.removeIf(filter);
		StringBuilder features = new StringBuilder();
		for (ClassInfo applicationClass : featureClasses)
		{
			features.append(applicationClass.loadClass().getCanonicalName()).append(",");
		}
		if (!featureClasses.isEmpty())
		{
			features = new StringBuilder(features.substring(0, features.length() - 1));
		}
		log.config("JaxRS Features : " + features);
		initParams.put("jaxrs.features", features.toString());
		
		
		ClassInfoList extensionClasses = scanResult.getClassesWithAnnotation(RestExtension.class);
		extensionClasses.removeIf(filter);
		StringBuilder extensions = new StringBuilder();
		for (ClassInfo applicationClass : extensionClasses)
		{
			extensions.append(applicationClass.loadClass().getCanonicalName()).append(",");
		}
		if (!extensionClasses.isEmpty())
		{
			extensions = new StringBuilder(extensions.substring(0, extensions.length() - 1));
		}
		log.config("JaxRS Extensions : " + extensions);
		initParams.put("jaxrs.extensions", extensions.toString());
		
		
		ClassInfoList inInterceptorClasses = scanResult.getClassesWithAnnotation(RestInInterceptor.class);
		inInterceptorClasses.removeIf(filter);
		StringBuilder inInterceptors = new StringBuilder();
		for (ClassInfo applicationClass : inInterceptorClasses)
		{
			inInterceptors.append(applicationClass.loadClass().getCanonicalName()).append(",");
		}
		if (!inInterceptorClasses.isEmpty())
		{
			inInterceptors = new StringBuilder(inInterceptors.substring(0, inInterceptors.length() - 1));
		}
		log.config("JaxRS In Interceptors : " + inInterceptors);
		initParams.put("jaxrs.inInterceptors", inInterceptors.toString());
		
		ClassInfoList outInterceptorClasses = scanResult.getClassesWithAnnotation(RestOutInterceptor.class);
		outInterceptorClasses.removeIf(filter);
		StringBuilder outInterceptors = new StringBuilder();
		for (ClassInfo applicationClass : outInterceptorClasses)
		{
			outInterceptors.append(applicationClass.loadClass().getCanonicalName()).append(",");
		}
		if (!outInterceptorClasses.isEmpty())
		{
			outInterceptors = new StringBuilder(outInterceptors.substring(0, outInterceptors.length() - 1));
		}
		log.config("JaxRS Out Interceptors : " + outInterceptors);
		initParams.put("jaxrs.outInterceptors", outInterceptors.toString());
		
		ClassInfoList outFaultInterceptorClasses = scanResult.getClassesWithAnnotation(RestOutFaultInterceptor.class);
		outFaultInterceptorClasses.removeIf(filter);
		StringBuilder outFaultInterceptors = new StringBuilder();
		for (ClassInfo applicationClass : outFaultInterceptorClasses)
		{
			outFaultInterceptors.append(applicationClass.loadClass().getCanonicalName()).append(",");
		}
		if (!outFaultInterceptorClasses.isEmpty())
		{
			outFaultInterceptors = new StringBuilder(outFaultInterceptors.substring(0, outFaultInterceptors.length() - 1));
		}
		log.config("JaxRS Out Fault Interceptors : " + outFaultInterceptors);
		initParams.put("jaxrs.outFaultInterceptors", outFaultInterceptors.toString());
		
		
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
