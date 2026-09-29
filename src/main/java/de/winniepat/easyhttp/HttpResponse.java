package de.winniepat.easyhttp;

import java.net.http.HttpHeaders;
import java.util.Optional;

public record HttpResponse(int status, HttpHeaders headers, String body) {

    public boolean isSuccessful() {
        return status >= 200 && status < 300;
    }

    public Optional<String> header(String name) {
        return headers.firstValue(name);
    }

    public Optional<String> contentType() {
        return header("content-type");
    }
}
