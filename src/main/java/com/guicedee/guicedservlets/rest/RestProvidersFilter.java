package com.guicedee.guicedservlets.rest;

import com.guicedee.guicedinjection.interfaces.IDefaultService;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface RestProvidersFilter<J extends RestProvidersFilter<J>> extends IDefaultService<J> {
    boolean disallowProvider(Class<?> clazz);
}
