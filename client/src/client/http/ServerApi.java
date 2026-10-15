package client.http;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dto.ActionResultDTO;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * All HTTP communication with the Guess Market server.
 * - Uses Java's built-in HttpClient (no external HTTP library is needed).
 * - A cookie manager keeps the session cookie (JSESSIONID), so the server knows who we are.
 * - get(...) is synchronous: used only by the background refresh thread.
 * - post(...) / uploadFile(...) are asynchronous: used by the JavaFX thread, which must never block.
 * Response bodies are always fully read as strings, so no connection is left open.
 */
public final class ServerApi {
    public static final String BASE_URL = "http://localhost:8080/guessmarket";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Gson GSON = new Gson();

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .connectTimeout(TIMEOUT)
            .build();

    private ServerApi() {
    }

    // ----- Synchronous (background threads only) -----

    public static <T> T get(String path, Map<String, String> params, Type resultType) {
        HttpRequest request = HttpRequest.newBuilder(uri(path, params)).timeout(TIMEOUT).GET().build();
        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return parse(response, resultType);
        } catch (IOException e) {
            throw new ServerException(connectionErrorMessage(e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServerException("The request was interrupted.");
        }
    }

    public static <T> T get(String path, Type resultType) {
        return get(path, Map.of(), resultType);
    }

    /** A short synchronous POST (used when the application closes). */
    public static void postAndWait(String path) {
        try {
            post(path, Map.of()).get();
        } catch (Exception ignored) {
            // The application is closing anyway
        }
    }

    // ----- Asynchronous (safe to call from the JavaFX thread) -----

    /** POST with form parameters. Completes with the server's message, or fails with a ServerException. */
    public static CompletableFuture<String> post(String path, Map<String, String> params) {
        HttpRequest request = HttpRequest.newBuilder(uri(path, Map.of()))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(encode(params)))
                .build();
        return sendAsync(request);
    }

    /** Uploads a file as multipart/form-data (part name "file"), like an HTML form would. */
    public static CompletableFuture<String> uploadFile(String path, String fileName, byte[] content) {
        String boundary = "----GuessMarket" + UUID.randomUUID().toString().replace("-", "");
        String head = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: application/xml\r\n\r\n";
        String tail = "\r\n--" + boundary + "--\r\n";

        byte[] body = concat(head.getBytes(StandardCharsets.UTF_8), content, tail.getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(uri(path, Map.of()))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        return sendAsync(request);
    }

    private static CompletableFuture<String> sendAsync(HttpRequest request) {
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .handle((response, error) -> {
                    if (error != null) {
                        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                        throw new ServerException(connectionErrorMessage(cause));
                    }
                    ActionResultDTO result = parse(response, ActionResultDTO.class);
                    return result.message();
                });
    }

    // ----- Helpers -----

    private static <T> T parse(HttpResponse<String> response, Type resultType) {
        if (response.statusCode() != 200) {
            throw new ServerException(errorMessageOf(response));
        }
        try {
            return GSON.fromJson(response.body(), resultType);
        } catch (JsonParseException e) {
            throw new ServerException("Unexpected answer from the server: " + e.getMessage());
        }
    }

    private static String errorMessageOf(HttpResponse<String> response) {
        try {
            ActionResultDTO error = GSON.fromJson(response.body(), ActionResultDTO.class);
            if (error != null && error.message() != null) {
                return error.message();
            }
        } catch (JsonParseException ignored) {
            // Not our JSON (for example a Tomcat error page)
        }
        if (response.statusCode() == 404) {
            return "The server application was not found at " + BASE_URL + ". Is guessmarket.war deployed in Tomcat?";
        }
        return "Server error (HTTP " + response.statusCode() + ").";
    }

    private static String connectionErrorMessage(Throwable error) {
        if (error instanceof ConnectException) {
            return "Cannot connect to the server at " + BASE_URL + ". Please make sure Tomcat is running.";
        }
        return "Communication error with the server: " + error.getMessage();
    }

    private static URI uri(String path, Map<String, String> params) {
        String query = params.isEmpty() ? "" : "?" + encode(params);
        return URI.create(BASE_URL + path + query);
    }

    private static String encode(Map<String, String> params) {
        return new LinkedHashMap<>(params).entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static byte[] concat(byte[]... parts) {
        int length = 0;
        for (byte[] part : parts) {
            length += part.length;
        }
        byte[] result = new byte[length];
        int position = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, result, position, part.length);
            position += part.length;
        }
        return result;
    }
}
