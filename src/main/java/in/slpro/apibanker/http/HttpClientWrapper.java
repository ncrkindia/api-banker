package in.slpro.apibanker.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;


import in.slpro.apibanker.logger.ConsoleLogger;
import in.slpro.apibanker.model.CollectionModel;
import in.slpro.apibanker.model.EnvironmentModel;
import in.slpro.apibanker.model.KeyValueItem;
import in.slpro.apibanker.model.RequestModel;
import in.slpro.apibanker.model.ResponseModel;
import in.slpro.apibanker.model.ScriptResult;
import in.slpro.apibanker.ui.MainFrame;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;

/**
 * HttpClientWrapper
 *
 * <p>
 * This class serves as the core networking engine for ApiBanker. It is
 * responsible
 * for
 * taking a {@link RequestModel} and translating it into an actual
 * {@link java.net.http.HttpRequest}.
 * 
 * It manages the entire lifecycle of an HTTP call including:
 * <ul>
 * <li>Resolving Postman-style {{variables}} dynamically from Collections and
 * Environments.</li>
 * <li>Executing pre-request and post-request JavaScript code via
 * {@link ScriptExecutor}.</li>
 * <li>Injecting cookies using the {@link CookieJar}.</li>
 * <li>Constructing Authorization headers (Bearer, Basic, API Key).</li>
 * <li>Building complex request payloads (Raw, Form-Data, URLEncoded,
 * GraphQL).</li>
 * </ul>
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.6.0-beta
 * @since 1.0.0
 */
public class HttpClientWrapper {
    private final ScriptExecutor scriptExecutor = new ScriptExecutor();

    private boolean silentMode = false;

    /**
     * Enables or disables silent mode. When silent mode is active, the wrapper will
     * not
     * dispatch real-time request and response logs to the central
     * {@link ConsoleLogger}.
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
     * Resolves variables matching the {@code {{variable_name}}} syntax found within
     * the input string.
     * The method queries the provided {@link EnvironmentModel} first; if the
     * variable is not found,
     * it falls back to the parent {@link CollectionModel} variables. If neither
     * provides a resolution,
     * the original placeholder string is preserved.
     *
     * @param input        The raw input string containing potential variable
     *                     placeholders.
     * @param requestModel The current request being executed (used to trace parent
     *                     collections).
     * @param environment  The active environment containing overriding variable
     *                     definitions.
     * @return The interpolated string with fully resolved variables.
     */
    private String resolveVariables(String input, RequestModel requestModel, EnvironmentModel environment) {
        return in.slpro.apibanker.model.VariableHelper.resolveVariables(input, requestModel, environment);
    }

