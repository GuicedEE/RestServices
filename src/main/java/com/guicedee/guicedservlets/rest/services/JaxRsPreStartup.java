package com.guicedee.guicedservlets.rest.services;

import com.guicedee.guicedinjection.GuiceContext;
import com.guicedee.guicedinjection.interfaces.IGuicePreStartup;
import com.guicedee.guicedservlets.rest.internal.JaxRsPackageRegistrations;
import com.guicedee.logger.LogFactory;
import io.github.classgraph.ClassInfo;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static com.guicedee.guicedservlets.rest.RESTContext.*;

public class JaxRsPreStartup implements IGuicePreStartup<JaxRsPreStartup> {
	private static final Logger log = LogFactory.getLog(JaxRsPreStartup.class);

	public static final String serviceClassesString = "jaxrs.serviceClasses";
	public static final String providersString = "jaxrs.providers";
	public static final String inInterceptorsString = "jaxrs.inInterceptors";
	public static final String outInterceptorsString = "jaxrs.outInterceptors";
	public static final String outFaultInterceptorsString = "jaxrs.outFaultInterceptors";
	public static final String propertiesString = "jaxrs.properties";
	public static final String applicationsString = "javax.ws.rs.Application";

	public static final Set<Class<?>> mappedClasses = new HashSet<>();

	@Override
	public void onStartup() {
		scanClassesIn();
	}

	private void scanClassesIn() {
		for (ClassInfo classInfo : GuiceContext.instance()
											   .getScanResult()
											   .getClassesWithAnnotation(ApplicationPath.class.getCanonicalName())) {
			String path = classInfo.loadClass()
								   .getAnnotation(ApplicationPath.class)
								   .value();
			JaxRsPackageRegistrations.getPackageNames()
									 .add(classInfo.getPackageName());
			log.fine("Mapping Jax-RS Application - " + classInfo.loadClass()
																.getCanonicalName() + " to " + path);

			getApplications().add(classInfo.loadClass()
										   .getCanonicalName());
			mappedClasses.add(classInfo.loadClass());
		}

		for (ClassInfo classInfo : GuiceContext.instance()
											   .getScanResult()
											   .getClassesWithAnnotation(Path.class.getCanonicalName())) {
			if(classInfo.loadClass().getCanonicalName().startsWith("org.apache.cxf.rs.security.oauth"))
				continue;
			String path = classInfo.loadClass()
								   .getAnnotation(Path.class)
								   .value();

			log.fine("Mapping Jax-RS Path - " + classInfo.loadClass()
														 .getCanonicalName() + " to " + path);
			JaxRsPackageRegistrations.getPackageNames()
									 .add(classInfo.getPackageName());

			getPathServices().add(classInfo.loadClass()
										   .getCanonicalName());

			mappedClasses.add(classInfo.loadClass());
		}
		if (autoRegisterProviders)
		{
			for (ClassInfo classInfo : GuiceContext.instance()
			                                       .getScanResult()
			                                       .getClassesWithAnnotation(Provider.class.getCanonicalName()))
			{
				if (classInfo.isAbstract() || classInfo.isInterface() || !RestModule.validClass(classInfo.loadClass()))
				{
					continue;
				}
				continue;
				log.fine("Mapping Provider - " + classInfo.loadClass()
				                                          .getCanonicalName());
				JaxRsPackageRegistrations.getPackageNames()
				                         .add(classInfo.getPackageName());

				getProviders().add(classInfo.loadClass()
				                            .getCanonicalName());
				mappedClasses.add(classInfo.loadClass());
			}
		}
	}

}
