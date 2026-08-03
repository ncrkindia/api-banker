package in.slpro.japi.http;

import in.slpro.japi.logger.ConsoleLogger;
import in.slpro.japi.model.EnvironmentModel;
import in.slpro.japi.model.KeyValueItem;
import in.slpro.japi.model.RequestModel;
import in.slpro.japi.model.ResponseModel;
import in.slpro.japi.model.ScriptResult;

import in.slpro.japi.model.CollectionModel;
import in.slpro.japi.ui.MainFrame;

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
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;

/**
 * HttpClientWrapper
 *
 * <p>
 * This class serves as the core networking engine for JAPI. It is responsible for
 * taking a {@link RequestModel} and translating it into an actual {@link java.net.http.HttpRequest}.
 * 
 * It manages the entire lifecycle of an HTTP call including:
 * <ul>
 *   <li>Resolving Postman-style {{variables}} dynamically from Collections and Environments.</li>
 *   <li>Executing pre-request and post-request JavaScript code via {@link ScriptExecutor}.</li>
 *   <li>Injecting cookies using the {@link CookieJar}.</li>
 *   <li>Constructing Authorization headers (Bearer, Basic, API Key).</li>
 *   <li>Building complex request payloads (Raw, Form-Data, URLEncoded, GraphQL).</li>
 * </ul>
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.1.0-beta
 * @since 1.0.0
 */
public class HttpClientWrapper {
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    private final ScriptExecutor scriptExecutor = new ScriptExecutor();

    private boolean silentMode = false;

    /**
     * Enables or disables silent mode. When silent mode is active, the wrapper will not
     * dispatch real-time request and response logs to the central {@link ConsoleLogger}.
     * 
     * @param silentMode true to suppress console logging, false otherwise.
     */
    public void setSilentMode(boolean silentMode) {
        this.silentMode = silentMode;
        this.scriptExecutor.setSilentMode(silentMode);
    }

    public HttpClientWrapper() {
    }

