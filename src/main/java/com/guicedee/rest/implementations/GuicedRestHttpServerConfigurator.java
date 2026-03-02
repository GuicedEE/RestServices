package com.guicedee.rest.implementations;

import com.guicedee.vertx.web.spi.VertxHttpServerConfigurator;
import io.vertx.core.http.HttpServer;
import lombok.extern.log4j.Log4j2;

/**
 * Configures Vert.x HTTP/HTTPS servers for REST endpoints.
 *
 * <p>This implementation of {@link VertxHttpServerConfigurator} allows for
 * REST-specific server customizations.</p>
 */
@Log4j2
public class GuicedRestHttpServerConfigurator implements VertxHttpServerConfigurator
{
    @Override
    public HttpServer builder(HttpServer builder)
    {
        log.info("Configuring REST HTTP Server");
        return builder;
    }
}
