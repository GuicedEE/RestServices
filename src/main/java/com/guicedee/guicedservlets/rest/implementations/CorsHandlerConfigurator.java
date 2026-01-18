package com.guicedee.guicedservlets.rest.implementations;

import com.guicedee.client.Environment;
import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedservlets.rest.pathing.JakartaWsScanner;
import com.guicedee.guicedservlets.rest.services.Cors;
import io.github.classgraph.ScanResult;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Path;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Configures CORS handling for REST services.
 *
 * <p>Configuration can be supplied via {@link Cors} annotations or overridden
 * by environment variables and system properties. When multiple annotations are
 * present, the most specific annotation (method over class over verticle) is used.</p>
 */
@Log4j2
public class CorsHandlerConfigurator {

    /**
     * Configures CORS handling for the given router based on the Cors annotations found at different levels.
     *
     * @param router The router to configure
     * @param verticleAnnotation The Cors annotation on the verticle class, if present
     */
    public static void configureCors(Router router, Cors verticleAnnotation) {
        // Scan for resource classes with CORS annotations
        List<Class<?>> resourceClasses = scanForResourceClasses();

        // Collect all CORS annotations from different levels
        Map<String, Cors> corsAnnotations = collectCorsAnnotations(verticleAnnotation, resourceClasses);

        // If no CORS annotations were found, check if CORS is enabled via environment variables
        if (corsAnnotations.isEmpty()) {
            boolean corsEnabled = Boolean.parseBoolean(Environment.getSystemPropertyOrEnvironment("REST_CORS_ENABLED", "false"));
            if (!corsEnabled) {
                log.debug("CORS is disabled (no annotations found and not enabled via environment)");
                return;
            }

            // Create a default CORS handler from environment variables
            configureCorsHandler(router, null);
            return;
        }

        // Configure CORS handlers for each path
        for (Map.Entry<String, Cors> entry : corsAnnotations.entrySet()) {
            String path = entry.getKey();
            Cors annotation = entry.getValue();

            // Check if CORS is enabled for this annotation
            boolean corsEnabled = isCorsEnabled(annotation);
            if (!corsEnabled) {
                log.debug("CORS is disabled for path: " + path);
                continue;
            }

            // Configure CORS handler for this path
            configureCorsHandler(router, annotation, path);
        }
    }

    /**
     * Configures a CORS handler for the given router and annotation.
     *
     * @param router The router to configure
     * @param corsAnnotation The Cors annotation, if present
     * @param path The path to apply the CORS handler to, or null for all paths
     */
    private static void configureCorsHandler(Router router, Cors corsAnnotation, String... path) {
        // Get allowed origins
        Set<String> allowedOrigins = getAllowedOrigins(corsAnnotation);

        // Get allowed methods
        Set<HttpMethod> allowedMethods = getAllowedMethods(corsAnnotation);

        // Get allowed headers
        Set<String> allowedHeaders = getAllowedHeaders(corsAnnotation);

        // Get allow credentials
        boolean allowCredentials = getAllowCredentials(corsAnnotation);

        // Get max age
        int maxAgeSeconds = getMaxAgeSeconds(corsAnnotation);

        // Create the CORS handler
        CorsHandler corsHandler = CorsHandler.create()
                .allowedHeaders(allowedHeaders)
                .allowedMethods(allowedMethods)
                .allowCredentials(allowCredentials)
                .maxAgeSeconds(maxAgeSeconds);

        // Add allowed origins
        for (String origin : allowedOrigins) {
            corsHandler.addOrigin(origin);
            log.debug("Added CORS origin: " + origin);
        }

        // Add the handler to the router
        if (path == null || path.length == 0 || path[0] == null) {
            router.route().handler(corsHandler);
            log.debug("Added CORS handler to all routes");
        } else {
            for (String p : path) {
                if (p != null && !p.isEmpty()) {
                    // Add handler for the exact path
                    router.route(p).handler(corsHandler);
                    log.debug("Added CORS handler to exact path: " + p);

                    // Add handler for subpaths
                    router.route(p + "/*").handler(corsHandler);
                    log.debug("Added CORS handler to path with wildcard: " + p + "/*");
                }
            }
        }
    }

    /**
     * Scans for resource classes with Jakarta REST annotations.
     *
     * @return A list of resource classes
     */
    private static List<Class<?>> scanForResourceClasses() {
        ScanResult scanResult = IGuiceContext.instance().getScanResult();
        return JakartaWsScanner.scanForResourceClasses(scanResult);
    }

