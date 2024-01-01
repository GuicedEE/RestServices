import com.guicedee.guicedservlets.rest.RestProvidersFilter;
import com.guicedee.guicedservlets.rest.RestResourceProvidersFilter;
import com.guicedee.guicedservlets.rest.RestServicesFilter;
import com.guicedee.guicedservlets.rest.implementations.DefaultClassResourceFilterResource;
import com.guicedee.guicedservlets.rest.implementations.JAXBMarshaller;
import com.guicedee.guicedservlets.services.IGuiceSiteBinder;

module com.guicedee.guicedservlets.rest {
	uses RestResourceProvidersFilter;
	uses RestServicesFilter;
	uses RestProvidersFilter;
	
	provides RestResourceProvidersFilter with DefaultClassResourceFilterResource;
	provides RestServicesFilter with DefaultClassResourceFilterResource;
	
	exports com.guicedee.guicedservlets.rest;
	exports com.guicedee.guicedservlets.rest.services;
	exports com.guicedee.guicedservlets.rest.internal;
	
	
	requires jakarta.xml.bind;
	requires java.xml;
	requires transitive jakarta.ws.rs;
	
	requires static lombok;
	
	requires transitive com.fasterxml.jackson.jakarta.rs.json;
	requires transitive com.guicedee.guicedservlets.undertow;
	
	requires org.apache.cxf;
	requires org.apache.cxf.rest;
	
	requires org.apache.commons.io;
	
	
	provides com.guicedee.guicedinjection.interfaces.IGuicePostStartup with com.guicedee.guicedservlets.rest.services.JaxRsPostStartup;
	provides com.guicedee.guicedservlets.undertow.services.UndertowDeploymentConfigurator with com.guicedee.guicedservlets.rest.implementations.JaxRSUndertowDeploymentConfigurator;
	
	provides IGuiceSiteBinder with com.guicedee.guicedservlets.rest.RestModule;
	provides com.guicedee.guicedinjection.interfaces.IGuicePreStartup with com.guicedee.guicedservlets.rest.services.JaxRsPreStartup;
	provides com.guicedee.guicedinjection.interfaces.IGuiceConfigurator with com.guicedee.guicedservlets.rest.implementations.RestServiceScannerConfig;
	
	exports com.guicedee.guicedservlets.rest.implementations;
	opens com.guicedee.guicedservlets.rest.implementations to com.google.guice, org.apache.cxf;
	opens com.guicedee.guicedservlets.rest to com.google.guice, org.apache.cxf;
	
	provides jakarta.ws.rs.ext.MessageBodyReader with JAXBMarshaller;
	provides jakarta.ws.rs.ext.MessageBodyWriter with JAXBMarshaller;
}
