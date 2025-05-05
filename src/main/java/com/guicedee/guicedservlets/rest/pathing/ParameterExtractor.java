package com.guicedee.guicedservlets.rest.pathing;

import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Extracts parameters from the request based on Jakarta WS annotations.
 */
public class ParameterExtractor {

    /**
     * Extracts parameters for a method from the routing context.
     *
     * @param method The method to extract parameters for
     * @param context The routing context
     * @return An array of parameter values
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
     * @param parameter The parameter to extract
     * @param context The routing context
     * @return The parameter value
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
        if (context.body() != null && context.body().buffer() != null && context.body().buffer().length() > 0) {
            // For simplicity, we'll return the body as a string
            // In a real implementation, we would use a MessageBodyReader to convert the body to the parameter type
            return context.body().asString();
        }

        return null;
    }

    /**
     * Converts a string value to the specified type.
     *
     * @param value The string value
     * @param type The target type
     * @return The converted value
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
        }

        // For other types, we would need to use a MessageBodyReader
        // For simplicity, we'll return null for now
        return null;
    }

    /**
     * Gets information about the parameters of a method.
     *
     * @param method The method to get parameter information for
     * @return A list of parameter information
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
     * Enum representing the different types of parameters.
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
     * Class representing information about a parameter.
     */
    public static class ParameterInfo {
        private final String name;
        private final ParameterType type;
        private final Class<?> javaType;

        public ParameterInfo(String name, ParameterType type, Class<?> javaType) {
            this.name = name;
            this.type = type;
            this.javaType = javaType;
        }

        public String getName() {
            return name;
        }

        public ParameterType getType() {
            return type;
        }

        public Class<?> getJavaType() {
            return javaType;
        }
    }
}
