package com.guicedee.guicedservlets.rest.pathing;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * Converts resource method results into HTTP responses.
 *
 * <p>Handles synchronous results, {@link Future} and {@link Uni} types,
 * selects an appropriate content type, and writes the response body using
 * Vert.x JSON (backed by Jackson) for serialization.</p>
 */
@Slf4j
public class ResponseHandler {

    /**
     * Processes a method result and writes the HTTP response.
     *
     * @param context The routing context
     * @param result The result of the method invocation
     * @param method The resource method
     */
    @SuppressWarnings("unchecked")
    public static void processResponse(RoutingContext context, Object result, Method method) {
        if (result == null) {
            context.response().setStatusCode(204).end();
            return;
        }

        // Handle Future
        if (result instanceof Future) {
            Future<?> future = (Future<?>) result;
            future.onComplete(ar -> {
                if (ar.succeeded()) {
                    processResponse(context, ar.result(), method);
                } else {
                    handleException(context, ar.cause());
                }
            });
            return;
        }

        // Handle Uni
        if (result instanceof Uni) {
            Uni<Object> uni = (Uni<Object>) result;
            uni.subscribe().with(
                item -> processResponse(context, item, method),
                failure -> handleException(context, failure)
            );
            return;
        }

        // Handle JAX-RS Response directly (and avoid RuntimeDelegate usage)
        if (result instanceof Response) {
            Response jaxrs = (Response) result;

            // Status
            int status = jaxrs.getStatus();
            context.response().setStatusCode(status);

            // Headers
            MultivaluedMap<String, Object> headers = jaxrs.getHeaders();
            if (headers != null) {
                headers.forEach((name, values) -> {
                    if (values != null) {
                        // According to HTTP spec, multiple header values can be joined by comma
                        String joined = values.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
                        if (!joined.isEmpty()) {
                            context.response().putHeader(name, joined);
                        }
                    }
                });
            }

            // Content-Type (prefer entity-specific media type if present)
            String contentType = jaxrs.getMediaType() != null ? jaxrs.getMediaType().toString() : getContentType(method);
            if (contentType != null && !contentType.isEmpty()) {
                context.response().putHeader("Content-Type", contentType);
            }

            // Entity
            Object entity = jaxrs.getEntity();
            if (entity == null) {
                context.response().end();
            } else {
                try {
                    byte[] responseBody = convertToResponseBody(entity, contentType != null ? contentType : MediaType.APPLICATION_JSON);
                    context.response().end(Buffer.buffer(responseBody));
                } catch (Exception e) {
                    handleException(context, e);
                }
            }
            return;
        }

        // Get the content type
        String contentType = getContentType(method);

        // Set the content type header
        context.response().putHeader("Content-Type", contentType);

        // Convert the result to a response body
        try {
            byte[] responseBody = convertToResponseBody(result, contentType);
            context.response().setStatusCode(200).end(Buffer.buffer(responseBody));
        } catch (Exception e) {
            handleException(context, e);
        }
    }

    /**
     * Determines the response content type based on {@link Produces} annotations.
     *
     * @param method The method to inspect
     * @return The selected content type
     */
    private static String getContentType(Method method) {
        if (method.isAnnotationPresent(Produces.class)) {
            Produces produces = method.getAnnotation(Produces.class);
            if (produces.value().length > 0) {
                return produces.value()[0];
            }
        }

        Class<?> declaringClass = method.getDeclaringClass();
        if (declaringClass.isAnnotationPresent(Produces.class)) {
            Produces produces = declaringClass.getAnnotation(Produces.class);
            if (produces.value().length > 0) {
                return produces.value()[0];
            }
        }

        return MediaType.APPLICATION_JSON;
    }

    /**
     * Converts a result object to the byte representation for the response body.
     *
     * <p>Uses Vert.x JSON (backed by Jackson) for JSON serialization. For text/plain
     * and other content types, the result is converted to a string.</p>
     *
     * @param result The result object
     * @param contentType The content type
     * @return The response body as a byte array
     */
    private static byte[] convertToResponseBody(Object result, String contentType) {
        // Check if the content type is JSON (handles variations like application/json;charset=utf-8)
        if (isJsonContentType(contentType)) {
            // Use Vert.x Json which internally uses Jackson ObjectMapper
            return Json.encodeToBuffer(result).getBytes();
        } else if (contentType.equals(MediaType.TEXT_PLAIN) || contentType.startsWith("text/")) {
            return result.toString().getBytes(StandardCharsets.UTF_8);
        } else {
            // For other content types, try JSON encoding as a sensible default
            return Json.encodeToBuffer(result).getBytes();
        }
    }

    /**
     * Checks if the content type indicates JSON.
     *
     * @param contentType The content type string
     * @return true if the content type is a JSON type
     */
    private static boolean isJsonContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        String lowerContentType = contentType.toLowerCase();
        return lowerContentType.equals(MediaType.APPLICATION_JSON)
                || lowerContentType.startsWith(MediaType.APPLICATION_JSON + ";")
                || lowerContentType.contains("json");
    }

    /**
     * Handles an exception that occurred during processing.
     *
     * <p>The status code is derived using {@link ExceptionStatusMapper} and the
     * exception message is returned as plain text.</p>
     *
     * @param context The routing context
     * @param exception The exception
     */
    public static void handleException(RoutingContext context, Throwable exception) {
        // Get the status code for the exception
        int statusCode = ExceptionStatusMapper.getStatusCode(exception);

        // Set the status code and end the response
        context.response()
               .setStatusCode(statusCode)
               .putHeader("Content-Type", MediaType.TEXT_PLAIN)
               .end(exception.getMessage());
    }

    /**
     * Determines the effective return type for a method.
     *
     * <p>If a method returns {@link Future} or {@link Uni}, the first generic
     * type parameter is treated as the actual return type.</p>
     *
     * @param method The method to inspect
     * @return The actual return type
     */
    public static Class<?> getActualReturnType(Method method) {
        Class<?> returnType = method.getReturnType();

        if (returnType.equals(Future.class) || returnType.equals(Uni.class)) {
            Type genericReturnType = method.getGenericReturnType();
            if (genericReturnType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                if (typeArguments.length > 0 && typeArguments[0] instanceof Class) {
                    return (Class<?>) typeArguments[0];
                }
            }
        }

        return returnType;
    }
}
