package in.slpro.japi.http;

import in.slpro.japi.logger.ConsoleLogger;
import in.slpro.japi.model.EnvironmentModel;
import in.slpro.japi.model.KeyValueItem;
import in.slpro.japi.model.RequestModel;
import in.slpro.japi.model.ResponseModel;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpClientWrapper {
    private final HttpClient httpClient;
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    private boolean silentMode = false;

    public void setSilentMode(boolean silentMode) {
        this.silentMode = silentMode;
    }

    public HttpClientWrapper() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private String resolveVariables(String input, EnvironmentModel environment) {
        if (input == null || environment == null) return input;
        if (environment.getVariables() == null) return input;
        Matcher matcher = VAR_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            String value = environment.getVariables().stream()
                    .filter(kv -> kv.isEnabled() && varName.equals(kv.getKey()))
                    .map(KeyValueItem::getValue)
                    .findFirst()
                    .orElse(matcher.group(0));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private void executeScript(String script, EnvironmentModel environment) {
        if (script == null || script.trim().isEmpty()) return;
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("rhino");
            if (engine == null) engine = manager.getEngineByName("javascript");
            if (engine == null) return;
            if (environment != null && environment.getVariables() != null) {
                for (KeyValueItem kv : environment.getVariables()) {
                    if (kv.isEnabled()) {
                        engine.put(kv.getKey(), kv.getValue());
                    }
                }
            }
            engine.eval(script);
        } catch (Exception e) {
            System.err.println("Script error: " + e.getMessage());
        }
    }

    private String getStatusText(int code) {
        return switch (code) {
            case 200 -> "OK"; case 201 -> "Created"; case 204 -> "No Content";
            case 301 -> "Moved Permanently"; case 302 -> "Found"; case 304 -> "Not Modified";
            case 400 -> "Bad Request"; case 401 -> "Unauthorized"; case 403 -> "Forbidden";
            case 404 -> "Not Found"; case 405 -> "Method Not Allowed"; case 409 -> "Conflict";
            case 422 -> "Unprocessable Entity"; case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error"; case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable"; case 504 -> "Gateway Timeout";
            default -> "Unknown";
        };
    }

    private String getErrorMessage(Exception e) {
        if (e instanceof java.net.ConnectException) return "Connection refused: " + e.getMessage();
        if (e instanceof java.net.UnknownHostException) return "Unknown host: " + e.getMessage();
        if (e instanceof java.net.SocketTimeoutException) return "Connection timed out: " + e.getMessage();
        if (e instanceof javax.net.ssl.SSLException) return "SSL/TLS error: " + e.getMessage();
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }

    public ResponseModel execute(RequestModel requestModel, EnvironmentModel environment) {
        long startTime = System.currentTimeMillis();
        String resolvedUrl = requestModel.getUrl();
        String resolvedBodyStr = "";

        try {
            // 1. Resolve URL variables
            resolvedUrl = resolveVariables(requestModel.getUrl(), environment);

            // 2. Execute pre-request script
            executeScript(requestModel.getPreRequestScript(), environment);

            // 3. Build URL with query params
            StringBuilder urlBuilder = new StringBuilder(resolvedUrl);
            List<KeyValueItem> params = requestModel.getParams();
            if (params != null && !params.isEmpty()) {
                boolean first = urlBuilder.indexOf("?") < 0;
                for (KeyValueItem param : params) {
                    if (!param.isEnabled() || param.getKey() == null || param.getKey().isBlank()) continue;
                    urlBuilder.append(first ? "?" : "&")
                            .append(java.net.URLEncoder.encode(resolveVariables(param.getKey(), environment), StandardCharsets.UTF_8))
                            .append("=")
                            .append(java.net.URLEncoder.encode(resolveVariables(param.getValue() != null ? param.getValue() : "", environment), StandardCharsets.UTF_8));
                    first = false;
                }
            }
            resolvedUrl = urlBuilder.toString();

            // 4. Build request
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(resolvedUrl))
                    .timeout(Duration.ofSeconds(30));

            // 5. Add headers
            String method = requestModel.getMethod();
            if (requestModel.getHeaders() != null) {
                for (KeyValueItem header : requestModel.getHeaders()) {
                    if (!header.isEnabled() || header.getKey() == null || header.getKey().isBlank()) continue;
                    try {
                        reqBuilder.header(resolveVariables(header.getKey(), environment),
                                resolveVariables(header.getValue() != null ? header.getValue() : "", environment));
                    } catch (Exception ignored) {}
                }
            }

            // Auth
            String authType = requestModel.getAuthType();
            if ("bearer".equalsIgnoreCase(authType)) {
                String token = resolveVariables(requestModel.getAuthToken(), environment);
                if (token != null && !token.isBlank()) reqBuilder.header("Authorization", "Bearer " + token);
            } else if ("basic".equalsIgnoreCase(authType)) {
                String creds = requestModel.getAuthUsername() + ":" + requestModel.getAuthPassword();
                reqBuilder.header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8)));
            } else if ("apiKey".equalsIgnoreCase(authType)) {
                String keyName = resolveVariables(requestModel.getAuthApiKeyName(), environment);
                String keyValue = resolveVariables(requestModel.getAuthApiKeyValue(), environment);
                if ("header".equalsIgnoreCase(requestModel.getAuthApiKeyIn())) {
                    if (keyName != null && !keyName.isBlank()) reqBuilder.header(keyName, keyValue != null ? keyValue : "");
                }
            }
            reqBuilder.header("User-Agent", "JAPI API Client");

            // Body
            HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();
            if (!("GET".equals(method) || "DELETE".equals(method) || "HEAD".equals(method))) {
                String bodyType = requestModel.getBodyType();
                if ("raw".equalsIgnoreCase(bodyType)) {
                    resolvedBodyStr = resolveVariables(requestModel.getBodyRawContent(), environment);
                    if (resolvedBodyStr == null) resolvedBodyStr = "";
                    bodyPublisher = HttpRequest.BodyPublishers.ofString(resolvedBodyStr, StandardCharsets.UTF_8);
                    String rawType = requestModel.getBodyRawType();
                    if ("JSON".equalsIgnoreCase(rawType)) reqBuilder.header("Content-Type", "application/json");
                    else if ("XML".equalsIgnoreCase(rawType)) reqBuilder.header("Content-Type", "application/xml");
                    else if ("HTML".equalsIgnoreCase(rawType)) reqBuilder.header("Content-Type", "text/html");
                    else reqBuilder.header("Content-Type", "text/plain");
                } else if ("form".equalsIgnoreCase(bodyType)) {
                    StringBuilder formSb = new StringBuilder();
                    if (requestModel.getFormData() != null) {
                        for (KeyValueItem item : requestModel.getFormData()) {
                            if (!item.isEnabled() || item.getKey() == null || item.getKey().isBlank()) continue;
                            if (formSb.length() > 0) formSb.append("&");
                            formSb.append(java.net.URLEncoder.encode(resolveVariables(item.getKey(), environment), StandardCharsets.UTF_8))
                                    .append("=")
                                    .append(java.net.URLEncoder.encode(resolveVariables(item.getValue() != null ? item.getValue() : "", environment), StandardCharsets.UTF_8));
                        }
                    }
                    resolvedBodyStr = formSb.toString();
                    bodyPublisher = HttpRequest.BodyPublishers.ofString(resolvedBodyStr, StandardCharsets.UTF_8);
                    reqBuilder.header("Content-Type", "application/x-www-form-urlencoded");
                } else {
                    bodyPublisher = HttpRequest.BodyPublishers.noBody();
                }
            }

            reqBuilder.method(method, bodyPublisher);

            // 6. Execute
            HttpResponse<String> httpResponse = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            long executionTimeMs = System.currentTimeMillis() - startTime;
            Map<String, List<String>> headers = httpResponse.headers().map();
            String responseBody = httpResponse.body();
            long sizeBytes = responseBody.getBytes(StandardCharsets.UTF_8).length;
            int statusCode = httpResponse.statusCode();
            String statusText = getStatusText(statusCode);

            if (!silentMode && in.slpro.japi.storage.StorageManager.getInstance().getSettings().isEnableLogging()) {
                Map<String, List<String>> reqHeaders = reqBuilder.build().headers().map();
                ConsoleLogger.getInstance().logRequest(method, resolvedUrl, statusCode, executionTimeMs,
                        reqHeaders, resolvedBodyStr, headers, responseBody);
            }

            executeScript(requestModel.getPostRequestScript(), environment);

            ResponseModel response = new ResponseModel(statusCode, statusText, executionTimeMs, sizeBytes, responseBody, headers);
            response.setActualUrl(resolvedUrl);
            return response;
        } catch (Exception e) {
            long executionTimeMs = System.currentTimeMillis() - startTime;
            String errorMsg = getErrorMessage(e);
            if (!silentMode && in.slpro.japi.storage.StorageManager.getInstance().getSettings().isEnableLogging()) {
                ConsoleLogger.getInstance().logRequest(requestModel.getMethod(), resolvedUrl, 0, executionTimeMs,
                        Map.of(), resolvedBodyStr, Map.of(), errorMsg);
            }
            ResponseModel response = new ResponseModel(0, "Error", executionTimeMs,
                    errorMsg.getBytes(StandardCharsets.UTF_8).length, errorMsg, Map.of());
            response.setActualUrl(resolvedUrl);
            return response;
        }
    }
}
