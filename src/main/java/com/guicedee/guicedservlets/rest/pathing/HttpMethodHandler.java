package com.guicedee.guicedservlets.rest.pathing;

import io.vertx.core.http.HttpMethod;
import jakarta.ws.rs.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps Jakarta REST HTTP method annotations to Vert.x {@link HttpMethod} values.
 *
 * <p>Used by the router registration to determine the appropriate Vert.x route
 * based on method annotations like {@link GET} and {@link POST}.</p>
 */
public class HttpMethodHandler {
    /**
     * Lookup map of Jakarta REST annotations to Vert.x HTTP methods.
     */
    private static final Map<Class<? extends Annotation>, HttpMethod> methodMap = new HashMap<>();

    static {
        methodMap.put(GET.class, HttpMethod.GET);
        methodMap.put(POST.class, HttpMethod.POST);
        methodMap.put(PUT.class, HttpMethod.PUT);
        methodMap.put(DELETE.class, HttpMethod.DELETE);
        methodMap.put(HEAD.class, HttpMethod.HEAD);
        methodMap.put(OPTIONS.class, HttpMethod.OPTIONS);
        methodMap.put(PATCH.class, HttpMethod.PATCH);
    }

    /**
     * Resolves a Vert.x HTTP method for the given Java method.
     *
     * @param method The method to inspect
     * @return The corresponding Vert.x HTTP method, or {@code null} if none found
     */
    public static HttpMethod getHttpMethod(Method method) {
        for (Class<? extends Annotation> annotationClass : methodMap.keySet()) {
            if (method.isAnnotationPresent(annotationClass)) {
                return methodMap.get(annotationClass);
            }
        }
        return null;
    }

    /**
     * Checks whether a method declares a supported HTTP method annotation.
     *
     * @param method The method to inspect
     * @return {@code true} if a supported HTTP annotation is present
     */
    public static boolean hasHttpMethodAnnotation(Method method) {
        for (Class<? extends Annotation> annotationClass : methodMap.keySet()) {
            if (method.isAnnotationPresent(annotationClass)) {
                return true;
            }
        }
        return false;
    }
}
