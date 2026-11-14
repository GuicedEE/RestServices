package com.guicedee.rest.test;

import com.guicedee.client.IGuiceContext;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConsumesIntegrationTest {
    private HttpClient client;
    @BeforeAll void start() throws Exception {
        IGuiceContext.instance().inject();
        TestServerReady.waitForServer();
        client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }
    @AfterAll void stop() throws Exception {
        if (client != null) client.close();
        IGuiceContext.get(Vertx.class).close().toCompletionStage().toCompletableFuture().get(5,TimeUnit.SECONDS);
    }
    private HttpResponse<String> post(String path, String type) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://localhost:8080/consumes-regression/"+path))
                .timeout(Duration.ofSeconds(5)).POST(HttpRequest.BodyPublishers.ofString("{}"));
        if(type!=null) request.header("Content-Type",type);
        return client.send(request.build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void classConsumesAcceptsCaseInsensitiveTypesAndParameters() throws Exception {
        assertEquals(200,post("inherited","Application/JSON; charset=utf-8").statusCode());
    }
    @Test void unsupportedEntityIsRejectedBeforeParameterConversionAndResourceInvocation() throws Exception {
        int before=ConsumesResource.calls.get();
        assertEquals(415,post("inherited?number=invalid","text/plain").statusCode());
        assertEquals(before,ConsumesResource.calls.get());
    }
    @Test void methodConsumesOverridesTheClass() throws Exception {
        assertEquals(200,post("override","text/plain").statusCode());
        assertEquals(415,post("override","application/json").statusCode());
    }
    @Test void wildcardAndMultipleTypesAreSupported() throws Exception {
        assertEquals(200,post("wildcard","application/xml").statusCode());
        assertEquals(415,post("wildcard","text/plain").statusCode());
        assertEquals(200,post("multiple","application/xml").statusCode());
        assertEquals(200,post("multiple","image/png").statusCode());
        assertEquals(200,post("any","video/mp4").statusCode());
    }
    @Test void missingContentTypeDefaultsToOctetStream() throws Exception {
        assertEquals(415,post("inherited",null).statusCode());
        assertEquals(200,post("octet",null).statusCode());
    }
    @Test void bodylessRequestsAreNotRejectedByClassConsumes() throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://localhost:8080/consumes-regression/empty")).GET().timeout(Duration.ofSeconds(5)).build();
        assertEquals(200,client.send(request,HttpResponse.BodyHandlers.ofString()).statusCode());
    }
}
