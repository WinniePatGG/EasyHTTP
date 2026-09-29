# EasyHTTP

A small, dependency-free HTTP client for Java 21, built on top of the JDK's
own `java.net.http.HttpClient`.

It exists to remove the three things that make the JDK client verbose: repeated
`HttpRequest.Builder` setup, hand-rolled body decoding, and exception handling
for error statuses.

[![Release](https://github.com/WinniePatGG/EasyHTTP/actions/workflows/release.yml/badge.svg)](https://github.com/WinniePatGG/EasyHTTP/actions/workflows/release.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## Features

- One-liner methods for `GET`, `HEAD`, `DELETE`, `POST`, `PUT` and `PATCH`
- Response bodies decoded automatically using the charset from `Content-Type`,
  falling back to UTF-8
- **Error statuses are returned, not thrown** — inspect `isSuccessful()`
- Configurable request and connect timeouts, redirect policy, default headers
- Bearer and Basic auth helpers
- Immutable and thread-safe; build once, reuse everywhere
- An `sendAsync` escape hatch for anything the convenience methods miss
- No runtime dependencies

## Requirements

Java 21 or newer. Nothing else.

## Installation

Coordinates:

```text
de.winniepat:easyhttp:<version>
```

### Maven (reposilite)

```xml
<repositories>
  <repository>
    <id>winniepat</id>
    <url>https://maven.winniepat.de/releases</url>
  </repository>
</repositories>

<dependency>
  <groupId>de.winniepat</groupId>
  <artifactId>easyhttp</artifactId>
  <version>1.1</version>
</dependency>
```

### Gradle (reposilite)

```kotlin
repositories {
    maven { url = uri("https://maven.winniepat.de/releases") }
}

dependencies {
    implementation("de.winniepat:easyhttp:1.1")
}
```

### GitHub Packages

GitHub Packages requires authentication even for public artifacts.

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/WinniePatGG/EasyHTTP")
        credentials {
            username = findProperty("gpr.user") as String
            password = findProperty("gpr.key") as String
        }
    }
}

dependencies {
    implementation("de.winniepat:easyhttp:1.1")
}
```

## Usage

Build a client once and reuse it — instances are immutable and thread-safe.

```java
import de.winniepat.easyhttp.EasyHttpClient;
import de.winniepat.easyhttp.HttpResponse;

import java.time.Duration;

EasyHttpClient client = EasyHttpClient.builder()
        .timeout(Duration.ofSeconds(10))
        .bearerAuth("my-token")
        .build();

HttpResponse response = client.get("https://api.example.com/users");

if (response.isSuccessful()) {
    System.out.println(response.body());
} else {
    System.err.println("HTTP " + response.status() + ": " + response.body());
}
```

### Requests with a body

`post`, `put` and `patch` default to `application/json`:

```java
client.post("https://api.example.com/users", "{\"name\":\"Ada\"}");

// or with an explicit content type
client.post("https://api.example.com/login", "user=ada&pass=secret",
        "application/x-www-form-urlencoded");
```

### Other verbs

```java
client.get(url);
client.head(url);     // headers only, no body transferred
client.delete(url);
```

### Reading the response

```java
int status = response.status();
String body = response.body();
boolean ok = response.isSuccessful();

String type = response.contentType().orElse("unknown");
String etag = response.header("ETag").orElse("none");
```

`body()` is never `null` — it is an empty string for `HEAD` and
`204 No Content` replies.

### Configuration

```java
EasyHttpClient client = EasyHttpClient.builder()
        .timeout(Duration.ofSeconds(30))          // per-request timeout
        .connectTimeout(Duration.ofSeconds(5))    // TCP connect timeout
        .followRedirects(HttpClient.Redirect.NEVER)
        .header("Accept", "application/json")     // sent with every request
        .basicAuth("ada", "secret")
        .build();
```

To share a connection pool or install a custom executor, hand in your own JDK
client:

```java
EasyHttpClient client = EasyHttpClient.builder()
        .client(HttpClient.newHttpClient())
        .build();
```

> When you supply your own client, the redirect policy and connect timeout you
> set on the builder are ignored — the delegate's own settings win. The request
> timeout and default headers still apply.

### Custom requests and async

`sendAsync` accepts any request builder and body handler, so unsupported verbs,
streaming and custom decoding are all still reachable:

```java
CompletableFuture<java.net.http.HttpResponse<String>> future =
        client.sendAsync(
                HttpRequest.newBuilder(URI.create(url)).GET(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
```

The configured timeout and default headers are applied automatically.

## Error handling

| Situation | What happens |
| --- | --- |
| `4xx` / `5xx` status | Returned as a normal response; check `isSuccessful()` |
| Timeout elapsed | Throws `HttpTimeoutException` (a subclass of `IOException`) |
| Connection refused / DNS failure | Throws `IOException` |
| Calling thread interrupted | Throws `InterruptedException` |
| Malformed URL | Throws `IllegalArgumentException` |
| `null` request body | Throws `NullPointerException` |

## Building locally

```bash
./gradlew build     # compile + test
./gradlew javadoc   # generate docs into build/docs/javadoc
```

## Releasing

Releases are automated. Pushing to `main` runs the full pipeline in
[`.github/workflows/release.yml`](.github/workflows/release.yml):

1. Build and test the project
2. Derive the next version from the most recent `v*` git tag
3. Publish `jar`, `sources` and `javadoc` artifacts to reposilite and GitHub Packages
4. Tag the release as `v<version>` and push the tag

Pull requests run the build only — they never publish.

### Version scheme

Versions increment the **minor** number on every successful push to `main`:

```text
1.0  →  1.1  →  1.2  →  1.3  →  ...
```

`1.0` is the assumed baseline if no tag exists yet, so the first automated
release publishes `1.1`. The version is derived from git tags, which means it
is reproducible from the repository state and a failed publish never consumes a
version number — the tag is only pushed after publishing succeeds.

To publish a specific version manually, trigger the workflow via
**Actions → Release → Run workflow** from the `main` branch.

### Required repository secrets

Add these under **Settings → Secrets and variables → Actions**:

| Secret | Purpose |
| --- | --- |
| `REPOSILITE_USERNAME` | reposilite user for publishing |
| `REPOSILITE_PASSWORD` | reposilite token/password for publishing |

GitHub Packages uses the automatically provided `GITHUB_TOKEN`, so no extra
secret is needed for that repository.

### Publishing from your machine

The build reads credentials from `gradle.properties` (which is gitignored):

```properties
reposilite.username=...
reposilite.password=...
gpr.user=...
gpr.key=...
```

The publish tasks fail fast with a clear message if a required key is missing.

## License

Released under the [MIT License](LICENSE).
