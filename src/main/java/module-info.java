import com.guicedee.guicedinjection.interfaces.*;
import com.guicedee.guicedservlets.rest.implementations.RestModule;
import com.guicedee.guicedservlets.rest.implementations.VertXRestRouter;
import com.guicedee.guicedservlets.rest.services.GuicedRestPreStartup;
import com.guicedee.vertx.spi.VertxRouterConfigurator;

module com.guicedee.guicedservlets.rest {

	exports com.guicedee.guicedservlets.rest.services;
	requires transitive jakarta.ws.rs;
	
	requires static lombok;
	requires com.guicedee.jsonrepresentation;
	requires com.guicedee.xmlrepresentation;

	requires com.guicedee.client;
    requires io.vertx.rest;
	requires io.vertx;
	requires guiced.vertx;

	provides com.guicedee.guicedinjection.interfaces.IGuiceConfigurator with com.guicedee.guicedservlets.rest.implementations.RestServiceScannerConfig;
	provides IGuicePreStartup with GuicedRestPreStartup;
	provides VertxRouterConfigurator with VertXRestRouter;
	provides IGuiceModule with RestModule;

	opens com.guicedee.guicedservlets.rest.services to com.google.guice;
	opens com.guicedee.guicedservlets.rest.implementations to com.google.guice;
}
