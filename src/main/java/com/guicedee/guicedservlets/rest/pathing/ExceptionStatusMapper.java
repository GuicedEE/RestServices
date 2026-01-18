package com.guicedee.guicedservlets.rest.pathing;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Maps thrown exceptions to HTTP status codes for REST responses.
 *
 * <p>This utility keeps a small default mapping and optionally consults
 * {@link jakarta.ws.rs.ext.ExceptionMapper} instances loaded from the service
 * provider configuration. The lookup is performed by traversing the exception
 * type hierarchy, so a mapper or default mapping for a superclass can be used
 * for subclassed exceptions.</p>
 *
 * <p>When a {@link WebApplicationException} is supplied, its embedded response
 * status is always used.</p>
 */
public class ExceptionStatusMapper {
    /**
     * Default exception-to-status mapping used when no custom mapper is found.
     */
    private static final Map<Class<? extends Throwable>, Integer> defaultStatusCodes = new HashMap<>();
    /**
     * Exception mappers discovered via the service loader, keyed by exception type.
     */
    private static final Map<Class<? extends Throwable>, jakarta.ws.rs.ext.ExceptionMapper<? extends Throwable>> mappers = new HashMap<>();
    
    static {
        // Initialize default status codes
        defaultStatusCodes.put(IllegalArgumentException.class, 400);
        defaultStatusCodes.put(IllegalStateException.class, 400);
        defaultStatusCodes.put(SecurityException.class, 403);
        defaultStatusCodes.put(NullPointerException.class, 500);
        defaultStatusCodes.put(RuntimeException.class, 500);
        defaultStatusCodes.put(Exception.class, 500);
        
        // Load exception mappers from service loader
        ServiceLoader<jakarta.ws.rs.ext.ExceptionMapper> serviceLoader = ServiceLoader.load(jakarta.ws.rs.ext.ExceptionMapper.class);
        for (jakarta.ws.rs.ext.ExceptionMapper<?> mapper : serviceLoader) {
            Class<?> exceptionType = getExceptionType(mapper);
            if (exceptionType != null) {
                mappers.put((Class<? extends Throwable>) exceptionType, mapper);
            }
        }
    }
    
    /**
     * Attempts to infer the exception type handled by an exception mapper.
     *
     * <p>This inspects the generic parameter of {@link ExceptionMapper} on the
     * implementation class. If it cannot be resolved, {@code null} is returned
     * and the mapper is ignored.</p>
     *
     * @param mapper The exception mapper instance
     * @return The exception type handled by the mapper, or {@code null} if unknown
     */
    private static Class<?> getExceptionType(jakarta.ws.rs.ext.ExceptionMapper<?> mapper) {
        // This is a bit of a hack to get the exception type from the generic parameter
        // In a real implementation, we would use reflection to get the generic type
        try {
            Class<?> mapperClass = mapper.getClass();
            java.lang.reflect.Type[] genericInterfaces = mapperClass.getGenericInterfaces();
            for (java.lang.reflect.Type genericInterface : genericInterfaces) {
                if (genericInterface instanceof java.lang.reflect.ParameterizedType) {
                    java.lang.reflect.ParameterizedType parameterizedType = (java.lang.reflect.ParameterizedType) genericInterface;
                    if (parameterizedType.getRawType().equals(jakarta.ws.rs.ext.ExceptionMapper.class)) {
                        java.lang.reflect.Type[] typeArguments = parameterizedType.getActualTypeArguments();
                        if (typeArguments.length > 0 && typeArguments[0] instanceof Class) {
                            return (Class<?>) typeArguments[0];
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    /**
     * Resolves the HTTP status code to use for a given exception.
     *
     * @param exception The exception to map
     * @return The HTTP status code to return to the client
     */
    public static int getStatusCode(Throwable exception) {
        // Check if the exception is a WebApplicationException
        if (exception instanceof WebApplicationException) {
            return ((WebApplicationException) exception).getResponse().getStatus();
        }
        
        // Check if there's a mapper for this exception
        jakarta.ws.rs.ext.ExceptionMapper<? extends Throwable> mapper = findMapper(exception.getClass());
        if (mapper != null) {
            try {
                // This is a bit of a hack to invoke the mapper
                // In a real implementation, we would use reflection to invoke the mapper
                Response response = ((jakarta.ws.rs.ext.ExceptionMapper<Throwable>) mapper).toResponse(exception);
                return response.getStatus();
            } catch (Exception e) {
                // Ignore
            }
        }
        
        // Check if there's a default status code for this exception
        Integer statusCode = findStatusCode(exception.getClass());
        if (statusCode != null) {
            return statusCode;
        }
        
        // Default to 500
        return 500;
    }
    
    /**
     * Finds the closest {@link ExceptionMapper} for the provided exception type.
     *
     * @param exceptionClass The exception class to match
     * @return The mapper, or {@code null} if none found
     */
    private static jakarta.ws.rs.ext.ExceptionMapper<? extends Throwable> findMapper(Class<? extends Throwable> exceptionClass) {
        // Check if there's a mapper for this exact class
        jakarta.ws.rs.ext.ExceptionMapper<? extends Throwable> mapper = mappers.get(exceptionClass);
        if (mapper != null) {
            return mapper;
        }
        
        // Check if there's a mapper for a superclass
        Class<?> superclass = exceptionClass.getSuperclass();
        if (superclass != null && Throwable.class.isAssignableFrom(superclass)) {
            return findMapper((Class<? extends Throwable>) superclass);
        }
        
        return null;
    }
    
    /**
     * Finds the closest default status code for the provided exception type.
     *
     * @param exceptionClass The exception class to match
     * @return The status code, or {@code null} if none found
     */
    private static Integer findStatusCode(Class<? extends Throwable> exceptionClass) {
        // Check if there's a status code for this exact class
        Integer statusCode = defaultStatusCodes.get(exceptionClass);
        if (statusCode != null) {
            return statusCode;
        }
        
        // Check if there's a status code for a superclass
        Class<?> superclass = exceptionClass.getSuperclass();
        if (superclass != null && Throwable.class.isAssignableFrom(superclass)) {
            return findStatusCode((Class<? extends Throwable>) superclass);
        }
        
        return null;
    }
    
    /**
     * Registers or overrides the status code for a specific exception class.
     *
     * @param exceptionClass The exception class to map
     * @param statusCode The HTTP status code to return
     */
    public static void registerStatusCode(Class<? extends Throwable> exceptionClass, int statusCode) {
        defaultStatusCodes.put(exceptionClass, statusCode);
    }
}
