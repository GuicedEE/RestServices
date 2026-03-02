package com.guicedee.rest.test;
import java.net.HttpURLConnection;
import java.net.URI;
public final class TestServerReady {
    private TestServerReady() {}
    public static void waitForServer(int port, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        System.out.println("Waiting for server on port " + port + " (timeout " + timeoutMs + "ms)...");
        while (System.currentTimeMillis() < deadline) {
            try {
                HttpURLConnection conn = (HttpURLConnection) URI.create("http://127.0.0.1:" + port + "/").toURL().openConnection();
                conn.setConnectTimeout(200);
                conn.setReadTimeout(200);
                conn.setRequestMethod("GET");
                int code = conn.getResponseCode();
                conn.disconnect();
                System.out.println("Server is ready on port " + port + " (probe returned " + code + ")");
                return;
            } catch (Exception ignored) {
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for server", e);
            }
        }
        throw new IllegalStateException("Server on port " + port + " did not start within " + timeoutMs + "ms");
    }
    public static void waitForServer() {
        waitForServer(8080, 5_000);
    }
}