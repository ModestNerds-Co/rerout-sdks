/*
 * rerout-java — test support.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * A tiny single-connection mock API backed by the JDK's
 * {@link com.sun.net.httpserver.HttpServer}. No third-party mock server.
 *
 * <p>Responses are enqueued in advance; each inbound request consumes the next
 * one. Every request is recorded so tests can assert method, path, headers,
 * and body. Modelled on the {@code MockWebServer} story the Kotlin tests use.
 */
final class MockApi implements AutoCloseable {

    /** A queued, ready-to-serve mock response. */
    static final class MockResponse {
        final int status;
        final String body;
        final String contentType;
        final long delayMillis;

        private MockResponse(
                int status,
                String body,
                String contentType,
                long delayMillis) {
            this.status = status;
            this.body = body;
            this.contentType = contentType;
            this.delayMillis = delayMillis;
        }

        static MockResponse json(String body) {
            return json(body, 200);
        }

        static MockResponse json(String body, int status) {
            return new MockResponse(status, body, "application/json", 0);
        }

        static MockResponse text(String body, int status, String contentType) {
            return new MockResponse(status, body, contentType, 0);
        }

        static MockResponse status(int status) {
            return new MockResponse(status, "", null, 0);
        }

        static MockResponse delayed(String body, long delayMillis) {
            return new MockResponse(200, body, "application/json", delayMillis);
        }
    }

    /**
     * A bare TCP listener that accepts a connection and closes it immediately
     * without writing any HTTP response — simulates a mid-flight network
     * failure. Modelled on {@code MockWebServer}'s
     * {@code DISCONNECT_AT_START} socket policy.
     */
    static final class BrokenServer implements AutoCloseable {
        private final ServerSocket socket;
        private final Thread acceptor;
        private volatile boolean running = true;

        BrokenServer() {
            try {
                socket = new ServerSocket();
                socket.bind(new InetSocketAddress("127.0.0.1", 0));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to start broken server", e);
            }
            acceptor = new Thread(() -> {
                while (running) {
                    try (Socket connection = socket.accept()) {
                        // Drop the connection immediately — no HTTP reply.
                        connection.setSoLinger(true, 0);
                    } catch (IOException e) {
                        return;
                    }
                }
            }, "mock-broken-server");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        /** {@return the base URL the SDK should target} */
        String baseUrl() {
            return "http://127.0.0.1:" + socket.getLocalPort();
        }

        @Override
        public void close() {
            running = false;
            try {
                socket.close();
            } catch (IOException ignored) {
                // best-effort shutdown
            }
        }
    }

    /** A recorded inbound request. */
    static final class RecordedRequest {
        final String method;
        final String path;
        final Map<String, List<String>> headers;
        final String body;

        RecordedRequest(
                String method,
                String path,
                Map<String, List<String>> headers,
                String body) {
            this.method = method;
            this.path = path;
            this.headers = headers;
            this.body = body;
        }

        String header(String name) {
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                if (e.getKey().equalsIgnoreCase(name)) {
                    return e.getValue().isEmpty() ? null : e.getValue().get(0);
                }
            }
            return null;
        }
    }

    private final HttpServer server;
    private final Deque<MockResponse> responses = new ArrayDeque<>();
    private final List<RecordedRequest> recorded = new ArrayList<>();

    MockApi() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start mock server", e);
        }
        server.createContext("/", this::handle);
        server.start();
    }

    /** {@return the base URL the SDK should target} */
    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** {@return a Rerout client pointed at this mock with the given key} */
    Rerout client(String apiKey) {
        return Rerout.builder(apiKey).baseUrl(baseUrl()).build();
    }

    /** {@return a Rerout client pointed at this mock with a default key} */
    Rerout client() {
        return client("rrk_test");
    }

    /** Enqueue a response to be served by the next inbound request. */
    void enqueue(MockResponse response) {
        responses.addLast(response);
    }

    /** {@return the next recorded request, removing it from the log} */
    RecordedRequest takeRequest() {
        if (recorded.isEmpty()) {
            throw new IllegalStateException("No request was recorded");
        }
        return recorded.remove(0);
    }

    /** {@return the number of requests received so far} */
    int requestCount() {
        return recorded.size();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String body;
            try (InputStream in = exchange.getRequestBody()) {
                body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            String path = exchange.getRequestURI().getRawPath();
            if (exchange.getRequestURI().getRawQuery() != null) {
                path = path + "?" + exchange.getRequestURI().getRawQuery();
            }
            recorded.add(new RecordedRequest(
                    exchange.getRequestMethod(),
                    path,
                    exchange.getRequestHeaders(),
                    body));

            MockResponse response = responses.isEmpty()
                    ? MockResponse.status(500)
                    : responses.removeFirst();

            if (response.delayMillis > 0) {
                try {
                    Thread.sleep(response.delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            byte[] payload = response.body == null
                    ? new byte[0]
                    : response.body.getBytes(StandardCharsets.UTF_8);
            if (response.contentType != null) {
                exchange.getResponseHeaders().set("Content-Type", response.contentType);
            }
            // A zero-length body must be sent with a -1 length sentinel.
            exchange.sendResponseHeaders(
                    response.status, payload.length == 0 ? -1 : payload.length);
            if (payload.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(payload);
                }
            }
        } finally {
            exchange.close();
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }

    /** A sample link JSON body reused across tests. */
    static final String SAMPLE_LINK_JSON =
            "{"
            + "\"code\":\"q4\","
            + "\"short_url\":\"https://go.brand.com/q4\","
            + "\"domain_hostname\":\"go.brand.com\","
            + "\"target_url\":\"https://example.com/q4-sale\","
            + "\"project_id\":\"prj_123\","
            + "\"expires_at\":null,"
            + "\"is_active\":true,"
            + "\"seo_title\":\"Q4 Sale\","
            + "\"seo_description\":null,"
            + "\"seo_image_url\":null,"
            + "\"seo_canonical_url\":null,"
            + "\"seo_noindex\":false,"
            + "\"seo_updated_at\":null,"
            + "\"created_at\":1716000000,"
            + "\"updated_at\":1716000000"
            + "}";
}
