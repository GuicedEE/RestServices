package com.guicedee.guicedservlets.rest;

import com.guicedee.guicedinjection.interfaces.IDefaultService;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface RestServicesFilter<J extends RestServicesFilter<J>> extends IDefaultService<J> {
    Map<Class<?>, Map<String, List<String>>> processServicesList(Map<Class<?>, Map<String, List<String>>> list);
}
