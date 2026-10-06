package com.taskhub;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

// A stand-in for auth-service: a JDK HTTP server whose answer each test chooses. It counts how many calls reach it.
final class FakeAuthServer {

    interface Behaviour {
        void answer(HttpExchange exchange) throws Exception;
    }

    static final String LUIS_JSON = "{\"id\":2,\"name\":\"Luis User\",\"role\":\"USER\"}";

    final AtomicInteger hits = new AtomicInteger();
    final List<String> received = new CopyOnWriteArrayList<>();
    volatile Behaviour behaviour = ex -> reply(ex, 200, LUIS_JSON);
    private final HttpServer server;

    FakeAuthServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        server.createContext("/", exchange -> {
            try {
                hits.incrementAndGet();
                received.add(exchange.getRequestURI().getPath()
                        + " authorization=" + exchange.getRequestHeaders().getFirst("Authorization")
                        + " x-request-id=" + exchange.getRequestHeaders().getFirst("X-Request-Id"));
                behaviour.answer(exchange);
            } catch (Exception e) {
                exchange.close();
            }
        });
        // The default executor is single-threaded: a deliberately slow answer would block the next test's requests.
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    String url() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    void reset() {
        hits.set(0);
        received.clear();
        behaviour = ex -> reply(ex, 200, LUIS_JSON);
    }

    void stop() {
        server.stop(0);
    }

    static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
