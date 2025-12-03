package com.guicedee.guicedservlets.rest.implementations;

import com.guicedee.client.services.config.IPackageRejectListScanner;

import java.util.Set;

public class PackageRejectListScanner implements IPackageRejectListScanner
{
    @Override
    public Set<String> exclude()
    {
        return Set.of("io.swagger.v3.jaxrs2");
    }
}
