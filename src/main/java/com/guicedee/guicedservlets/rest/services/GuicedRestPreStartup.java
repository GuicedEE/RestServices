package com.guicedee.guicedservlets.rest.services;

import com.guicedee.client.services.lifecycle.IGuicePreStartup;
import io.vertx.core.Future;

import java.util.Collections;
import java.util.List;

/**
 * Pre-startup hook for REST module initialization.
 *
 * <p>Currently no pre-startup tasks are required, so the implementation returns
 * an empty list of futures.</p>
 */
public class GuicedRestPreStartup implements IGuicePreStartup<GuicedRestPreStartup>
{
    /**
     * Executes pre-startup logic before the Vert.x REST services are started.
     *
     * @return A list of futures representing startup tasks
     */
    @Override
    public List<Future<Boolean>> onStartup()
    {
        // No pre-startup tasks needed for our Jakarta WS implementation
        return Collections.emptyList();
    }
}
