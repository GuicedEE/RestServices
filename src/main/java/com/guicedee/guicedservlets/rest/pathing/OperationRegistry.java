package com.guicedee.guicedservlets.rest.pathing;

import com.google.inject.Inject;
import com.guicedee.client.IGuiceContext;
import com.guicedee.vertx.web.spi.VertxRouterConfigurator;
import io.github.classgraph.ScanResult;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.internal.ContextInternal;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

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
public class OperationRegistry implements VertxRouterConfigurator<OperationRegistry> {
    private static final Logger logger = LogManager.getLogger(OperationRegistry.class);
    private final Set<String> registeredRoutes = new HashSet<>();

    @Inject
    private Vertx vertx;

    @Override
    public Integer sortOrder() {
        return 200;
    }

    /**
     * Builds a router by registering all discovered resource classes.
     *
     * @param builder The router to configure
     * @return The configured router
     */
    @Override
    public Router builder(Router builder) {
        // Get the scan result from IGuiceContext
        ScanResult scanResult = IGuiceContext.instance().getScanResult();

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
     * @param router        The router to register with
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
     * @param router       The router to register with
     * @param resourceInfo The resource metadata
     * @param method       The resource method to register
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

            logger.debug("Registered route: " + httpMethod + " " + fullPath);
        } catch (Exception e) {
            logger.error("Error registering resource method: " + method.getName(), e);
        }
    }

    /**
     * Executes the resource method and renders a response.
     *
     * <p>Each request receives its own <strong>duplicated Vert.x context</strong> (via
     * {@code ContextInternal.duplicate()}) so that Hibernate Reactive sessions, Mutiny
     * subscriptions, and any other context-local state are fully isolated per request.
     * The duplicated context runs on the <strong>same event-loop thread</strong> as its
     * parent, which is critical for thread-affinity with the SQL pool connection.</p>
     *
     * <p>When the resource class belongs to a package annotated with
     * {@code @Verticle}, blocking methods are dispatched to that verticle's
     * named worker pool. Reactive methods (returning {@code Uni} or {@code Future})
     * run on the event loop within the duplicated context.</p>
     *
     * @param context      The routing context
     * @param resourceInfo The resource metadata
     * @param method       The resource method to invoke
     */
    private void handleRequest(RoutingContext context, JakartaWsScanner.ResourceInfo resourceInfo, Method method) {
        long startTime = System.currentTimeMillis();
        logger.debug("Handling request: " + context.request().method() + " " + context.request().path());

        context.response().endHandler(v -> {
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Finished response for " + context.request().method() + " " + context.request().path() + " in " + duration + "ms");
        });

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

        // Create a duplicated context for this request. Each HTTP request needs its own
        // isolated Vert.x context-local storage so that Hibernate Reactive sessions,
        // Mutiny subscriptions, and other context-local state are not shared between
        // concurrent requests.
        //
        // ContextInternal.duplicate() creates a child context on the SAME event-loop
        // thread as the parent, which is critical: the SQL pool connection's I/O is
        // bound to a specific event-loop thread, and the session must be pinned to
        // that same thread. A duplicated context preserves the thread while providing
        // isolated local storage.
        //
        // DO NOT use createEventLoopContext() — that assigns to a RANDOM event-loop
        // thread which will differ from the SQL connection's thread, causing HR000069.
        ContextInternal currentContext = (ContextInternal) vertx.getOrCreateContext();
        ContextInternal duplicatedContext = currentContext.duplicate();
        duplicatedContext.runOnContext(v -> {
            // Dispatch through EventLoopHandler so that:
            //  - blocking methods run on the @Verticle worker pool (or default worker pool)
            //  - reactive (Uni/Future) methods run on the event loop
            EventLoopHandler.executeTask(vertx, context, () -> {
                try {
                    logger.debug("Executing method: " + method.getName() + " on class: " + resourceInfo.getResourceClass().getName());

                    // Extract parameters
                    logger.debug("Extracting parameters for method: " + method.getName());
                    Object[] parameters = ParameterExtractor.extractParameters(method, context);

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
                } catch (Throwable e) {
                    logger.error("Error handling request", e);
                    ResponseHandler.handleException(context, e);
                }
            }, method, resourceInfo.getResourceClass());
        });
    }
}