    /**
     * Resolves variables matching the {@code {{variable_name}}} syntax found within the input string.
     * The method queries the provided {@link EnvironmentModel} first; if the variable is not found,
     * it falls back to the parent {@link CollectionModel} variables. If neither provides a resolution,
     * the original placeholder string is preserved.
     *
     * @param input The raw input string containing potential variable placeholders.
     * @param requestModel The current request being executed (used to trace parent collections).
     * @param environment The active environment containing overriding variable definitions.
     * @return The interpolated string with fully resolved variables.
     */
    private String resolveVariables(String input, RequestModel requestModel, EnvironmentModel environment) {
        if (input == null) return input;
        CollectionModel collection = MainFrame.findParentCollection(requestModel);
        Matcher matcher = VAR_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            String value = null;
            if (environment != null && environment.getVariables() != null) {
                value = environment.getVariables().stream()
                        .filter(kv -> kv.isEnabled() && varName.equals(kv.getKey()))
                        .map(KeyValueItem::getValue)
                        .findFirst()
                        .orElse(null);
            }
            if (value == null && collection != null && collection.getVariables() != null) {
                value = collection.getVariables().stream()
                        .filter(kv -> kv.isEnabled() && varName.equals(kv.getKey()))
                        .map(KeyValueItem::getValue)
                        .findFirst()
                        .orElse(null);
            }
            if (value == null) {
                value = matcher.group(0);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Merges two distinct {@link ScriptResult} objects (typically one from the collection level
     * and one from the request level) into a single consolidated result object containing all logs,
     * assertions, and script errors.
     * 
     * @param r1 The first script result (e.g., Collection-level).
     * @param r2 The second script result (e.g., Request-level).
     * @return A merged ScriptResult instance.
     */
    private ScriptResult mergeScriptResults(ScriptResult r1, ScriptResult r2) {
        ScriptResult merged = new ScriptResult();
        if (r1 != null) {
            for (ScriptResult.TestAssertion a : r1.getAssertions()) {
                merged.addAssertion(a.getName(), a.isPassed(), a.getFailureMessage());
            }
            for (String log : r1.getConsoleLogs()) {
                merged.addConsoleLog(log);
            }
            if (r1.hasError()) {
                merged.setError(r1.getError());
            }
        }
        if (r2 != null) {
            for (ScriptResult.TestAssertion a : r2.getAssertions()) {
                merged.addAssertion(a.getName(), a.isPassed(), a.getFailureMessage());
            }
            for (String log : r2.getConsoleLogs()) {
                merged.addConsoleLog(log);
            }
            if (r2.hasError()) {
                if (merged.hasError()) {
                    merged.setError(merged.getError() + "\n" + r2.getError());
                } else {
                    merged.setError(r2.getError());
                }
            }
        }
        return merged;
    }

    /**
     * Result container for a request execution that includes script results.
     */
    public static class ExecutionResult {
        private final ResponseModel response;
        private final ScriptResult preRequestResult;
        private final ScriptResult testResult;

        public ExecutionResult(ResponseModel response, ScriptResult preRequestResult, ScriptResult testResult) {
            this.response = response;
            this.preRequestResult = preRequestResult;
            this.testResult = testResult;
        }

        public ResponseModel getResponse() { return response; }
        public ScriptResult getPreRequestResult() { return preRequestResult; }
        public ScriptResult getTestResult() { return testResult; }
    }

    /**
     * Executes the API request described by the given {@link RequestModel} within the context of 
     * the provided {@link EnvironmentModel}. This includes evaluating all pre-request and 
     * post-request (test) scripts attached to both the Request and its parent Collection.
     * 
     * @param requestModel The configuration and definition of the request to be fired.
     * @param environment The active environment state for variable interpolation.
     * @return An {@link ExecutionResult} encapsulating the final HTTP response along with metadata 
     *         from script executions.
     */
    public ExecutionResult executeWithScripts(RequestModel requestModel, EnvironmentModel environment) {
        long startTime = System.currentTimeMillis();
        String resolvedUrl = requestModel.getUrl();
        String resolvedBodyStr = "";
        CollectionModel collection = MainFrame.findParentCollection(requestModel);

        // Execute pre-request scripts: Collection-level first, then Request-level
        ScriptResult collPreResult = new ScriptResult();
        if (collection != null && collection.getPreRequestScript() != null && !collection.getPreRequestScript().isBlank()) {
            collPreResult = scriptExecutor.executePreRequestScript(
                    collection.getPreRequestScript(), requestModel, environment);
        }

        ScriptResult reqPreResult = new ScriptResult();
        if (requestModel.getPreRequestScript() != null && !requestModel.getPreRequestScript().isBlank()) {
            reqPreResult = scriptExecutor.executePreRequestScript(
                    requestModel.getPreRequestScript(), requestModel, environment);
        }

        ScriptResult preResult = mergeScriptResults(collPreResult, reqPreResult);
        long preRequestTimeMs = System.currentTimeMillis() - startTime;

        // If pre-request script had an error, we can still proceed with the request
        // (Postman behavior — logs error but doesn't block request)

        try {
            // 1. Resolve URL variables (after pre-request script may have modified env)
            resolvedUrl = resolveVariables(requestModel.getUrl(), requestModel, environment);

            // 2. Build URL with query params
            StringBuilder urlBuilder = new StringBuilder(resolvedUrl);
            List<KeyValueItem> params = requestModel.getParams();
            if (params != null && !params.isEmpty()) {
                boolean first = urlBuilder.indexOf("?") < 0;
                for (KeyValueItem param : params) {
                    if (!param.isEnabled() || param.getKey() == null || param.getKey().isBlank()) continue;
                    urlBuilder.append(first ? "?" : "&")
                            .append(java.net.URLEncoder.encode(resolveVariables(param.getKey(), requestModel, environment), StandardCharsets.UTF_8))
                            .append("=")
                            .append(java.net.URLEncoder.encode(resolveVariables(param.getValue() != null ? param.getValue() : "", requestModel, environment), StandardCharsets.UTF_8));
                    first = false;
                }
            }
            resolvedUrl = urlBuilder.toString();

            // 3. Build request
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(resolvedUrl))
                    .timeout(Duration.ofSeconds(30));

            // 4. Add headers
            String method = requestModel.getMethod();
            if (requestModel.getHeaders() != null) {
                for (KeyValueItem header : requestModel.getHeaders()) {
                    if (!header.isEnabled() || header.getKey() == null || header.getKey().isBlank()) continue;
                    try {
                        reqBuilder.header(resolveVariables(header.getKey(), requestModel, environment),
                                resolveVariables(header.getValue() != null ? header.getValue() : "", requestModel, environment));
                    } catch (Exception ignored) {}
                }
            }

            // Auth
            String authType = requestModel.getAuthType();
            String authToken = requestModel.getAuthToken();
            String authUsername = requestModel.getAuthUsername();
            String authPassword = requestModel.getAuthPassword();
            String authApiKeyName = requestModel.getAuthApiKeyName();
            String authApiKeyValue = requestModel.getAuthApiKeyValue();
            String authApiKeyIn = requestModel.getAuthApiKeyIn();

            if ("inherit".equalsIgnoreCase(authType) || authType == null) {
                if (collection != null) {
                    authType = collection.getAuthType();
                    authToken = collection.getAuthToken();
                    authUsername = collection.getAuthUsername();
                    authPassword = collection.getAuthPassword();
                    authApiKeyName = collection.getAuthApiKeyName();
                    authApiKeyValue = collection.getAuthApiKeyValue();
                    authApiKeyIn = collection.getAuthApiKeyIn();
                } else {
                    authType = "none";
                }
            }

            if ("bearer".equalsIgnoreCase(authType)) {
                String token = resolveVariables(authToken, requestModel, environment);
                if (token != null && !token.isBlank()) reqBuilder.header("Authorization", "Bearer " + token);
            } else if ("basic".equalsIgnoreCase(authType)) {
                String creds = authUsername + ":" + authPassword;
                reqBuilder.header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8)));
            } else if ("apiKey".equalsIgnoreCase(authType)) {
                String keyName = resolveVariables(authApiKeyName, requestModel, environment);
                String keyValue = resolveVariables(authApiKeyValue, requestModel, environment);
                if ("header".equalsIgnoreCase(authApiKeyIn)) {
                    if (keyName != null && !keyName.isBlank()) reqBuilder.header(keyName, keyValue != null ? keyValue : "");
                } else if ("query".equalsIgnoreCase(authApiKeyIn)) {
                    if (keyName != null && !keyName.isBlank()) {
                        String delim = resolvedUrl.contains("?") ? "&" : "?";
                        resolvedUrl += delim + java.net.URLEncoder.encode(keyName, StandardCharsets.UTF_8) + "=" +
                                java.net.URLEncoder.encode(keyValue != null ? keyValue : "", StandardCharsets.UTF_8);
                        reqBuilder.uri(URI.create(resolvedUrl));
                    }
                }
            }
            reqBuilder.header("User-Agent", "JAPI API Client");

