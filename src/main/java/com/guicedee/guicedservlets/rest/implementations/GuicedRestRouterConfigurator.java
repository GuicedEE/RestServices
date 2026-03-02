package com.guicedee.guicedservlets.rest.implementations;

import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedservlets.rest.pathing.JakartaWsScanner;
import com.guicedee.guicedservlets.rest.pathing.PathHandler;
import com.guicedee.vertx.web.spi.VertxRouterConfigurator;
import io.github.classgraph.ScanResult;
import io.vertx.ext.web.Router;
import lombok.extern.log4j.Log4j2;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configures the Vert.x router for REST services.
 * This includes setting up CORS and request logging.
 */
@Log4j2
public class GuicedRestRouterConfigurator implements VertxRouterConfigurator<GuicedRestRouterConfigurator> {

    private String packageFilter;

    public GuicedRestRouterConfigurator setPackageFilter(String packageFilter) {
        this.packageFilter = packageFilter;
        return this;
    }

    @Override
    public Router builder(Router router) {
        log.info("Configuring Rest Router" + (packageFilter != null ? " for package: " + packageFilter : ""));

        // Scan for resource classes to find the base paths
        ScanResult scanResult = IGuiceContext.instance().getScanResult();
        List<Class<?>> resourceClasses = JakartaWsScanner.scanForResourceClasses(scanResult, packageFilter);
        Set<String> basePaths = new HashSet<>();
        for (Class<?> resourceClass : resourceClasses) {
            String basePath = PathHandler.getBasePath(resourceClass);
            if (basePath != null && !basePath.isEmpty()) {
                if (!basePath.startsWith("/")) {
                    basePath = "/" + basePath;
                }
                // We want to match the base path and anything under it
                if (basePath.endsWith("/")) {
                    basePaths.add(basePath + "*");
                } else if (!basePath.equals("/")) {
                    basePaths.add(basePath);
                    basePaths.add(basePath + "/*");
                } else {
                    // If it's just "/", we don't want to match EVERYTHING as REST
                    // but we might have a resource at "/"
                    basePaths.add("/");
                }
            }
        }

        // Add a request logger to log only REST requests
        if (basePaths.isEmpty()) {
            log.debug("No REST resource classes found, skipping logger and CORS configuration");
        } else {
            for (String path : basePaths) {
                log.debug("Adding REST logger and CORS for path: {}", path);
                router.route(path).order(-1).handler(ctx -> {
                    log.debug("REST Request received: " + ctx.request().method() + " " + ctx.request().path());
                    ctx.next();
                });
            }
            // Configure CORS
            CorsHandlerConfigurator.configureCors(router, null);
        }

        return router;
    }

    @Override
    public Integer sortOrder() {
        // Infrastructure level
        return 100;
    }
}
