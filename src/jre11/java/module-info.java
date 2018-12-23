import com.jwebmp.guiced.rest.RestEasyModule;
import com.jwebmp.guiced.rest.implementations.RestEasyUndertowServletExtension;
import io.undertow.servlet.ServletExtension;

module com.jwebmp.guiced.rest {

	exports com.jwebmp.guiced.rest;
	//exports com.jwebmp.guiced.rest.implementations;


	requires com.jwebmp.guicedinjection;
	requires com.google.guice.extensions.servlet;
	requires com.google.guice;

	requires javax.servlet.api;
	requires beta.jboss.jaxrs.api_2_1;
	requires resteasy.client;
	requires resteasy.guice;

	requires com.google.common;
	requires resteasy.core;

	//Undertow Registrations
	requires static undertow.servlet;

	requires com.jwebmp.guicedservlets;

	provides com.jwebmp.guicedservlets.services.IGuiceSiteBinder with RestEasyModule;
	provides ServletExtension with RestEasyUndertowServletExtension;
}
