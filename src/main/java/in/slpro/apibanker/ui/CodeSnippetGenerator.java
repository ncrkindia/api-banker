package in.slpro.apibanker.ui;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import in.slpro.apibanker.model.CollectionModel;
import in.slpro.apibanker.model.EnvironmentModel;
import in.slpro.apibanker.model.KeyValueItem;
import in.slpro.apibanker.model.RequestModel;

/**
 * CodeSnippetGenerator
 *
 * <p>
 * Core functionality and implementation logic for CodeSnippetGenerator.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 2.0.0
 * @since 1.0.0
 */
public class CodeSnippetGenerator {
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    public static String generate(String language, RequestModel request, EnvironmentModel env, boolean resolveVars) {
        String method = request.getMethod() != null ? request.getMethod() : "GET";
        String url = request.getUrl() != null ? request.getUrl() : "";
        if (resolveVars) {
            url = resolveString(url, request, env);
        }

        // Build parameters into URL if enabled
        List<KeyValueItem> params = request.getParams();
        if (params != null && !params.isEmpty()) {
            StringBuilder urlBuilder = new StringBuilder(url);
            boolean first = urlBuilder.indexOf("?") < 0;
            for (KeyValueItem param : params) {
                if (!param.isEnabled() || param.getKey() == null || param.getKey().isBlank())
                    continue;
                String k = param.getKey();
                String v = param.getValue() != null ? param.getValue() : "";
                if (resolveVars) {
                    k = resolveString(k, request, env);
                    v = resolveString(v, request, env);
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

        Map<String, String> headers = new HashMap<>();
        if (request.getHeaders() != null) {
            for (KeyValueItem header : request.getHeaders()) {
                if (!header.isEnabled() || header.getKey() == null || header.getKey().isBlank())
                    continue;
                String k = header.getKey();
                String v = header.getValue() != null ? header.getValue() : "";
                if (resolveVars) {
                    k = resolveString(k, request, env);
                    v = resolveString(v, request, env);
                }
                headers.put(k, v);
            }
        }

        // Auth
        String authType = request.getAuthType();
        String authToken = request.getAuthToken();
        String authUsername = request.getAuthUsername();
        String authPassword = request.getAuthPassword();
        String authApiKeyName = request.getAuthApiKeyName();
        String authApiKeyValue = request.getAuthApiKeyValue();
        String authApiKeyIn = request.getAuthApiKeyIn();

        if ("inherit".equalsIgnoreCase(authType) || authType == null) {
            CollectionModel collection = MainFrame.findParentCollection(request);
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
            String token = authToken;
            if (resolveVars) {
                token = resolveString(token, request, env);
            }
            if (token != null && !token.isBlank()) {
                headers.put("Authorization", "Bearer " + token);
            }
        } else if ("basic".equalsIgnoreCase(authType)) {
            String u = authUsername != null ? authUsername : "";
            String p = authPassword != null ? authPassword : "";
            if (resolveVars) {
                u = resolveString(u, request, env);
                p = resolveString(p, request, env);
            }
            String creds = u + ":" + p;
            headers.put("Authorization",
                    "Basic " + Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8)));
        } else if ("apiKey".equalsIgnoreCase(authType)) {
            String keyName = authApiKeyName;
            String keyValue = authApiKeyValue;
            if (resolveVars) {
                keyName = resolveString(keyName, request, env);
                keyValue = resolveString(keyValue, request, env);
            }
            if (keyName != null && !keyName.isBlank()) {
                if ("header".equalsIgnoreCase(authApiKeyIn)) {
                    headers.put(keyName, keyValue != null ? keyValue : "");
                } else if ("query".equalsIgnoreCase(authApiKeyIn)) {
                    String delim = url.contains("?") ? "&" : "?";
                    try {
                        url += delim + java.net.URLEncoder.encode(keyName, StandardCharsets.UTF_8) + "=" +
                                java.net.URLEncoder.encode(keyValue != null ? keyValue : "", StandardCharsets.UTF_8);
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // Body
        String bodyContent = "";
        String contentType = null;
        if (!("GET".equals(method) || "DELETE".equals(method) || "HEAD".equals(method))) {
            String bodyType = request.getBodyType();
            if ("raw".equalsIgnoreCase(bodyType)) {
                bodyContent = request.getBodyRawContent();
                if (resolveVars) {
                    bodyContent = resolveString(bodyContent, request, env);
                }
                if (bodyContent == null)
                    bodyContent = "";
                String rawType = request.getBodyRawType();
                if ("JSON".equalsIgnoreCase(rawType))
                    contentType = "application/json";
                else if ("XML".equalsIgnoreCase(rawType))
                    contentType = "application/xml";
                else if ("HTML".equalsIgnoreCase(rawType))
                    contentType = "text/html";
                else
                    contentType = "text/plain";
            } else if ("form".equalsIgnoreCase(bodyType)) {
                StringBuilder formSb = new StringBuilder();
                if (request.getFormData() != null) {
                    for (KeyValueItem item : request.getFormData()) {
                        if (!item.isEnabled() || item.getKey() == null || item.getKey().isBlank())
                            continue;
                        if (formSb.length() > 0)
                            formSb.append("&");
                        String k = item.getKey();
                        String v = item.getValue() != null ? item.getValue() : "";
                        if (resolveVars) {
                            k = resolveString(k, request, env);
                            v = resolveString(v, request, env);
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
            }
        }
        if (contentType != null && !headers.containsKey("Content-Type")) {
            headers.put("Content-Type", contentType);
        }

        return switch (language.toLowerCase()) {
            case "curl" -> generateCurl(method, url, headers, bodyContent);
            case "javascript", "js", "fetch" -> generateFetch(method, url, headers, bodyContent);
            case "python" -> generatePython(method, url, headers, bodyContent);
            case "java" -> generateJava(method, url, headers, bodyContent);
            default -> "";
        };
    }

    private static String resolveString(String input, RequestModel requestModel, EnvironmentModel env) {
        if (input == null)
            return null;
        CollectionModel collection = MainFrame.findParentCollection(requestModel);
        Matcher matcher = VAR_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            String value = null;
            if (env != null && env.getVariables() != null) {
                value = env.getVariables().stream()
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

    private static String generateCurl(String method, String url, Map<String, String> headers, String body) {
        StringBuilder sb = new StringBuilder();
        sb.append("curl -X ").append(method).append(" \"").append(url).append("\"");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(" \\\n  -H \"").append(entry.getKey()).append(": ").append(escapeCurlHeader(entry.getValue()))
                    .append("\"");
        }
        if (body != null && !body.isEmpty()) {
            sb.append(" \\\n  -d ").append(escapeCurlBody(body));
        }
        return sb.toString();
    }

    private static String escapeCurlHeader(String val) {
        return val.replace("\"", "\\\"");
    }

    private static String escapeCurlBody(String body) {
        if (!body.contains("\n")) {
            return "'" + body.replace("'", "'\\''") + "'";
        } else {
            return "$'" + body.replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r") + "'";
        }
    }

    private static String generateFetch(String method, String url, Map<String, String> headers, String body) {
        StringBuilder sb = new StringBuilder();
        sb.append("const myHeaders = new Headers();\n");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append("myHeaders.append(\"").append(entry.getKey()).append("\", \"").append(escapeJs(entry.getValue()))
                    .append("\");\n");
        }
        sb.append("\n");
        if (body != null && !body.isEmpty()) {
            sb.append("const requestOptions = {\n");
            sb.append("  method: \"").append(method).append("\",\n");
            sb.append("  headers: myHeaders,\n");
            sb.append("  body: ").append(formatJsBody(body, headers.get("Content-Type"))).append(",\n");
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
        return val.replace("\\", "\\\\").replace("\"", "\\\"");
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

    private static String generatePython(String method, String url, Map<String, String> headers, String body) {
        StringBuilder sb = new StringBuilder();
        sb.append("import requests\n\n");
        sb.append("url = \"").append(url).append("\"\n\n");
        if (!headers.isEmpty()) {
            sb.append("headers = {\n");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                sb.append("  '").append(entry.getKey()).append("': '").append(escapePython(entry.getValue()))
                        .append("',\n");
            }
            sb.append("}\n\n");
        } else {
            sb.append("headers = {}\n\n");
        }
        if (body != null && !body.isEmpty()) {
            if (headers.get("Content-Type") != null && headers.get("Content-Type").contains("application/json")) {
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
        return val.replace("\\", "\\\\").replace("'", "\\'");
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

    private static String generateJava(String method, String url, Map<String, String> headers, String body) {
        StringBuilder sb = new StringBuilder();
        sb.append("import java.net.URI;\n");
        sb.append("import java.net.http.HttpClient;\n");
        sb.append("import java.net.http.HttpRequest;\n");
        sb.append("import java.net.http.HttpResponse;\n\n");
        sb.append("public class ApiBankerClientExample {\n");
        sb.append("    public static void main(String[] args) throws Exception {\n");
        sb.append("        HttpClient client = HttpClient.newHttpClient();\n\n");
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
        sb.append(
                "        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());\n");
        sb.append("        System.out.println(response.statusCode());\n");
        sb.append("        System.out.println(response.body());\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String escapeJava(String val) {
        return val.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String formatJavaBody(String body) {
        if (!body.contains("\n")) {
            return "\"" + escapeJava(body) + "\"";
        } else {
            return "\"\"\"\n" + body.replace("\"\"\"", "\\\"\\\"\\\"") + "\"\"\"";
        }
    }
}


