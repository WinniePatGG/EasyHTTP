package de.winniepat.easyhttp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EasyHttpClientTest {

    private TestServer server;
    private EasyHttpClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new TestServer();
        client = EasyHttpClient.builder().build();
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void getReturnsBodyAndStatus() throws Exception {
        server.respond("/ok", 200, "text/plain; charset=UTF-8", "hello");

        HttpResponse response = client.get(server.url("/ok"));

        assertEquals(200, response.status());
        assertTrue(response.isSuccessful());
        assertEquals("hello", response.body());
    }

    @Test
    void getExposesErrorStatus() throws Exception {
        server.respond("/missing", 404, "text/plain; charset=UTF-8", "nope");

        HttpResponse response = client.get(server.url("/missing"));

        assertEquals(404, response.status());
        assertFalse(response.isSuccessful());
    }

    @Test
    void exposesResponseHeaders() throws Exception {
        server.respond("/hdr", 200, "text/plain; charset=UTF-8", "x");

        assertEquals("text/plain; charset=UTF-8", client.get(server.url("/hdr")).contentType().orElseThrow());
    }

    @Test
    void decodesResponseCharsetFromContentType() throws Exception {
        server.respond("/latin1", 200, "text/plain; charset=ISO-8859-1",
                "Grüße".getBytes(StandardCharsets.ISO_8859_1));

        assertEquals("Grüße", client.get(server.url("/latin1")).body());
    }

    @Test
    void fallsBackToUtf8WhenCharsetIsUndeclared() throws Exception {
        server.respond("/utf8", 200, "text/plain", "Grüße");

        assertEquals("Grüße", client.get(server.url("/utf8")).body());
    }

    @Test
    void followsRedirects() throws Exception {
        server.respond("/target", 200, "text/plain; charset=UTF-8", "arrived");
        server.redirect("/start", "/target", 302);

        HttpResponse response = client.get(server.url("/start"));

        assertEquals(200, response.status());
        assertEquals("arrived", response.body());
    }

    @Test
    void headUsesHeadMethod() throws Exception {
        server.echo("/head", 200);

        client.head(server.url("/head"));

        assertEquals("HEAD", server.lastRequest().method());
    }

    @Test
    void deleteUsesDeleteMethod() throws Exception {
        server.echo("/del", 204);

        assertTrue(client.delete(server.url("/del")).isSuccessful());
        assertEquals("DELETE", server.lastRequest().method());
    }

    @Test
    void postSendsJsonByDefault() throws Exception {
        server.echo("/json", 201);

        HttpResponse response = client.post(server.url("/json"), "{\"a\":1}");

        assertEquals(201, response.status());
        assertEquals("POST", server.lastRequest().method());
        assertEquals("application/json", server.lastRequest().contentType());
        assertEquals("{\"a\":1}", server.lastRequest().body());
    }

    @Test
    void postAcceptsCustomContentType() throws Exception {
        server.echo("/form", 200);

        client.post(server.url("/form"), "a=1", "application/x-www-form-urlencoded");

        assertEquals("application/x-www-form-urlencoded", server.lastRequest().contentType());
    }

    @Test
    void putUsesPutMethod() throws Exception {
        server.echo("/put", 200);

        client.put(server.url("/put"), "{}");

        assertEquals("PUT", server.lastRequest().method());
        assertEquals("application/json", server.lastRequest().contentType());
    }

    @Test
    void putAcceptsCustomContentType() throws Exception {
        server.echo("/put-xml", 200);

        client.put(server.url("/put-xml"), "<a/>", "application/xml");

        assertEquals("application/xml", server.lastRequest().contentType());
    }

    @Test
    void patchUsesPatchMethod() throws Exception {
        server.echo("/patch", 200);

        client.patch(server.url("/patch"), "{}");

        assertEquals("PATCH", server.lastRequest().method());
        assertEquals("application/json", server.lastRequest().contentType());
    }

    @Test
    void timesOutOnSlowServer() {
        server.delay("/slow", Duration.ofSeconds(3));
        EasyHttpClient slow = EasyHttpClient.builder().timeout(Duration.ofMillis(200)).build();

        assertThrows(HttpTimeoutException.class, () -> slow.get(server.url("/slow")));
    }

    @Test
    void sendAsyncAppliesDefaultsAndReturnsBody() throws Exception {
        server.echo("/async", 200);
        EasyHttpClient authed = EasyHttpClient.builder().header("X-Api-Key", "secret").build();

        CompletableFuture<java.net.http.HttpResponse<String>> future = authed.sendAsync(
                HttpRequest.newBuilder(URI.create(server.url("/async"))).GET(),
                java.net.http.HttpResponse.BodyHandlers.ofString());

        assertEquals(200, future.get().statusCode());
        assertEquals("secret", server.lastRequest().header("X-Api-Key"));
    }

    @Test
    void connectFailureThrowsIoException() {
        EasyHttpClient offline = EasyHttpClient.builder()
                .connectTimeout(Duration.ofMillis(200))
                .build();

        assertThrows(IOException.class, () -> offline.get("http://127.0.0.1:1/nothing"));
    }
}
