package de.winniepat.easyhttp;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EasyHttpClientBuilder {

    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    HttpClient client;
    Duration timeout = DEFAULT_TIMEOUT;
    Duration connectTimeout = DEFAULT_TIMEOUT;
    HttpClient.Redirect redirects = HttpClient.Redirect.NORMAL;
    final Map<String, String> defaultHeaders = new LinkedHashMap<>();

    public EasyHttpClientBuilder client(HttpClient client) {
        this.client = client;
        return this;
    }

    public EasyHttpClientBuilder timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public EasyHttpClientBuilder connectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public EasyHttpClientBuilder followRedirects(HttpClient.Redirect policy) {
        this.redirects = policy;
        return this;
    }

    public EasyHttpClientBuilder header(String name, String value) {
        this.defaultHeaders.put(name, value);
        return this;
    }

    public EasyHttpClientBuilder bearerAuth(String token) {
        return header("Authorization", "Bearer " + token);
    }

    public EasyHttpClientBuilder basicAuth(String username, String password) {
        String credentials = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return header("Authorization", "Basic " + credentials);
    }

    public EasyHttpClient build() {
        return new EasyHttpClient(this);
    }
}
