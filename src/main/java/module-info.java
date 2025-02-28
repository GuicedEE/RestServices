import com.guicedee.guicedservlets.rest.implementations.GuicedRestHttpServerConfigurator;
import com.guicedee.guicedservlets.rest.implementations.PackageRejectListScanner;
import com.guicedee.guicedservlets.rest.implementations.RestModule;
import com.guicedee.vertx.spi.*;
import com.guicedee.vertx.web.spi.*;
import com.guicedee.guicedinjection.interfaces.*;
import com.guicedee.guicedservlets.websockets.services.*;
import com.guicedee.guicedservlets.rest.services.*;

module com.guicedee.rest {
    uses com.guicedee.vertx.web.spi.VertxRouterConfigurator;
    uses com.guicedee.vertx.web.spi.VertxHttpServerConfigurator;
    uses com.guicedee.vertx.web.spi.VertxHttpServerOptionsConfigurator;

    requires transitive com.guicedee.vertx.web;
    requires com.zandero.rest.vertx;

    exports com.guicedee.guicedservlets.rest.services;

	requires static lombok;

    provides com.guicedee.guicedinjection.interfaces.IGuiceConfigurator with com.guicedee.guicedservlets.rest.implementations.RestServiceScannerConfig;
	provides IGuicePreStartup with GuicedRestPreStartup;
	provides IGuiceModule with RestModule;
	provides IPackageRejectListScanner with PackageRejectListScanner;


	opens com.guicedee.guicedservlets.rest.services to com.google.guice;
	opens com.guicedee.guicedservlets.rest.implementations to com.google.guice;

	provides VerticleStartup with com.guicedee.guicedservlets.rest.implementations.GuicedRestHttpServerConfigurator;
}
