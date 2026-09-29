package de.winniepat.easyhttp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EasyHttpClientBuilderTest {

    private TestServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new TestServer();
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void sendsDefaultHeaders() throws Exception {
        server.echo("/anything", 200);

        EasyHttpClient.builder().header("X-Api-Key", "secret").build().get(server.url("/anything"));

        assertEquals("secret", server.lastRequest().header("X-Api-Key"));
    }

    @Test
    void bearerAuthSetsAuthorizationHeader() throws Exception {
        server.echo("/anything", 200);

        EasyHttpClient.builder().bearerAuth("t0ken").build().get(server.url("/anything"));

        assertEquals("Bearer t0ken", server.lastRequest().header("Authorization"));
    }

    @Test
    void basicAuthEncodesCredentials() throws Exception {
        server.echo("/anything", 200);

        EasyHttpClient.builder().basicAuth("user", "pass").build().get(server.url("/anything"));

        String expected = "Basic " + Base64.getEncoder()
                .encodeToString("user:pass".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, server.lastRequest().header("Authorization"));
    }

    @Test
    void followsRedirectsByDefault() throws Exception {
        server.respond("/target", 200, "text/plain; charset=UTF-8", "arrived");
        server.redirect("/start", "/target", 302);

        assertEquals(200, EasyHttpClient.builder().build().get(server.url("/start")).status());
    }

    @Test
    void canDisableRedirectFollowing() throws Exception {
        server.respond("/target", 200, "text/plain; charset=UTF-8", "arrived");
        server.redirect("/start", "/target", 302);

        HttpResponse response = EasyHttpClient.builder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()
                .get(server.url("/start"));

        assertEquals(302, response.status());
    }

    @Test
    void injectedClientTakesPrecedenceOverRedirectSetting() throws Exception {
        server.respond("/target", 200, "text/plain; charset=UTF-8", "arrived");
        server.redirect("/start", "/target", 302);

        HttpClient noRedirects = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        HttpResponse response = EasyHttpClient.builder()
                .client(noRedirects)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
                .get(server.url("/start"));

        assertEquals(302, response.status());
    }
}
