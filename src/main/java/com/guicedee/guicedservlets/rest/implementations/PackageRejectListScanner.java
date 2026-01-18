package com.guicedee.guicedservlets.rest.implementations;

import com.guicedee.client.services.config.IPackageRejectListScanner;

import java.util.Set;

/**
 * Declares packages that should be excluded from annotation scanning.
 */
public class PackageRejectListScanner implements IPackageRejectListScanner
{
    /**
     * Returns packages to exclude from scanning.
     *
     * @return A set of excluded package prefixes
     */
    @Override
    public Set<String> exclude()
    {
        return Set.of("io.swagger.v3.jaxrs2");
    }
}
