package com.guicedee.guicedservlets.rest.implementations;

import com.guicedee.client.Environment;
import com.guicedee.guicedservlets.rest.services.Cors;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configures CORS handling for REST services based on the Cors annotation or environment variables.
 */
@Log4j2
public class CorsHandlerConfigurator {

    /**
     * Configures CORS handling for the given router based on the Cors annotation or environment variables.
     *
     * @param router The router to configure
     * @param corsAnnotation The Cors annotation, if present
     */
    public static void configureCors(Router router, Cors corsAnnotation) {
        // Check if CORS is enabled
        boolean corsEnabled = isCorsEnabled(corsAnnotation);
        if (!corsEnabled) {
            log.debug("CORS is disabled");
            return;
        }

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
        router.route().handler(corsHandler);
        log.debug("Added CORS handler to router");
    }

    /**
     * Checks if CORS is enabled based on the annotation or environment variables.
     *
     * @param corsAnnotation The Cors annotation, if present
     * @return true if CORS is enabled, false otherwise
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
     * @return true if credentials are allowed, false otherwise
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
