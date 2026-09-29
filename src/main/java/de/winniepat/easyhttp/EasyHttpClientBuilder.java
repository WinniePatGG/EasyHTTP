package de.winniepat.easyhttp;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fluent builder for {@link EasyHttpClient}.
 *
 * <p>Every method returns {@code this} so calls can be chained; a single builder
 * may be reused to create several clients. Builders are <em>not</em> thread-safe,
 * but the {@link EasyHttpClient} instances they produce are.
 *
 * <pre>{@code
 * EasyHttpClient client = EasyHttpClient.builder()
 *         .timeout(Duration.ofSeconds(5))
 *         .connectTimeout(Duration.ofSeconds(2))
 *         .followRedirects(HttpClient.Redirect.NEVER)
 *         .header("Accept", "application/json")
 *         .build();
 * }</pre>
 *
 * @see EasyHttpClient#builder()
 */
public final class EasyHttpClientBuilder {

    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    HttpClient client;
    Duration timeout = DEFAULT_TIMEOUT;
    Duration connectTimeout = DEFAULT_TIMEOUT;
    HttpClient.Redirect redirects = HttpClient.Redirect.NORMAL;
    final Map<String, String> defaultHeaders = new LinkedHashMap<>();

    /**
     * Supplies a pre-configured JDK client to delegate to.
     *
     * <p>Useful for sharing a connection pool, installing a custom executor or
     * proxy. When set, the client's own redirect policy and connect timeout are
     * used, so {@link #followRedirects(HttpClient.Redirect)} and
     * {@link #connectTimeout(Duration)} are ignored.
     *
     * @param client the delegate client, or {@code null} to let the builder
     *               create its own
     * @return this builder
     */
    public EasyHttpClientBuilder client(HttpClient client) {
        this.client = client;
        return this;
    }

    /**
     * Sets the per-request timeout. The clock starts once the request has been
     * handed to the underlying client and covers the whole exchange including
     * reading the response body.
     *
     * <p>Defaults to 30 seconds. On expiry the call fails with
     * {@link java.net.http.HttpTimeoutException}.
     *
     * @param timeout the timeout, must be positive
     * @return this builder
     * @throws IllegalArgumentException if {@code timeout} is negative or zero
     */
    public EasyHttpClientBuilder timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Sets how long establishing a TCP connection may take before it is aborted.
     *
     * <p>Defaults to 30 seconds. Has no effect when
     * {@link #client(HttpClient)} supplies its own client.
     *
     * @param connectTimeout the connect timeout, must be positive
     * @return this builder
     * @throws IllegalArgumentException if {@code connectTimeout} is negative or zero
     */
    public EasyHttpClientBuilder connectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * Sets the redirect policy.
     *
     * <p>Defaults to {@link HttpClient.Redirect#NORMAL}, which follows redirects
     * except those that change the protocol between HTTP and HTTPS. Has no effect
     * when {@link #client(HttpClient)} supplies its own client.
     *
     * @param policy the redirect policy to apply
     * @return this builder
     */
    public EasyHttpClientBuilder followRedirects(HttpClient.Redirect policy) {
        this.redirects = policy;
        return this;
    }

    /**
     * Adds a header that is sent with every request issued by the client.
     *
     * <p>Adding the same name twice replaces the previous value, which is how the
     * auth helpers overwrite a stale {@code Authorization} header.
     *
     * @param name  the header name
     * @param value the header value
     * @return this builder
     */
    public EasyHttpClientBuilder header(String name, String value) {
        this.defaultHeaders.put(name, value);
        return this;
    }

    /**
     * Authenticates every request with an OAuth-style bearer token by setting
     * {@code Authorization: Bearer <token>}.
     *
     * @param token the raw, unencoded access token
     * @return this builder
     * @see #basicAuth(String, String)
     */
    public EasyHttpClientBuilder bearerAuth(String token) {
        return header("Authorization", "Bearer " + token);
    }

    /**
     * Authenticates every request with HTTP Basic credentials by setting
     * {@code Authorization: Basic <base64>}. The username and password are joined
     * with a colon and Base64-encoded as UTF-8.
     *
     * @param username the user name
     * @param password the password
     * @return this builder
     * @see #bearerAuth(String)
     */
    public EasyHttpClientBuilder basicAuth(String username, String password) {
        String credentials = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return header("Authorization", "Basic " + credentials);
    }

    /**
     * Creates an immutable client from the current configuration. The builder's
     * default headers are copied, so later changes to the builder do not affect
     * clients that have already been built.
     *
     * @return a new, ready-to-use client
     */
    public EasyHttpClient build() {
        return new EasyHttpClient(this);
    }
}
