import com.guicedee.client.services.lifecycle.IGuiceConfigurator;
import com.guicedee.client.services.lifecycle.IGuiceModule;
import com.guicedee.client.services.lifecycle.IGuicePreStartup;
import com.guicedee.client.services.config.IPackageRejectListScanner;
import com.guicedee.rest.implementations.GuicedRestHttpServerConfigurator;
import com.guicedee.rest.implementations.GuicedRestRouterConfigurator;
import com.guicedee.rest.implementations.PackageRejectListScanner;
import com.guicedee.rest.implementations.RestModule;
import com.guicedee.rest.pathing.OperationRegistry;
import com.guicedee.rest.services.GuicedRestPreStartup;
import com.guicedee.rest.services.RestInterceptor;
import com.guicedee.rest.implementations.RestServiceScannerConfig;

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

    exports com.guicedee.rest.services;
    exports com.guicedee.rest.pathing;
    exports com.guicedee.rest.implementations;

	requires static lombok;
    requires org.slf4j;

    provides IGuiceConfigurator with RestServiceScannerConfig;
	provides IGuicePreStartup with GuicedRestPreStartup;
	provides IGuiceModule with RestModule;
	provides IPackageRejectListScanner with PackageRejectListScanner;


    provides com.guicedee.vertx.web.spi.VertxRouterConfigurator with OperationRegistry, GuicedRestRouterConfigurator;

	opens com.guicedee.rest.services to com.google.guice;
	opens com.guicedee.rest.implementations to com.google.guice;
	opens com.guicedee.rest.pathing to com.google.guice, guiced.rest.services.test;

	provides com.guicedee.vertx.web.spi.VertxHttpServerConfigurator with GuicedRestHttpServerConfigurator;


    uses RestInterceptor;
}
