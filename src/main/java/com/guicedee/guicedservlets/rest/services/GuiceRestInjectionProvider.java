package com.guicedee.guicedservlets.rest.services;

import com.guicedee.client.IGuiceContext;
import com.zandero.rest.injection.InjectionProvider;

public class GuiceRestInjectionProvider implements InjectionProvider
{

    public GuiceRestInjectionProvider() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getInstance(Class clazz) {
        return IGuiceContext.get(clazz);
    }
}