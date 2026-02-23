package com.guicedee.guicedservlets.rest.pathing;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guicedee.client.IGuiceContext;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.*;
import lombok.extern.log4j.Log4j2;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static com.guicedee.client.implementations.ObjectBinderKeys.DefaultObjectMapper;

/**
 * Extracts method parameters for Jakarta REST-style endpoints.
 *
 * <p>Supports common parameter annotations like {@link PathParam},
 * {@link QueryParam}, {@link HeaderParam}, {@link CookieParam},
 * {@link FormParam}, and {@link MatrixParam}. If no annotation is present,
 * the request body is deserialized using Vert.x JSON (backed by Jackson).</p>
 */
@Log4j2
public class ParameterExtractor {

    /**
     * Extracts parameters for a method from the routing context.
     *
     * @param method  The method to extract parameters for
     * @param context The routing context
     * @return An array of parameter values, aligned to method parameters
     */
    public static Object[] extractParameters(Method method, RoutingContext context) {
        Parameter[] parameters = method.getParameters();
        Object[] paramValues = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            paramValues[i] = extractParameter(parameter, context);
        }

        return paramValues;
    }

    /**
     * Extracts a single parameter value from the routing context.
     *
     * <p>The method recognizes Jakarta REST parameter annotations and performs
     * basic string-to-type conversions for primitives, boxed types, and enums.</p>
     *
     * @param parameter The parameter to extract
     * @param context   The routing context
     * @return The parameter value, or {@code null} when unavailable
     */
    private static Object extractParameter(Parameter parameter, RoutingContext context) {
        // Check if the parameter is the RoutingContext itself
        if (parameter.getType().equals(RoutingContext.class)) {
            return context;
        }

        // Check for parameter annotations
        for (Annotation annotation : parameter.getAnnotations()) {
            if (annotation instanceof PathParam) {
                String name = ((PathParam) annotation).value();
                return convertValue(context.pathParam(name), parameter.getType());
            } else if (annotation instanceof QueryParam) {
                String name = ((QueryParam) annotation).value();
                return convertValue(context.request().getParam(name), parameter.getType());
            } else if (annotation instanceof HeaderParam) {
                String name = ((HeaderParam) annotation).value();
                return convertValue(context.request().getHeader(name), parameter.getType());
            } else if (annotation instanceof CookieParam) {
                String name = ((CookieParam) annotation).value();
                return context.request().getCookie(name) != null ?
                        convertValue(context.request().getCookie(name).getValue(), parameter.getType()) : null;
            } else if (annotation instanceof FormParam) {
                String name = ((FormParam) annotation).value();
                return convertValue(context.request().getFormAttribute(name), parameter.getType());
            } else if (annotation instanceof MatrixParam) {
                // Matrix parameters are part of the path segment
                // For example: /path;name=value
                // This is a bit more complex to extract
                String name = ((MatrixParam) annotation).value();
                String path = context.request().path();
                String[] segments = path.split("/");
                for (String segment : segments) {
                    if (segment.contains(";")) {
                        String[] matrixParams = segment.split(";");
                        for (int i = 1; i < matrixParams.length; i++) {
                            String[] keyValue = matrixParams[i].split("=");
                            if (keyValue.length == 2 && keyValue[0].equals(name)) {
                                return convertValue(keyValue[1], parameter.getType());
                            }
                        }
                    }
                }
                return null;
            } else if (annotation instanceof BeanParam) {
                // BeanParam is used to inject a bean with properties annotated with @PathParam, @QueryParam, etc.
                // This is more complex and would require reflection to set properties on the bean
                // For simplicity, we'll return null for now
                return null;
            }
        }

        // If no annotation is found, try to extract from request body
        // Handle raw byte[] parameters — return the buffer bytes directly
        if (parameter.getType() == byte[].class) {
            if (context.body() != null && context.body().buffer() != null && context.body().buffer().length() > 0) {
                return context.body().buffer().getBytes();
            }
            return null;
        }

        String bodyString = null;
        if (context.body() != null && context.body().buffer() != null && context.body().buffer().length() > 0) {
            bodyString = context.body().asString();
        }
        // Fallback: read directly from the raw buffer when asString() returned nothing usable
        if ((bodyString == null || bodyString.isEmpty()) && context.body() != null && context.body().buffer() != null) {
            bodyString = context.body().buffer().toString();
        }

        if (bodyString != null && !bodyString.isEmpty()) {
            Class<?> targetType = parameter.getType();
            Type genericType = parameter.getParameterizedType();
            log.debug("Deserializing request body to type: {} (generic: {}) from body: {}", targetType.getName(), genericType.getTypeName(), bodyString);
            // If the target type is String, return as-is
            if (targetType.equals(String.class)) {
                if (context.parsedHeaders().contentType().value().equalsIgnoreCase("application/json")) {
                    JsonObject body = context.body().asJsonObject();
                    return body.mapTo(targetType);
                }
                return bodyString;
            }

            // Use the configured DatabindCodec ObjectMapper for deserialization
            try {
                ObjectMapper mapper = IGuiceContext.get(DefaultObjectMapper);
                // Build a JavaType from the full generic type so that collections/arrays
                // honour the element type (e.g. List<MyDTO> instead of List<LinkedHashMap>)
                JavaType javaType = mapper.getTypeFactory().constructType(genericType);
                return mapper.readValue(bodyString, javaType);
            } catch (Exception e) {
                log.error("Failed to deserialize request body to type: {} from body: {}", targetType.getName(), bodyString, e);
                // If ObjectMapper deserialization fails, try simple type conversion as last resort
                return convertValue(bodyString, targetType);
            }
        }

        return null;
    }

    /**
     * Converts a string value to the specified Java type.
     *
     * <p>Only a small set of simple types is supported. For complex types, a
     * {@link jakarta.ws.rs.ext.MessageBodyReader} would be required.</p>
     *
     * @param value The string value
     * @param type  The target type
     * @param <T>   The target type
     * @return The converted value, or {@code null} when conversion is unsupported
     */
    @SuppressWarnings("unchecked")
    private static <T> T convertValue(String value, Class<T> type) {
        if (value == null) {
            return null;
        }

        if (type.equals(String.class)) {
            return (T) value;
        } else if (type.equals(Integer.class) || type.equals(int.class)) {
            return (T) Integer.valueOf(value);
        } else if (type.equals(Long.class) || type.equals(long.class)) {
            return (T) Long.valueOf(value);
        } else if (type.equals(Double.class) || type.equals(double.class)) {
            return (T) Double.valueOf(value);
        } else if (type.equals(Float.class) || type.equals(float.class)) {
            return (T) Float.valueOf(value);
        } else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return (T) Boolean.valueOf(value);
        } else if (type.equals(Short.class) || type.equals(short.class)) {
            return (T) Short.valueOf(value);
        } else if (type.equals(Byte.class) || type.equals(byte.class)) {
            return (T) Byte.valueOf(value);
        } else if (type.equals(Character.class) || type.equals(char.class)) {
            return (T) Character.valueOf(value.charAt(0));
        } else if (type.isEnum()) {
            return (T) Enum.valueOf((Class<Enum>) type, value);
        } else if (type.equals(java.util.UUID.class)) {
            return (T) java.util.UUID.fromString(value);
        } else if (type.equals(java.math.BigDecimal.class)) {
            return (T) new java.math.BigDecimal(value);
        } else if (type.equals(java.math.BigInteger.class)) {
            return (T) new java.math.BigInteger(value);
        }

        // JAX-RS §3.2 fallback: try static valueOf(String), fromString(String), then String constructor
        try {
            java.lang.reflect.Method valueOf = type.getDeclaredMethod("valueOf", String.class);
            if (java.lang.reflect.Modifier.isStatic(valueOf.getModifiers()) && type.isAssignableFrom(valueOf.getReturnType())) {
                return (T) valueOf.invoke(null, value);
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            log.trace("valueOf(String) failed for type {}: {}", type.getName(), e.getMessage());
        }
        try {
            java.lang.reflect.Method fromString = type.getDeclaredMethod("fromString", String.class);
            if (java.lang.reflect.Modifier.isStatic(fromString.getModifiers()) && type.isAssignableFrom(fromString.getReturnType())) {
                return (T) fromString.invoke(null, value);
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            log.trace("fromString(String) failed for type {}: {}", type.getName(), e.getMessage());
        }
        try {
            java.lang.reflect.Constructor<T> ctor = type.getDeclaredConstructor(String.class);
            return ctor.newInstance(value);
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            log.trace("String constructor failed for type {}: {}", type.getName(), e.getMessage());
        }

        return null;
    }

    /**
     * Describes how each parameter in a method is sourced.
     *
     * @param method The method to describe
     * @return A list of parameter metadata
     */
    public static List<ParameterInfo> getParameterInfo(Method method) {
        Parameter[] parameters = method.getParameters();
        List<ParameterInfo> parameterInfos = new ArrayList<>();

        for (Parameter parameter : parameters) {
            ParameterType type = ParameterType.BODY; // Default to body
            String name = parameter.getName();

            for (Annotation annotation : parameter.getAnnotations()) {
                if (annotation instanceof PathParam) {
                    type = ParameterType.PATH;
                    name = ((PathParam) annotation).value();
                } else if (annotation instanceof QueryParam) {
                    type = ParameterType.QUERY;
                    name = ((QueryParam) annotation).value();
                } else if (annotation instanceof HeaderParam) {
                    type = ParameterType.HEADER;
                    name = ((HeaderParam) annotation).value();
                } else if (annotation instanceof CookieParam) {
                    type = ParameterType.COOKIE;
                    name = ((CookieParam) annotation).value();
                } else if (annotation instanceof FormParam) {
                    type = ParameterType.FORM;
                    name = ((FormParam) annotation).value();
                } else if (annotation instanceof MatrixParam) {
                    type = ParameterType.MATRIX;
                    name = ((MatrixParam) annotation).value();
                } else if (annotation instanceof BeanParam) {
                    type = ParameterType.BEAN;
                }
            }

            parameterInfos.add(new ParameterInfo(name, type, parameter.getType()));
        }

        return parameterInfos;
    }

    /**
     * Enumerates supported parameter sources.
     */
    public enum ParameterType {
        PATH,
        QUERY,
        HEADER,
        COOKIE,
        FORM,
        MATRIX,
        BEAN,
        BODY
    }

    /**
     * Describes a single parameter's name, source type, and Java type.
     */
    public static class ParameterInfo {
        private final String name;
        private final ParameterType type;
        private final Class<?> javaType;

        /**
         * Creates a parameter descriptor.
         *
         * @param name     The parameter name or annotation value
         * @param type     The parameter source type
         * @param javaType The Java type of the parameter
         */
        public ParameterInfo(String name, ParameterType type, Class<?> javaType) {
            this.name = name;
            this.type = type;
            this.javaType = javaType;
        }

        /**
         * @return The parameter name or annotation value
         */
        public String getName() {
            return name;
        }

        /**
         * @return The parameter source type
         */
        public ParameterType getType() {
            return type;
        }

        /**
         * @return The Java type of the parameter
         */
        public Class<?> getJavaType() {
            return javaType;
        }
    }
}
