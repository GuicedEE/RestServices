package com.guicedee.guicedservlets.rest.test;

import com.guicedee.client.IGuiceContext;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RestEasyModuleTest
{

    @Test
    void configureServlets() throws Exception
    {
        System.out.println("Starting...");
        IGuiceContext.instance()
                .inject();

        //Do stuff
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.of(5, ChronoUnit.SECONDS))
                .build();
        HttpResponse response = client.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI("http://localhost:8080/rest/hello/world"))
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.discarding());
        assertEquals(200, response.statusCode());
        response = client.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI("http://localhost:8080/rest/hello/helloObject/world"))
                        .timeout(Duration.of(5, ChronoUnit.SECONDS))
                        .build(),
                HttpResponse.BodyHandlers.discarding());
        assertEquals(200, response.statusCode());

        System.out.println("Done");
        IGuiceContext.get(Vertx.class).close();
        System.exit(0);
    }
}
