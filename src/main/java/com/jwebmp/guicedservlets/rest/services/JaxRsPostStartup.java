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
	private static Undertow server;

	public void createBus()  {
		Bus bus = BusFactory.newInstance().createBus();
		BusFactory.setDefaultBus(bus);
	}

	@Override
	public void postLoad() {
		if(runSeparately) {
			createBus();

			JaxRsPreStartup.deploymentManager.deploy();

			PathHandler pathToListenOn = null;
			try {
				pathToListenOn = Handlers
						.path(Handlers.redirect("/"))
						.addPrefixPath("/", JaxRsPreStartup.deploymentManager.start());
			} catch (ServletException e) {
				log.log(Level.SEVERE, "Unable to set paths to listen on", e);
			}

			if (server == null) {
				server = Undertow.builder()
								 .addHttpListener(RESTContext.port, RESTContext.listeningAddress)
								 .setHandler(pathToListenOn)
								 .build();
			}
			server.start();
			log.info("Rest Server Started from RESTContext.deploymentManager");
		}
	}

	public static Undertow getServer() {
		return server;
	}

	public static void setServer(Undertow server) {
		JaxRsPostStartup.server = server;
	}

}
