package com.guicedee.guicedservlets.rest.services;

import com.guicedee.guicedinjection.interfaces.IGuicePreStartup;
import io.vertx.core.Future;

import java.util.Collections;
import java.util.List;

/**
 * Performs pre-startup tasks for the Jakarta WS implementation.
 */
public class GuicedRestPreStartup implements IGuicePreStartup<GuicedRestPreStartup>
{
    @Override
    public List<Future<Boolean>> onStartup()
    {
        // No pre-startup tasks needed for our Jakarta WS implementation
        return Collections.emptyList();
    }
}
