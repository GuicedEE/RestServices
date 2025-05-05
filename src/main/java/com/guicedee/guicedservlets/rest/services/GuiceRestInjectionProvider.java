package com.guicedee.guicedservlets.rest.services;

import com.guicedee.client.IGuiceContext;

/**
 * Provides dependency injection for Jakarta WS resources.
 * This class is used by the OperationRegistry to get instances of resource classes.
 */
public class GuiceRestInjectionProvider
{
    public GuiceRestInjectionProvider() {
    }

    /**
     * Gets an instance of a class from the Guice context.
     *
     * @param clazz The class to get an instance of
     * @return The instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T getInstance(Class<T> clazz) {
        return IGuiceContext.get(clazz);
    }
}
