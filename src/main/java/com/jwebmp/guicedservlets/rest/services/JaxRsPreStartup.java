package com.guicedee.guicedservlets.rest.services;

import io.github.classgraph.ClassInfo;
import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import com.guicedee.guicedinjection.GuiceContext;
import com.guicedee.guicedinjection.interfaces.IGuicePreStartup;
import com.guicedee.guicedservlets.rest.GuicedCXFNonSpringJaxrsServlet;
import com.guicedee.guicedservlets.rest.RESTContext;
import com.guicedee.guicedservlets.rest.internal.JaxRsPackageRegistrations;
import com.guicedee.logger.LogFactory;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.undertow.servlet.Servlets.*;
import static com.guicedee.guicedservlets.rest.RESTContext.*;

public class JaxRsPreStartup implements IGuicePreStartup<JaxRsPreStartup> {
	private static final Logger log = LogFactory.getLog(JaxRsPreStartup.class);

	public static final String serviceClassesString = "jaxrs.serviceClasses";
	public static final String providersString = "jaxrs.providers";
	public static final String inInterceptorsString = "jaxrs.inInterceptors";
	public static final String outInterceptorsString = "jaxrs.outInterceptors";
	public static final String propertiesString = "jaxrs.properties";
	public static final String applicationsString = "javax.ws.rs.Application";


	public  static DeploymentManager deploymentManager;
	private Undertow server;

	private Set<Class<?>> mappedClasses = new HashSet<>();

	@Override
	public void onStartup() {
		scanClassesIn();
		bootDeployment();
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

	}

	private void bootDeployment() {
		String path = RESTContext.protocol + "://" + RESTContext.listeningAddress + ":" + RESTContext.port + "/" +  RESTContext.baseWSUrl;
		try {
			final DeploymentInfo deploymentInfo = deployment()
					.setClassLoader(Thread.currentThread().getContextClassLoader())
					.setContextPath( "/")
					.setDeploymentName("sse-test")
					.addServlets(
							servlet("GuicedJaxRsDeployment", GuicedCXFNonSpringJaxrsServlet.class)
								//	.addInitParam("jaxrs.providers", JacksonJsonProvider.class.getName()) Later 2.10.1 jackson
								//	.addInitParam("jaxrs.serviceClasses", BookStore.class.getName())
									.setAsyncSupported(true)
									.setLoadOnStartup(1)
									.addMapping("/" + RESTContext.baseWSUrl + "/*")
					);

			if(!getPathServices().isEmpty())
				deploymentInfo.addServletContextAttribute(serviceClassesString, buildInitParamFromSet(getPathServices()));
			if(!getProviders() .isEmpty())
				deploymentInfo.addServletContextAttribute(providersString, buildInitParamFromSet(getProviders()));
			if(!getInInterceptors() .isEmpty())
				deploymentInfo.addServletContextAttribute(inInterceptorsString, buildInitParamFromSet(getInInterceptors()));
			if(!getOutInterceptors().isEmpty())
				deploymentInfo.addServletContextAttribute(outInterceptorsString, buildInitParamFromSet(getOutInterceptors()));
			if(!getProperties() .isEmpty())
				deploymentInfo.addServletContextAttribute(propertiesString, buildInitParamFromSet(getProperties()));
			if(!getApplications() .isEmpty())
				deploymentInfo.addServletContextAttribute(applicationsString, buildInitParamFromSet(getApplications()));

			if(!getPathServices().isEmpty())
			deploymentInfo.addInitParameter(serviceClassesString, buildInitParamFromSet(getPathServices()));
			if(!getProviders() .isEmpty())
			deploymentInfo.addInitParameter(providersString, buildInitParamFromSet(getProviders()));
			if(!getInInterceptors() .isEmpty())
			deploymentInfo.addInitParameter(inInterceptorsString, buildInitParamFromSet(getInInterceptors()));
			if(!getOutInterceptors().isEmpty())
			deploymentInfo.addInitParameter(outInterceptorsString, buildInitParamFromSet(getOutInterceptors()));
			if(!getProperties() .isEmpty())
			deploymentInfo.addInitParameter(propertiesString, buildInitParamFromSet(getProperties()));
			if(!getApplications() .isEmpty())
			deploymentInfo.addInitParameter(applicationsString, buildInitParamFromSet(getApplications()));

		//	deploymentInfo.addInitParameter("jaxrs.extensions", "xml=application/xml,json=application/json");

			final DeploymentManager manager = Servlets.defaultContainer()
													  .addDeployment(deploymentInfo);
			deploymentManager = manager;
			log.info("Rest service registered. DeploymentManager Ready");
		} catch (final Exception ex) {
			log.log(Level.SEVERE, "Failed Mapping Jax-RS Application - " + path + "/*", ex);
		}
	}


	private String buildInitParamFromSet(Set<String> values)
	{
		if(values.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		for (String value : values) {
			sb.append(value)
			  .append(",");
		}
		if(!values.isEmpty())
			sb.deleteCharAt(sb.length() - 1);

		return sb.toString();
	}

}
