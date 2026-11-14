package com.guicedee.rest.test;

import com.guicedee.rest.pathing.ParameterExtractor;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class ParameterDefaultsTest {
    public void primitives(@QueryParam("b") boolean b, @QueryParam("c") char c,
                           @QueryParam("y") byte y, @QueryParam("s") short s,
                           @QueryParam("i") int i, @QueryParam("l") long l,
                           @QueryParam("f") float f, @QueryParam("d") double d) {}
    public void nullable(@QueryParam("b") Boolean b, @QueryParam("i") Integer i) {}
    public void defaults(@QueryParam("b") @DefaultValue("true") boolean b,
                         @QueryParam("i") @DefaultValue("17") int i) {}
    public void sources(@PathParam("absent") int path, @HeaderParam("absent") int header,
                        @CookieParam("absent") int cookie, @FormParam("absent") int form,
                        @MatrixParam("absent") int matrix) {}

    private Object[] extract(String name, Map<String,String> values) {
        HttpServerRequest request = (HttpServerRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{HttpServerRequest.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getParam" -> values.get(args[0]);
                    case "path" -> "/resource";
                    default -> null;
                });
        RoutingContext context = (RoutingContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{RoutingContext.class}, (proxy, method, args) -> method.getName().equals("request") ? request : null);
        Method method = Arrays.stream(getClass().getDeclaredMethods()).filter(m -> m.getName().equals(name)).findFirst().orElseThrow();
        return ParameterExtractor.extractParameters(method, context);
    }

    @Test void missingPrimitivesUseTheirJavaDefaults() {
        assertArrayEquals(new Object[]{false,'\0',(byte)0,(short)0,0,0L,0F,0D},extract("primitives",Map.of()));
    }
    @Test void missingReferencesStayNull() { assertArrayEquals(new Object[]{null,null},extract("nullable",Map.of())); }
    @Test void annotationDefaultsPrecedePrimitiveDefaultsAndExplicitValuesWin() {
        assertArrayEquals(new Object[]{true,17},extract("defaults",Map.of()));
        assertArrayEquals(new Object[]{false,18},extract("defaults",Map.of("b","false","i","18")));
    }
    @Test void missingPrimitiveDefaultsApplyToEverySupportedParameterSource() {
        assertArrayEquals(new Object[]{0,0,0,0,0},extract("sources",Map.of()));
    }
    @Test void suppliedValuesAreConvertedAndMalformedValuesAreNotDefaulted() {
        assertArrayEquals(new Object[]{true,'Z',(byte)1,(short)2,3,4L,5F,6D},
                extract("primitives",Map.of("b","true","c","Z","y","1","s","2","i","3","l","4","f","5","d","6")));
        assertThrows(NumberFormatException.class,()->extract("defaults",Map.of("i","invalid")));
    }
}
