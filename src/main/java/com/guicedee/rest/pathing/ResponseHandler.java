package com.guicedee.rest.pathing;

import com.guicedee.client.IGuiceContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.guicedee.client.implementations.ObjectBinderKeys.DefaultObjectMapper;

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
    public static void processResponse(RoutingContext context, Object result, Method method) {
        if (result == null) {
            context.response().setStatusCode(204).end();
            return;
        }

        // Handle Future — each Future has a definitive start (composition)
        // and end (onComplete callback) scoped to this request.
        if (result instanceof Future<?> future) {
            final AtomicBoolean completed = new AtomicBoolean(false);
            future.onComplete(ar -> {
                if (completed.compareAndSet(false, true)) {
                    if (ar.succeeded()) {
                        processResponse(context, ar.result(), method);
                    } else {
                        handleException(context, ar.cause());
                    }
                }
            });

            // Cancel on client disconnect
            context.request().connection().closeHandler(v -> {
                if (completed.compareAndSet(false, true)) {
                    log.debug("Client disconnected, Future result discarded for {} {}", context.request().method(), context.request().path());
                }
            });
            return;
        }

        // Handle Uni — each request gets its own isolated Uni lifecycle with
        // a definitive start (subscription) and end (response completion).
        // The Uni is NOT chained into any shared/global pipeline — it is
        // self-contained per request. Cancellation on client disconnect
        // prevents orphaned work from continuing after the client is gone.
        if (result instanceof Uni<?> uni) {
            final AtomicBoolean completed = new AtomicBoolean(false);

            // Subscribe to the Uni — this is the definitive START of this request's reactive chain
            Cancellable cancellable = uni.subscribe().with(
                item -> {
                    // Definitive END (success) — process the resolved value and write the response
                    if (completed.compareAndSet(false, true)) {
                        processResponse(context, item, method);
                    }
                },
                failure -> {
                    // Definitive END (failure) — write the error response
                    if (completed.compareAndSet(false, true)) {
                        handleException(context, failure);
                    }
                }
            );

            // If the client disconnects before the Uni completes, cancel the
            // subscription so no further work is performed for this request.
            context.request().connection().closeHandler(v -> {
                if (completed.compareAndSet(false, true)) {
                    cancellable.cancel();
                    log.debug("Client disconnected, cancelled Uni for {} {}", context.request().method(), context.request().path());
                }
            });
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

        // Handle raw byte[] — write directly without JSON encoding
        if (result instanceof byte[] bytes) {
            String contentType = getContentType(method);
            if (isJsonContentType(contentType)) {
                // byte[] with JSON content type makes no sense; override to octet-stream
                contentType = MediaType.APPLICATION_OCTET_STREAM;
            }
            context.response().putHeader("Content-Type", contentType);
            context.response().setStatusCode(200).end(Buffer.buffer(bytes));
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
     * <p>Uses the GuicedEE-configured {@code DefaultObjectMapper} (Jackson) for JSON
     * serialization. Vert.x's built-in {@code Json}/{@code DatabindCodec} is intentionally
     * avoided because it binds to Jackson 2 ({@code com.fasterxml.jackson.databind}); the
     * GuicedEE runtime uses Jackson 3 ({@code tools.jackson.databind}), so delegating to the
     * managed mapper keeps serialization consistent and avoids the
     * "Mapping ... is not available without Jackson Databind on the classpath" error.
     * For text/plain and other content types, the result is converted to a string.</p>
     *
     * @param result The result object
     * @param contentType The content type
     * @return The response body as a byte array
     */
    private static byte[] convertToResponseBody(Object result, String contentType) {
        // Raw byte arrays are written as-is regardless of content type
        if (result instanceof byte[] bytes) {
            return bytes;
        }
        // Check if the content type is JSON (handles variations like application/json;charset=utf-8)
        if (isJsonContentType(contentType)) {
            // Use the GuicedEE-managed Jackson 3 ObjectMapper for serialization
            return encodeJson(result);
        } else if (contentType.equals(MediaType.TEXT_PLAIN) || contentType.startsWith("text/")) {
            return result.toString().getBytes(StandardCharsets.UTF_8);
        } else {
            // For other content types, try JSON encoding as a sensible default
            return encodeJson(result);
        }
    }

    /**
     * Serializes the given value to JSON bytes using the GuicedEE-managed Jackson 3
     * {@code DefaultObjectMapper} binding.
     *
     * @param result The value to serialize
     * @return The JSON-encoded bytes
     */
    private static byte[] encodeJson(Object result) {
        ObjectMapper mapper = IGuiceContext.get(DefaultObjectMapper);
        return mapper.writeValueAsBytes(result);
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

        log.error("Error handling request: " + context.request().method() + " " + context.request().path(), exception);
        // Set the status code and end the response
        if (context.response().ended() || context.response().closed()) {
            return;
        }
        String message = exception.getMessage();
        if (message == null) {
            message = exception.getClass().getSimpleName();
        }
        context.response()
               .setStatusCode(statusCode)
               .putHeader("Content-Type", MediaType.TEXT_PLAIN)
               .end(message);
    }

}
