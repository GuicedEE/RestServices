package com.guicedee.guicedservlets.rest.implementations;

import com.google.common.base.Strings;
import com.guicedee.guicedservlets.rest.*;

import java.util.*;

public class DefaultClassResourceFilterResource implements RestResourceProvidersFilter<DefaultClassResourceFilterResource>,RestServicesFilter<DefaultClassResourceFilterResource> {
    private static final Set<String> bannedResources =new HashSet<>();
    private static final Set<String> samlResources =new HashSet<>();
    private static final Set<String> aegisResources =new HashSet<>();
    private static final Set<String> atomResources =new HashSet<>();

    static {
        bannedResources.add("org.apache.cxf.rs.security.saml.sso.AbstractSSOSpHandler");
    }

    static {
        samlResources.add("org.apache.cxf.rs.security.saml.sso.RequestAssertionConsumerService");
        samlResources.add("org.apache.cxf.rs.security.saml.sso.MetadataService");
        samlResources.add("org.apache.cxf.rs.security.saml.sso.LogoutService");
        samlResources.add("org.apache.cxf.rs.security.saml.sso.state.HTTPSPStateManager");
    }

    static {
        aegisResources.add("org.apache.cxf.jaxrs.provider.aegis.AegisJSONProvider");
        aegisResources.add("org.apache.cxf.jaxrs.provider.aegis.AegisElementProvider");
    }

    static {
        atomResources.add("org.apache.cxf.jaxrs.provider.atom.AtomEntryProvider");
        atomResources.add("org.apache.cxf.jaxrs.provider.atom.AtomFeedProvider");
        atomResources.add("org.apache.cxf.jaxrs.provider.atom.AtomPojoProvider");
    }

    @Override
    public Map<Class<?>, Map<String, List<String>>> processResourceList(Map<Class<?>, Map<String, List<String>>> list) {
        Map<Class<?>, Map<String, List<String>>> l2 = new HashMap<>();
        list.forEach((key,value)->{
            if(key != null) {
                if (!checkInValid(key.getCanonicalName())) {
                    l2.put(key, value);
                }
            }
        });
        return l2;
    }

    private boolean checkInValid(String className)
    {
        if (Strings.isNullOrEmpty(className))
        {
            return false;
        }
        return bannedResources.contains(className) ||
                (!RESTContext.isUseSaml() && samlResources.contains(className) ) ||
                (!RESTContext.isUseAegis() &&  aegisResources.contains(className) ) ||
                (!RESTContext.isUseAtom() &&  atomResources.contains(className) );
    }

    @Override
    public Map<Class<?>, Map<String, List<String>>> processServicesList(Map<Class<?>, Map<String, List<String>>> list) {
        Map<Class<?>, Map<String, List<String>>> l2 = new HashMap<>();
        list.forEach((key,value)->{
            if(key != null) {
                if (!checkInValid(key.getCanonicalName())) {
                    l2.put(key, value);
                }
            }
        });
        return l2;
    }
}
