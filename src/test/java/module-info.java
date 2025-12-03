import com.guicedee.client.services.lifecycle.IGuiceModule;
import com.guicedee.guicedservlets.rest.test.RestTestBinding;

module guiced.rest.services.test {
	requires com.guicedee.rest;

	requires java.net.http;

	requires org.junit.jupiter.api;
	//requires org.slf4j;
	//requires org.slf4j.simple;
	requires static lombok;

	// Add required dependencies for security tests
	requires io.vertx.auth.common;
	requires jakarta.annotation;


	requires com.google.guice;
	requires com.guicedee.client;

	provides IGuiceModule with RestTestBinding;

	exports com.guicedee.guicedservlets.rest.test;

	opens com.guicedee.guicedservlets.rest.test to org.junit.platform.commons,com.google.guice,com.fasterxml.jackson.databind;
}
