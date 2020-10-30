package com.guicedee.guicedservlets.rest;

import com.guicedee.guicedinjection.interfaces.IDefaultService;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface RestProvidersFilter<J extends RestProvidersFilter<J>> extends IDefaultService<J> {
    Map<Class<?>, Map<String, List<String>>> processResourceList(Map<Class<?>, Map<String, List<String>>> list);
}
