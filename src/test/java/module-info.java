module guiced.rest.services.test {
	requires com.guicedee.guicedservlets.rest;
	
	requires java.net.http;
	
	requires org.junit.jupiter.api;
	requires org.slf4j;
	requires org.slf4j.simple;
	requires static lombok;
	
	requires jakarta.ws.rs;
	requires com.google.guice;
	requires com.guicedee.guicedinjection;
	requires com.guicedee.guicedservlets.undertow;
	
	opens com.guicedee.guicedservlets.rest.test to org.junit.platform.commons,com.google.guice,org.apache.cxf,com.fasterxml.jackson.databind;
}