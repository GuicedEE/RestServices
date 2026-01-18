package com.guicedee.guicedservlets.rest.services;

import io.vertx.core.http.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for configuring CORS (Cross-Origin Resource Sharing) settings for REST services.
 *
 * <p>Values defined here can be overridden by system properties or environment variables,
 * allowing operators to change CORS behavior without code changes. When applied at
 * class or method level, the handler configuration will respect the closest annotation.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Cors {
    /**
     * Allowed origins for CORS requests.
     * Can be overridden by the REST_CORS_ALLOWED_ORIGINS system property or environment variable.
     * Default is "*" (all origins).
     * Multiple origins can be specified as a comma-separated list.
     *
     * @return Allowed origin patterns
     */
    String[] allowedOrigins() default {"*"};

    /**
     * Allowed HTTP methods for CORS requests.
     * Can be overridden by the REST_CORS_ALLOWED_METHODS system property or environment variable.
     * Default includes GET, POST, PUT, DELETE, PATCH, OPTIONS.
     *
     * @return Allowed HTTP methods
     */
    String[] allowedMethods() default {
            "GET",
            "POST",
            "PUT",
            "DELETE",
            "PATCH",
            "OPTIONS"
    };

    /**
     * Allowed headers for CORS requests.
     * Can be overridden by the REST_CORS_ALLOWED_HEADERS system property or environment variable.
     * Default includes common headers used in REST APIs.
     *
     * @return Allowed header names
     */
    String[] allowedHeaders() default {
            "x-requested-with",
            "Access-Control-Allow-Origin",
            "origin",
            "Content-Type",
            "accept",
            "Authorization"
    };

    /**
     * Whether to allow credentials for CORS requests.
     * Can be overridden by the REST_CORS_ALLOW_CREDENTIALS system property or environment variable.
     * Default is true.
     *
     * @return Whether credentials are allowed
     */
    boolean allowCredentials() default true;

    /**
     * Max age in seconds for CORS preflight requests.
     * Can be overridden by the REST_CORS_MAX_AGE system property or environment variable.
     * Default is 3600 seconds (1 hour).
     *
     * @return Max age in seconds
     */
    int maxAgeSeconds() default 3600;

    /**
     * Whether CORS is enabled.
     * Can be overridden by the REST_CORS_ENABLED system property or environment variable.
     * Default is true.
     *
     * @return {@code true} to enable CORS handling
     */
    boolean enabled() default true;
}
