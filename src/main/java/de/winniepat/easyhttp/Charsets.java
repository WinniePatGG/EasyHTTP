package de.winniepat.easyhttp;

import java.net.http.HttpHeaders;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Charsets {

    private static final Pattern CHARSET =
            Pattern.compile("charset\\s*=\\s*\"?([^\";\\s]+)", Pattern.CASE_INSENSITIVE);

    private Charsets() {
    }

    static Charset of(HttpHeaders headers) {
        return headers.firstValue("content-type")
                .map(CHARSET::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1))
                .map(Charsets::orUtf8)
                .orElse(StandardCharsets.UTF_8);
    }

    static Charset orUtf8(String name) {
        try {
            return Charset.forName(name);
        } catch (IllegalArgumentException e) {
            return StandardCharsets.UTF_8;
        }
    }
}
