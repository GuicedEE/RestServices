package com.guicedee.guicedservlets.rest.test;

import com.guicedee.guicedservlets.undertow.GuicedUndertow;
import io.undertow.Undertow;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

public class RestEasyModuleTest
{

	@Test
	void configureServlets() throws Exception
	{
		/*
		RESTContext.getProviders()
		           .add(JacksonJsonProvider.class.getCanonicalName());
		*/;

		//LogFactory.configureConsoleColourOutput(Level.FINE);
		Undertow undertow = GuicedUndertow.boot("0.0.0.0", 6003);

		
		//Do stuff
		HttpClient client = HttpClient.newBuilder()
		                              .connectTimeout(Duration.of(5, ChronoUnit.SECONDS))
		                              .build();
		HttpResponse response = client.send(HttpRequest.newBuilder()
		                                               .GET()
		                                               .uri(new URI("http://localhost:6003/rest/hello/world"))
		                                               .build(),
		                                    HttpResponse.BodyHandlers.discarding());
		assertEquals(200, response.statusCode());
		response = client.send(HttpRequest.newBuilder()
		                                  .GET()
		                                  .uri(new URI("http://localhost:6003/rest/hello/helloObject/world"))
		                                  .build(),
		                       HttpResponse.BodyHandlers.discarding());
		assertEquals(200, response.statusCode());
		undertow.stop();
	}
}
