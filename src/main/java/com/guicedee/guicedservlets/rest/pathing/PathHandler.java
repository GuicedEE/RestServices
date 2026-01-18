package com.guicedee.guicedservlets.rest.pathing;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Path;

import java.lang.reflect.Method;

/**
 * Extracts and normalizes path information from Jakarta REST annotations.
 *
 * <p>Class-level {@link ApplicationPath} and {@link Path} annotations are
 * combined with method-level {@link Path} annotations to produce a full route
 * path for registration.</p>
 */
public class PathHandler {

    /**
     * Builds the base path for a resource class.
     *
     * @param resourceClass The resource class
     * @return The base path, or empty string if no path annotation is present
     */
    public static String getBasePath(Class<?> resourceClass) {
        StringBuilder basePath = new StringBuilder();
        
        // Check for ApplicationPath annotation
        if (resourceClass.isAnnotationPresent(ApplicationPath.class)) {
            ApplicationPath applicationPath = resourceClass.getAnnotation(ApplicationPath.class);
            String path = applicationPath.value();
            if (!path.startsWith("/")) {
                basePath.append("/");
            }
            basePath.append(path);
            if (!path.endsWith("/") && !path.isEmpty()) {
                basePath.append("/");
            }
        }
        
        // Check for Path annotation on the class
        if (resourceClass.isAnnotationPresent(Path.class)) {
            Path pathAnnotation = resourceClass.getAnnotation(Path.class);
            String path = pathAnnotation.value();
            if (!path.startsWith("/") && !basePath.toString().endsWith("/") && !basePath.isEmpty()) {
                basePath.append("/");
            }
            basePath.append(path);
        }
        
        return basePath.toString();
    }
    
    /**
     * Builds the full path for a resource method.
     *
     * @param resourceClass The resource class
     * @param method The resource method
     * @return The full path for the method
     */
    public static String getFullPath(Class<?> resourceClass, Method method) {
        String basePath = getBasePath(resourceClass);
        
        if (method.isAnnotationPresent(Path.class)) {
            Path pathAnnotation = method.getAnnotation(Path.class);
            String methodPath = pathAnnotation.value();
            
            if (!methodPath.startsWith("/") && !basePath.endsWith("/") && !basePath.isEmpty()) {
                basePath += "/";
            }
            
            basePath += methodPath;
        }
        
        // Normalize path to ensure it starts with /
        if (!basePath.startsWith("/")) {
            basePath = "/" + basePath;
        }
        
        return basePath;
    }
    
    /**
     * Normalizes a path to start with a leading slash and to avoid trailing slashes.
     *
     * @param path The path to normalize
     * @return The normalized path
     */
    public static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        // Remove trailing slash except for root path
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        
        return path;
    }
}
