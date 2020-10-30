package com.guicedee.guicedservlets.rest.implementations;

import com.guicedee.guicedservlets.rest.RestProvidersFilter;

import java.util.*;

public class DefaultClassResourceFilter implements RestProvidersFilter<DefaultClassResourceFilter> {
    private static final Set<String> bannedResources =new HashSet<>();

    static {
        bannedResources.add("org.apache.cxf.rs.security.saml.sso.AbstractSSOSpHandler");
        bannedResources.add("org.apache.cxf.rs.security.saml.sso.RequestAssertionConsumerService");
        bannedResources.add("org.apache.cxf.rs.security.saml.sso.MetadataService");
    }

    @Override
    public Map<Class<?>, Map<String, List<String>>> processResourceList(Map<Class<?>, Map<String, List<String>>> list) {
        Map<Class<?>, Map<String, List<String>>> l2 = new HashMap<>();
        l2.putAll(list);
        l2.entrySet().removeIf(a-> bannedResources.contains(a.getKey().getCanonicalName()));
        return l2;
    }
}
