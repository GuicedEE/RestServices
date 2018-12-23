package com.jwebmp.guiced.rest.internal;

import java.util.HashSet;
import java.util.Set;

/**
 * A list of package registrations
 */
public class RestEasyPackageRegistrations
{
	private static final Set<String> packageNames = new HashSet<>();

	public static Set<String> getPackageNames()
	{
		return packageNames;
	}
}
