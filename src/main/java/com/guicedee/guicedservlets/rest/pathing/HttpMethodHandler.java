package com.guicedee.guicedservlets.rest.pathing;

import io.vertx.core.http.HttpMethod;
import jakarta.ws.rs.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles mapping of Jakarta WS HTTP method annotations to Vert.x HTTP methods.
 */
public class HttpMethodHandler {
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
     * Gets the Vert.x HTTP method for a given method based on its Jakarta WS annotations.
     *
     * @param method The method to check
     * @return The corresponding Vert.x HTTP method, or null if none found
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
     * Checks if a method has a Jakarta WS HTTP method annotation.
     *
     * @param method The method to check
     * @return true if the method has an HTTP method annotation, false otherwise
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