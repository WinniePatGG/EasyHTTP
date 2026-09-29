package de.winniepat.easyhttp;

import java.net.http.HttpHeaders;
import java.util.Optional;

/**
 * Immutable result of a request made by {@link EasyHttpClient}: the status code,
 * the response headers and the body already decoded to a {@link String}.
 *
 * <p>Note that this type intentionally shares its name with the JDK's
 * {@link java.net.http.HttpResponse}. Inside this package the local record wins,
 * so the JDK type is always written out in full.
 *
 * @param status  the HTTP status code, e.g. {@code 200} or {@code 404}
 * @param headers the response headers, including any repeated values
 * @param body    the decoded response body; empty for {@code HEAD} and
 *                {@code 204 No Content} replies, never {@code null}
 */
public record HttpResponse(int status, HttpHeaders headers, String body) {

    /**
     * Tells whether the server reported success, i.e. a status in the
     * {@code 2xx} range.
     *
     * <p>This is a convenience check only; redirect ({@code 3xx}) and error
     * ({@code 4xx}, {@code 5xx}) responses return {@code false} and still carry a
     * fully populated body.
     *
     * @return {@code true} if {@link #status()} is between 200 (inclusive) and
     *         300 (exclusive)
     */
    public boolean isSuccessful() {
        return status >= 200 && status < 300;
    }

    /**
     * Looks up a single response header.
     *
     * @param name the header name; matched case-insensitively
     * @return the first value for the header, or {@link Optional#empty()} if the
     *         response did not include it
     */
    public Optional<String> header(String name) {
        return headers.firstValue(name);
    }

    /**
     * Returns the raw {@code Content-Type} header, for example
     * {@code "application/json; charset=UTF-8"}.
     *
     * <p>Use {@link #isSuccessful()} alongside this to decide whether the body is
     * worth parsing, since error responses frequently carry an HTML or
     * JSON error document.
     *
     * @return the {@code Content-Type} value, or {@link Optional#empty()} if the
     *         server sent none
     */
    public Optional<String> contentType() {
        return header("content-type");
    }
}
