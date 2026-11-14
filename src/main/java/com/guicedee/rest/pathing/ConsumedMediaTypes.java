package com.guicedee.rest.pathing;

import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/** Compiles the resource's request-entity media contract once, during route registration. */
final class ConsumedMediaTypes {
    private final List<MediaType> accepted;

    ConsumedMediaTypes(Class<?> resourceClass, Method method) {
        Consumes annotation = method.getAnnotation(Consumes.class);
        if (annotation == null) annotation = resourceClass.getAnnotation(Consumes.class);
        accepted = annotation == null ? List.of(MediaType.WILDCARD_TYPE)
                : Arrays.stream(annotation.value()).flatMap(value -> Arrays.stream(value.split(",")))
                        .map(String::trim).map(MediaType::valueOf).toList();
    }

    boolean accepts(RoutingContext context) {
        // @Consumes constrains the entity, not bodyless requests such as GET.
        if (context.body() == null || context.body().buffer() == null || context.body().buffer().length() == 0) return true;
        String header = context.request().getHeader("Content-Type");
        MediaType actual = header == null || header.isBlank() ? MediaType.APPLICATION_OCTET_STREAM_TYPE : MediaType.valueOf(header);
        return accepted.stream().anyMatch(type -> type.isCompatible(actual));
    }
}