            // Body
            HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();
            if (!("GET".equals(method) || "DELETE".equals(method) || "HEAD".equals(method))) {
                String bodyType = requestModel.getBodyType();
                if ("raw".equalsIgnoreCase(bodyType)) {
                    resolvedBodyStr = resolveVariables(requestModel.getBodyRawContent(), requestModel, environment);
                    if (resolvedBodyStr == null) resolvedBodyStr = "";
                    bodyPublisher = HttpRequest.BodyPublishers.ofString(resolvedBodyStr, StandardCharsets.UTF_8);
                    String rawType = requestModel.getBodyRawType();
                    if ("JSON".equalsIgnoreCase(rawType)) reqBuilder.header("Content-Type", "application/json");
                    else if ("XML".equalsIgnoreCase(rawType)) reqBuilder.header("Content-Type", "application/xml");
                    else if ("HTML".equalsIgnoreCase(rawType)) reqBuilder.header("Content-Type", "text/html");
                    else reqBuilder.header("Content-Type", "text/plain");
                } else if ("form-data".equalsIgnoreCase(bodyType)) {
                    String boundary = "JAPIBoundary" + System.currentTimeMillis();
                    try {
                        byte[] multipartData = buildMultipartBody(requestModel.getFormData(), boundary, requestModel, environment);
                        bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(multipartData);
                        reqBuilder.header("Content-Type", "multipart/form-data; boundary=" + boundary);
                        resolvedBodyStr = "[Multipart/Form-Data Payload: " + multipartData.length + " bytes]";
                    } catch (Exception ex) {
                        resolvedBodyStr = "Error building multipart body: " + ex.getMessage();
                        bodyPublisher = HttpRequest.BodyPublishers.noBody();
                    }
                } else if ("form".equalsIgnoreCase(bodyType) || "x-www-form-urlencoded".equalsIgnoreCase(bodyType)) {
                    StringBuilder formSb = new StringBuilder();
                    List<KeyValueItem> items = requestModel.getUrlencodedData();
                    if (items == null || items.isEmpty()) {
                        items = requestModel.getFormData();
                    }
                    if (items != null) {
                        for (KeyValueItem item : items) {
                            if (!item.isEnabled() || item.getKey() == null || item.getKey().isBlank()) continue;
                            if (formSb.length() > 0) formSb.append("&");
                            formSb.append(java.net.URLEncoder.encode(resolveVariables(item.getKey(), requestModel, environment), StandardCharsets.UTF_8))
                                    .append("=")
                                    .append(java.net.URLEncoder.encode(resolveVariables(item.getValue() != null ? item.getValue() : "", requestModel, environment), StandardCharsets.UTF_8));
                        }
                    }
                    resolvedBodyStr = formSb.toString();
                    bodyPublisher = HttpRequest.BodyPublishers.ofString(resolvedBodyStr, StandardCharsets.UTF_8);
                    reqBuilder.header("Content-Type", "application/x-www-form-urlencoded");
                } else if ("graphql".equalsIgnoreCase(bodyType)) {
                    reqBuilder.header("Content-Type", "application/json");
                    try {
                        String rawContent = requestModel.getBodyRawContent();
                        String query = "";
                        String variablesStr = "";
                        if (rawContent != null && rawContent.trim().startsWith("{")) {
                            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(rawContent).getAsJsonObject();
                            if (json.has("query")) {
                                query = json.get("query").getAsString();
                            }
                            if (json.has("variables")) {
                                variablesStr = json.get("variables").getAsString();
                            }
                        } else {
                            query = rawContent != null ? rawContent : "";
                        }

                        String resolvedQuery = resolveVariables(query, requestModel, environment);
                        String resolvedVars = resolveVariables(variablesStr, requestModel, environment);

                        com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
                        payload.addProperty("query", resolvedQuery);
                        if (resolvedVars != null && !resolvedVars.isBlank()) {
                            try {
                                com.google.gson.JsonElement varsJson = com.google.gson.JsonParser.parseString(resolvedVars);
                                payload.add("variables", varsJson);
                            } catch (Exception e) {
                                payload.addProperty("variables", resolvedVars);
                            }
                        } else {
                            payload.add("variables", new com.google.gson.JsonObject());
                        }

                        resolvedBodyStr = new com.google.gson.Gson().toJson(payload);
                        bodyPublisher = HttpRequest.BodyPublishers.ofString(resolvedBodyStr, StandardCharsets.UTF_8);
                    } catch (Exception ex) {
                        resolvedBodyStr = "Error building GraphQL body: " + ex.getMessage();
                        bodyPublisher = HttpRequest.BodyPublishers.noBody();
                    }
                } else {
                    bodyPublisher = HttpRequest.BodyPublishers.noBody();
                }
            }

