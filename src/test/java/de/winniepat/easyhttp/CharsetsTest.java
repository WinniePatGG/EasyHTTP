package de.winniepat.easyhttp;

import org.junit.jupiter.api.Test;

import java.net.http.HttpHeaders;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CharsetsTest {

    private static Charset resolve(String contentType) {
        HttpHeaders headers = HttpHeaders.of(Map.of("content-type", List.of(contentType)), (a, b) -> true);
        return Charsets.of(headers);
    }

    @Test
    void readsDeclaredCharset() {
        assertEquals(StandardCharsets.ISO_8859_1, resolve("text/plain; charset=ISO-8859-1"));
    }

    @Test
    void readsQuotedCharset() {
        assertEquals(StandardCharsets.ISO_8859_1, resolve("text/plain; charset=\"ISO-8859-1\""));
    }

    @Test
    void toleratesWhitespaceAndCase() {
        assertEquals(StandardCharsets.UTF_8, resolve("text/plain; CHARSET = utf-8"));
    }

    @Test
    void readsCharsetBeforeOtherParameters() {
        assertEquals(StandardCharsets.ISO_8859_1, resolve("text/html; charset=ISO-8859-1; boundary=x"));
    }

    @Test
    void defaultsToUtf8WhenCharsetMissing() {
        assertEquals(StandardCharsets.UTF_8, resolve("text/plain"));
    }

    @Test
    void defaultsToUtf8ForUnknownCharsetName() {
        assertEquals(StandardCharsets.UTF_8, resolve("text/plain; charset=not-a-real-charset"));
    }

    @Test
    void defaultsToUtf8WhenContentTypeAbsent() {
        assertEquals(StandardCharsets.UTF_8, Charsets.of(HttpHeaders.of(Map.of(), (a, b) -> true)));
    }
}
