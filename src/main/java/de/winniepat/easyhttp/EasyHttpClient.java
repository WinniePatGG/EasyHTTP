package de.winniepat.easyhttp;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * A thin, synchronous wrapper around the JDK's {@link java.net.http.HttpClient}
 * that returns the response body as a decoded {@link String} and exposes the
 * status code instead of throwing on 4xx/5xx replies.
 *
 * <p>Instances are immutable, thread-safe and created through {@link #builder()}:
 *
 * <pre>{@code
 * EasyHttpClient client = EasyHttpClient.builder()
 *         .timeout(Duration.ofSeconds(10))
 *         .bearerAuth("my-token")
 *         .build();
 *
 * HttpResponse response = client.get("https://api.example.com/status");
 * if (response.isSuccessful()) {
 *     System.out.println(response.body());
 * }
 * }</pre>
 *
 * <p>Every request automatically carries the client's timeout and default headers
 * configured on the builder.
 *
 * <h2>Error handling</h2>
 * <p>HTTP error statuses are <em>not</em> turned into exceptions; inspect
 * {@link HttpResponse#isSuccessful()} and {@link HttpResponse#status()} instead.
 * Only transport-level failures surface as {@link IOException} (including
 * {@link java.net.http.HttpTimeoutException} when the configured timeout elapses
 * or the connection cannot be established).
 */
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

    /**
     * Creates a new builder pre-configured with the library defaults:
     * a 30 second request and connect timeout, {@link HttpClient.Redirect#NORMAL}
     * redirect handling and no default headers.
     *
     * @return a fresh, mutable builder
     */
    public static EasyHttpClientBuilder builder() {
        return new EasyHttpClientBuilder();
    }

    /**
     * Sends a {@code GET} request.
     *
     * @param url absolute URL to request, e.g. {@code "https://example.com/data"}
     * @return the response with its body decoded using the charset advertised in
     *         the {@code Content-Type} header, falling back to UTF-8
     * @throws IOException              if the request fails, the connection cannot
     *                                  be established, or the response times out
     * @throws InterruptedException     if the calling thread is interrupted while waiting
     * @throws IllegalArgumentException if {@code url} is not a valid URI
     */
    public HttpResponse get(String url) throws IOException, InterruptedException {
        return send(request(url).GET());
    }

    /**
     * Sends a {@code HEAD} request to retrieve headers without transferring a body.
     *
     * @param url absolute URL to request
     * @return the response; its {@link HttpResponse#body()} is typically empty
     * @throws IOException          if the request fails or the response times out
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    public HttpResponse head(String url) throws IOException, InterruptedException {
        return send(request(url).method("HEAD", HttpRequest.BodyPublishers.noBody()));
    }

    /**
     * Sends a {@code DELETE} request.
     *
     * @param url absolute URL to request
     * @return the response; a {@code 204 No Content} result yields an empty body
     * @throws IOException          if the request fails or the response times out
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    public HttpResponse delete(String url) throws IOException, InterruptedException {
        return send(request(url).DELETE());
    }

    /**
     * Sends a {@code POST} request with an {@code application/json} content type.
     *
     * @param url  absolute URL to request
     * @param body request body; must not be {@code null}, use an empty string for
     *             an empty body
     * @return the response
     * @throws IOException              if the request fails or the response times out
     * @throws InterruptedException     if the calling thread is interrupted while waiting
     * @throws NullPointerException     if {@code body} is {@code null}
     * @see #post(String, String, String)
     */
    public HttpResponse post(String url, String body) throws IOException, InterruptedException {
        return post(url, body, JSON);
    }

    /**
     * Sends a {@code POST} request with an explicit content type.
     *
     * @param url         absolute URL to request
     * @param body        request body; must not be {@code null}, use an empty
     *                    string for an empty body
     * @param contentType value for the {@code Content-Type} header, e.g.
     *                   {@code "application/x-www-form-urlencoded"}
     * @return the response
     * @throws IOException              if the request fails or the response times out
     * @throws InterruptedException     if the calling thread is interrupted while waiting
     * @throws NullPointerException     if {@code body} is {@code null}
     */
    public HttpResponse post(String url, String body, String contentType)
            throws IOException, InterruptedException {
        return send(request(url).header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body)));
    }

    /**
     * Sends a {@code PUT} request with an {@code application/json} content type.
     *
     * @param url  absolute URL to request
     * @param body request body; must not be {@code null}, use an empty string for
     *             an empty body
     * @return the response
     * @throws IOException              if the request fails or the response times out
     * @throws InterruptedException     if the calling thread is interrupted while waiting
     * @throws NullPointerException     if {@code body} is {@code null}
     * @see #put(String, String, String)
     */
    public HttpResponse put(String url, String body) throws IOException, InterruptedException {
        return put(url, body, JSON);
    }

    /**
     * Sends a {@code PUT} request with an explicit content type.
     *
     * @param url         absolute URL to request
     * @param body        request body; must not be {@code null}, use an empty
     *                    string for an empty body
     * @param contentType value for the {@code Content-Type} header
     * @return the response
     * @throws IOException              if the request fails or the response times out
     * @throws InterruptedException     if the calling thread is interrupted while waiting
     * @throws NullPointerException     if {@code body} is {@code null}
     */
    public HttpResponse put(String url, String body, String contentType)
            throws IOException, InterruptedException {
        return send(request(url).header("Content-Type", contentType)
                .PUT(HttpRequest.BodyPublishers.ofString(body)));
    }

    /**
     * Sends a {@code PATCH} request with an {@code application/json} content type.
     *
     * @param url  absolute URL to request
     * @param body request body describing the partial update; must not be
     *             {@code null}, use an empty string for an empty body
     * @return the response
     * @throws IOException              if the request fails or the response times out
     * @throws InterruptedException     if the calling thread is interrupted while waiting
     * @throws NullPointerException     if {@code body} is {@code null}
     * @see #patch(String, String, String)
     */
    public HttpResponse patch(String url, String body) throws IOException, InterruptedException {
        return patch(url, body, JSON);
    }

    /**
     * Sends a {@code PATCH} request with an explicit content type.
     *
     * @param url         absolute URL to request
     * @param body        request body; must not be {@code null}, use an empty
     *                    string for an empty body
     * @param contentType value for the {@code Content-Type} header
     * @return the response
     * @throws IOException              if the request fails or the response times out
     * @throws InterruptedException     if the calling thread is interrupted while waiting
     * @throws NullPointerException     if {@code body} is {@code null}
     */
    public HttpResponse patch(String url, String body, String contentType)
            throws IOException, InterruptedException {
        return send(request(url).header("Content-Type", contentType)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body)));
    }

    /**
     * Escape hatch for requests the convenience methods do not cover, e.g. custom
     * methods, {@code PROPFIND} or streaming uploads. The configured timeout and
     * default headers are applied to the supplied builder.
     *
     * <p>Unlike the synchronous methods this returns the JDK's own response type,
     * so the caller decides how the body is decoded and must complete the
     * returned future themselves.
     *
     * @param <T>     the type the body handler produces
     * @param builder a request builder, e.g.
     *                {@code HttpRequest.newBuilder(URI.create(url)).GET()}
     * @param handler the body handler controlling decoding, e.g.
     *                {@code HttpResponse.BodyHandlers.ofString()}
     * @return a future completing with the JDK {@link java.net.http.HttpResponse}
     *         when the response arrives
     */
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
