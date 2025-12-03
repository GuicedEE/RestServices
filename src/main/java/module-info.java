import com.guicedee.client.services.lifecycle.IGuiceConfigurator;
import com.guicedee.client.services.lifecycle.IGuiceModule;
import com.guicedee.client.services.lifecycle.IGuicePreStartup;
import com.guicedee.client.services.config.IPackageRejectListScanner;
import com.guicedee.guicedservlets.rest.implementations.PackageRejectListScanner;
import com.guicedee.guicedservlets.rest.implementations.RestModule;
import com.guicedee.guicedservlets.rest.pathing.OperationRegistry;
import com.guicedee.guicedservlets.rest.services.GuicedRestPreStartup;
import com.guicedee.guicedservlets.rest.services.RestInterceptor;
import com.guicedee.vertx.spi.VerticleStartup;

module com.guicedee.rest {
    uses com.guicedee.vertx.web.spi.VertxRouterConfigurator;
    uses com.guicedee.vertx.web.spi.VertxHttpServerConfigurator;
    uses com.guicedee.vertx.web.spi.VertxHttpServerOptionsConfigurator;
    uses jakarta.ws.rs.ext.ExceptionMapper;

    requires transitive com.guicedee.vertx.web;

    // Add Vert.x auth modules
    requires transitive io.vertx.auth.common;

    requires transitive jakarta.ws.rs;
    requires jakarta.annotation;

    exports com.guicedee.guicedservlets.rest.services;
    exports com.guicedee.guicedservlets.rest.pathing;
    exports com.guicedee.guicedservlets.rest.implementations;

	requires static lombok;
    requires org.slf4j;

    provides IGuiceConfigurator with com.guicedee.guicedservlets.rest.implementations.RestServiceScannerConfig;
	provides IGuicePreStartup with GuicedRestPreStartup;
	provides IGuiceModule with RestModule;
	provides IPackageRejectListScanner with PackageRejectListScanner;


    provides com.guicedee.vertx.web.spi.VertxRouterConfigurator with OperationRegistry;

	opens com.guicedee.guicedservlets.rest.services to com.google.guice;
	opens com.guicedee.guicedservlets.rest.implementations to com.google.guice;
	opens com.guicedee.guicedservlets.rest.pathing to com.google.guice, guiced.rest.services.test;

	provides VerticleStartup with com.guicedee.guicedservlets.rest.implementations.GuicedRestHttpServerConfigurator;

    uses jakarta.ws.rs.ext.MessageBodyWriter;
    uses jakarta.ws.rs.ext.MessageBodyReader;

    uses RestInterceptor;
}
