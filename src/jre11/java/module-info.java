import com.guicedee.guicedservlets.rest.RestProvidersFilter;
import com.guicedee.guicedservlets.rest.RestResourceProvidersFilter;
import com.guicedee.guicedservlets.rest.RestServicesFilter;
import com.guicedee.guicedservlets.rest.implementations.DefaultClassResourceFilterResource;
import com.guicedee.guicedservlets.rest.implementations.JAXBMarshaller;

module com.guicedee.guicedservlets.rest {
    uses RestResourceProvidersFilter;
    uses RestServicesFilter;
    uses RestProvidersFilter;
    provides RestResourceProvidersFilter with DefaultClassResourceFilterResource;

    exports com.guicedee.guicedservlets.rest;
	exports com.guicedee.guicedservlets.rest.services;
	exports com.guicedee.guicedservlets.rest.internal;

	requires java.xml.bind;

	requires transitive com.guicedee.guicedservlets.undertow;
	//JDK 11 Tests
	requires static java.net.http;

	requires transitive org.apache.cxf;
	requires transitive java.ws.rs;
	requires transitive org.apache.commons.io;
	requires transitive com.fasterxml.jackson.module.paramnames;

	provides com.guicedee.guicedinjection.interfaces.IGuicePostStartup with com.guicedee.guicedservlets.rest.services.JaxRsPostStartup;
	provides com.guicedee.guicedservlets.undertow.services.UndertowDeploymentConfigurator with com.guicedee.guicedservlets.rest.implementations.JaxRSUndertowDeploymentConfigurator;

	provides com.guicedee.guicedservlets.services.IGuiceSiteBinder with com.guicedee.guicedservlets.rest.RestModule;
	provides com.guicedee.guicedinjection.interfaces.IGuicePreStartup with com.guicedee.guicedservlets.rest.services.JaxRsPreStartup;
	provides com.guicedee.guicedinjection.interfaces.IGuiceConfigurator with com.guicedee.guicedservlets.rest.implementations.RestServiceScannerConfig;

	opens com.guicedee.guicedservlets.rest.implementations to com.google.guice, org.apache.cxf;
	opens com.guicedee.guicedservlets.rest to com.google.guice, org.apache.cxf;

	provides javax.ws.rs.ext.MessageBodyReader with JAXBMarshaller;
	provides javax.ws.rs.ext.MessageBodyWriter with JAXBMarshaller;
}
