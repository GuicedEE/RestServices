package com.guicedee.guicedservlets.rest.internal;

import java.util.HashSet;
import java.util.Set;

/**
 * A list of package registrations
 */
public class JaxRsPackageRegistrations
{
	private static final Set<String> packageNames = new HashSet<>();

	public static Set<String> getPackageNames()
	{
		return packageNames;
	}
}
