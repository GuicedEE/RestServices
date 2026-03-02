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
 * Tests that REST endpoints can handle parallel/concurrent requests correctly.
 * Verifies that the same endpoint can be invoked simultaneously from multiple threads
 * and that each request receives its own correct response.
 * <p>
 * Every test logs thread names, timestamps, and timing overlap to prove that
 * requests truly execute in parallel rather than sequentially.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ParallelRestTest {

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
        logTimingsAndAssertParallel(testName, timings, true);
    }

    /**
     * Prints a timing summary table and checks that requests overlapped in time.
     *
     * @param requireDistinctThreads when false, skips the distinct-thread assertion
     *                               (useful for async tests where callbacks coalesce onto one thread)
     */
    private void logTimingsAndAssertParallel(String testName, List<RequestTiming> timings, boolean requireDistinctThreads) {
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

        long distinctThreads = timings.stream().map(RequestTiming::threadName).distinct().count();
        if (requireDistinctThreads) {
            assertTrue(distinctThreads >= 2,
                    testName + ": Expected at least 2 distinct threads but got " + distinctThreads);
        }

        boolean foundOverlap = false;
        for (int i = 0; i < timings.size() && !foundOverlap; i++) {
            for (int j = i + 1; j < timings.size() && !foundOverlap; j++) {
                RequestTiming a = timings.get(i);
                RequestTiming b = timings.get(j);
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
        System.out.println("Initializing Guice context for parallel tests...");
        IGuiceContext.instance().inject();

        System.out.println("Waiting for server to start...");
        TestServerReady.waitForServer();
        System.out.println("Server should be started now");

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.of(10, ChronoUnit.SECONDS))
                .build();
    }

    @AfterAll
    void tearDown() {
        IGuiceContext.instance().destroy();
    }

    /**
     * Tests that multiple GET requests to the same endpoint can execute in parallel,
     * each with a different path parameter, and each returns the correct response.
     */
    @Test
    void testParallelGetRequests() throws Exception {
        int parallelCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(parallelCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CopyOnWriteArrayList<RequestTiming> timings = new CopyOnWriteArrayList<>();
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();

        for (int i = 0; i < parallelCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                startLatch.await();
                String thread = Thread.currentThread().getName();
                long start = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] → GET /hello/user" + index);

                HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                                .GET()
                                .uri(new URI("http://localhost:8080/rest/hello/user" + index))
                                .setHeader("Accept", "application/json")
                                .setHeader("Content-Type", "application/json")
                                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

                long end = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] ← user" + index
                        + " → " + response.statusCode() + " in " + (end - start) + "ms");
                timings.add(new RequestTiming("GET user" + index, thread, start, end, response.statusCode()));
                return response;
            }));
        }

        startLatch.countDown();

        for (int i = 0; i < parallelCount; i++) {
            HttpResponse<String> response = futures.get(i).get(15, TimeUnit.SECONDS);
            assertEquals(200, response.statusCode(),
                    "Parallel GET request " + i + " should return 200");
            assertEquals("\"Hello user" + i + "\"", response.body(),
                    "Parallel GET request " + i + " should return correct greeting");
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        logTimingsAndAssertParallel("testParallelGetRequests", new ArrayList<>(timings));
    }

    /**
     * Tests that multiple GET requests to the helloObject endpoint can execute in parallel
     * and each returns a correctly serialized JSON object.
     */
    @Test
    void testParallelGetObjectRequests() throws Exception {
        int parallelCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(parallelCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CopyOnWriteArrayList<RequestTiming> timings = new CopyOnWriteArrayList<>();
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();

        for (int i = 0; i < parallelCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                startLatch.await();
                String thread = Thread.currentThread().getName();
                long start = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] → GET /hello/helloObject/parallel" + index);

                HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                                .GET()
                                .uri(new URI("http://localhost:8080/rest/hello/helloObject/parallel" + index))
                                .setHeader("Accept", "application/json")
                                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

                long end = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] ← parallel" + index
                        + " → " + response.statusCode() + " in " + (end - start) + "ms");
                timings.add(new RequestTiming("GET obj parallel" + index, thread, start, end, response.statusCode()));
                return response;
            }));
        }

        startLatch.countDown();

        for (int i = 0; i < parallelCount; i++) {
            HttpResponse<String> response = futures.get(i).get(15, TimeUnit.SECONDS);
            assertEquals(200, response.statusCode(),
                    "Parallel GET object request " + i + " should return 200");
            assertEquals("{\"name\":\"parallel" + i + "\"}", response.body(),
                    "Parallel GET object request " + i + " should return correct JSON");
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        logTimingsAndAssertParallel("testParallelGetObjectRequests", new ArrayList<>(timings));
    }

    /**
     * Tests that multiple POST requests to the same endpoint can execute in parallel,
     * each with a different request body, and each returns the correct combined response.
     */
    @Test
    void testParallelPostRequests() throws Exception {
        int parallelCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(parallelCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CopyOnWriteArrayList<RequestTiming> timings = new CopyOnWriteArrayList<>();
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();

        for (int i = 0; i < parallelCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                startLatch.await();
                String thread = Thread.currentThread().getName();
                long start = System.currentTimeMillis();
                String requestBody = "{\"name\":\"Body" + index + "\"}";
                System.out.println("[" + Instant.now() + "] [" + thread + "] → POST /hello/helloObject/Path" + index);

                HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                                .uri(new URI("http://localhost:8080/rest/hello/helloObject/Path" + index))
                                .setHeader("Accept", "application/json")
                                .setHeader("Content-Type", "application/json")
                                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

                long end = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] ← POST Path" + index
                        + " → " + response.statusCode() + " in " + (end - start) + "ms: " + response.body());
                timings.add(new RequestTiming("POST Path" + index, thread, start, end, response.statusCode()));
                return response;
            }));
        }

        startLatch.countDown();

        for (int i = 0; i < parallelCount; i++) {
            HttpResponse<String> response = futures.get(i).get(15, TimeUnit.SECONDS);
            assertEquals(200, response.statusCode(),
                    "Parallel POST request " + i + " should return 200");
            assertEquals("{\"name\":\"Path" + i + "Body" + i + "\"}", response.body(),
                    "Parallel POST request " + i + " should return correctly concatenated result");
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        logTimingsAndAssertParallel("testParallelPostRequests", new ArrayList<>(timings));
    }

    /**
     * Tests that multiple PUT requests to the same endpoint can execute in parallel.
     */
    @Test
    void testParallelPutRequests() throws Exception {
        int parallelCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(parallelCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CopyOnWriteArrayList<RequestTiming> timings = new CopyOnWriteArrayList<>();
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();

        for (int i = 0; i < parallelCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                startLatch.await();
                String thread = Thread.currentThread().getName();
                long start = System.currentTimeMillis();
                String requestBody = "{\"name\":\"PutBody" + index + "\"}";
                System.out.println("[" + Instant.now() + "] [" + thread + "] → PUT /hello/helloObject/PutPath" + index);

                HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                                .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                                .uri(new URI("http://localhost:8080/rest/hello/helloObject/PutPath" + index))
                                .setHeader("Accept", "application/json")
                                .setHeader("Content-Type", "application/json")
                                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

                long end = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] ← PUT PutPath" + index
                        + " → " + response.statusCode() + " in " + (end - start) + "ms: " + response.body());
                timings.add(new RequestTiming("PUT PutPath" + index, thread, start, end, response.statusCode()));
                return response;
            }));
        }

        startLatch.countDown();

        for (int i = 0; i < parallelCount; i++) {
            HttpResponse<String> response = futures.get(i).get(15, TimeUnit.SECONDS);
            assertEquals(200, response.statusCode(),
                    "Parallel PUT request " + i + " should return 200");
            assertEquals("{\"name\":\"PUT:PutPath" + i + "PutBody" + i + "\"}", response.body(),
                    "Parallel PUT request " + i + " should return correctly prefixed result");
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        logTimingsAndAssertParallel("testParallelPutRequests", new ArrayList<>(timings));
    }

    /**
     * Tests a mix of GET, POST, and PUT requests executing in parallel against different endpoints.
     * Ensures that different HTTP methods don't interfere with each other when executed concurrently.
     */
    @Test
    void testParallelMixedMethodRequests() throws Exception {
        int requestsPerMethod = 10;
        int totalRequests = requestsPerMethod * 3; // GET, POST, PUT
        ExecutorService executor = Executors.newFixedThreadPool(totalRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        CopyOnWriteArrayList<RequestTiming> timings = new CopyOnWriteArrayList<>();
        List<Future<String>> futures = new ArrayList<>();

        // Submit GET requests
        for (int i = 0; i < requestsPerMethod; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                startLatch.await();
                String thread = Thread.currentThread().getName();
                long start = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] → GET /hello/mixed" + index);

                HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                                .GET()
                                .uri(new URI("http://localhost:8080/rest/hello/mixed" + index))
                                .setHeader("Accept", "application/json")
                                .setHeader("Content-Type", "application/json")
                                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

                long end = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] ← GET mixed" + index
                        + " → " + response.statusCode() + " in " + (end - start) + "ms");
                timings.add(new RequestTiming("GET mixed" + index, thread, start, end, response.statusCode()));

                assertEquals(200, response.statusCode(), "Mixed GET " + index + " should return 200");
                return "GET:" + index + ":" + response.body();
            }));
        }

        // Submit POST requests
        for (int i = 0; i < requestsPerMethod; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                startLatch.await();
                String thread = Thread.currentThread().getName();
                long start = System.currentTimeMillis();
                String requestBody = "{\"name\":\"MixPost" + index + "\"}";
                System.out.println("[" + Instant.now() + "] [" + thread + "] → POST /hello/helloObject/MixPath" + index);

                HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                                .uri(new URI("http://localhost:8080/rest/hello/helloObject/MixPath" + index))
                                .setHeader("Accept", "application/json")
                                .setHeader("Content-Type", "application/json")
                                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

                long end = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] ← POST MixPath" + index
                        + " → " + response.statusCode() + " in " + (end - start) + "ms: " + response.body());
                timings.add(new RequestTiming("POST MixPath" + index, thread, start, end, response.statusCode()));

                assertEquals(200, response.statusCode(), "Mixed POST " + index + " should return 200");
                return "POST:" + index + ":" + response.body();
            }));
        }

        // Submit PUT requests
        for (int i = 0; i < requestsPerMethod; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                startLatch.await();
                String thread = Thread.currentThread().getName();
                long start = System.currentTimeMillis();
                String requestBody = "{\"name\":\"MixPut" + index + "\"}";
                System.out.println("[" + Instant.now() + "] [" + thread + "] → PUT /hello/helloObject/MixPutPath" + index);

                HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                                .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                                .uri(new URI("http://localhost:8080/rest/hello/helloObject/MixPutPath" + index))
                                .setHeader("Accept", "application/json")
                                .setHeader("Content-Type", "application/json")
                                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

                long end = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] ← PUT MixPutPath" + index
                        + " → " + response.statusCode() + " in " + (end - start) + "ms: " + response.body());
                timings.add(new RequestTiming("PUT MixPutPath" + index, thread, start, end, response.statusCode()));

                assertEquals(200, response.statusCode(), "Mixed PUT " + index + " should return 200");
                return "PUT:" + index + ":" + response.body();
            }));
        }

        // Fire all at once
        startLatch.countDown();

        // Verify all results
        List<String> results = new ArrayList<>();
        for (Future<String> future : futures) {
            results.add(future.get(15, TimeUnit.SECONDS));
        }

        // Verify GET responses
        for (int i = 0; i < requestsPerMethod; i++) {
            String result = results.get(i);
            assertTrue(result.startsWith("GET:" + i + ":"), "GET result should be properly tagged");
            assertTrue(result.contains("Hello mixed" + i), "GET result should contain correct greeting");
        }

        // Verify POST responses
        for (int i = 0; i < requestsPerMethod; i++) {
            String result = results.get(requestsPerMethod + i);
            assertTrue(result.startsWith("POST:" + i + ":"), "POST result should be properly tagged");
            assertTrue(result.contains("MixPath" + i + "MixPost" + i), "POST result should contain combined path and body");
        }

        // Verify PUT responses
        for (int i = 0; i < requestsPerMethod; i++) {
            String result = results.get(2 * requestsPerMethod + i);
            assertTrue(result.startsWith("PUT:" + i + ":"), "PUT result should be properly tagged");
            assertTrue(result.contains("PUT:MixPutPath" + i + "MixPut" + i), "PUT result should contain prefixed combined path and body");
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        logTimingsAndAssertParallel("testParallelMixedMethodRequests", new ArrayList<>(timings));
    }

    /**
     * Tests the complex POST endpoint with parallel requests containing different DTOs.
     * Verifies that complex JSON deserialization works correctly under concurrency.
     */
    @Test
    void testParallelComplexPostRequests() throws Exception {
        int parallelCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(parallelCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CopyOnWriteArrayList<RequestTiming> timings = new CopyOnWriteArrayList<>();
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();

        for (int i = 0; i < parallelCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                startLatch.await();
                String thread = Thread.currentThread().getName();
                long start = System.currentTimeMillis();
                String requestBody = "{\"type\":\"Type" + index + "\",\"classification\":\"Class" + index
                        + "\",\"metadata\":{\"key\":\"val" + index + "\"}}";
                System.out.println("[" + Instant.now() + "] [" + thread + "] → POST /hello/complex/Name" + index);

                HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                                .uri(new URI("http://localhost:8080/rest/hello/complex/Name" + index))
                                .setHeader("Accept", "application/json")
                                .setHeader("Content-Type", "application/json")
                                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

                long end = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] ← complex Name" + index
                        + " → " + response.statusCode() + " in " + (end - start) + "ms: " + response.body());
                timings.add(new RequestTiming("POST complex Name" + index, thread, start, end, response.statusCode()));
                return response;
            }));
        }

        startLatch.countDown();

        for (int i = 0; i < parallelCount; i++) {
            HttpResponse<String> response = futures.get(i).get(15, TimeUnit.SECONDS);
            assertEquals(200, response.statusCode(),
                    "Parallel complex POST request " + i + " should return 200");
            assertEquals("{\"name\":\"Name" + i + ":Type" + i + ":Class" + i + ":1\"}", response.body(),
                    "Parallel complex POST request " + i + " should return correctly formatted result");
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        logTimingsAndAssertParallel("testParallelComplexPostRequests", new ArrayList<>(timings));
    }

    /**
     * Tests that the same endpoint can handle a high volume of identical requests concurrently.
     * All requests use the same path parameter to test thread-safety of the endpoint handler itself.
     */
    @Test
    void testParallelIdenticalRequests() throws Exception {
        int parallelCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(parallelCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        CopyOnWriteArrayList<RequestTiming> timings = new CopyOnWriteArrayList<>();
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();

        for (int i = 0; i < parallelCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                startLatch.await();
                String thread = Thread.currentThread().getName();
                long start = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] → identical GET sameUser #" + index);

                HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                                .GET()
                                .uri(new URI("http://localhost:8080/rest/hello/helloObject/sameUser"))
                                .setHeader("Accept", "application/json")
                                .timeout(Duration.of(10, ChronoUnit.SECONDS))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

                long end = System.currentTimeMillis();
                System.out.println("[" + Instant.now() + "] [" + thread + "] ← identical sameUser #" + index
                        + " → " + response.statusCode() + " in " + (end - start) + "ms");
                timings.add(new RequestTiming("identical #" + index, thread, start, end, response.statusCode()));
                return response;
            }));
        }

        startLatch.countDown();

        for (Future<HttpResponse<String>> future : futures) {
            HttpResponse<String> response = future.get(15, TimeUnit.SECONDS);
            if (response.statusCode() == 200
                    && "{\"name\":\"sameUser\"}".equals(response.body())) {
                successCount.incrementAndGet();
            } else {
                failureCount.incrementAndGet();
                System.err.println("Unexpected response: " + response.statusCode() + " - " + response.body());
            }
        }

        assertEquals(parallelCount, successCount.get(),
                "All " + parallelCount + " identical parallel requests should succeed");
        assertEquals(0, failureCount.get(), "No requests should fail");

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        logTimingsAndAssertParallel("testParallelIdenticalRequests", new ArrayList<>(timings));
    }

    /**
     * Tests parallel requests using the async sendAsync method of HttpClient,
     * verifying that the server handles async-dispatched parallel requests correctly.
     */
    @Test
    void testParallelAsyncRequests() throws Exception {
        int parallelCount = 30;
        CopyOnWriteArrayList<RequestTiming> timings = new CopyOnWriteArrayList<>();
        List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>();

        for (int i = 0; i < parallelCount; i++) {
            final int index = i;
            long sendTime = System.currentTimeMillis();
            futures.add(client.sendAsync(HttpRequest.newBuilder()
                            .GET()
                            .uri(new URI("http://localhost:8080/rest/hello/async" + index))
                            .setHeader("Accept", "application/json")
                            .setHeader("Content-Type", "application/json")
                            .timeout(Duration.of(10, ChronoUnit.SECONDS))
                            .build(),
                    HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        long end = System.currentTimeMillis();
                        String thread = Thread.currentThread().getName();
                        System.out.println("[" + Instant.now() + "] [" + thread + "] ← async #" + index
                                + " → " + response.statusCode() + " in " + (end - sendTime) + "ms");
                        timings.add(new RequestTiming("async #" + index, thread, sendTime, end, response.statusCode()));
                        return response;
                    }));
        }

        System.out.println("[" + Instant.now() + "] All " + parallelCount + " async requests dispatched");

        // Wait for all futures and verify
        CompletableFuture<Void> allDone = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
        allDone.get(15, TimeUnit.SECONDS);

        for (int i = 0; i < parallelCount; i++) {
            HttpResponse<String> response = futures.get(i).get();
            assertEquals(200, response.statusCode(),
                    "Async parallel request " + i + " should return 200");
            assertEquals("\"Hello async" + i + "\"", response.body(),
                    "Async parallel request " + i + " should return correct greeting");
        }

        logTimingsAndAssertParallel("testParallelAsyncRequests", new ArrayList<>(timings), false);
    }

    /**
     * Stress test: sends multiple waves of parallel requests to verify the server
     * remains stable and responsive under sustained concurrent load.
     */
    @Test
    void testSustainedParallelLoad() throws Exception {
        int waves = 5;
        int requestsPerWave = 15;
        AtomicInteger totalSuccess = new AtomicInteger(0);
        CopyOnWriteArrayList<RequestTiming> allTimings = new CopyOnWriteArrayList<>();

        for (int wave = 0; wave < waves; wave++) {
            System.out.println("\n── Wave " + (wave + 1) + " of " + waves + " ──");
            ExecutorService executor = Executors.newFixedThreadPool(requestsPerWave);
            CountDownLatch startLatch = new CountDownLatch(1);
            List<Future<HttpResponse<String>>> futures = new ArrayList<>();

            for (int i = 0; i < requestsPerWave; i++) {
                final int index = i;
                final int currentWave = wave;
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    String thread = Thread.currentThread().getName();
                    long start = System.currentTimeMillis();
                    String label = "w" + currentWave + "/req" + index;
                    System.out.println("[" + Instant.now() + "] [" + thread + "] → " + label);

                    HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                                    .GET()
                                    .uri(new URI("http://localhost:8080/rest/hello/wave" + currentWave + "req" + index))
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

            startLatch.countDown();

            for (int i = 0; i < requestsPerWave; i++) {
                HttpResponse<String> response = futures.get(i).get(15, TimeUnit.SECONDS);
                assertEquals(200, response.statusCode(),
                        "Wave " + wave + " request " + i + " should return 200");
                assertEquals("\"Hello wave" + wave + "req" + i + "\"", response.body(),
                        "Wave " + wave + " request " + i + " should return correct greeting");
                totalSuccess.incrementAndGet();
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        assertEquals(waves * requestsPerWave, totalSuccess.get(),
                "All requests across all waves should succeed");
        logTimingsAndAssertParallel("testSustainedParallelLoad", new ArrayList<>(allTimings));
    }
}

