import com.guicedee.guicedinjection.interfaces.IGuiceScanModuleInclusions;
import com.guicedee.guicedservlets.rest.implementations.IncludeModuleInScans;
import com.guicedee.guicedservlets.rest.implementations.JAXBMarshaller;
import com.guicedee.guicedservlets.rest.implementations.RestModule;
import com.guicedee.guicedservlets.services.IGuiceSiteBinder;

module com.guicedee.guicedservlets.rest {

	exports com.guicedee.guicedservlets.rest;
	exports com.guicedee.guicedservlets.rest.annotations;
	exports com.guicedee.guicedservlets.rest.services;
	
	requires jakarta.xml.bind;
	requires java.xml;
	
	requires static lombok;
	
	requires transitive com.fasterxml.jackson.jakarta.rs.json;
	requires com.guicedee.guicedservlets;
	
	requires com.guicedee.jsonrepresentation;
	requires com.guicedee.xmlrepresentation;
	
	requires org.apache.cxf;
	requires org.apache.cxf.rest;
	requires org.apache.commons.io;
	
	provides com.guicedee.guicedinjection.interfaces.IGuicePostStartup with com.guicedee.guicedservlets.rest.services.JaxRsPostStartup;
	
	provides IGuiceSiteBinder with RestModule;
	provides IGuiceScanModuleInclusions with IncludeModuleInScans;
	
	provides com.guicedee.guicedinjection.interfaces.IGuiceConfigurator with com.guicedee.guicedservlets.rest.implementations.RestServiceScannerConfig;
	
	opens com.guicedee.guicedservlets.rest.implementations to com.google.guice, org.apache.cxf;
	opens com.guicedee.guicedservlets.rest to com.google.guice, org.apache.cxf;
	opens com.guicedee.guicedservlets.rest.annotations to com.google.guice, org.apache.cxf;

	
	provides jakarta.ws.rs.ext.MessageBodyReader with JAXBMarshaller;
	provides jakarta.ws.rs.ext.MessageBodyWriter with JAXBMarshaller;
}
