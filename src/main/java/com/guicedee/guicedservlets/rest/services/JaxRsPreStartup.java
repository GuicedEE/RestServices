package com.guicedee.guicedservlets.rest.services;

import com.guicedee.guicedinjection.GuiceContext;
import com.guicedee.guicedinjection.interfaces.IGuicePreStartup;
import com.guicedee.guicedservlets.rest.RestModule;
import com.guicedee.guicedservlets.rest.internal.JaxRsPackageRegistrations;
import io.github.classgraph.ClassInfo;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.java.Log;

import java.util.HashSet;
import java.util.Set;

@Log
public class JaxRsPreStartup implements IGuicePreStartup<JaxRsPreStartup> {
	public static final String serviceClassesString = "jaxrs.serviceClasses";
	public static final String providersString = "jaxrs.providers";
	public static final String inInterceptorsString = "jaxrs.inInterceptors";
	public static final String outInterceptorsString = "jaxrs.outInterceptors";
	public static final String outFaultInterceptorsString = "jaxrs.outFaultInterceptors";
	public static final String propertiesString = "jaxrs.properties";
	public static final String featuresString = "jaxrs.features";
	public static final String applicationsString = "jakarta.ws.rs.Application";

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
			log.config("Mapping Jax-RS Application - " + classInfo.loadClass()
																.getCanonicalName() + " to " + path);
		/*	getApplications().add(classInfo.loadClass()
										   .getCanonicalName());
			*/
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

			log.config("Mapping Jax-RS Path - " + classInfo.loadClass()
														 .getCanonicalName() + " to " + path);
			JaxRsPackageRegistrations.getPackageNames()
									 .add(classInfo.getPackageName());

	/*		getPathServices().add(classInfo.loadClass()
										   .getCanonicalName());*/

			mappedClasses.add(classInfo.loadClass());
		}

	}

}
