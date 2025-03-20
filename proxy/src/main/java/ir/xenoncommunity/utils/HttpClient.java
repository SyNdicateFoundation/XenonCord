package ir.xenoncommunity.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@UtilityClass
public class HttpClient {

    private final Map<URL, List<String>> cachedResponse = Maps.newConcurrentMap();
    private final Gson gson = new Gson();
    private final JsonParser parser = new JsonParser();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // Asynchronous POST method
    public CompletableFuture<JsonObject> post(String url, JsonObject object) throws MalformedURLException {
        return postAsync(new URL(url), object);
    }

    // Asynchronous GET method
    public CompletableFuture<ArrayList<String>> get(URL url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ArrayList<String> string = Lists.newArrayList();
                URLConnection ec = url.openConnection();
                ec.setRequestProperty("user-agent", "Mozilla/5.0 (X11; Linux x86_64; rv:136.0) Gecko/20100101 Firefox/136.0");
                ec.setRequestProperty("Host", url.getHost());
                ec.setDoOutput(true);
                try (BufferedReader in = new BufferedReader(new InputStreamReader(ec.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        string.add(line.trim());
                    }
                }
                return string;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    // Asynchronous POST method using URL
    public CompletableFuture<JsonObject> postAsync(URL url, JsonObject object) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final String json = gson.toJson(object);
                final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("user-agent", "Mozilla/5.0 (X11; Linux x86_64; rv:136.0) Gecko/20100101 Firefox/136.0");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    return parser.parse(br).getAsJsonObject();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    // Asynchronous Discord POST method
    public CompletableFuture<JsonObject> discord(String message, String webhookLink) throws MalformedURLException {
        final JsonObject object = new JsonObject();
        object.addProperty("content", message);
        return postAsync(new URL(webhookLink), object);
    }

    // Get cached response asynchronously
    public CompletableFuture<List<String>> getCached(URL url) {
        return CompletableFuture.supplyAsync(() -> cachedResponse.computeIfAbsent(url, k -> {
            try {
                return get(url).join();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }), executorService);
    }

    // Asynchronous GET JSON method
    @SneakyThrows
    public CompletableFuture<JsonObject> GETJson(URL url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("user-agent", "Mozilla/5.0 (X11; Linux x86_64; rv:136.0) Gecko/20100101 Firefox/136.0");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoInput(true);

                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    return parser.parse(br).getAsJsonObject();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    // Asynchronous GET JSON method by URL string
    public CompletableFuture<JsonObject> GETJson(String url) throws MalformedURLException {
        return GETJson(new URL(url));
    }
}
