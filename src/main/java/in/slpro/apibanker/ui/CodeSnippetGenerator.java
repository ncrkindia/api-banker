package in.slpro.apibanker.ui;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import in.slpro.apibanker.model.CollectionModel;
import in.slpro.apibanker.model.EnvironmentModel;
import in.slpro.apibanker.model.KeyValueItem;
import in.slpro.apibanker.model.RequestModel;
import in.slpro.apibanker.model.VariableHelper;

/**
 * CodeSnippetGenerator
 *
 * <p>
 * Core functionality and implementation logic for CodeSnippetGenerator.
 * Generates ready-to-run code snippets in cURL, JavaScript (fetch),
 * Python (requests), and Java (HttpClient) with complete parameter,
 * header, recursive inherited auth, and body support.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 2.0.2
 * @since 1.0.0
 */
public class CodeSnippetGenerator {

    public static String generate(String language, RequestModel request, EnvironmentModel env, boolean resolveVars) {
        List<CollectionModel> collections = MainFrame.getInstance() != null
                ? MainFrame.getInstance().getCollections()
                : null;
        return generate(language, request, env, collections, resolveVars);
    }

    public static String generate(String language, RequestModel request, EnvironmentModel env,
            List<CollectionModel> collections, boolean resolveVars) {
        if (request == null) {
            return "";
        }
        if (collections == null && MainFrame.getInstance() != null) {
            collections = MainFrame.getInstance().getCollections();
        }

        String method = request.getMethod() != null ? request.getMethod() : "GET";
        String rawUrl = request.getUrl() != null ? request.getUrl() : "";

        // Build parameters into URL if enabled (strip query string from rawUrl to avoid duplication)
        List<KeyValueItem> params = request.getParams();
        String baseUrl = rawUrl;
        if (params != null && !params.isEmpty()) {
            int qIdx = baseUrl.indexOf('?');
            if (qIdx >= 0) {
                baseUrl = baseUrl.substring(0, qIdx);
            }
        }
        String url = baseUrl;
        if (resolveVars) {
            url = resolveVariables(url, request, env);
        }

        if (params != null && !params.isEmpty()) {
            StringBuilder urlBuilder = new StringBuilder(url);
            boolean first = urlBuilder.indexOf("?") < 0;
            for (KeyValueItem param : params) {
                if (!param.isEnabled() || param.getKey() == null || param.getKey().isBlank())
                    continue;
                String k = param.getKey();
                String v = param.getValue() != null ? param.getValue() : "";
                if (resolveVars) {
                    k = resolveVariables(k, request, env);
                    v = resolveVariables(v, request, env);
                }
                try {
                    urlBuilder.append(first ? "?" : "&")
                            .append(java.net.URLEncoder.encode(k, StandardCharsets.UTF_8))
                            .append("=")
                            .append(java.net.URLEncoder.encode(v, StandardCharsets.UTF_8));
                } catch (Exception ignored) {
                }
                first = false;
            }
            url = urlBuilder.toString();
        }

        // Header Params
        Map<String, String> headers = new LinkedHashMap<>();
        if (request.getHeaders() != null) {
            for (KeyValueItem header : request.getHeaders()) {
                if (!header.isEnabled() || header.getKey() == null || header.getKey().isBlank())
                    continue;
                String k = header.getKey().trim();
                if (k.equalsIgnoreCase("Host") || k.equalsIgnoreCase("Content-Length")) {
                    continue;
                }
                String v = header.getValue() != null ? header.getValue() : "";
                if (resolveVars) {
                    k = resolveVariables(k, request, env);
                    v = resolveVariables(v, request, env);
                }
                putHeader(headers, k, v);
            }
        }

        // Resolve Auth Header recursively whenever applicable (e.g. for inherit)
        String authType = request.getAuthType();
        String authToken = request.getAuthToken();
        String authUsername = request.getAuthUsername();
        String authPassword = request.getAuthPassword();
        String authApiKeyName = request.getAuthApiKeyName();
        String authApiKeyValue = request.getAuthApiKeyValue();
        String authApiKeyIn = request.getAuthApiKeyIn();
        String oauth2AccessToken = request.getOauth2AccessToken();

        if (authType == null || "inherit".equalsIgnoreCase(authType) || authType.trim().isEmpty()) {
            CollectionModel currentParent = findRequestParent(collections, request);
            if (currentParent == null && MainFrame.getInstance() != null) {
                currentParent = MainFrame.findParentCollection(request);
            }

            while (currentParent != null) {
                String parentAuth = currentParent.getAuthType();
                if (parentAuth != null && !parentAuth.equalsIgnoreCase("inherit") && !parentAuth.trim().isEmpty()) {
                    authType = parentAuth;
                    authToken = currentParent.getAuthToken();
                    authUsername = currentParent.getAuthUsername();
                    authPassword = currentParent.getAuthPassword();
                    authApiKeyName = currentParent.getAuthApiKeyName();
                    authApiKeyValue = currentParent.getAuthApiKeyValue();
                    authApiKeyIn = currentParent.getAuthApiKeyIn();
                    oauth2AccessToken = currentParent.getOauth2AccessToken();
                    break;
                }
                CollectionModel nextParent = findFolderParent(collections, currentParent);
                if (nextParent == null && MainFrame.getInstance() != null) {
                    nextParent = MainFrame.getInstance().findFolderParent(currentParent);
                }
                currentParent = nextParent;
            }
            if (currentParent == null || authType == null || "inherit".equalsIgnoreCase(authType)
                    || authType.trim().isEmpty()) {
                authType = "none";
            }
        }

        if ("bearer".equalsIgnoreCase(authType)) {
            String token = authToken != null ? authToken : "";
            if (resolveVars) {
                token = resolveVariables(token, request, env);
            }
            if (!token.isBlank()) {
                putHeader(headers, "Authorization", "Bearer " + token);
            }
        } else if ("oauth2".equalsIgnoreCase(authType)) {
            String token = oauth2AccessToken != null ? oauth2AccessToken : "";
            if (resolveVars) {
                token = resolveVariables(token, request, env);
            }
            if (!token.isBlank()) {
                putHeader(headers, "Authorization", "Bearer " + token);
            }
        } else if ("basic".equalsIgnoreCase(authType)) {
            String u = authUsername != null ? authUsername : "";
            String p = authPassword != null ? authPassword : "";
            if (resolveVars) {
                u = resolveVariables(u, request, env);
                p = resolveVariables(p, request, env);
            }
            String creds = (u != null ? u : "") + ":" + (p != null ? p : "");
            putHeader(headers, "Authorization",
                    "Basic " + Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8)));
        } else if ("apiKey".equalsIgnoreCase(authType)) {
            String keyName = authApiKeyName;
            String keyValue = authApiKeyValue != null ? authApiKeyValue : "";
            if (resolveVars) {
                keyName = resolveVariables(keyName, request, env);
                keyValue = resolveVariables(keyValue, request, env);
            }
            if (keyName != null && !keyName.isBlank()) {
                if ("query".equalsIgnoreCase(authApiKeyIn)) {
                    String delim = url.contains("?") ? "&" : "?";
                    try {
                        url += delim + java.net.URLEncoder.encode(keyName, StandardCharsets.UTF_8) + "=" +
                                java.net.URLEncoder.encode(keyValue, StandardCharsets.UTF_8);
                    } catch (Exception ignored) {
                    }
                } else {
                    putHeader(headers, keyName, keyValue);
                }
            }
        }

        // Body resolution (supports all non-GET/HEAD methods, including DELETE, POST, PUT, PATCH)
        String bodyContent = "";
        String contentType = null;
        List<KeyValueItem> resolvedFormData = new ArrayList<>();
        boolean allowsBody = !"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method);

        String bodyType = request.getBodyType();
        if (allowsBody && bodyType != null && !"none".equalsIgnoreCase(bodyType)) {
            if ("raw".equalsIgnoreCase(bodyType)) {
                bodyContent = request.getBodyRawContent() != null ? request.getBodyRawContent() : "";
                if (resolveVars) {
                    bodyContent = resolveVariables(bodyContent, request, env);
                }
                String rawType = request.getBodyRawType();
                if ("JSON".equalsIgnoreCase(rawType))
                    contentType = "application/json";
                else if ("XML".equalsIgnoreCase(rawType))
                    contentType = "application/xml";
                else if ("HTML".equalsIgnoreCase(rawType))
                    contentType = "text/html";
                else if ("JavaScript".equalsIgnoreCase(rawType))
                    contentType = "application/javascript";
                else
                    contentType = "text/plain";
            } else if ("x-www-form-urlencoded".equalsIgnoreCase(bodyType) || "form".equalsIgnoreCase(bodyType)
                    || "urlencoded".equalsIgnoreCase(bodyType)) {
                List<KeyValueItem> items = request.getUrlencodedData();
                if (items == null || items.isEmpty()) {
                    items = request.getFormData();
                }
                StringBuilder formSb = new StringBuilder();
                if (items != null) {
                    for (KeyValueItem item : items) {
                        if (!item.isEnabled() || item.getKey() == null || item.getKey().isBlank())
                            continue;
                        if (formSb.length() > 0)
                            formSb.append("&");
                        String k = item.getKey();
                        String v = item.getValue() != null ? item.getValue() : "";
                        if (resolveVars) {
                            k = resolveVariables(k, request, env);
                            v = resolveVariables(v, request, env);
                        }
                        try {
                            formSb.append(java.net.URLEncoder.encode(k, StandardCharsets.UTF_8))
                                    .append("=")
                                    .append(java.net.URLEncoder.encode(v, StandardCharsets.UTF_8));
                        } catch (Exception ignored) {
                        }
                    }
                }
                bodyContent = formSb.toString();
                contentType = "application/x-www-form-urlencoded";
            } else if ("graphql".equalsIgnoreCase(bodyType)) {
                contentType = "application/json";
                String rawContent = request.getBodyRawContent();
                String query = "";
                String variablesStr = "";
                if (rawContent != null && rawContent.trim().startsWith("{")) {
                    try {
                        com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(rawContent)
                                .getAsJsonObject();
                        if (json.has("query")) {
                            query = json.get("query").getAsString();
                        }
                        if (json.has("variables")) {
                            variablesStr = json.get("variables").getAsString();
                        }
                    } catch (Exception e) {
                        query = rawContent != null ? rawContent : "";
                    }
                } else {
                    query = rawContent != null ? rawContent : "";
                }

                if (resolveVars) {
                    query = resolveVariables(query, request, env);
                    variablesStr = resolveVariables(variablesStr, request, env);
                }

                com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
                payload.addProperty("query", query != null ? query : "");
                if (variablesStr != null && !variablesStr.isBlank()) {
                    try {
                        com.google.gson.JsonElement varsJson = com.google.gson.JsonParser.parseString(variablesStr);
                        payload.add("variables", varsJson);
                    } catch (Exception e) {
                        payload.addProperty("variables", variablesStr);
                    }
                } else {
                    payload.add("variables", new com.google.gson.JsonObject());
                }
                bodyContent = new com.google.gson.Gson().toJson(payload);
            } else if ("form-data".equalsIgnoreCase(bodyType)) {
                List<KeyValueItem> items = request.getFormData();
                if (items != null) {
                    for (KeyValueItem item : items) {
                        if (!item.isEnabled() || item.getKey() == null || item.getKey().isBlank())
                            continue;
                        String k = item.getKey();
                        String v = item.getValue() != null ? item.getValue() : "";
                        if (resolveVars) {
                            k = resolveVariables(k, request, env);
                            v = resolveVariables(v, request, env);
                        }
                        KeyValueItem resolvedItem = new KeyValueItem(k, v, true);
                        resolvedItem.setType(item.getType());
                        resolvedFormData.add(resolvedItem);
                    }
                }
            }
        }

        if (contentType != null) {
            boolean hasCt = headers.keySet().stream().anyMatch(k -> k.equalsIgnoreCase("Content-Type"));
            if (!hasCt && !"form-data".equalsIgnoreCase(bodyType)) {
                headers.put("Content-Type", contentType);
            }
        }

        return switch (language.toLowerCase()) {
            case "curl" -> generateCurl(method, url, headers, bodyContent, bodyType, resolvedFormData);
            case "javascript", "js", "fetch" -> generateFetch(method, url, headers, bodyContent, bodyType, resolvedFormData);
            case "python" -> generatePython(method, url, headers, bodyContent, bodyType, resolvedFormData);
            case "java" -> generateJava(method, url, headers, bodyContent, bodyType, resolvedFormData);
            default -> "";
        };
    }

    private static CollectionModel findRequestParent(List<CollectionModel> collections, RequestModel req) {
        if (collections == null || req == null)
            return null;
        for (CollectionModel col : collections) {
            CollectionModel parent = findRequestParentRecursive(col, req);
            if (parent != null)
                return parent;
        }
        return null;
    }

    private static CollectionModel findRequestParentRecursive(CollectionModel col, RequestModel req) {
        if (col.getRequests() != null && col.getRequests().contains(req)) {
            return col;
        }
        if (col.getFolders() != null) {
            for (CollectionModel sub : col.getFolders()) {
                CollectionModel parent = findRequestParentRecursive(sub, req);
                if (parent != null)
                    return parent;
            }
        }
        return null;
    }

    private static CollectionModel findFolderParent(List<CollectionModel> collections, CollectionModel target) {
        if (collections == null || target == null)
            return null;
        for (CollectionModel col : collections) {
            if (col == target)
                return null;
            CollectionModel parent = findFolderParentRecursive(col, target);
            if (parent != null)
                return parent;
        }
        return null;
    }

    private static CollectionModel findFolderParentRecursive(CollectionModel col, CollectionModel target) {
        if (col.getFolders() != null && col.getFolders().contains(target)) {
            return col;
        }
        if (col.getFolders() != null) {
            for (CollectionModel sub : col.getFolders()) {
                CollectionModel parent = findFolderParentRecursive(sub, target);
                if (parent != null)
                    return parent;
            }
        }
        return null;
    }

    private static void putHeader(Map<String, String> headers, String key, String value) {
        if (key == null || key.isBlank()) return;
        String existingKey = headers.keySet().stream()
                .filter(k -> k.equalsIgnoreCase(key.trim()))
                .findFirst()
                .orElse(null);
        if (existingKey != null) {
            headers.remove(existingKey);
        }
        headers.put(key.trim(), value != null ? value : "");
    }

    private static String getHeaderValue(Map<String, String> headers, String name) {
        if (headers == null || name == null) return null;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String resolveVariables(String input, RequestModel requestModel, EnvironmentModel env) {
        if (input == null)
            return null;
        return VariableHelper.resolveVariables(input, requestModel, env);
    }

    private static String generateCurl(String method, String url, Map<String, String> headers, String body,
            String bodyType, List<KeyValueItem> formData) {
        StringBuilder sb = new StringBuilder();
        sb.append("curl -X ").append(method).append(" \"").append(url).append("\"");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(" \\\n  -H \"").append(entry.getKey()).append(": ").append(escapeCurlHeader(entry.getValue()))
                    .append("\"");
        }
        if ("form-data".equalsIgnoreCase(bodyType) && formData != null && !formData.isEmpty()) {
            for (KeyValueItem item : formData) {
                if ("file".equalsIgnoreCase(item.getType())) {
                    sb.append(" \\\n  -F \"").append(escapeCurlHeader(item.getKey())).append("=@")
                            .append(escapeCurlHeader(item.getValue() != null ? item.getValue() : "")).append("\"");
                } else {
                    sb.append(" \\\n  -F \"").append(escapeCurlHeader(item.getKey())).append("=")
                            .append(escapeCurlHeader(item.getValue() != null ? item.getValue() : "")).append("\"");
                }
            }
        } else if (body != null && !body.isEmpty()) {
            sb.append(" \\\n  -d ").append(escapeCurlBody(body));
        }
        return sb.toString();
    }

    private static String escapeCurlHeader(String val) {
        return val != null ? val.replace("\"", "\\\"") : "";
    }

    private static String escapeCurlBody(String body) {
        if (!body.contains("\n")) {
            return "'" + body.replace("'", "'\\''") + "'";
        } else {
            return "$'" + body.replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r") + "'";
        }
    }

    private static String generateFetch(String method, String url, Map<String, String> headers, String body,
            String bodyType, List<KeyValueItem> formData) {
        StringBuilder sb = new StringBuilder();
        sb.append("const myHeaders = new Headers();\n");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if ("form-data".equalsIgnoreCase(bodyType) && entry.getKey().equalsIgnoreCase("Content-Type")) {
                continue;
            }
            sb.append("myHeaders.append(\"").append(entry.getKey()).append("\", \"").append(escapeJs(entry.getValue()))
                    .append("\");\n");
        }
        sb.append("\n");

        if ("form-data".equalsIgnoreCase(bodyType) && formData != null && !formData.isEmpty()) {
            sb.append("const formdata = new FormData();\n");
            for (KeyValueItem item : formData) {
                if ("file".equalsIgnoreCase(item.getType())) {
                    sb.append("// Note: Select file in browser:\n");
                    sb.append("formdata.append(\"").append(escapeJs(item.getKey())).append("\", fileInput.files[0], \"")
                            .append(escapeJs(new java.io.File(item.getValue() != null ? item.getValue() : "file").getName()))
                            .append("\");\n");
                } else {
                    sb.append("formdata.append(\"").append(escapeJs(item.getKey())).append("\", \"")
                            .append(escapeJs(item.getValue() != null ? item.getValue() : "")).append("\");\n");
                }
            }
            sb.append("\n");
            sb.append("const requestOptions = {\n");
            sb.append("  method: \"").append(method).append("\",\n");
            sb.append("  headers: myHeaders,\n");
            sb.append("  body: formdata,\n");
            sb.append("  redirect: \"follow\"\n");
            sb.append("};\n");
        } else if (body != null && !body.isEmpty()) {
            sb.append("const requestOptions = {\n");
            sb.append("  method: \"").append(method).append("\",\n");
            sb.append("  headers: myHeaders,\n");
            sb.append("  body: ").append(formatJsBody(body, getHeaderValue(headers, "Content-Type"))).append(",\n");
            sb.append("  redirect: \"follow\"\n");
            sb.append("};\n");
        } else {
            sb.append("const requestOptions = {\n");
            sb.append("  method: \"").append(method).append("\",\n");
            sb.append("  headers: myHeaders,\n");
            sb.append("  redirect: \"follow\"\n");
            sb.append("};\n");
        }
        sb.append("\n");
        sb.append("fetch(\"").append(url).append("\", requestOptions)\n")
                .append("  .then((response) => response.text())\n")
                .append("  .then((result) => console.log(result))\n")
                .append("  .catch((error) => console.error(error));");
        return sb.toString();
    }

    private static String escapeJs(String val) {
        return val != null ? val.replace("\\", "\\\\").replace("\"", "\\\"") : "";
    }

    private static String formatJsBody(String body, String contentType) {
        if (contentType != null && contentType.contains("application/json")) {
            try {
                com.google.gson.JsonParser.parseString(body);
                return "JSON.stringify(" + body + ")";
            } catch (Exception e) {
                // fallback to raw string
            }
        }
        if (!body.contains("\n")) {
            return "\"" + escapeJs(body) + "\"";
        } else {
            return "`" + body.replace("\\", "\\\\").replace("`", "\\`").replace("${", "\\${") + "`";
        }
    }

    private static String generatePython(String method, String url, Map<String, String> headers, String body,
            String bodyType, List<KeyValueItem> formData) {
        StringBuilder sb = new StringBuilder();
        sb.append("import requests\n\n");
        sb.append("url = \"").append(url).append("\"\n\n");
        if (!headers.isEmpty()) {
            sb.append("headers = {\n");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if ("form-data".equalsIgnoreCase(bodyType) && entry.getKey().equalsIgnoreCase("Content-Type")) {
                    continue;
                }
                sb.append("  '").append(entry.getKey()).append("': '").append(escapePython(entry.getValue()))
                        .append("',\n");
            }
            sb.append("}\n\n");
        } else {
            sb.append("headers = {}\n\n");
        }

        if ("form-data".equalsIgnoreCase(bodyType) && formData != null && !formData.isEmpty()) {
            List<KeyValueItem> files = new ArrayList<>();
            List<KeyValueItem> fields = new ArrayList<>();
            for (KeyValueItem item : formData) {
                if ("file".equalsIgnoreCase(item.getType())) {
                    files.add(item);
                } else {
                    fields.add(item);
                }
            }
            sb.append("payload = {\n");
            for (KeyValueItem field : fields) {
                sb.append("  '").append(escapePython(field.getKey())).append("': '")
                        .append(escapePython(field.getValue() != null ? field.getValue() : "")).append("',\n");
            }
            sb.append("}\n");
            if (!files.isEmpty()) {
                sb.append("files = [\n");
                for (KeyValueItem file : files) {
                    sb.append("  ('").append(escapePython(file.getKey())).append("', open('")
                            .append(escapePython(file.getValue() != null ? file.getValue() : "")).append("', 'rb')),\n");
                }
                sb.append("]\n\n");
                sb.append("response = requests.request(\"").append(method)
                        .append("\", url, headers=headers, data=payload, files=files)\n\n");
            } else {
                sb.append("\nresponse = requests.request(\"").append(method)
                        .append("\", url, headers=headers, data=payload)\n\n");
            }
        } else if (body != null && !body.isEmpty()) {
            String ct = getHeaderValue(headers, "Content-Type");
            if (ct != null && ct.contains("application/json")) {
                sb.append("payload = ").append(formatPythonBody(body, true)).append("\n\n");
                sb.append("response = requests.request(\"").append(method)
                        .append("\", url, headers=headers, json=payload)\n\n");
            } else {
                sb.append("payload = ").append(formatPythonBody(body, false)).append("\n\n");
                sb.append("response = requests.request(\"").append(method)
                        .append("\", url, headers=headers, data=payload)\n\n");
            }
        } else {
            sb.append("response = requests.request(\"").append(method).append("\", url, headers=headers)\n\n");
        }
        sb.append("print(response.text)\n");
        return sb.toString();
    }

    private static String escapePython(String val) {
        return val != null ? val.replace("\\", "\\\\").replace("'", "\\'") : "";
    }

    private static String formatPythonBody(String body, boolean isJson) {
        if (isJson) {
            return "\"\"\"\n" + body + "\n\"\"\"";
        }
        if (!body.contains("\n")) {
            return "'" + escapePython(body) + "'";
        } else {
            return "\"\"\"\n" + body + "\n\"\"\"";
        }
    }

    private static String generateJava(String method, String url, Map<String, String> headers, String body,
            String bodyType, List<KeyValueItem> formData) {
        StringBuilder sb = new StringBuilder();
        sb.append("import java.net.URI;\n");
        sb.append("import java.net.http.HttpClient;\n");
        sb.append("import java.net.http.HttpRequest;\n");
        sb.append("import java.net.http.HttpResponse;\n\n");
        sb.append("public class ApiBankerClientExample {\n");
        sb.append("    public static void main(String[] args) throws Exception {\n");
        sb.append("        HttpClient client = HttpClient.newHttpClient();\n\n");

        if ("form-data".equalsIgnoreCase(bodyType) && formData != null && !formData.isEmpty()) {
            String boundary = "----ApiBankerBoundary" + System.currentTimeMillis();
            sb.append("        String boundary = \"").append(boundary).append("\";\n");
            sb.append("        StringBuilder bodyBuilder = new StringBuilder();\n");
            for (KeyValueItem item : formData) {
                sb.append("        bodyBuilder.append(\"--\").append(boundary).append(\"\\r\\n\");\n");
                if ("file".equalsIgnoreCase(item.getType())) {
                    sb.append("        bodyBuilder.append(\"Content-Disposition: form-data; name=\\\"")
                            .append(escapeJava(item.getKey())).append("\\\"; filename=\\\"")
                            .append(escapeJava(new java.io.File(item.getValue() != null ? item.getValue() : "file").getName()))
                            .append("\\\"\\r\\n\");\n");
                    sb.append("        bodyBuilder.append(\"Content-Type: application/octet-stream\\r\\n\\r\\n\");\n");
                    sb.append("        // Note: Insert file bytes or content\n");
                } else {
                    sb.append("        bodyBuilder.append(\"Content-Disposition: form-data; name=\\\"")
                            .append(escapeJava(item.getKey())).append("\\\"\\r\\n\\r\\n\");\n");
                    sb.append("        bodyBuilder.append(\"").append(escapeJava(item.getValue() != null ? item.getValue() : ""))
                            .append("\\r\\n\");\n");
                }
            }
            sb.append("        bodyBuilder.append(\"--\").append(boundary).append(\"--\\r\\n\");\n\n");
            sb.append("        HttpRequest request = HttpRequest.newBuilder()\n");
            sb.append("                .uri(URI.create(\"").append(url).append("\"))\n");
            sb.append("                .header(\"Content-Type\", \"multipart/form-data; boundary=\" + boundary)\n");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (!entry.getKey().equalsIgnoreCase("Content-Type")) {
                    sb.append("                .header(\"").append(entry.getKey()).append("\", \"")
                            .append(escapeJava(entry.getValue())).append("\")\n");
                }
            }
            sb.append("                .method(\"").append(method).append("\", HttpRequest.BodyPublishers.ofString(bodyBuilder.toString()))\n");
            sb.append("                .build();\n\n");
        } else {
            sb.append("        HttpRequest request = HttpRequest.newBuilder()\n");
            sb.append("                .uri(URI.create(\"").append(url).append("\"))\n");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                sb.append("                .header(\"").append(entry.getKey()).append("\", \"")
                        .append(escapeJava(entry.getValue())).append("\")\n");
            }
            if (body != null && !body.isEmpty()) {
                sb.append("                .method(\"").append(method).append("\", HttpRequest.BodyPublishers.ofString(")
                        .append(formatJavaBody(body)).append("))\n");
            } else {
                sb.append("                .method(\"").append(method).append("\", HttpRequest.BodyPublishers.noBody())\n");
            }
            sb.append("                .build();\n\n");
        }

        sb.append(
                "        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());\n");
        sb.append("        System.out.println(response.statusCode());\n");
        sb.append("        System.out.println(response.body());\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String escapeJava(String val) {
        return val != null ? val.replace("\\", "\\\\").replace("\"", "\\\"") : "";
    }

    private static String formatJavaBody(String body) {
        if (!body.contains("\n")) {
            return "\"" + escapeJava(body) + "\"";
        } else {
            return "\"\"\"\n" + body.replace("\"\"\"", "\\\"\\\"\\\"") + "\"\"\"";
        }
    }
}
