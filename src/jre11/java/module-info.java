import com.guicedee.guicedinjection.interfaces.IGuiceConfigurator;
import com.guicedee.guicedinjection.interfaces.IGuicePostStartup;
import com.guicedee.guicedinjection.interfaces.IGuicePreStartup;
import com.guicedee.guicedservlets.rest.RestModule;
import com.guicedee.guicedservlets.rest.implementations.JaxRSUndertowDeploymentConfigurator;
import com.guicedee.guicedservlets.rest.implementations.RestServiceScannerConfig;
import com.guicedee.guicedservlets.rest.services.JaxRsPostStartup;
import com.guicedee.guicedservlets.rest.services.JaxRsPreStartup;
import com.guicedee.guicedservlets.services.IGuiceSiteBinder;
import com.guicedee.guicedservlets.undertow.services.UndertowDeploymentConfigurator;

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

	provides IGuicePostStartup with JaxRsPostStartup;
	provides UndertowDeploymentConfigurator with JaxRSUndertowDeploymentConfigurator;

	provides IGuiceSiteBinder with RestModule;
	provides IGuicePreStartup with JaxRsPreStartup;
	provides IGuiceConfigurator with RestServiceScannerConfig;
}
