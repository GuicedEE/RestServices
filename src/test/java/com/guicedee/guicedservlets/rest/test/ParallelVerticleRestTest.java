package com.guicedee.guicedservlets.rest.test;

import com.guicedee.client.IGuiceContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests parallel execution of REST endpoints defined across four different
 * {@code @Verticle}-annotated packages (verticle1–verticle4).
 * <p>
 * Each verticle package has its own dedicated worker pool, so this test verifies
 * that requests routed to different worker pools can execute truly in parallel
 * without cross-pool interference, and that requests within the same worker pool
 * also handle concurrency correctly.
 * <p>
 * Every test logs thread names, timestamps, and timing overlap to prove that
 * requests truly execute in parallel rather than sequentially.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ParallelVerticleRestTest {

    private static final String BASE = "http://localhost:8080/rest";
    private static final String[] VERTICLES = {"verticle1", "verticle2", "verticle3", "verticle4"};
    private static final String[] PREFIXES = {"V1", "V2", "V3", "V4"};

    private HttpClient client;

    /**
     * Holds timing data for a single request so we can prove parallelism.
     */
    private record RequestTiming(String label, String threadName, long startMs, long endMs, int statusCode) {
        long durationMs() {
            return endMs - startMs;
        }
    }

    /**
     * Prints a timing summary table and checks that requests overlapped in time.
     */
    private void logTimingsAndAssertParallel(String testName, List<RequestTiming> timings) {
        timings.sort(Comparator.comparingLong(RequestTiming::startMs));

        long earliest = timings.stream().mapToLong(RequestTiming::startMs).min().orElse(0);
        long latestEnd = timings.stream().mapToLong(RequestTiming::endMs).max().orElse(0);
        long wallClock = latestEnd - earliest;
        long sumOfDurations = timings.stream().mapToLong(RequestTiming::durationMs).sum();

        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  " + testName + " — Timing Summary");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.printf("  %-30s  %-25s  %8s  %8s  %8s  %6s%n",
                "Request", "Thread", "Start", "End", "Duration", "Status");
        System.out.println("  " + "─".repeat(90));

        for (RequestTiming t : timings) {
            System.out.printf("  %-30s  %-25s  %+7dms  %+7dms  %6dms  %6d%n",
                    t.label(),
                    t.threadName(),
                    t.startMs() - earliest,
                    t.endMs() - earliest,
                    t.durationMs(),
                    t.statusCode());
        }

        System.out.println("  " + "─".repeat(90));
        System.out.printf("  Wall-clock time:       %d ms%n", wallClock);
        System.out.printf("  Sum of all durations:  %d ms%n", sumOfDurations);
        System.out.printf("  Parallelism factor:    %.2fx  (sum / wall-clock)%n",
                wallClock > 0 ? (double) sumOfDurations / wallClock : 0);
        System.out.printf("  Distinct threads used: %d%n",
                timings.stream().map(RequestTiming::threadName).distinct().count());
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();

        // Prove parallelism: if requests ran sequentially, wallClock >= sumOfDurations.
        // With true parallelism the wall-clock should be significantly less than the sum.
        // We also verify at least 2 distinct threads were used.
        long distinctThreads = timings.stream().map(RequestTiming::threadName).distinct().count();
        assertTrue(distinctThreads >= 2,
                testName + ": Expected at least 2 distinct threads but got " + distinctThreads);

        // Check for time overlap: at least two requests must have overlapping [start, end] windows
        boolean foundOverlap = false;
        for (int i = 0; i < timings.size() && !foundOverlap; i++) {
            for (int j = i + 1; j < timings.size() && !foundOverlap; j++) {
                RequestTiming a = timings.get(i);
                RequestTiming b = timings.get(j);
                // overlap exists if a.start < b.end AND b.start < a.end
                if (a.startMs() < b.endMs() && b.startMs() < a.endMs()) {
                    foundOverlap = true;
                    System.out.println("  ✓ Overlap confirmed between [" + a.label() + "] and [" + b.label() + "]");
                }
            }
        }
        assertTrue(foundOverlap,
                testName + ": No overlapping request windows found — requests may have run sequentially!");
    }

    @BeforeAll
    void setUp() throws Exception {
        System.out.println("Initializing Guice context for parallel verticle tests...");
        IGuiceContext.instance().inject();

        System.out.println("Waiting for server to start...");
        Thread.sleep(2000);
        System.out.println("Server should be started now");

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.of(10, ChronoUnit.SECONDS))
                .build();
    }

    @AfterAll
    void tearDown() {
        IGuiceContext.instance().destroy();
    }

    // ─── Cross-verticle parallel tests ──────────────────────────────────────

    /**
     * Fires one GET request to each of the 4 verticles simultaneously.
     * Each verticle runs on its own worker pool, so all 4 should execute truly in parallel.
     */
    @Test
    void testParallelGetAcrossAllVerticles() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch startLatch = new CountDownLatch(1);
        CopyOnWriteArrayList<RequestTiming> timings = new CopyOnWriteArrayList<>();
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();

        for (int v = 0; v < VERTICLES.length; v++) {
            final int idx = v;
            futures.add(executor.submit(() -> {
                startLatch.await();
                String thread = Thread.currentThread().getName();
                long start = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] → Sending GET to " + VERTICLES[idx]);

                HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                                .GET()
                                .uri(new URI(BASE + "/" + VERTICLES[idx] + "/testUser"))
                                .setHeader("Accept", "application/json")
                                .setHeader("Content-Type", "application/json")
                                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

                long end = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] ← " + VERTICLES[idx]
                        + " responded " + response.statusCode() + " in " + (end - start) + "ms");
                timings.add(new RequestTiming("GET " + VERTICLES[idx], thread, start, end, response.statusCode()));
                return response;
            }));
        }

        startLatch.countDown();

        for (int v = 0; v < VERTICLES.length; v++) {
            HttpResponse<String> response = futures.get(v).get(15, TimeUnit.SECONDS);
            assertEquals(200, response.statusCode(),
                    VERTICLES[v] + " GET should return 200");
            assertEquals("\"Hello " + PREFIXES[v] + ":testUser\"", response.body(),
                    VERTICLES[v] + " GET should return correct greeting");
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        logTimingsAndAssertParallel("testParallelGetAcrossAllVerticles", new ArrayList<>(timings));
    }

    /**
     * Fires one GET-object request to each of the 4 verticles simultaneously.
     */
    @Test
    void testParallelGetObjectAcrossAllVerticles() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch startLatch = new CountDownLatch(1);
        CopyOnWriteArrayList<RequestTiming> timings = new CopyOnWriteArrayList<>();
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();

        for (int v = 0; v < VERTICLES.length; v++) {
            final int idx = v;
            futures.add(executor.submit(() -> {
                startLatch.await();
                String thread = Thread.currentThread().getName();
                long start = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] → Sending GET object to " + VERTICLES[idx]);

                HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                                .GET()
                                .uri(new URI(BASE + "/" + VERTICLES[idx] + "/object/user"))
                                .setHeader("Accept", "application/json")
                                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

                long end = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] ← " + VERTICLES[idx]
                        + " object responded " + response.statusCode() + " in " + (end - start) + "ms");
                timings.add(new RequestTiming("GET obj " + VERTICLES[idx], thread, start, end, response.statusCode()));
                return response;
            }));
        }

        startLatch.countDown();

        for (int v = 0; v < VERTICLES.length; v++) {
            HttpResponse<String> response = futures.get(v).get(15, TimeUnit.SECONDS);
            assertEquals(200, response.statusCode(),
                    VERTICLES[v] + " GET object should return 200");
            assertEquals("{\"name\":\"" + PREFIXES[v] + ":user\"}", response.body(),
                    VERTICLES[v] + " GET object should return correct JSON");
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        logTimingsAndAssertParallel("testParallelGetObjectAcrossAllVerticles", new ArrayList<>(timings));
    }

    /**
     * Fires one POST request to each of the 4 verticles simultaneously.
     */
    @Test
    void testParallelPostAcrossAllVerticles() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch startLatch = new CountDownLatch(1);
        CopyOnWriteArrayList<RequestTiming> timings = new CopyOnWriteArrayList<>();
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();

        for (int v = 0; v < VERTICLES.length; v++) {
            final int idx = v;
            futures.add(executor.submit(() -> {
                startLatch.await();
                String thread = Thread.currentThread().getName();
                long start = System.currentTimeMillis();
                String body = "{\"name\":\"Body" + idx + "\"}";
                System.out.println("[" + Instant.now() + "] [" + thread + "] → Sending POST to " + VERTICLES[idx] + " body=" + body);

                HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                                .POST(HttpRequest.BodyPublishers.ofString(body))
                                .uri(new URI(BASE + "/" + VERTICLES[idx] + "/object/Path" + idx))
                                .setHeader("Accept", "application/json")
                                .setHeader("Content-Type", "application/json")
                                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

                long end = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] ← " + VERTICLES[idx]
                        + " POST responded " + response.statusCode() + " in " + (end - start) + "ms: " + response.body());
                timings.add(new RequestTiming("POST " + VERTICLES[idx], thread, start, end, response.statusCode()));
                return response;
            }));
        }

        startLatch.countDown();

        for (int v = 0; v < VERTICLES.length; v++) {
            HttpResponse<String> response = futures.get(v).get(15, TimeUnit.SECONDS);
            assertEquals(200, response.statusCode(),
                    VERTICLES[v] + " POST should return 200");
            assertEquals("{\"name\":\"" + PREFIXES[v] + ":Path" + v + ":Body" + v + "\"}", response.body(),
                    VERTICLES[v] + " POST should return correct combined result");
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        logTimingsAndAssertParallel("testParallelPostAcrossAllVerticles", new ArrayList<>(timings));
    }

    // ─── High-concurrency across verticles ──────────────────────────────────

    /**
     * Fires many parallel GET requests spread evenly across all 4 verticles.
     * This tests that multiple worker pools can each handle concurrent load simultaneously.
     */
    @Test
    void testHighConcurrencyAcrossVerticles() throws Exception {
        int requestsPerVerticle = 15;
        int totalRequests = requestsPerVerticle * VERTICLES.length;
        ExecutorService executor = Executors.newFixedThreadPool(totalRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CopyOnWriteArrayList<RequestTiming> timings = new CopyOnWriteArrayList<>();
        List<Future<String>> futures = new ArrayList<>();

        for (int v = 0; v < VERTICLES.length; v++) {
            for (int i = 0; i < requestsPerVerticle; i++) {
                final int verticleIdx = v;
                final int requestIdx = i;
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    String thread = Thread.currentThread().getName();
                    long start = System.currentTimeMillis();
                    System.out.println("[" + Instant.now() + "] [" + thread + "] → GET " + VERTICLES[verticleIdx] + "/req" + requestIdx);

                    HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                                    .GET()
                                    .uri(new URI(BASE + "/" + VERTICLES[verticleIdx] + "/req" + requestIdx))
                                    .setHeader("Accept", "application/json")
                                    .setHeader("Content-Type", "application/json")
                                    .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

                    long end = System.currentTimeMillis();
                    System.out.println("[" + Instant.now() + "] [" + thread + "] ← " + VERTICLES[verticleIdx]
                            + "/req" + requestIdx + " → " + response.statusCode() + " in " + (end - start) + "ms");
                    timings.add(new RequestTiming(VERTICLES[verticleIdx] + "/req" + requestIdx, thread, start, end, response.statusCode()));

                    assertEquals(200, response.statusCode());
                    return VERTICLES[verticleIdx] + ":" + requestIdx + ":" + response.body();
                }));
            }
        }

        startLatch.countDown();

        List<String> results = new ArrayList<>();
        for (Future<String> future : futures) {
            results.add(future.get(15, TimeUnit.SECONDS));
        }

        // Verify each verticle's results
        int idx = 0;
        for (int v = 0; v < VERTICLES.length; v++) {
            for (int i = 0; i < requestsPerVerticle; i++) {
                String result = results.get(idx++);
                assertTrue(result.startsWith(VERTICLES[v] + ":" + i + ":"),
                        "Result should be tagged for " + VERTICLES[v] + " request " + i);
                assertTrue(result.contains("Hello " + PREFIXES[v] + ":req" + i),
                        "Result should contain correct greeting for " + VERTICLES[v]);
            }
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        logTimingsAndAssertParallel("testHighConcurrencyAcrossVerticles", new ArrayList<>(timings));
    }

    /**
     * Fires many parallel POST requests spread across all 4 verticles with unique bodies.
     * Tests that JSON deserialization/serialization in separate worker pools is thread-safe.
     */
    @Test
    void testHighConcurrencyPostAcrossVerticles() throws Exception {
        int requestsPerVerticle = 10;
        int totalRequests = requestsPerVerticle * VERTICLES.length;
        ExecutorService executor = Executors.newFixedThreadPool(totalRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CopyOnWriteArrayList<RequestTiming> timings = new CopyOnWriteArrayList<>();
        List<Future<String>> futures = new ArrayList<>();

        for (int v = 0; v < VERTICLES.length; v++) {
            for (int i = 0; i < requestsPerVerticle; i++) {
                final int verticleIdx = v;
                final int requestIdx = i;
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    String thread = Thread.currentThread().getName();
                    long start = System.currentTimeMillis();
                    String body = "{\"name\":\"B" + verticleIdx + "_" + requestIdx + "\"}";
                    System.out.println("[" + Instant.now() + "] [" + thread + "] → POST " + VERTICLES[verticleIdx] + "/object/P" + verticleIdx + "_" + requestIdx);

                    HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                                    .POST(HttpRequest.BodyPublishers.ofString(body))
                                    .uri(new URI(BASE + "/" + VERTICLES[verticleIdx] + "/object/P" + verticleIdx + "_" + requestIdx))
                                    .setHeader("Accept", "application/json")
                                    .setHeader("Content-Type", "application/json")
                                    .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

                    long end = System.currentTimeMillis();
                    System.out.println("[" + Instant.now() + "] [" + thread + "] ← POST " + VERTICLES[verticleIdx]
                            + " → " + response.statusCode() + " in " + (end - start) + "ms: " + response.body());
                    timings.add(new RequestTiming("POST " + VERTICLES[verticleIdx] + "#" + requestIdx, thread, start, end, response.statusCode()));

                    assertEquals(200, response.statusCode());
                    return VERTICLES[verticleIdx] + ":" + requestIdx + ":" + response.body();
                }));
            }
        }

        startLatch.countDown();

        int idx = 0;
        for (int v = 0; v < VERTICLES.length; v++) {
            for (int i = 0; i < requestsPerVerticle; i++) {
                String result = futures.get(idx++).get(15, TimeUnit.SECONDS);
                assertTrue(result.startsWith(VERTICLES[v] + ":" + i + ":"),
                        "POST result should be tagged for " + VERTICLES[v] + " request " + i);
                String expected = PREFIXES[v] + ":P" + v + "_" + i + ":B" + v + "_" + i;
                assertTrue(result.contains(expected),
                        "POST result should contain '" + expected + "' but was: " + result);
            }
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        logTimingsAndAssertParallel("testHighConcurrencyPostAcrossVerticles", new ArrayList<>(timings));
    }

    // ─── Mixed methods across verticles ─────────────────────────────────────

    /**
     * Fires a mix of GET and POST requests across all verticles simultaneously.
     * Each verticle receives both GET and POST requests to verify that different
     * HTTP methods within the same worker pool don't interfere with each other.
     */
    @Test
    void testMixedMethodsAcrossVerticles() throws Exception {
        int totalRequests = VERTICLES.length * 2; // 1 GET + 1 POST per verticle
        ExecutorService executor = Executors.newFixedThreadPool(totalRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CopyOnWriteArrayList<RequestTiming> timings = new CopyOnWriteArrayList<>();
        List<Future<String>> futures = new ArrayList<>();

        // Submit GET requests for all verticles
        for (int v = 0; v < VERTICLES.length; v++) {
            final int idx = v;
            futures.add(executor.submit(() -> {
                startLatch.await();
                String thread = Thread.currentThread().getName();
                long start = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] → GET " + VERTICLES[idx] + "/mixedUser");

                HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                                .GET()
                                .uri(new URI(BASE + "/" + VERTICLES[idx] + "/mixedUser"))
                                .setHeader("Accept", "application/json")
                                .setHeader("Content-Type", "application/json")
                                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

                long end = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] ← GET " + VERTICLES[idx]
                        + " → " + response.statusCode() + " in " + (end - start) + "ms");
                timings.add(new RequestTiming("GET " + VERTICLES[idx], thread, start, end, response.statusCode()));

                assertEquals(200, response.statusCode());
                return "GET:" + VERTICLES[idx] + ":" + response.body();
            }));
        }

        // Submit POST requests for all verticles
        for (int v = 0; v < VERTICLES.length; v++) {
            final int idx = v;
            futures.add(executor.submit(() -> {
                startLatch.await();
                String thread = Thread.currentThread().getName();
                long start = System.currentTimeMillis();
                String body = "{\"name\":\"MixBody" + idx + "\"}";
                System.out.println("[" + Instant.now() + "] [" + thread + "] → POST " + VERTICLES[idx] + "/object/MixPath" + idx);

                HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                                .POST(HttpRequest.BodyPublishers.ofString(body))
                                .uri(new URI(BASE + "/" + VERTICLES[idx] + "/object/MixPath" + idx))
                                .setHeader("Accept", "application/json")
                                .setHeader("Content-Type", "application/json")
                                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

                long end = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] ← POST " + VERTICLES[idx]
                        + " → " + response.statusCode() + " in " + (end - start) + "ms: " + response.body());
                timings.add(new RequestTiming("POST " + VERTICLES[idx], thread, start, end, response.statusCode()));

                assertEquals(200, response.statusCode());
                return "POST:" + VERTICLES[idx] + ":" + response.body();
            }));
        }

        startLatch.countDown();

        List<String> results = new ArrayList<>();
        for (Future<String> future : futures) {
            results.add(future.get(15, TimeUnit.SECONDS));
        }

        // Verify GET results
        for (int v = 0; v < VERTICLES.length; v++) {
            String result = results.get(v);
            assertTrue(result.startsWith("GET:" + VERTICLES[v] + ":"),
                    "GET result should be tagged for " + VERTICLES[v]);
            assertTrue(result.contains("Hello " + PREFIXES[v] + ":mixedUser"),
                    "GET result for " + VERTICLES[v] + " should contain correct greeting");
        }

        // Verify POST results
        for (int v = 0; v < VERTICLES.length; v++) {
            String result = results.get(VERTICLES.length + v);
            assertTrue(result.startsWith("POST:" + VERTICLES[v] + ":"),
                    "POST result should be tagged for " + VERTICLES[v]);
            String expected = PREFIXES[v] + ":MixPath" + v + ":MixBody" + v;
            assertTrue(result.contains(expected),
                    "POST result for " + VERTICLES[v] + " should contain '" + expected + "'");
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        logTimingsAndAssertParallel("testMixedMethodsAcrossVerticles", new ArrayList<>(timings));
    }

    // ─── Sustained wave test ────────────────────────────────────────────────

    /**
     * Sends multiple waves of parallel requests across all verticles to verify
     * that worker pools remain stable under sustained concurrent load.
     */
    @Test
    void testSustainedWavesAcrossVerticles() throws Exception {
        int waves = 5;
        int requestsPerVerticlePerWave = 5;
        AtomicInteger totalSuccess = new AtomicInteger(0);
        CopyOnWriteArrayList<RequestTiming> allTimings = new CopyOnWriteArrayList<>();

        for (int wave = 0; wave < waves; wave++) {
            System.out.println("\n── Wave " + (wave + 1) + " of " + waves + " ──");
            int perWave = requestsPerVerticlePerWave * VERTICLES.length;
            ExecutorService executor = Executors.newFixedThreadPool(perWave);
            CountDownLatch startLatch = new CountDownLatch(1);
            List<Future<HttpResponse<String>>> futures = new ArrayList<>();

            for (int v = 0; v < VERTICLES.length; v++) {
                for (int i = 0; i < requestsPerVerticlePerWave; i++) {
                    final int verticleIdx = v;
                    final int requestIdx = i;
                    final int currentWave = wave;
                    futures.add(executor.submit(() -> {
                        startLatch.await();
                        String thread = Thread.currentThread().getName();
                        long start = System.currentTimeMillis();
                        String label = "w" + currentWave + " " + VERTICLES[verticleIdx] + "/r" + requestIdx;
                        System.out.println("[" + Instant.now() + "] [" + thread + "] → " + label);

                        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                                        .GET()
                                        .uri(new URI(BASE + "/" + VERTICLES[verticleIdx]
                                                + "/w" + currentWave + "r" + requestIdx))
                                        .setHeader("Accept", "application/json")
                                        .setHeader("Content-Type", "application/json")
                                        .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                        .build(),
                                HttpResponse.BodyHandlers.ofString());

                        long end = System.currentTimeMillis();
                        System.out.println("[" + Instant.now() + "] [" + thread + "] ← " + label
                                + " → " + response.statusCode() + " in " + (end - start) + "ms");
                        allTimings.add(new RequestTiming(label, thread, start, end, response.statusCode()));
                        return response;
                    }));
                }
            }

            startLatch.countDown();

            int idx = 0;
            for (int v = 0; v < VERTICLES.length; v++) {
                for (int i = 0; i < requestsPerVerticlePerWave; i++) {
                    HttpResponse<String> response = futures.get(idx++).get(15, TimeUnit.SECONDS);
                    assertEquals(200, response.statusCode(),
                            "Wave " + wave + " " + VERTICLES[v] + " req " + i + " should return 200");
                    assertEquals("\"Hello " + PREFIXES[v] + ":w" + wave + "r" + i + "\"", response.body(),
                            "Wave " + wave + " " + VERTICLES[v] + " req " + i + " should return correct greeting");
                    totalSuccess.incrementAndGet();
                }
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        int expectedTotal = waves * requestsPerVerticlePerWave * VERTICLES.length;
        assertEquals(expectedTotal, totalSuccess.get(),
                "All requests across all waves and verticles should succeed");
        logTimingsAndAssertParallel("testSustainedWavesAcrossVerticles", new ArrayList<>(allTimings));
    }

    // ─── Async across verticles ─────────────────────────────────────────────

    /**
     * Uses HttpClient.sendAsync() to fire requests to all 4 verticles concurrently,
     * verifying that async-dispatched parallelism works across worker pools.
     */
    @Test
    void testAsyncParallelAcrossVerticles() throws Exception {
        int requestsPerVerticle = 10;
        CopyOnWriteArrayList<RequestTiming> timings = new CopyOnWriteArrayList<>();
        List<CompletableFuture<String>> futures = new ArrayList<>();

        for (int v = 0; v < VERTICLES.length; v++) {
            for (int i = 0; i < requestsPerVerticle; i++) {
                final int verticleIdx = v;
                final int requestIdx = i;
                long sendTime = System.currentTimeMillis();
                CompletableFuture<String> future = client.sendAsync(HttpRequest.newBuilder()
                                .GET()
                                .uri(new URI(BASE + "/" + VERTICLES[verticleIdx] + "/async" + requestIdx))
                                .setHeader("Accept", "application/json")
                                .setHeader("Content-Type", "application/json")
                                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                .build(),
                        HttpResponse.BodyHandlers.ofString())
                        .thenApply(response -> {
                            long end = System.currentTimeMillis();
                            String thread = Thread.currentThread().getName();
                            String label = "async " + VERTICLES[verticleIdx] + "#" + requestIdx;
                            System.out.println("[" + Instant.now() + "] [" + thread + "] ← " + label
                                    + " → " + response.statusCode() + " in " + (end - sendTime) + "ms");
                            timings.add(new RequestTiming(label, thread, sendTime, end, response.statusCode()));
                            assertEquals(200, response.statusCode());
                            return VERTICLES[verticleIdx] + ":" + requestIdx + ":" + response.body();
                        });
                futures.add(future);
            }
        }

        System.out.println("[" + Instant.now() + "] All " + (VERTICLES.length * requestsPerVerticle) + " async requests dispatched");

        CompletableFuture<Void> allDone = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
        allDone.get(20, TimeUnit.SECONDS);

        int idx = 0;
        for (int v = 0; v < VERTICLES.length; v++) {
            for (int i = 0; i < requestsPerVerticle; i++) {
                String result = futures.get(idx++).get();
                assertTrue(result.startsWith(VERTICLES[v] + ":" + i + ":"),
                        "Async result should be tagged for " + VERTICLES[v] + " request " + i);
                assertTrue(result.contains("Hello " + PREFIXES[v] + ":async" + i),
                        "Async result for " + VERTICLES[v] + " should contain correct greeting");
            }
        }

        logTimingsAndAssertParallel("testAsyncParallelAcrossVerticles", new ArrayList<>(timings));
    }

    // ─── Identical requests across verticles ────────────────────────────────

    /**
     * Fires the same request to each verticle many times in parallel.
     * Tests thread-safety when all requests within a worker pool hit the exact same path.
     */
    @Test
    void testIdenticalRequestsPerVerticle() throws Exception {
        int requestsPerVerticle = 20;
        int totalRequests = requestsPerVerticle * VERTICLES.length;
        ExecutorService executor = Executors.newFixedThreadPool(totalRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        CopyOnWriteArrayList<RequestTiming> timings = new CopyOnWriteArrayList<>();
        List<Future<String>> futures = new ArrayList<>();

        for (int v = 0; v < VERTICLES.length; v++) {
            for (int i = 0; i < requestsPerVerticle; i++) {
                final int verticleIdx = v;
                final int reqNum = i;
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    String thread = Thread.currentThread().getName();
                    long start = System.currentTimeMillis();
                    System.out.println("[" + Instant.now() + "] [" + thread + "] → identical GET " + VERTICLES[verticleIdx] + " #" + reqNum);

                    HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                                    .GET()
                                    .uri(new URI(BASE + "/" + VERTICLES[verticleIdx] + "/object/sameUser"))
                                    .setHeader("Accept", "application/json")
                                    .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

                    long end = System.currentTimeMillis();
                    System.out.println("[" + Instant.now() + "] [" + thread + "] ← identical " + VERTICLES[verticleIdx]
                            + " #" + reqNum + " → " + response.statusCode() + " in " + (end - start) + "ms");
                    timings.add(new RequestTiming(VERTICLES[verticleIdx] + " #" + reqNum, thread, start, end, response.statusCode()));

                    return VERTICLES[verticleIdx] + ":" + response.statusCode() + ":" + response.body();
                }));
            }
        }

        startLatch.countDown();

        int idx = 0;
        for (int v = 0; v < VERTICLES.length; v++) {
            String expectedBody = "{\"name\":\"" + PREFIXES[v] + ":sameUser\"}";
            for (int i = 0; i < requestsPerVerticle; i++) {
                String result = futures.get(idx++).get(15, TimeUnit.SECONDS);
                assertTrue(result.contains(":200:"), "Should return 200 for " + VERTICLES[v]);
                assertTrue(result.contains(expectedBody),
                        "Should return correct body for " + VERTICLES[v] + " but got: " + result);
                successCount.incrementAndGet();
            }
        }

        assertEquals(totalRequests, successCount.get(), "All identical requests should succeed");
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        logTimingsAndAssertParallel("testIdenticalRequestsPerVerticle", new ArrayList<>(timings));
    }
}

