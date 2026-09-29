package de.winniepat.easyhttp;

import java.net.http.HttpHeaders;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internal helper that determines which charset a response body is encoded in.
 *
 * <p>The {@code charset} parameter of the {@code Content-Type} header is parsed
 * with a lenient regular expression rather than the strict
 * {@code ContentType} parser, so malformed or unquoted values still yield a
 * usable result. Anything unusable degrades to {@link StandardCharsets#UTF_8},
 * which is also the correct default under RFC 9110.
 */
final class Charsets {

    private static final Pattern CHARSET =
            Pattern.compile("charset\\s*=\\s*\"?([^\";\\s]+)", Pattern.CASE_INSENSITIVE);

    private Charsets() {
    }

    /**
     * Resolves the response charset from its headers.
     *
     * @param headers the response headers to inspect
     * @return the charset named by the {@code Content-Type} header, or
     *         {@link StandardCharsets#UTF_8} if the header is absent, declares no
     *         charset, or names one this JVM does not support
     */
    static Charset of(HttpHeaders headers) {
        return headers.firstValue("content-type")
                .map(CHARSET::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1))
                .map(Charsets::orUtf8)
                .orElse(StandardCharsets.UTF_8);
    }

    /**
     * Looks up a charset by name without failing on unknown or malformed names.
     *
     * @param name the charset name as it appeared in the {@code Content-Type} header
     * @return the named charset, or {@link StandardCharsets#UTF_8} if it is not
     *         supported by this runtime
     */
    static Charset orUtf8(String name) {
        try {
            return Charset.forName(name);
        } catch (IllegalArgumentException e) {
            return StandardCharsets.UTF_8;
        }
    }
}
