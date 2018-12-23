package com.jwebmp.guiced.rest.implementations;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.jwebmp.guiced.rest.internal.RestEasyPackageRegistrations;

import javax.ws.rs.Path;

/**
 * Collects type registered with @Path and adds the packages to a set list for retrieval later (open api etc)
 */
public class RestEasyPackagesTypeInterceptor
		implements TypeListener
{
	@Override
	public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter)
	{
		Class<?> clazz = type.getRawType();
		if (clazz != null)
		{
			if (clazz.isAnnotationPresent(Path.class))
			{
				String packageName = clazz.getCanonicalName();
				packageName = packageName.substring(0, packageName.lastIndexOf('.'));
				RestEasyPackageRegistrations.getPackageNames()
				                            .add(packageName);
			}
		}
	}
}