            // Cookie Jar Manager support: attach matching cookies
            String cookieHeader = CookieJar.getInstance().getCookieHeaderForUrl(resolvedUrl);
            if (cookieHeader != null && !cookieHeader.isBlank()) {
                reqBuilder.header("Cookie", cookieHeader);
            }

            reqBuilder.method(method, bodyPublisher);

            // 5. Execute
            boolean verifySsl = in.slpro.japi.ui.MainFrame.resolveSslVerificationStatic(requestModel);
            boolean followRedirects = in.slpro.japi.ui.MainFrame.resolveRedirectSettingStatic(requestModel);
            
            long requestStartTime = System.currentTimeMillis();
            HttpResponse<String> httpResponse = getClient(verifySsl, followRedirects).send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            long networkTimeMs = System.currentTimeMillis() - requestStartTime;
            
            long executionTimeMs = System.currentTimeMillis() - startTime;
            Map<String, List<String>> headers = httpResponse.headers().map();

            // Cookie Jar Manager support: capture response cookies
            List<String> setCookieHeaders = headers.get("set-cookie");
            if (setCookieHeaders == null || setCookieHeaders.isEmpty()) {
                // Try case-insensitive lookup
                for (String key : headers.keySet()) {
                    if (key != null && key.equalsIgnoreCase("set-cookie")) {
                        setCookieHeaders = headers.get(key);
                        break;
                    }
                }
            }
            if (setCookieHeaders != null && !setCookieHeaders.isEmpty()) {
                CookieJar.getInstance().parseAndStoreCookies(resolvedUrl, setCookieHeaders);
            }
            String responseBody = httpResponse.body();
            long sizeBytes = responseBody.getBytes(StandardCharsets.UTF_8).length;
            int statusCode = httpResponse.statusCode();
            String statusText = getStatusText(statusCode);

