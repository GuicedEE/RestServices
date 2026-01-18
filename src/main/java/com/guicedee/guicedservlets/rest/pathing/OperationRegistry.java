package com.guicedee.guicedservlets.rest.pathing;

import com.google.inject.Inject;
import com.guicedee.client.IGuiceContext;
import com.guicedee.vertx.web.spi.VertxRouterConfigurator;
import io.github.classgraph.ScanResult;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Discovers and registers Jakarta REST endpoint methods with a Vert.x router.
 *
 * <p>The registry scans for resource classes via {@link JakartaWsScanner}, maps
 * HTTP method annotations to Vert.x routes, and wires request handling to the
 * request lifecycle utilities such as {@link ParameterExtractor},
 * {@link EventLoopHandler}, and {@link ResponseHandler}.</p>
 */
public class OperationRegistry implements VertxRouterConfigurator<OperationRegistry>
{
    private static final Logger logger = LogManager.getLogger(OperationRegistry.class);
    private static final Set<String> registeredRoutes = new HashSet<>();

    @Inject
    private Vertx vertx;

    @Override
    public Integer sortOrder()
    {
        return 100;
    }

    /**
     * Builds a router by registering all discovered resource classes.
     *
     * @param builder The router to configure
     * @return The configured router
     */
    @Override
    public Router builder(Router builder)
    {
        // Get the scan result from IGuiceContext
        ScanResult scanResult = IGuiceContext.instance().getScanResult();

        // Ensure body handler is registered
        builder.route().handler(BodyHandler.create());

        // Scan for resource classes
        List<Class<?>> resourceClasses = JakartaWsScanner.scanForResourceClasses(scanResult);

        // Register each resource class
        for (Class<?> resourceClass : resourceClasses) {
            registerResourceClass(builder, resourceClass);
        }

        return builder;
    }

    /**
     * Registers every resource method from the provided resource class.
     *
     * @param router The router to register with
     * @param resourceClass The resource class to register
     */
    private void registerResourceClass(Router router, Class<?> resourceClass) {
        try {
            // Get resource info
            JakartaWsScanner.ResourceInfo resourceInfo = JakartaWsScanner.getResourceInfo(resourceClass);

            // Register each resource method
            for (Method method : resourceInfo.getResourceMethods()) {
                registerResourceMethod(router, resourceInfo, method);
            }
        } catch (Exception e) {
            logger.error("Error registering resource class: " + resourceClass.getName(), e);
        }
    }

    /**
     * Registers a single resource method and binds the request handler.
     *
     * @param router The router to register with
     * @param resourceInfo The resource metadata
     * @param method The resource method to register
     */
    private void registerResourceMethod(Router router, JakartaWsScanner.ResourceInfo resourceInfo, Method method) {
        try {
            // Get HTTP method
            HttpMethod httpMethod = HttpMethodHandler.getHttpMethod(method);
            if (httpMethod == null) {
                logger.warn("No HTTP method annotation found for method: " + method.getName());
                return;
            }

            // Get full path
            String fullPath = PathHandler.getFullPath(resourceInfo.getResourceClass(), method);

            // Create a unique key for this route
            String routeKey = httpMethod.name() + ":" + fullPath;

            // Check if this route has already been registered
            if (registeredRoutes.contains(routeKey)) {
                logger.debug("Route already registered, skipping: " + httpMethod + " " + fullPath);
                return;
            }

            // Add to the set of registered routes
            registeredRoutes.add(routeKey);

            // Convert Jakarta WS path parameters {name} to Vert.x path parameters :name
            String vertxPath = fullPath.replaceAll("\\{([^/]+?)\\}", ":$1");

            // Create route
            router.route(httpMethod, vertxPath).handler(context -> handleRequest(context, resourceInfo, method));

            logger.info("Registered route: " + httpMethod + " " + fullPath);
        } catch (Exception e) {
            logger.error("Error registering resource method: " + method.getName(), e);
        }
    }

    /**
     * Executes the resource method and renders a response.
     *
     * <p>This method performs authentication checks, extracts parameters,
     * invokes the resource method, and delegates the response rendering.</p>
     *
     * @param context The routing context
     * @param resourceInfo The resource metadata
     * @param method The resource method to invoke
     */
    private void handleRequest(RoutingContext context, JakartaWsScanner.ResourceInfo resourceInfo, Method method) {
        logger.debug("Handling request: " + context.request().method() + " " + context.request().path());

        // Check authentication and authorization
        if (SecurityHandler.requiresAuthentication(resourceInfo.getResourceClass(), method)) {
            if (context.user() == null) {
                context.response().setStatusCode(401).end("Authentication required");
                return;
            }

            if (!SecurityHandler.isAuthorized(context, resourceInfo.getResourceClass(), method)) {
                context.response().setStatusCode(403).end("Not authorized");
                return;
            }
        }

        // Execute the method on the appropriate thread
        EventLoopHandler.executeTask(vertx, context, () -> {
            try {
                logger.debug("Executing method: " + method.getName() + " on class: " + resourceInfo.getResourceClass().getName());

                // Extract parameters
                logger.debug("Extracting parameters for method: " + method.getName());
                Object[] parameters = ParameterExtractor.extractParameters(method, context);

                // Log parameters at FINER level
                if (logger.isTraceEnabled()) {
                    for (int i = 0; i < parameters.length; i++) {
                        logger.trace("Parameter " + i + ": " + (parameters[i] != null ? parameters[i].toString() : "null"));
                    }
                }

                // Get resource instance
                Object instance = resourceInfo.getResourceInstance();
                logger.debug("Resource instance: " + (instance != null ? instance.getClass().getName() : "null"));

                // Invoke the method
                logger.debug("Invoking method: " + method.getName());
                Object result = method.invoke(instance, parameters);
                logger.debug("Method execution completed, result: " + (result != null ? result.toString() : "null"));

                // Process the response
                logger.debug("Processing response");
                ResponseHandler.processResponse(context, result, method);
            } catch (Exception e) {
                logger.error("Error handling request", e);
                ResponseHandler.handleException(context, e);
            }
        }, method);
    }
}
