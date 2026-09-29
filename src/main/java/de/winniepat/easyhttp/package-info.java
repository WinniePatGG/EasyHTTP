/**
 * A minimal, dependency-free HTTP client for Java 21 built on top of
 * {@link java.net.http.HttpClient}.
 *
 * <p>The library exists to remove the three things that usually make the JDK
 * client verbose: repeated {@link java.net.http.HttpRequest.Builder} setup,
 * hand-rolled body decoding, and exception handling for error statuses.
 *
 * <h2>Getting started</h2>
 * <p>Build a client once, configure it, and reuse it — instances are immutable
 * and thread-safe:
 *
 * <pre>{@code
 * EasyHttpClient client = EasyHttpClient.builder()
 *         .timeout(Duration.ofSeconds(10))
 *         .bearerAuth("my-token")
 *         .build();
 *
 * HttpResponse response = client.get("https://api.example.com/users");
 * if (response.isSuccessful()) {
 *     System.out.println(response.body());
 * }
 * }</pre>
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li>{@link de.winniepat.easyhttp.EasyHttpClient} — the client itself</li>
 *   <li>{@link de.winniepat.easyhttp.EasyHttpClientBuilder} — configuration for
 *       timeouts, redirects, default headers and authentication</li>
 *   <li>{@link de.winniepat.easyhttp.HttpResponse} — the immutable result type</li>
 * </ul>
 *
 * <h2>Behaviour worth knowing</h2>
 * <ul>
 *   <li>Error statuses are returned, not thrown. Check
 *       {@link de.winniepat.easyhttp.HttpResponse#isSuccessful()}.</li>
 *   <li>Request bodies are UTF-8 encoded, and {@code post}, {@code put} and
 *       {@code patch} default to an {@code application/json} content type.</li>
 *   <li>Response bodies are decoded with the charset from the
 *       {@code Content-Type} header, falling back to UTF-8 when absent or
 *       unrecognised.</li>
 *   <li>Timeouts, connection failures and interruptions surface as
 *       {@link java.io.IOException} or {@link java.lang.InterruptedException}.</li>
 *   <li>For full control over method, body type or decoding, use
 *       {@link de.winniepat.easyhttp.EasyHttpClient#sendAsync}.</li>
 * </ul>
 */
package de.winniepat.easyhttp;