            if (!silentMode && ConsoleLogger.getInstance().isEnableLogging()) {
                Map<String, List<String>> reqHeaders = reqBuilder.build().headers().map();
                
                // Log intermediate redirects
                java.util.List<HttpResponse<String>> prevResponses = new java.util.ArrayList<>();
                java.util.Optional<HttpResponse<String>> prev = httpResponse.previousResponse();
                while (prev.isPresent()) {
                    prevResponses.add(0, prev.get()); // add to beginning to keep chronological order
                    prev = prev.get().previousResponse();
                }
                
                for (HttpResponse<String> pr : prevResponses) {
                    ConsoleLogger.getInstance().logRequest(pr.request().method(), pr.request().uri().toString(), 
                            pr.statusCode(), 0, pr.request().headers().map(), "", pr.headers().map(), "");
                }
                
                ConsoleLogger.getInstance().logRequest(method, httpResponse.uri().toString(), statusCode, executionTimeMs,
                        reqHeaders, resolvedBodyStr, headers, responseBody);
            }

            ResponseModel response = new ResponseModel(statusCode, statusText, executionTimeMs, sizeBytes, responseBody, headers);
            response.setActualUrl(httpResponse.uri().toString());
            response.setPreRequestTimeMs(preRequestTimeMs);
            response.setNetworkTimeMs(networkTimeMs);
            
