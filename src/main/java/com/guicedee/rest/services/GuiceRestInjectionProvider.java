package com.guicedee.rest.services;

import com.guicedee.client.IGuiceContext;
import com.guicedee.rest.pathing.OperationRegistry;

/**
 * Provides Guice-backed instance creation for REST resources.
 *
 * <p>Used by {@link OperationRegistry}
 * to obtain resource instances that can participate in dependency injection.</p>
 */
public class GuiceRestInjectionProvider
{
    /**
     * Creates a new injection provider.
     */
    public GuiceRestInjectionProvider() {
    }

    /**
     * Gets an instance of a class from the Guice context.
     *
     * @param clazz The class to get an instance of
     * @param <T> The type of the instance
     * @return The injected instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T getInstance(Class<T> clazz) {
        return IGuiceContext.get(clazz);
    }
}
