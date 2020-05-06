package com.guicedee.guicedservlets.rest;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.guicedee.guicedservlets.rest.implementations.JAXBMarshaller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RESTContext
{
	private static final Set<String> pathServices = new HashSet<>();
	private static final Set<String> providers = new HashSet<>(List.of(JacksonJsonProvider.class.getCanonicalName(),
	                                                                   JacksonJaxbJsonProvider.class.getCanonicalName(),
	                                                                   JAXBMarshaller.class.getCanonicalName(),
	                                                                   "org.apache.cxf.jaxrs.provider.JAXBElementProvider",
	                                                                   "org.apache.cxf.jaxrs.validation.JAXRSBeanValidationInvoker",
	                                                                   "com.guicedee.guicedservlets.rest.services.JavaTimeTypesParamConverterProvider",
	                                                                   "org.apache.cxf.jaxrs.validation.JAXRSParameterNameProvider"));
	private static final Set<String> inInterceptors = new HashSet<>(List.of("org.apache.cxf.jaxrs.validation.JAXRSBeanValidationInInterceptor"
	                                                                       ));
	private static final Set<String> outInterceptors = new HashSet<>();
	private static final Set<String> outFaultInterceptors = new HashSet<>();
	private static final Set<String> properties = new HashSet<>();
	private static final Set<String> applications = new HashSet<>();


	/**
	 * Provides the url that the module will use to provide Web Services.
	 * Does not default to module name, default to WebServices
	 * <p>
	 *
	 * <p>
	 * e.g. http://localhost/WebServices/helloworld
	 */
	public static String baseWSUrl = "rest";
	/**
	 * Whether to register all the providers found on the path.. warning - this has a tendency to enable oauth everywhere
	 */
	public static boolean autoRegisterProviders = false;

	public static String renderServices(Set<String> values)
	{
		StringBuilder sb = new StringBuilder();
		for (String pathService : values)
		{
			sb.append(pathService + ",");
		}
		if (!values.isEmpty())
		{
			sb = sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	public static Set<String> getPathServices()
	{
		return pathServices;
	}

	public static Set<String> getProviders()
	{
		return providers;
	}

	public static Set<String> getInInterceptors()
	{
		return inInterceptors;
	}

	public static Set<String> getOutInterceptors()
	{
		return outInterceptors;
	}

	public static Set<String> getProperties()
	{
		return properties;
	}

	public static Set<String> getApplications()
	{
		return applications;
	}

	/**
	 * Getter for property 'outFaultInterceptors'.
	 *
	 * @return Value for property 'outFaultInterceptors'.
	 */
	public static Set<String> getOutFaultInterceptors()
	{
		return outFaultInterceptors;
	}
}
