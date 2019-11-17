module com.guicedee.guicedservlets.rest {

	exports com.guicedee.guicedservlets.rest;

	exports com.guicedee.guicedservlets.rest.internal;

	requires com.guicedee.guicedinjection;
	requires com.google.guice.extensions.servlet;
	requires com.google.guice;

	requires com.guicedee.guicedservlets.undertow;

	requires com.fasterxml.jackson.databind;

	requires javax.servlet.api;

	requires com.google.common;

	//Undertow Registrations
	requires undertow.servlet;
	requires undertow.core;
	//JDK 11 Tests
	requires static java.net.http;

	requires com.guicedee.guicedservlets;

	requires java.ws.rs;

	requires io.github.classgraph;
	requires org.apache.cxf;
	requires aopalliance;
	requires javax.inject;
	requires com.fasterxml.jackson.jaxrs.json;

	provides com.guicedee.guicedinjection.interfaces.IGuicePostStartup with com.guicedee.guicedservlets.rest.services.JaxRsPostStartup;
	provides com.guicedee.guicedservlets.undertow.services.UndertowDeploymentConfigurator with com.guicedee.guicedservlets.rest.implementations.JaxRSUndertowDeploymentConfigurator;

	provides com.guicedee.guicedservlets.services.IGuiceSiteBinder with com.guicedee.guicedservlets.rest.RestModule;
	provides com.guicedee.guicedinjection.interfaces.IGuicePreStartup with com.guicedee.guicedservlets.rest.services.JaxRsPreStartup;
	provides com.guicedee.guicedinjection.interfaces.IGuiceConfigurator with com.guicedee.guicedservlets.rest.implementations.RestServiceScannerConfig;
}
