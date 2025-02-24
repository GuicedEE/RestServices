import com.guicedee.guicedservlets.rest.implementations.GuicedRestHttpServerConfigurator;
import com.guicedee.guicedservlets.rest.implementations.PackageRejectListScanner;
import com.guicedee.guicedservlets.rest.implementations.RestModule;
import com.guicedee.guicedservlets.rest.services.*;
import com.guicedee.vertx.spi.*;
import com.guicedee.guicedinjection.interfaces.*;

module com.guicedee.rest {
    uses VertxHttpServerOptionsConfigurator;
    uses VertxHttpServerConfigurator;
    uses VertxRouterConfigurator;

    exports com.guicedee.guicedservlets.rest.services;

	requires static lombok;

	requires com.guicedee.jsonrepresentation;
	requires com.guicedee.xmlrepresentation;

	requires transitive com.guicedee.vertx;
    requires com.zandero.rest.vertx;

	requires transitive com.guicedee.client;

    provides com.guicedee.guicedinjection.interfaces.IGuiceConfigurator with com.guicedee.guicedservlets.rest.implementations.RestServiceScannerConfig;
	provides IGuicePreStartup with GuicedRestPreStartup;
	provides IGuiceModule with RestModule;
	provides IPackageRejectListScanner with PackageRejectListScanner;


	opens com.guicedee.guicedservlets.rest.services to com.google.guice;
	opens com.guicedee.guicedservlets.rest.implementations to com.google.guice;

	provides VerticleStartup with GuicedRestHttpServerConfigurator;
}
