package de.winniepat.easyhttp;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

final class TestServer implements AutoCloseable {

    record Recorded(String method, String contentType, String body, Map<String, List<String>> headers) {
        String header(String name) {
            List<String> values = headers.get(name);
            return values == null || values.isEmpty() ? null : values.get(0);
        }
    }

    private final HttpServer server;
    private final AtomicReference<Recorded> last = new AtomicReference<>();

    TestServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    void respond(String path, int status, String contentType, byte[] body) {
        server.createContext(path, exchange -> {
            drain(exchange.getRequestBody());
            if (contentType != null) {
                exchange.getResponseHeaders().add("Content-Type", contentType);
            }
            exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            if (body.length > 0) {
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
    }

    void respond(String path, int status, String contentType, String body) {
        respond(path, status, contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    void echo(String path, int responseStatus) {
        server.createContext(path, exchange -> {
            last.set(new Recorded(
                    exchange.getRequestMethod(),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
                    exchange.getRequestHeaders()));
            exchange.sendResponseHeaders(responseStatus, -1);
            exchange.close();
        });
    }

    void redirect(String path, String location, int status) {
        server.createContext(path, exchange -> {
            drain(exchange.getRequestBody());
            exchange.getResponseHeaders().add("Location", location);
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
    }

    void delay(String path, Duration delay) {
        server.createContext(path, exchange -> {
            try {
                Thread.sleep(delay.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            drain(exchange.getRequestBody());
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
    }

    Recorded lastRequest() {
        return last.get();
    }

    private static void drain(InputStream in) throws IOException {
        try (in) {
            in.readAllBytes();
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