    /**
     * Collects CORS annotations from different levels (verticle, application, class, method).
     *
     * @param verticleAnnotation The Cors annotation on the verticle class, if present
     * @param resourceClasses The resource classes to check for CORS annotations
     * @return A map of paths to CORS annotations
     */
    private static Map<String, Cors> collectCorsAnnotations(Cors verticleAnnotation, List<Class<?>> resourceClasses) {
        Map<String, Cors> corsAnnotations = new HashMap<>();

        // Add verticle annotation (applies to all paths)
        if (verticleAnnotation != null) {
            corsAnnotations.put("", verticleAnnotation);
        }

        // Add annotations from resource classes
        for (Class<?> resourceClass : resourceClasses) {
            // Check for CORS annotation on the class
            if (resourceClass.isAnnotationPresent(Cors.class)) {
                Cors classAnnotation = resourceClass.getAnnotation(Cors.class);

                // Get the base path for this class
                String basePath = "";
                if (resourceClass.isAnnotationPresent(ApplicationPath.class)) {
                    ApplicationPath applicationPath = resourceClass.getAnnotation(ApplicationPath.class);
                    basePath = applicationPath.value();
                    if (!basePath.startsWith("/")) {
                        basePath = "/" + basePath;
                    }
                }

                if (resourceClass.isAnnotationPresent(Path.class)) {
                    Path pathAnnotation = resourceClass.getAnnotation(Path.class);
                    String classPath = pathAnnotation.value();
                    if (!classPath.startsWith("/")) {
                        classPath = "/" + classPath;
                    }

                    if (!basePath.isEmpty() && !basePath.endsWith("/")) {
                        basePath += "/";
                    }

                    basePath += classPath;
                }

                // Add the annotation for this path
                corsAnnotations.put(basePath, classAnnotation);

                // Check for CORS annotations on methods
                for (Method method : resourceClass.getMethods()) {
                    if (method.isAnnotationPresent(Cors.class) && method.isAnnotationPresent(Path.class)) {
                        Cors methodAnnotation = method.getAnnotation(Cors.class);
                        Path methodPath = method.getAnnotation(Path.class);

                        String fullPath = basePath;
                        if (!fullPath.isEmpty() && !fullPath.endsWith("/")) {
                            fullPath += "/";
                        }

                        String methodPathValue = methodPath.value();
                        if (methodPathValue.startsWith("/")) {
                            methodPathValue = methodPathValue.substring(1);
                        }

                        fullPath += methodPathValue;

                        // Add the annotation for this path
                        corsAnnotations.put(fullPath, methodAnnotation);
                    }
                }
            }
        }

        return corsAnnotations;
    }

    /**
     * Checks if CORS is enabled based on the annotation or environment variables.
     *
     * @param corsAnnotation The Cors annotation, if present
     * @return {@code true} if CORS is enabled
     */
    private static boolean isCorsEnabled(Cors corsAnnotation) {
        if (corsAnnotation != null) {
            return Boolean.parseBoolean(Environment.getSystemPropertyOrEnvironment("REST_CORS_ENABLED", 
                    String.valueOf(corsAnnotation.enabled())));
        }
        return Boolean.parseBoolean(Environment.getSystemPropertyOrEnvironment("REST_CORS_ENABLED", "false"));
    }

    /**
     * Gets the allowed origins based on the annotation or environment variables.
     *
     * @param corsAnnotation The Cors annotation, if present
     * @return The set of allowed origins
     */
    private static Set<String> getAllowedOrigins(Cors corsAnnotation) {
        String originsStr;
        if (corsAnnotation != null) {
            originsStr = Environment.getSystemPropertyOrEnvironment("REST_CORS_ALLOWED_ORIGINS", 
                    String.join(",", corsAnnotation.allowedOrigins()));
        } else {
            originsStr = Environment.getSystemPropertyOrEnvironment("REST_CORS_ALLOWED_ORIGINS", "*");
        }

        return Arrays.stream(originsStr.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    /**
     * Gets the allowed methods based on the annotation or environment variables.
     *
     * @param corsAnnotation The Cors annotation, if present
     * @return The set of allowed methods
     */
    private static Set<HttpMethod> getAllowedMethods(Cors corsAnnotation) {
        String methodsStr;
        if (corsAnnotation != null) {
            methodsStr = Environment.getSystemPropertyOrEnvironment("REST_CORS_ALLOWED_METHODS", 
                    String.join(",", corsAnnotation.allowedMethods()));
        } else {
            methodsStr = Environment.getSystemPropertyOrEnvironment("REST_CORS_ALLOWED_METHODS", 
                    "GET,POST,PUT,DELETE,PATCH,OPTIONS");
        }

        Set<HttpMethod> methods = new HashSet<>();
        for (String method : methodsStr.split(",")) {
            try {
                methods.add(HttpMethod.valueOf(method.trim()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid HTTP method: " + method);
            }
        }
        return methods;
    }

    /**
     * Gets the allowed headers based on the annotation or environment variables.
     *
     * @param corsAnnotation The Cors annotation, if present
     * @return The set of allowed headers
     */
    private static Set<String> getAllowedHeaders(Cors corsAnnotation) {
        String headersStr;
        if (corsAnnotation != null) {
            headersStr = Environment.getSystemPropertyOrEnvironment("REST_CORS_ALLOWED_HEADERS", 
                    String.join(",", corsAnnotation.allowedHeaders()));
        } else {
            headersStr = Environment.getSystemPropertyOrEnvironment("REST_CORS_ALLOWED_HEADERS", 
                    "x-requested-with,Access-Control-Allow-Origin,origin,Content-Type,accept,Authorization");
        }

        return Arrays.stream(headersStr.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    /**
     * Gets the allow credentials setting based on the annotation or environment variables.
     *
     * @param corsAnnotation The Cors annotation, if present
     * @return {@code true} if credentials are allowed
     */
    private static boolean getAllowCredentials(Cors corsAnnotation) {
        if (corsAnnotation != null) {
            return Boolean.parseBoolean(Environment.getSystemPropertyOrEnvironment("REST_CORS_ALLOW_CREDENTIALS", 
                    String.valueOf(corsAnnotation.allowCredentials())));
        }
        return Boolean.parseBoolean(Environment.getSystemPropertyOrEnvironment("REST_CORS_ALLOW_CREDENTIALS", "true"));
    }

    /**
     * Gets the max age in seconds based on the annotation or environment variables.
     *
     * @param corsAnnotation The Cors annotation, if present
     * @return The max age in seconds
     */
    private static int getMaxAgeSeconds(Cors corsAnnotation) {
        if (corsAnnotation != null) {
            return Integer.parseInt(Environment.getSystemPropertyOrEnvironment("REST_CORS_MAX_AGE", 
                    String.valueOf(corsAnnotation.maxAgeSeconds())));
        }
        return Integer.parseInt(Environment.getSystemPropertyOrEnvironment("REST_CORS_MAX_AGE", "3600"));
    }
}
