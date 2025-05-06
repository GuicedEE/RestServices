package com.guicedee.guicedservlets.rest.pathing;

import com.guicedee.services.jsonrepresentation.IJsonRepresentation;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ServiceLoader;

/**
 * Handles processing of responses from resource methods.
 */
@Slf4j
public class ResponseHandler {

    /**
     * Processes the result of a resource method invocation and sends the response.
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

        // Get the content type
        String contentType = getContentType(method);

        // Set the content type header
        context.response().putHeader("Content-Type", contentType);

        // Convert the result to a response body
        try {
            byte[] responseBody = convertToResponseBody(result, contentType, method.getReturnType(), method.getGenericReturnType(), method.getAnnotations());
            context.response().setStatusCode(200).end(Buffer.buffer(responseBody));
        } catch (Exception e) {
            handleException(context, e);
        }
    }

    /**
     * Gets the content type for a method based on its annotations.
     *
     * @param method The method
     * @return The content type
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
     * Converts a result object to a response body.
     *
     * @param result The result object
     * @param contentType The content type
     * @param resultClass The class of the result
     * @param genericType The generic type of the result
     * @param annotations The annotations on the method
     * @return The response body as a byte array
     * @throws IOException If an error occurs during conversion
     */
    private static byte[] convertToResponseBody(Object result, String contentType, Class<?> resultClass, Type genericType, Annotation[] annotations) throws IOException {
        // Try to find a MessageBodyWriter for the result type
        for (MessageBodyWriter writer : ServiceLoader.load(MessageBodyWriter.class)) {
            try
            {
                if (writer.isWriteable(resultClass, genericType, annotations, MediaType.valueOf(contentType)))
                {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    writer.writeTo(result, resultClass, genericType, annotations, MediaType.valueOf(contentType), null, outputStream);
                    return outputStream.toByteArray();
                }
            }catch (Exception classNotFoundException)
            {
                log.warn("Could not load class for MessageBodyWriter: " + classNotFoundException.getMessage() + "for writer " + writer.getClass().getName() + ", skipping");
            }
        }

        // If no MessageBodyWriter is found, use default conversion
        if (contentType.equals(MediaType.APPLICATION_JSON)) {
            return IJsonRepresentation.getObjectMapper().writeValueAsBytes(result);
        } else if (contentType.equals(MediaType.TEXT_PLAIN)) {
            return result.toString().getBytes(StandardCharsets.UTF_8);
        } else {
            // For other content types, try to convert to string
            return result.toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Handles an exception that occurred during processing.
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
     * Gets the return type of a method, handling Future and Uni types.
     *
     * @param method The method
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
