package com.guicedee.guicedservlets.rest;

import java.util.HashSet;
import java.util.Set;

public class RESTContext
{
	private static final Set<String> pathServices = new HashSet<>();
	private static final Set<String> providers = new HashSet<>();
	private static final Set<String> inInterceptors = new HashSet<>();
	private static final Set<String> outInterceptors = new HashSet<>();
	private static final Set<String> properties = new HashSet<>();
	private static final Set<String> applications = new HashSet<>();

	public static boolean runSeparately = false;
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
	 * A separate port to run the Web Services on - The do not filter through the main handler
	 */
	public static Integer port = 6005;
	/**
	 * The protocol the WS is listening on
	 */
	public static String protocol = "http";
	/**
	 * The address that the server will respond to
	 */
	public static String listeningAddress = "localhost";


	public static String renderPathServices() {
		StringBuilder sb = new StringBuilder();
		for (String pathService : pathServices) {
			sb.append(pathService + ",");
		}
		if(!pathServices.isEmpty())
		{
			sb = sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	public static Set<String> getPathServices() {
		return pathServices;
	}

	public static Set<String> getProviders() {
		return providers;
	}

	public static Set<String> getInInterceptors() {
		return inInterceptors;
	}

	public static Set<String> getOutInterceptors() {
		return outInterceptors;
	}

	public static Set<String> getProperties() {
		return properties;
	}

	public static Set<String> getApplications() {
		return applications;
	}


}
