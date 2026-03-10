import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class CrawlerServer {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/crawl", new CrawlHandler());
        server.createContext("/health", exchange -> {
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("Crawler server started on http://localhost:" + port);
    }

    private static class CrawlHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Only GET is supported\"}");
                return;
            }

            Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
            String targetUrl = params.get("url");
            if (targetUrl == null || targetUrl.isBlank()) {
                sendJson(exchange, 400, "{\"error\":\"Query parameter 'url' is required\"}");
                return;
            }

            ProcessBuilder pb = new ProcessBuilder("node", "crawler/crawl.js", targetUrl);
            pb.redirectErrorStream(true);

            Process process;
            try {
                process = pb.start();
            } catch (IOException e) {
                sendJson(exchange, 500, "{\"error\":\"Failed to start Node.js crawler process\"}");
                return;
            }

            String output;
            try (InputStream is = process.getInputStream()) {
                output = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            try {
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    String escaped = output.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
                    sendJson(exchange, 500, "{\"error\":\"Crawler failed\",\"details\":\"" + escaped + "\"}");
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendJson(exchange, 500, "{\"error\":\"Crawler process interrupted\"}");
                return;
            }

            sendJson(exchange, 200, output);
        }

        private Map<String, String> parseQuery(String query) {
            Map<String, String> result = new HashMap<>();
            if (query == null || query.isBlank()) {
                return result;
            }
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                result.put(key, value);
            }
            return result;
        }

        private void sendJson(HttpExchange exchange, int statusCode, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
