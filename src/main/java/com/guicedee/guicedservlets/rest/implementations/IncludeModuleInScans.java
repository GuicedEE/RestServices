package com.guicedee.guicedservlets.rest.implementations;

import com.guicedee.guicedinjection.interfaces.IGuiceScanModuleInclusions;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public class IncludeModuleInScans implements IGuiceScanModuleInclusions<IncludeModuleInScans>
{
	@Override
	public @NotNull Set<String> includeModules()
	{
		return Set.of("com.guicedee.guicedservlets.rest",
						"org.apache.cxf.rest");
	}
}
