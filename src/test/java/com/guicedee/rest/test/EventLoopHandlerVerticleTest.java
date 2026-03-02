package com.guicedee.rest.test;

import com.guicedee.rest.pathing.EventLoopHandler;
import com.guicedee.vertx.spi.Verticle;
import com.guicedee.vertx.spi.VerticleBuilder;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import org.junit.jupiter.api.*;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for verifying that REST service methods are dispatched to the correct
 * Vert.x worker pool based on {@code @Verticle} package annotations.
 *
 * <p>Covers:
 * <ul>
 *   <li>Worker pool resolution via {@link VerticleBuilder#getVerticleAnnotation(Class)}</li>
 *   <li>Blocking task dispatch to the named worker pool</li>
 *   <li>Fallback to the default worker pool when no verticle annotation exists</li>
 *   <li>Event-loop execution for non-blocking (Future/Uni) return types</li>
 *   <li>Most-specific package prefix matching</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EventLoopHandlerVerticleTest {

    private Vertx vertx;

    @BeforeAll
    void setUp() {
        vertx = Vertx.vertx();
        // Clear any prior state
        VerticleBuilder.getVerticleAnnotations().clear();
    }

    @AfterAll
    void tearDown() {
        VerticleBuilder.getVerticleAnnotations().clear();
        if (vertx != null) {
            vertx.close();
        }
    }

    // ─── Helper to build a synthetic @Verticle annotation ───────────────────

    private static Verticle createVerticleAnnotation(String workerPoolName, int workerPoolSize) {
        return new Verticle() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return Verticle.class;
            }

            @Override
            public ThreadingModel threadingModel() {
                return ThreadingModel.EVENT_LOOP;
            }

            @Override
            public int defaultInstances() {
                return 1;
            }

            @Override
            public boolean ha() {
                return false;
            }

            @Override
            public String value() {
                return workerPoolName;
            }

            @Override
            public int workerPoolSize() {
                return workerPoolSize;
            }

            @Override
            public long maxWorkerExecuteTime() {
                return 2;
            }

            @Override
            public TimeUnit maxWorkerExecuteTimeUnit() {
                return TimeUnit.MINUTES;
            }

            @Override
            public Capabilities[] capabilities() {
                return new Capabilities[0];
            }
        };
    }

    // ─── Dummy classes in simulated packages for resolution tests ───────────

    // HelloResource is in com.guicedee.guicedservlets.rest.test

    // ─── getVerticleAnnotation tests ────────────────────────────────────────

    @Test
    @Order(1)
    void testGetVerticleAnnotation_noAnnotations_returnsEmpty() {
        VerticleBuilder.getVerticleAnnotations().clear();
        Optional<Verticle> result = VerticleBuilder.getVerticleAnnotation(HelloResource.class);
        assertTrue(result.isEmpty(), "Should return empty when no verticle annotations are registered");
    }

    @Test
    @Order(2)
    void testGetVerticleAnnotation_matchingPackage_returnsAnnotation() {
        VerticleBuilder.getVerticleAnnotations().clear();
        Verticle annotation = createVerticleAnnotation("test-rest-pool", 5);
        // Register for the package that HelloResource lives in
        VerticleBuilder.getVerticleAnnotations().put("com.guicedee.guicedservlets.rest.test", annotation);

        Optional<Verticle> result = VerticleBuilder.getVerticleAnnotation(HelloResource.class);
        assertTrue(result.isPresent(), "Should match the package annotation");
        assertEquals("test-rest-pool", result.get().value());
    }

    @Test
    @Order(3)
    void testGetVerticleAnnotation_parentPackage_matchesViaPrefix() {
        VerticleBuilder.getVerticleAnnotations().clear();
        Verticle annotation = createVerticleAnnotation("rest-pool", 10);
        // Register for a parent package
        VerticleBuilder.getVerticleAnnotations().put("com.guicedee.guicedservlets.rest", annotation);

        Optional<Verticle> result = VerticleBuilder.getVerticleAnnotation(HelloResource.class);
        assertTrue(result.isPresent(), "Should match via parent package prefix");
        assertEquals("rest-pool", result.get().value());
    }

    @Test
    @Order(4)
    void testGetVerticleAnnotation_mostSpecificWins() {
        VerticleBuilder.getVerticleAnnotations().clear();
        Verticle parentAnnotation = createVerticleAnnotation("parent-pool", 5);
        Verticle childAnnotation = createVerticleAnnotation("child-pool", 3);
        VerticleBuilder.getVerticleAnnotations().put("com.guicedee.guicedservlets.rest", parentAnnotation);
        VerticleBuilder.getVerticleAnnotations().put("com.guicedee.guicedservlets.rest.test", childAnnotation);

        Optional<Verticle> result = VerticleBuilder.getVerticleAnnotation(HelloResource.class);
        assertTrue(result.isPresent(), "Should resolve to the most specific match");
        assertEquals("child-pool", result.get().value(),
                "Most specific (longest prefix) package should win");
    }

    @Test
    @Order(5)
    void testGetVerticleAnnotation_unrelatedPackage_noMatch() {
        VerticleBuilder.getVerticleAnnotations().clear();
        Verticle annotation = createVerticleAnnotation("other-pool", 5);
        VerticleBuilder.getVerticleAnnotations().put("com.example.other", annotation);

        Optional<Verticle> result = VerticleBuilder.getVerticleAnnotation(HelloResource.class);
        assertTrue(result.isEmpty(), "Should not match an unrelated package");
    }

    @Test
    @Order(6)
    void testGetVerticleAnnotation_emptyKeyIgnored() {
        VerticleBuilder.getVerticleAnnotations().clear();
        Verticle defaultAnnotation = createVerticleAnnotation("default-pool", 5);
        VerticleBuilder.getVerticleAnnotations().put("", defaultAnnotation);

        Optional<Verticle> result = VerticleBuilder.getVerticleAnnotation(HelloResource.class);
        assertTrue(result.isEmpty(), "Empty key (default verticle) should be excluded from matching");
    }

    // ─── shouldRunOnWorkerThread tests ──────────────────────────────────────

    @Test
    @Order(10)
    void testShouldRunOnWorkerThread_blockingReturnType() throws NoSuchMethodException {
        // HelloResource.hello returns String → blocking
        Method method = HelloResource.class.getMethod("hello", String.class);
        assertTrue(EventLoopHandler.shouldRunOnWorkerThread(method),
                "Methods returning String should run on a worker thread");
    }

    @Test
    @Order(11)
    void testShouldRunOnWorkerThread_futureReturnType() throws NoSuchMethodException {
        // Use a method returning a Future type from our test helper
        Method method = FutureReturningResource.class.getMethod("asyncGet");
        assertFalse(EventLoopHandler.shouldRunOnWorkerThread(method),
                "Methods returning Future should NOT run on a worker thread");
    }

    @Test
    @Order(12)
    void testShouldRunOnWorkerThread_uniReturnType() throws NoSuchMethodException {
        Method method = UniReturningResource.class.getMethod("reactiveGet");
        assertFalse(EventLoopHandler.shouldRunOnWorkerThread(method),
                "Methods returning Uni should NOT run on a worker thread");
    }

    // ─── executeTask dispatch tests ─────────────────────────────────────────

    @Test
    @Order(20)
    void testExecuteTask_dispatchesToNamedWorkerPool() throws Exception {
        VerticleBuilder.getVerticleAnnotations().clear();
        Verticle annotation = createVerticleAnnotation("test-dispatch-pool", 4);
        VerticleBuilder.getVerticleAnnotations().put("com.guicedee.guicedservlets.rest.test", annotation);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> executingThread = new AtomicReference<>();

        // Create a minimal routing context via a real route
        Router router = Router.router(vertx);
        router.get("/dispatch-test").handler(ctx -> {
            Method method;
            try {
                method = HelloResource.class.getMethod("hello", String.class);
            } catch (NoSuchMethodException e) {
                ctx.response().setStatusCode(500).end("Method not found");
                return;
            }

            EventLoopHandler.executeTask(vertx, ctx, () -> {
                executingThread.set(Thread.currentThread().getName());
                ctx.response()
                        .putHeader("content-type", "text/plain")
                        .end("dispatched");
                latch.countDown();
            }, method, HelloResource.class);
        });

        var server = vertx.createHttpServer();
        server.requestHandler(router);
        CountDownLatch serverLatch = new CountDownLatch(1);
        server.listen(0).onSuccess(s -> serverLatch.countDown());
        assertTrue(serverLatch.await(5, TimeUnit.SECONDS), "Server should start");

        int port = server.actualPort();
        var client = java.net.http.HttpClient.newHttpClient();
        client.send(java.net.http.HttpRequest.newBuilder()
                .GET()
                .uri(java.net.URI.create("http://localhost:" + port + "/dispatch-test"))
                .build(), java.net.http.HttpResponse.BodyHandlers.ofString());

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Task should complete");
        assertNotNull(executingThread.get(), "Thread name should be captured");
        assertTrue(executingThread.get().contains("test-dispatch-pool"),
                "Task should execute on the named worker pool thread, but ran on: " + executingThread.get());


        server.close();
    }

    @Test
    @Order(21)
    void testExecuteTask_fallsBackToDefaultPool() throws Exception {
        VerticleBuilder.getVerticleAnnotations().clear();
        // No verticle annotations registered → should use default pool

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> executingThread = new AtomicReference<>();

        Router router = Router.router(vertx);
        router.get("/default-pool-test").handler(ctx -> {
            Method method;
            try {
                method = HelloResource.class.getMethod("hello", String.class);
            } catch (NoSuchMethodException e) {
                ctx.response().setStatusCode(500).end("Method not found");
                return;
            }

            EventLoopHandler.executeTask(vertx, ctx, () -> {
                executingThread.set(Thread.currentThread().getName());
                ctx.response()
                        .putHeader("content-type", "text/plain")
                        .end("default-dispatched");
                latch.countDown();
            }, method, HelloResource.class);
        });

        var server = vertx.createHttpServer();
        server.requestHandler(router);
        CountDownLatch serverLatch = new CountDownLatch(1);
        server.listen(0).onSuccess(s -> serverLatch.countDown());
        assertTrue(serverLatch.await(5, TimeUnit.SECONDS), "Server should start");

        int port = server.actualPort();
        var client = java.net.http.HttpClient.newHttpClient();
        client.send(java.net.http.HttpRequest.newBuilder()
                .GET()
                .uri(java.net.URI.create("http://localhost:" + port + "/default-pool-test"))
                .build(), java.net.http.HttpResponse.BodyHandlers.ofString());

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Task should complete");
        assertNotNull(executingThread.get(), "Thread name should be captured");
        // Should NOT contain our named pool
        assertFalse(executingThread.get().contains("test-dispatch-pool"),
                "Task should NOT execute on a named pool when none is configured, ran on: " + executingThread.get());

        server.close();
    }

    @Test
    @Order(22)
    void testExecuteTask_eventLoopMethod_runsInline() throws Exception {
        VerticleBuilder.getVerticleAnnotations().clear();
        Verticle annotation = createVerticleAnnotation("should-not-use-pool", 4);
        VerticleBuilder.getVerticleAnnotations().put("com.guicedee.guicedservlets.rest.test", annotation);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> executingThread = new AtomicReference<>();

        Router router = Router.router(vertx);
        router.get("/event-loop-test").handler(ctx -> {
            Method method;
            try {
                method = FutureReturningResource.class.getMethod("asyncGet");
            } catch (NoSuchMethodException e) {
                ctx.response().setStatusCode(500).end("Method not found");
                return;
            }

            // This method returns Future → should NOT use executeBlocking
            EventLoopHandler.executeTask(vertx, ctx, () -> {
                executingThread.set(Thread.currentThread().getName());
                ctx.response()
                        .putHeader("content-type", "text/plain")
                        .end("event-loop");
                latch.countDown();
            }, method, FutureReturningResource.class);
        });

        var server = vertx.createHttpServer();
        server.requestHandler(router);
        CountDownLatch serverLatch = new CountDownLatch(1);
        server.listen(0).onSuccess(s -> serverLatch.countDown());
        assertTrue(serverLatch.await(5, TimeUnit.SECONDS), "Server should start");

        int port = server.actualPort();
        var client = java.net.http.HttpClient.newHttpClient();
        client.send(java.net.http.HttpRequest.newBuilder()
                .GET()
                .uri(java.net.URI.create("http://localhost:" + port + "/event-loop-test"))
                .build(), java.net.http.HttpResponse.BodyHandlers.ofString());

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Task should complete");
        assertNotNull(executingThread.get(), "Thread name should be captured");
        // Event loop threads are named "vert.x-eventloop-thread-X"
        assertTrue(executingThread.get().contains("vert.x-eventloop"),
                "Event-loop method should run on the event loop thread, but ran on: " + executingThread.get());

        server.close();
    }

    @Test
    @Order(23)
    void testExecuteTask_differentPoolsForDifferentPackages() throws Exception {
        VerticleBuilder.getVerticleAnnotations().clear();
        Verticle poolA = createVerticleAnnotation("pool-a", 2);
        Verticle poolB = createVerticleAnnotation("pool-b", 2);
        VerticleBuilder.getVerticleAnnotations().put("com.guicedee.guicedservlets.rest.test", poolA);
        VerticleBuilder.getVerticleAnnotations().put("com.example.other", poolB);

        // HelloResource is in com.guicedee.guicedservlets.rest.test → should get pool-a
        Optional<Verticle> resolved = VerticleBuilder.getVerticleAnnotation(HelloResource.class);
        assertTrue(resolved.isPresent());
        assertEquals("pool-a", resolved.get().value(),
                "HelloResource should resolve to pool-a");

        // A class in com.example.other would get pool-b
        // (We can't easily create classes in other packages at runtime, so we verify the map directly)
        String otherPackage = "com.example.other.SomeResource";
        Optional<Verticle> otherResolved = VerticleBuilder.getVerticleAnnotations().entrySet().stream()
                .filter(entry -> !entry.getKey().isEmpty() && otherPackage.startsWith(entry.getKey()))
                .max(java.util.Comparator.comparingInt(e -> e.getKey().length()))
                .map(Map.Entry::getValue);
        assertTrue(otherResolved.isPresent());
        assertEquals("pool-b", otherResolved.get().value());
    }

    // ─── Helper classes for return-type tests ───────────────────────────────

    /**
     * Dummy resource that returns a Future (non-blocking).
     */
    public static class FutureReturningResource {
        public java.util.concurrent.Future<String> asyncGet() {
            return java.util.concurrent.CompletableFuture.completedFuture("async");
        }
    }

    /**
     * Dummy resource that returns a Uni (non-blocking).
     */
    public static class UniReturningResource {
        public io.smallrye.mutiny.Uni<String> reactiveGet() {
            return io.smallrye.mutiny.Uni.createFrom().item("reactive");
        }
    }
}

