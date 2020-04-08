module com.guicedee.guicedservlets.rest {

	exports com.guicedee.guicedservlets.rest;
	exports com.guicedee.guicedservlets.rest.services;
	exports com.guicedee.guicedservlets.rest.internal;

	requires transitive com.guicedee.guicedservlets.undertow;
	requires transitive java.ws.rs;

	//JDK 11 Tests
	requires static java.net.http;
	requires transitive org.apache.cxf;

	requires transitive com.fasterxml.jackson.jaxrs.json;
	requires transitive com.fasterxml.jackson.module.paramnames;

	provides com.guicedee.guicedinjection.interfaces.IGuicePostStartup with com.guicedee.guicedservlets.rest.services.JaxRsPostStartup;
	provides com.guicedee.guicedservlets.undertow.services.UndertowDeploymentConfigurator with com.guicedee.guicedservlets.rest.implementations.JaxRSUndertowDeploymentConfigurator;

	provides com.guicedee.guicedservlets.services.IGuiceSiteBinder with com.guicedee.guicedservlets.rest.RestModule;
	provides com.guicedee.guicedinjection.interfaces.IGuicePreStartup with com.guicedee.guicedservlets.rest.services.JaxRsPreStartup;
	provides com.guicedee.guicedinjection.interfaces.IGuiceConfigurator with com.guicedee.guicedservlets.rest.implementations.RestServiceScannerConfig;
}
