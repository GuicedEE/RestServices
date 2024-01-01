package com.guicedee.guicedservlets.rest.implementations;

import com.google.common.base.Strings;
import com.guicedee.guicedservlets.rest.RestResourceProvidersFilter;
import com.guicedee.guicedservlets.rest.RestServicesFilter;

import java.util.*;

public class DefaultClassResourceFilterResource implements RestResourceProvidersFilter<DefaultClassResourceFilterResource>,RestServicesFilter<DefaultClassResourceFilterResource> {
    private static final Set<String> bannedResources =new HashSet<>();
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
        return bannedResources.contains(className);
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