            // Extract SSL Certificate Details
            if (httpResponse.sslSession().isPresent()) {
                try {
                    java.security.cert.Certificate[] certs = httpResponse.sslSession().get().getPeerCertificates();
                    if (certs.length > 0 && certs[0] instanceof java.security.cert.X509Certificate) {
                        java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) certs[0];
                        StringBuilder sb = new StringBuilder("<html><b style='font-size:11px'>SSL Certificate Details:</b><br><br>");
                        sb.append("<b>Subject:</b> ").append(cert.getSubjectX500Principal().getName()).append("<br>");
                        sb.append("<b>Issuer:</b> ").append(cert.getIssuerX500Principal().getName()).append("<br>");
                        sb.append("<b>Valid From:</b> ").append(cert.getNotBefore()).append("<br>");
                        sb.append("<b>Valid Until:</b> ").append(cert.getNotAfter()).append("<br>");
                        
                        boolean dateValid = true;
                        try {
                            cert.checkValidity();
                        } catch (Exception e) {
                            dateValid = false;
                        }
                        sb.append("<b>Status:</b> ").append(dateValid ? (verifySsl ? "Verified & Valid" : "Valid (Unverified)") : "Expired/Invalid").append("</html>");
                        
                        response.setSslDetails(sb.toString());
                        response.setSslValid(verifySsl && dateValid);
                    }
                } catch (Exception e) {
                    // Ignore SSL extraction errors
                }
            }
            
            // Capture redirects for Collection Runner
            java.util.List<ResponseModel> redirects = new java.util.ArrayList<>();
            java.util.Optional<HttpResponse<String>> p = httpResponse.previousResponse();
            while (p.isPresent()) {
                HttpResponse<String> pr = p.get();
                ResponseModel rm = new ResponseModel(pr.statusCode(), getStatusText(pr.statusCode()), 0, 0, "", pr.headers().map());
                rm.setActualUrl(pr.request().uri().toString());
                
                // Set SSL details for redirect if needed (optional)
                
                redirects.add(0, rm);
                p = pr.previousResponse();
            }
            response.setRedirects(redirects);

            // Execute test scripts: Collection-level first, then Request-level
            long testStartTime = System.currentTimeMillis();
            ScriptResult collTestResult = new ScriptResult();
            if (collection != null && collection.getPostRequestScript() != null && !collection.getPostRequestScript().isBlank()) {
                collTestResult = scriptExecutor.executeTestScript(
                        collection.getPostRequestScript(), requestModel, response, environment);
            }

            ScriptResult reqTestResult = new ScriptResult();
            if (requestModel.getPostRequestScript() != null && !requestModel.getPostRequestScript().isBlank()) {
                reqTestResult = scriptExecutor.executeTestScript(
                        requestModel.getPostRequestScript(), requestModel, response, environment);
            }

            ScriptResult testResult = mergeScriptResults(collTestResult, reqTestResult);
            long testScriptTimeMs = System.currentTimeMillis() - testStartTime;
            response.setTestScriptTimeMs(testScriptTimeMs);

            return new ExecutionResult(response, preResult, testResult);

        } catch (Exception e) {
            long executionTimeMs = System.currentTimeMillis() - startTime;
            String errorMsg = getErrorMessage(e);
            if (!silentMode && ConsoleLogger.getInstance().isEnableLogging()) {
                ConsoleLogger.getInstance().logRequest(requestModel.getMethod(), resolvedUrl, 0, executionTimeMs,
                        Map.of(), resolvedBodyStr, Map.of(), errorMsg);
            }
            ResponseModel response = new ResponseModel(0, "Error", executionTimeMs,
                    errorMsg.getBytes(StandardCharsets.UTF_8).length, errorMsg, Map.of());
            response.setActualUrl(resolvedUrl);
            return new ExecutionResult(response, preResult, new ScriptResult());
        }
    }

    /**
     * Maps standard HTTP Status codes into their corresponding human-readable reason phrases.
     * 
     * @param code The HTTP status code.
     * @return The human-readable string representation of the code.
     */
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

    /**
     * Converts a raw Java network Exception into a user-friendly error message.
     * 
     * @param e The exception thrown during network execution.
     * @return A clear, descriptive error string (e.g. "Connection refused: ...").
     */
    private String getErrorMessage(Exception e) {
        if (e instanceof java.net.ConnectException) return "Connection refused: " + e.getMessage();
        if (e instanceof java.net.UnknownHostException) return "Unknown host: " + e.getMessage();
        if (e instanceof java.net.SocketTimeoutException) return "Connection timed out: " + e.getMessage();
        if (e instanceof javax.net.ssl.SSLException) return "SSL/TLS error: " + e.getMessage();
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }

    /**
     * Backward-compatible execute method used by the Collection Runner.
     * Delegates to executeWithScripts and returns only the response.
     */
    public ResponseModel execute(RequestModel requestModel, EnvironmentModel environment) {
        ExecutionResult result = executeWithScripts(requestModel, environment);
        return result.getResponse();
    }

    /**
     * Serializes a list of Form-Data key-value pairs into a standardized MultiPart HTTP byte payload,
     * including resolving any embedded variables and handling actual physical file uploads.
     * 
     * @param items The form-data parameters to serialize.
     * @param boundary The unique multipart boundary string.
     * @param requestModel The parent request context (for variable resolution).
     * @param environment The active environment (for variable resolution).
     * @return The generated MultiPart byte array payload ready for HTTP transmission.
     * @throws Exception If an error occurs reading an attached file.
     */
    private byte[] buildMultipartBody(List<KeyValueItem> items, String boundary, RequestModel requestModel, EnvironmentModel environment) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] newline = "\r\n".getBytes(StandardCharsets.UTF_8);

        for (KeyValueItem item : items) {
            if (!item.isEnabled() || item.getKey() == null || item.getKey().isBlank()) continue;

            String key = resolveVariables(item.getKey(), requestModel, environment);
            String type = item.getType() != null ? item.getType() : "text";

            bos.write(("--" + boundary).getBytes(StandardCharsets.UTF_8));
            bos.write(newline);

            if ("file".equalsIgnoreCase(type)) {
                String filePath = resolveVariables(item.getValue() != null ? item.getValue() : "", requestModel, environment);
                File file = new File(filePath);
                String fileName = file.getName();
                bos.write(String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"", key, fileName).getBytes(StandardCharsets.UTF_8));
                bos.write(newline);
                String contentType = Files.probeContentType(file.toPath());
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                bos.write(String.format("Content-Type: %s", contentType).getBytes(StandardCharsets.UTF_8));
                bos.write(newline);
                bos.write(newline);

                if (file.exists() && file.isFile()) {
                    bos.write(Files.readAllBytes(file.toPath()));
                }
            } else {
                String val = resolveVariables(item.getValue() != null ? item.getValue() : "", requestModel, environment);
                bos.write(String.format("Content-Disposition: form-data; name=\"%s\"", key).getBytes(StandardCharsets.UTF_8));
                bos.write(newline);
                bos.write(newline);
                bos.write(val.getBytes(StandardCharsets.UTF_8));
            }
            bos.write(newline);
        }

        bos.write(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
        bos.write(newline);

        return bos.toByteArray();
    }

    /**
     * Dynamically builds a {@link java.net.http.HttpClient} instance enforcing custom configurations 
     * such as Follow Redirects flags and strict/loose SSL Certification verification requirements.
     * 
     * @param sslVerification True if strict SSL verification should be enforced.
     * @param followRedirects True if the client should automatically follow HTTP 3xx redirects.
     * @return The configured HttpClient instance ready to send requests.
     */
    private HttpClient getClient(boolean sslVerification, boolean followRedirects) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(followRedirects ? HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER);
        if (!sslVerification) {
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
            try {
                javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
                sslContext.init(null, new javax.net.ssl.TrustManager[]{
                    new javax.net.ssl.X509ExtendedTrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType, java.net.Socket socket) {}
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType, java.net.Socket socket) {}
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType, javax.net.ssl.SSLEngine engine) {}
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType, javax.net.ssl.SSLEngine engine) {}
                    }
                }, new java.security.SecureRandom());
                builder.sslContext(sslContext);

                javax.net.ssl.SSLParameters sslParams = new javax.net.ssl.SSLParameters();
                sslParams.setEndpointIdentificationAlgorithm("");
                builder.sslParameters(sslParams);
            } catch (Exception ignored) {}
        } else {
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "false");
        }
        return builder.build();
    }
}
