package de.winniepat.easyhttp;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class EasyHttpClient {

    private static final String JSON = "application/json";

    private final HttpClient client;
    private final Duration timeout;
    private final Map<String, String> defaultHeaders;

    EasyHttpClient(EasyHttpClientBuilder builder) {
        this.client = builder.client != null
                ? builder.client
                : HttpClient.newBuilder()
                        .followRedirects(builder.redirects)
                        .connectTimeout(builder.connectTimeout)
                        .build();
        this.timeout = builder.timeout;
        this.defaultHeaders = Map.copyOf(builder.defaultHeaders);
    }

    public static EasyHttpClientBuilder builder() {
        return new EasyHttpClientBuilder();
    }

    public HttpResponse get(String url) throws IOException, InterruptedException {
        return send(request(url).GET());
    }

    public HttpResponse head(String url) throws IOException, InterruptedException {
        return send(request(url).method("HEAD", HttpRequest.BodyPublishers.noBody()));
    }

    public HttpResponse delete(String url) throws IOException, InterruptedException {
        return send(request(url).DELETE());
    }

    public HttpResponse post(String url, String body) throws IOException, InterruptedException {
        return post(url, body, JSON);
    }

    public HttpResponse post(String url, String body, String contentType)
            throws IOException, InterruptedException {
        return send(request(url).header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body)));
    }

    public HttpResponse put(String url, String body) throws IOException, InterruptedException {
        return put(url, body, JSON);
    }

    public HttpResponse put(String url, String body, String contentType)
            throws IOException, InterruptedException {
        return send(request(url).header("Content-Type", contentType)
                .PUT(HttpRequest.BodyPublishers.ofString(body)));
    }

    public HttpResponse patch(String url, String body) throws IOException, InterruptedException {
        return patch(url, body, JSON);
    }

    public HttpResponse patch(String url, String body, String contentType)
            throws IOException, InterruptedException {
        return send(request(url).header("Content-Type", contentType)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body)));
    }

    public <T> CompletableFuture<java.net.http.HttpResponse<T>> sendAsync(
            HttpRequest.Builder builder, java.net.http.HttpResponse.BodyHandler<T> handler) {
        return client.sendAsync(applyDefaults(builder), handler);
    }

    private static HttpRequest.Builder request(String url) {
        return HttpRequest.newBuilder(URI.create(url));
    }

    private HttpRequest applyDefaults(HttpRequest.Builder builder) {
        builder.timeout(timeout);
        defaultHeaders.forEach(builder::header);
        return builder.build();
    }

    private HttpResponse send(HttpRequest.Builder builder) throws IOException, InterruptedException {
        java.net.http.HttpResponse<byte[]> raw =
                client.send(applyDefaults(builder), java.net.http.HttpResponse.BodyHandlers.ofByteArray());
        return new HttpResponse(raw.statusCode(), raw.headers(),
                new String(raw.body(), Charsets.of(raw.headers())));
    }
}
