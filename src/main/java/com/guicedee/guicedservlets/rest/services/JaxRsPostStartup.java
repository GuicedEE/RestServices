package com.guicedee.guicedservlets.rest.services;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import com.guicedee.guicedinjection.interfaces.IGuicePostStartup;
import com.guicedee.guicedservlets.rest.RESTContext;
import com.guicedee.logger.LogFactory;

import javax.servlet.ServletException;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.guicedee.guicedservlets.rest.RESTContext.*;

public class JaxRsPostStartup implements IGuicePostStartup<JaxRsPostStartup> {

	private static final Logger log = LogFactory.getLog(JaxRsPostStartup.class);

	public void createBus()  {
		log.fine("Creating Jax-RS Bus");
		Bus bus = BusFactory.newInstance().createBus();
		BusFactory.setDefaultBus(bus);
	}

	@Override
	public void postLoad() {
	}


}