    /**
     * Merges two distinct {@link ScriptResult} objects (typically one from the
     * collection level
     * and one from the request level) into a single consolidated result object
     * containing all logs,
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

        public ResponseModel getResponse() {
            return response;
        }

        public ScriptResult getPreRequestResult() {
            return preRequestResult;
        }

        public ScriptResult getTestResult() {
            return testResult;
        }
    }

    /**
     * Executes the API request described by the given {@link RequestModel} within
     * the context of
     * the provided {@link EnvironmentModel}. This includes evaluating all
     * pre-request and
     * post-request (test) scripts attached to both the Request and its parent
     * Collection.
     * 
     * @param requestModel The configuration and definition of the request to be
     *                     fired.
     * @param environment  The active environment state for variable interpolation.
     * @return An {@link ExecutionResult} encapsulating the final HTTP response
     *         along with metadata
     *         from script executions.
     */
    public ExecutionResult executeWithScripts(RequestModel requestModel, EnvironmentModel environment) {
        long startTime = System.currentTimeMillis();
        String resolvedUrl = requestModel.getUrl();
        String resolvedBodyStr = "";
        CollectionModel collection = MainFrame.findParentCollection(requestModel);

        // Execute pre-request scripts: Collection-level first, then Request-level
        ScriptResult collPreResult = new ScriptResult();
        if (collection != null && collection.getPreRequestScript() != null
                && !collection.getPreRequestScript().isBlank()) {
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
            // 1. Resolve Base URL variables (strip query string to avoid duplicates as they are rebuilt below)
            String rawUrl = requestModel.getUrl() != null ? requestModel.getUrl() : "";
            int qIdx = rawUrl.indexOf('?');
            if (qIdx >= 0) {
                rawUrl = rawUrl.substring(0, qIdx);
            }
            resolvedUrl = resolveVariables(rawUrl, requestModel, environment);

            // 2. Build URL with query params
            StringBuilder urlBuilder = new StringBuilder(resolvedUrl);
            List<KeyValueItem> params = requestModel.getParams();
            if (params != null && !params.isEmpty()) {
                boolean first = true;
                for (KeyValueItem param : params) {
                    if (!param.isEnabled() || param.getKey() == null || param.getKey().isBlank())
                        continue;
                    urlBuilder.append(first ? "?" : "&")
                            .append(java.net.URLEncoder.encode(
                                    resolveVariables(param.getKey(), requestModel, environment),
                                    StandardCharsets.UTF_8))
                            .append("=")
                            .append(java.net.URLEncoder
                                    .encode(resolveVariables(param.getValue() != null ? param.getValue() : "",
                                            requestModel, environment), StandardCharsets.UTF_8));
                    first = false;
                }
            }
            resolvedUrl = urlBuilder.toString();

            int timeoutSeconds = in.slpro.apibanker.ui.MainFrame.resolveTimeoutStatic(requestModel);

            // 3. Build request
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(resolvedUrl))
                    .timeout(Duration.ofSeconds(timeoutSeconds));

            // 4. Add headers
            String method = requestModel.getMethod();
            Map<String, List<String>> combinedHeaders = new java.util.LinkedHashMap<>();
            
            if (requestModel.getHeaders() != null) {
                for (KeyValueItem header : requestModel.getHeaders()) {
                    if (!header.isEnabled() || header.getKey() == null || header.getKey().isBlank())
                        continue;
                    try {
                        String key = resolveVariables(header.getKey(), requestModel, environment).trim();
                        String val = resolveVariables(header.getValue() != null ? header.getValue() : "", requestModel,
                                environment).trim();
                                
                        if (key.equalsIgnoreCase("Host") || key.equalsIgnoreCase("Content-Length")) {
                            continue;
                        }
                        
                        final String finalKey = key;
                        String existingKey = combinedHeaders.keySet().stream()
                            .filter(k -> k.equalsIgnoreCase(finalKey)).findFirst().orElse(key);
                            
                        combinedHeaders.computeIfAbsent(existingKey, k -> new java.util.ArrayList<>()).add(val);
                    } catch (Exception ignored) {
                    }
                }
            }

            for (Map.Entry<String, List<String>> entry : combinedHeaders.entrySet()) {
                reqBuilder.header(entry.getKey(), String.join(", ", entry.getValue()));
            }
            
            boolean hasUserAgent = combinedHeaders.keySet().stream().anyMatch(k -> k.equalsIgnoreCase("User-Agent"));
            boolean hasContentType = combinedHeaders.keySet().stream().anyMatch(k -> k.equalsIgnoreCase("Content-Type"));

            // Auth
            String authType = requestModel.getAuthType();
            String authToken = requestModel.getAuthToken();
            String authUsername = requestModel.getAuthUsername();
            String authPassword = requestModel.getAuthPassword();
            String authApiKeyName = requestModel.getAuthApiKeyName();
            String authApiKeyValue = requestModel.getAuthApiKeyValue();
            String authApiKeyIn = requestModel.getAuthApiKeyIn();
            String oauth2AccessToken = requestModel.getOauth2AccessToken();

            if ("inherit".equalsIgnoreCase(authType) || authType == null) {
                CollectionModel currentParent = collection;
                while (currentParent != null) {
                    authType = currentParent.getAuthType();
                    if (authType != null && !authType.equalsIgnoreCase("inherit") && !authType.isEmpty()) {
                        authToken = currentParent.getAuthToken();
                        authUsername = currentParent.getAuthUsername();
                        authPassword = currentParent.getAuthPassword();
                        authApiKeyName = currentParent.getAuthApiKeyName();
                        authApiKeyValue = currentParent.getAuthApiKeyValue();
                        authApiKeyIn = currentParent.getAuthApiKeyIn();
                        oauth2AccessToken = currentParent.getOauth2AccessToken();
                        break;
                    }
                    if (in.slpro.apibanker.ui.MainFrame.getInstance() != null) {
                        currentParent = in.slpro.apibanker.ui.MainFrame.getInstance().findFolderParent(currentParent);
                    } else {
                        currentParent = null;
                    }
                }
                if (currentParent == null || authType == null || "inherit".equalsIgnoreCase(authType)
                        || authType.isEmpty()) {
                    authType = "none";
                }
            }

            if ("bearer".equalsIgnoreCase(authType)) {
                String token = resolveVariables(authToken, requestModel, environment);
                if (token != null && !token.isBlank())
                    reqBuilder.header("Authorization", "Bearer " + token);
            } else if ("oauth2".equalsIgnoreCase(authType)) {
                String token = resolveVariables(oauth2AccessToken, requestModel, environment);
                if (token != null && !token.isBlank())
                    reqBuilder.header("Authorization", "Bearer " + token);
            } else if ("basic".equalsIgnoreCase(authType)) {
                String creds = authUsername + ":" + authPassword;
                reqBuilder.header("Authorization", "Basic "
                        + java.util.Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8)));
            } else if ("apiKey".equalsIgnoreCase(authType)) {
                String keyName = resolveVariables(authApiKeyName, requestModel, environment);
                String keyValue = resolveVariables(authApiKeyValue, requestModel, environment);
                if ("header".equalsIgnoreCase(authApiKeyIn)) {
                    if (keyName != null && !keyName.isBlank())
                        reqBuilder.header(keyName, keyValue != null ? keyValue : "");
                } else if ("query".equalsIgnoreCase(authApiKeyIn)) {
                    if (keyName != null && !keyName.isBlank()) {
                        String delim = resolvedUrl.contains("?") ? "&" : "?";
                        resolvedUrl += delim + java.net.URLEncoder.encode(keyName, StandardCharsets.UTF_8) + "=" +
                                java.net.URLEncoder.encode(keyValue != null ? keyValue : "", StandardCharsets.UTF_8);
                        reqBuilder.uri(URI.create(resolvedUrl));
                    }
                }
            }
            if (!hasUserAgent) {
                reqBuilder.header("User-Agent", "ApiBanker Client");
            }

            // Body
            HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();
            if (!("GET".equals(method) || "DELETE".equals(method) || "HEAD".equals(method))) {
                String bodyType = requestModel.getBodyType();
                if ("raw".equalsIgnoreCase(bodyType)) {
                    resolvedBodyStr = resolveVariables(requestModel.getBodyRawContent(), requestModel, environment);
                    if (resolvedBodyStr == null)
                        resolvedBodyStr = "";
                    bodyPublisher = HttpRequest.BodyPublishers.ofString(resolvedBodyStr, StandardCharsets.UTF_8);
                    String rawType = requestModel.getBodyRawType();
                    if ("JSON".equalsIgnoreCase(rawType)) {
                        if (!hasContentType) reqBuilder.header("Content-Type", "application/json");
                    } else if ("XML".equalsIgnoreCase(rawType)) {
                        if (!hasContentType) reqBuilder.header("Content-Type", "application/xml");
                    } else if ("HTML".equalsIgnoreCase(rawType)) {
                        if (!hasContentType) reqBuilder.header("Content-Type", "text/html");
                    } else {
                        if (!hasContentType) reqBuilder.header("Content-Type", "text/plain");
                    }
                } else if ("form-data".equalsIgnoreCase(bodyType)) {
                    String boundary = "ApiBankerBoundary" + System.currentTimeMillis();
                    try {
                        byte[] multipartData = buildMultipartBody(requestModel.getFormData(), boundary, requestModel,
                                environment);
                        bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(multipartData);
                        if (!hasContentType) reqBuilder.header("Content-Type", "multipart/form-data; boundary=" + boundary);
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
                            if (!item.isEnabled() || item.getKey() == null || item.getKey().isBlank())
                                continue;
                            if (formSb.length() > 0)
                                formSb.append("&");
                            formSb.append(java.net.URLEncoder.encode(
                                    resolveVariables(item.getKey(), requestModel, environment), StandardCharsets.UTF_8))
                                    .append("=")
                                    .append(java.net.URLEncoder
                                            .encode(resolveVariables(item.getValue() != null ? item.getValue() : "",
                                                    requestModel, environment), StandardCharsets.UTF_8));
                        }
                    }
                    resolvedBodyStr = formSb.toString();
                    bodyPublisher = HttpRequest.BodyPublishers.ofString(resolvedBodyStr, StandardCharsets.UTF_8);
                    if (!hasContentType) reqBuilder.header("Content-Type", "application/x-www-form-urlencoded");
                } else if ("graphql".equalsIgnoreCase(bodyType)) {
                    if (!hasContentType) reqBuilder.header("Content-Type", "application/json");
                    try {
                        String rawContent = requestModel.getBodyRawContent();
                        String query = "";
                        String variablesStr = "";
                        if (rawContent != null && rawContent.trim().startsWith("{")) {
                            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(rawContent)
                                    .getAsJsonObject();
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
                                com.google.gson.JsonElement varsJson = com.google.gson.JsonParser
                                        .parseString(resolvedVars);
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
            boolean verifySsl = in.slpro.apibanker.ui.MainFrame.resolveSslVerificationStatic(requestModel);
            boolean followRedirects = in.slpro.apibanker.ui.MainFrame.resolveRedirectSettingStatic(requestModel);

            long requestStartTime = System.currentTimeMillis();
            HttpResponse<String> httpResponse = getClient(verifySsl, followRedirects, timeoutSeconds).send(reqBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());
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

                ConsoleLogger.getInstance().logRequest(method, httpResponse.uri().toString(), statusCode,
                        executionTimeMs,
                        reqHeaders, resolvedBodyStr, headers, responseBody);
            }

            ResponseModel response = new ResponseModel(statusCode, statusText, executionTimeMs, sizeBytes, responseBody,
                    headers);
            response.setActualUrl(httpResponse.uri().toString());
            response.setPreRequestTimeMs(preRequestTimeMs);
            response.setNetworkTimeMs(networkTimeMs);

            // Extract SSL Certificate Details
            if (httpResponse.sslSession().isPresent()) {
                try {
                    java.security.cert.Certificate[] certs = httpResponse.sslSession().get().getPeerCertificates();
                    if (certs.length > 0 && certs[0] instanceof java.security.cert.X509Certificate) {
                        java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) certs[0];
                        
                        boolean dateValid = true;
                        try {
                            cert.checkValidity();
                        } catch (Exception e) {
                            dateValid = false;
                        }

                        String subject = cert.getSubjectX500Principal().getName();
                        String issuer = cert.getIssuerX500Principal().getName();

                        String color = dateValid ? "green" : "red";
                        String highlightedSubject = subject.replaceAll("(CN=)([^,]+)", "$1<font color='" + color + "'><b>$2</b></font>");
                        String highlightedIssuer = issuer.replaceAll("(CN=)([^,]+)", "$1<font color='" + color + "'><b>$2</b></font>");

                        highlightedSubject = highlightedSubject.replace(",", ",<br>&nbsp;&nbsp;");
                        highlightedIssuer = highlightedIssuer.replace(",", ",<br>&nbsp;&nbsp;");

                        StringBuilder sb = new StringBuilder(
                                "<html><div style='width:350px;'><b style='font-size:11px'>SSL Certificate Details:</b><br><br>");
                        sb.append("<b>Subject:</b><br>&nbsp;&nbsp;").append(highlightedSubject).append("<br><br>");
                        sb.append("<b>Issuer:</b><br>&nbsp;&nbsp;").append(highlightedIssuer).append("<br><br>");
                        sb.append("<b>Valid From:</b> ").append(cert.getNotBefore()).append("<br>");
                        sb.append("<b>Valid Until:</b> ").append(cert.getNotAfter()).append("<br><br>");

                        String sslStatusText = dateValid ? (verifySsl ? "Verified & Valid" : "Valid (Unverified)") : "Expired/Invalid";
                        sb.append("<b>Status:</b> <font color='").append(color).append("'><b>").append(sslStatusText).append("</b></font>")
                                .append("</div></html>");

                        response.setSslDetails(sb.toString());
                        response.setSslValid(dateValid);
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
                ResponseModel rm = new ResponseModel(pr.statusCode(), getStatusText(pr.statusCode()), 0, 0, "",
                        pr.headers().map());
                rm.setActualUrl(pr.request().uri().toString());

                // Set SSL details for redirect if needed (optional)

                redirects.add(0, rm);
                p = pr.previousResponse();
            }
            response.setRedirects(redirects);

            // Execute test scripts: Collection-level first, then Request-level
            long testStartTime = System.currentTimeMillis();
            ScriptResult collTestResult = new ScriptResult();
            if (collection != null && collection.getPostRequestScript() != null
                    && !collection.getPostRequestScript().isBlank()) {
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
            String errorMsg = getErrorMessage(e, resolvedUrl);
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
     * Maps standard HTTP Status codes into their corresponding human-readable
     * reason phrases.
     * 
     * @param code The HTTP status code.
     * @return The human-readable string representation of the code.
     */
    private String getStatusText(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 304 -> "Not Modified";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 409 -> "Conflict";
            case 422 -> "Unprocessable Entity";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default -> "Unknown";
        };
    }

    /**
     * Converts a raw Java network Exception into a user-friendly error message.
     * 
     * @param e The exception thrown during network execution.
     * @return A clear, descriptive error string (e.g. "Connection refused: ...").
     */
    private String getErrorMessage(Exception e, String resolvedUrl) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String msg = cause.getMessage() != null ? cause.getMessage() : e.getMessage();
        if (msg == null) msg = "";

        if (msg.contains("{{") && msg.contains("}}")) {
            return "Unresolved Variable Error: The request failed due to an undefined variable (" + msg + "). Please ensure it is defined and enabled in your Environment, Collection, or Global variables.";
        }

        if (cause instanceof java.net.ConnectException)
            return "Connection Refused: Ensure the server is running, the port is correct, and there are no firewall issues. (" + msg + ")";
        if (cause instanceof java.net.UnknownHostException)
            return "Unknown Host: Could not resolve the server address. Check your internet connection, VPN, or DNS settings. (" + msg + ")";
        if (cause instanceof java.net.SocketTimeoutException || cause.getClass().getSimpleName().contains("HttpTimeoutException"))
            return "Connection Timed Out: The server took too long to respond. You can increase the timeout in the Request Settings tab. (" + msg + ")";
        if (cause instanceof javax.net.ssl.SSLHandshakeException || cause instanceof javax.net.ssl.SSLPeerUnverifiedException || cause instanceof javax.net.ssl.SSLException) {
            if (msg.contains("No name matching") || msg.contains("Certificate for") || msg.contains("does not match")) {
                return "SSL Certificate Error: The domain name does not match the certificate. You can disable SSL Verification in the Request Settings tab to bypass this.";
            } else if (msg.contains("unable to find valid certification path") || msg.contains("PKIX path building failed") || msg.contains("SunCertPathBuilderException")) {
                return "SSL Certificate Error: Untrusted or self-signed certificate detected. You can disable SSL Verification in the Request Settings tab to bypass this.";
            }
            return "SSL/TLS Security Error: " + msg + ". You can disable SSL Verification in the Request Settings tab to bypass this.";
        }
        if (cause instanceof IllegalArgumentException && msg.contains("URI")) {
             return "Invalid URL: Please check the URL format and ensure it includes the protocol (e.g., http:// or https://). (" + msg + ")";
        }
        
        return cause.getClass().getSimpleName() + ": " + msg;
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
     * Serializes a list of Form-Data key-value pairs into a standardized MultiPart
     * HTTP byte payload,
     * including resolving any embedded variables and handling actual physical file
     * uploads.
     * 
     * @param items        The form-data parameters to serialize.
     * @param boundary     The unique multipart boundary string.
     * @param requestModel The parent request context (for variable resolution).
     * @param environment  The active environment (for variable resolution).
     * @return The generated MultiPart byte array payload ready for HTTP
     *         transmission.
     * @throws Exception If an error occurs reading an attached file.
     */
    private byte[] buildMultipartBody(List<KeyValueItem> items, String boundary, RequestModel requestModel,
            EnvironmentModel environment) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] newline = "\r\n".getBytes(StandardCharsets.UTF_8);

        for (KeyValueItem item : items) {
            if (!item.isEnabled() || item.getKey() == null || item.getKey().isBlank())
                continue;

            String key = resolveVariables(item.getKey(), requestModel, environment);
            String type = item.getType() != null ? item.getType() : "text";

            bos.write(("--" + boundary).getBytes(StandardCharsets.UTF_8));
            bos.write(newline);

            if ("file".equalsIgnoreCase(type)) {
                String rawPath = item.getValue() != null ? item.getValue() : "";
                String filePath = resolveVariables(rawPath, requestModel, environment);
                if (filePath != null) {
                    filePath = filePath.trim();
                    if (filePath.startsWith("\"") && filePath.endsWith("\"") && filePath.length() > 1) {
                        filePath = filePath.substring(1, filePath.length() - 1).trim();
                    }
                }

                File file = (filePath != null && !filePath.isBlank()) ? new File(filePath) : null;
                String fileName = (file != null) ? file.getName() : "";
                String safeFileName = fileName.replace("\"", "\\\"");

                bos.write(String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"", key, safeFileName)
                        .getBytes(StandardCharsets.UTF_8));
                bos.write(newline);

                String contentType = null;
                if (file != null && file.exists()) {
                    try {
                        contentType = Files.probeContentType(file.toPath());
                    } catch (Exception ignored) {
                    }
                }
                if (contentType == null || contentType.isBlank()) {
                    contentType = "application/octet-stream";
                }
                bos.write(String.format("Content-Type: %s", contentType).getBytes(StandardCharsets.UTF_8));
                bos.write(newline);
                bos.write(newline);

                if (file != null && file.exists() && file.isFile()) {
                    try {
                        bos.write(Files.readAllBytes(file.toPath()));
                    } catch (Exception ex) {
                        // Log or ignore unreadable file bytes without breaking the rest of the request
                    }
                }
            } else {
                String val = resolveVariables(item.getValue() != null ? item.getValue() : "", requestModel,
                        environment);
                bos.write(String.format("Content-Disposition: form-data; name=\"%s\"", key)
                        .getBytes(StandardCharsets.UTF_8));
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
     * Dynamically builds a {@link java.net.http.HttpClient} instance enforcing
     * custom configurations
     * such as Follow Redirects flags and strict/loose SSL Certification
     * verification requirements.
     * 
     * @param sslVerification True if strict SSL verification should be enforced.
     * @param followRedirects True if the client should automatically follow HTTP
     *                        3xx redirects.
     * @return The configured HttpClient instance ready to send requests.
     */
    private HttpClient getClient(boolean sslVerification, boolean followRedirects, int timeoutSeconds) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .followRedirects(followRedirects ? HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER);
        if (!sslVerification) {
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
            try {
                javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
                sslContext.init(null, new javax.net.ssl.TrustManager[] {
                        new javax.net.ssl.X509ExtendedTrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }

                            public void checkClientTrusted(java.security.cert.X509Certificate[] certs,
                                    String authType) {
                            }

                            public void checkServerTrusted(java.security.cert.X509Certificate[] certs,
                                    String authType) {
                            }

                            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType,
                                    java.net.Socket socket) {
                            }

                            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType,
                                    java.net.Socket socket) {
                            }

                            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType,
                                    javax.net.ssl.SSLEngine engine) {
                            }

                            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType,
                                    javax.net.ssl.SSLEngine engine) {
                            }
                        }
                }, new java.security.SecureRandom());
                builder.sslContext(sslContext);

                javax.net.ssl.SSLParameters sslParams = new javax.net.ssl.SSLParameters();
                sslParams.setEndpointIdentificationAlgorithm("");
                builder.sslParameters(sslParams);
            } catch (Exception ignored) {
            }
        } else {
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "false");
        }
        return builder.build();
    }
}

