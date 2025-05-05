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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registers Jakarta WS annotated classes and methods with the Vert.x router.
 */
public class OperationRegistry implements VertxRouterConfigurator
{
    private static final Logger logger = Logger.getLogger(OperationRegistry.class.getName());
    private static final Set<String> registeredRoutes = new HashSet<>();

    @Inject
    private Vertx vertx;

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
     * Registers a resource class with the router.
     *
     * @param router The router
     * @param resourceClass The resource class
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
            logger.log(Level.SEVERE, "Error registering resource class: " + resourceClass.getName(), e);
        }
    }

    /**
     * Registers a resource method with the router.
     *
     * @param router The router
     * @param resourceInfo The resource info
     * @param method The resource method
     */
    private void registerResourceMethod(Router router, JakartaWsScanner.ResourceInfo resourceInfo, Method method) {
        try {
            // Get HTTP method
            HttpMethod httpMethod = HttpMethodHandler.getHttpMethod(method);
            if (httpMethod == null) {
                logger.warning("No HTTP method annotation found for method: " + method.getName());
                return;
            }

            // Get full path
            String fullPath = PathHandler.getFullPath(resourceInfo.getResourceClass(), method);

            // Create a unique key for this route
            String routeKey = httpMethod.name() + ":" + fullPath;

            // Check if this route has already been registered
            if (registeredRoutes.contains(routeKey)) {
                logger.fine("Route already registered, skipping: " + httpMethod + " " + fullPath);
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
            logger.log(Level.SEVERE, "Error registering resource method: " + method.getName(), e);
        }
    }

    /**
     * Handles a request.
     *
     * @param context The routing context
     * @param resourceInfo The resource info
     * @param method The resource method
     */
    private void handleRequest(RoutingContext context, JakartaWsScanner.ResourceInfo resourceInfo, Method method) {
        logger.fine("Handling request: " + context.request().method() + " " + context.request().path());

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
                logger.fine("Executing method: " + method.getName() + " on class: " + resourceInfo.getResourceClass().getName());

                // Extract parameters
                logger.fine("Extracting parameters for method: " + method.getName());
                Object[] parameters = ParameterExtractor.extractParameters(method, context);

                // Log parameters at FINER level
                if (logger.isLoggable(Level.FINER)) {
                    for (int i = 0; i < parameters.length; i++) {
                        logger.finer("Parameter " + i + ": " + (parameters[i] != null ? parameters[i].toString() : "null"));
                    }
                }

                // Get resource instance
                Object instance = resourceInfo.getResourceInstance();
                logger.fine("Resource instance: " + (instance != null ? instance.getClass().getName() : "null"));

                // Invoke the method
                logger.fine("Invoking method: " + method.getName());
                Object result = method.invoke(instance, parameters);
                logger.fine("Method execution completed, result: " + (result != null ? result.toString() : "null"));

                // Process the response
                logger.fine("Processing response");
                ResponseHandler.processResponse(context, result, method);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error handling request", e);
                ResponseHandler.handleException(context, e);
            }
        }, method);
    }
}
