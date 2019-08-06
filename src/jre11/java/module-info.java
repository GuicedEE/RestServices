module com.jwebmp.guiced.rest {

	exports com.jwebmp.guiced.rest;

	exports com.jwebmp.guiced.rest.internal to com.jwebmp.guiced.swagger;

	requires com.jwebmp.guicedinjection;
	requires com.google.guice.extensions.servlet;
	requires com.google.guice;

	requires javax.servlet.api;

	requires resteasy.client;
	requires resteasy.guice;

	requires com.google.common;

	//Undertow Registrations
	requires static undertow.servlet;
	//JDK 11 Tests
	requires static java.net.http;

	requires com.jwebmp.guicedservlets;
	requires static resteasy.core;
	requires static beta.jboss.jaxrs.api_2_1;

	provides com.jwebmp.guicedservlets.services.IGuiceSiteBinder with com.jwebmp.guiced.rest.RestEasyModule;
	provides io.undertow.servlet.ServletExtension with com.jwebmp.guiced.rest.implementations.RestEasyUndertowServletExtension;
}
