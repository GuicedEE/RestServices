import com.guicedee.client.services.lifecycle.IGuiceModule;
import com.guicedee.rest.test.RestTestBinding;

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

	exports com.guicedee.rest.test;
	exports com.guicedee.rest.test.verticle1;
	exports com.guicedee.rest.test.verticle2;
	exports com.guicedee.rest.test.verticle3;
	exports com.guicedee.rest.test.verticle4;

	opens com.guicedee.rest.test to org.junit.platform.commons,com.google.guice,com.fasterxml.jackson.databind;
	opens com.guicedee.rest.test.verticle1 to org.junit.platform.commons,com.google.guice,com.fasterxml.jackson.databind;
	opens com.guicedee.rest.test.verticle2 to org.junit.platform.commons,com.google.guice,com.fasterxml.jackson.databind;
	opens com.guicedee.rest.test.verticle3 to org.junit.platform.commons,com.google.guice,com.fasterxml.jackson.databind;
	opens com.guicedee.rest.test.verticle4 to org.junit.platform.commons,com.google.guice,com.fasterxml.jackson.databind;

}
