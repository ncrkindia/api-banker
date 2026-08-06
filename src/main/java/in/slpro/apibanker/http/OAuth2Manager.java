package in.slpro.apibanker.http;

import javax.swing.*;
import java.awt.Desktop;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.sun.net.httpserver.HttpServer;

import in.slpro.apibanker.model.CollectionModel;
import in.slpro.apibanker.model.RequestModel;
import in.slpro.apibanker.ui.CollectionPanel;
import in.slpro.apibanker.ui.RequestPanel;

import java.net.InetSocketAddress;
import java.io.InputStream;
import java.util.Scanner;

public class OAuth2Manager {

    public static void getNewAccessToken(Object model, JPanel panel) {
        String grantType = "";
        String callbackUrl = "";
        String authUrl = "";
        String accessTokenUrl = "";
        String clientId = "";
        String clientSecret = "";
        String scope = "";
        String state = "";
        String username = "";
        String password = "";
        String clientAuth = "";

        if (model instanceof RequestModel req) {
            grantType = req.getOauth2GrantType();
            callbackUrl = req.getOauth2CallbackUrl();
            authUrl = req.getOauth2AuthUrl();
            accessTokenUrl = req.getOauth2AccessTokenUrl();
            clientId = req.getOauth2ClientId();
            clientSecret = req.getOauth2ClientSecret();
            scope = req.getOauth2Scope();
            state = req.getOauth2State();
            username = req.getOauth2Username();
            password = req.getOauth2Password();
            clientAuth = req.getOauth2ClientAuth();
        } else if (model instanceof CollectionModel col) {
            grantType = col.getOauth2GrantType();
            callbackUrl = col.getOauth2CallbackUrl();
            authUrl = col.getOauth2AuthUrl();
            accessTokenUrl = col.getOauth2AccessTokenUrl();
            clientId = col.getOauth2ClientId();
            clientSecret = col.getOauth2ClientSecret();
            scope = col.getOauth2Scope();
            state = col.getOauth2State();
            username = col.getOauth2Username();
            password = col.getOauth2Password();
            clientAuth = col.getOauth2ClientAuth();
        } else {
            return;
        }

        if (grantType == null)
            grantType = "client_credentials";

        try {
            if ("client_credentials".equals(grantType) || "password".equals(grantType)) {
                StringBuilder body = new StringBuilder();
                body.append("grant_type=").append(URLEncoder.encode(grantType, StandardCharsets.UTF_8));

                if (scope != null && !scope.isBlank()) {
                    body.append("&scope=").append(URLEncoder.encode(scope, StandardCharsets.UTF_8));
                }

                if ("password".equals(grantType)) {
                    body.append("&username=")
                            .append(URLEncoder.encode(username == null ? "" : username, StandardCharsets.UTF_8));
                    body.append("&password=")
                            .append(URLEncoder.encode(password == null ? "" : password, StandardCharsets.UTF_8));
                }

                if ("body".equals(clientAuth)) {
                    body.append("&client_id=")
                            .append(URLEncoder.encode(clientId == null ? "" : clientId, StandardCharsets.UTF_8));
                    body.append("&client_secret=").append(
                            URLEncoder.encode(clientSecret == null ? "" : clientSecret, StandardCharsets.UTF_8));
                }

                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(accessTokenUrl))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString()));

                if ("header".equals(clientAuth)) {
                    String auth = (clientId == null ? "" : clientId) + ":" + (clientSecret == null ? "" : clientSecret);
                    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                    reqBuilder.header("Authorization", "Basic " + encodedAuth);
                }

                HttpClient client = HttpClient.newBuilder().build();
                client.sendAsync(reqBuilder.build(), HttpResponse.BodyHandlers.ofString())
                        .thenAccept(res -> {
                            handleTokenResponse(res.body(), model, panel);
                        })
                        .exceptionally(ex -> {
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(panel, "Failed to get token: " + ex.getMessage(),
                                        "OAuth2 Error", JOptionPane.ERROR_MESSAGE);
                            });
                            return null;
                        });

            } else if ("authorization_code".equals(grantType)) {
                // Generate URL
                StringBuilder url = new StringBuilder(authUrl);
                url.append(authUrl.contains("?") ? "&" : "?");
                url.append("response_type=code");
                url.append("&client_id=")
                        .append(URLEncoder.encode(clientId == null ? "" : clientId, StandardCharsets.UTF_8));
                if (callbackUrl != null && !callbackUrl.isBlank()) {
                    url.append("&redirect_uri=").append(URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8));
                }
                if (scope != null && !scope.isBlank()) {
                    url.append("&scope=").append(URLEncoder.encode(scope, StandardCharsets.UTF_8));
                }
                if (state != null && !state.isBlank()) {
                    url.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
                }

                // Parse callback port
                int port = 8080;
                try {
                    URI cbUri = new URI(callbackUrl);
                    if (cbUri.getPort() > 0)
                        port = cbUri.getPort();
                } catch (Exception e) {
                }

                startAuthServer(port, callbackUrl, accessTokenUrl, clientId, clientSecret, clientAuth, model, panel);
                Desktop.getDesktop().browse(new URI(url.toString()));

            } else {
                JOptionPane.showMessageDialog(panel,
                        "Grant type '" + grantType + "' is not fully automated. Please generate manually.", "Info",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(panel, "Error preparing OAuth2 request: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void startAuthServer(int port, String callbackUrl, String accessTokenUrl, String clientId,
            String clientSecret, String clientAuth, Object model, JPanel panel) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                String code = null;
                if (query != null) {
                    for (String param : query.split("&")) {
                        String[] pair = param.split("=");
                        if (pair.length > 1 && "code".equals(pair[0])) {
                            code = pair[1];
                            break;
                        }
                    }
                }

                String responseMsg = "<html><body><h2>ApiBanker Authorization</h2>";
                if (code != null) {
                    responseMsg += "<p>Authorization code received. You can close this tab and return to ApiBanker.</p></body></html>";
                    exchange.sendResponseHeaders(200, responseMsg.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(responseMsg.getBytes());
                    os.close();

                    // Exchange code for token
                    exchangeCodeForToken(code, callbackUrl, accessTokenUrl, clientId, clientSecret, clientAuth, model,
                            panel);
                } else {
                    responseMsg += "<p style='color:red'>Failed to get authorization code.</p></body></html>";
                    exchange.sendResponseHeaders(400, responseMsg.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(responseMsg.getBytes());
                    os.close();
                }

                // Stop server after getting code
                Executors.newSingleThreadExecutor().execute(() -> server.stop(1));
            });
            server.setExecutor(null);
            server.start();
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(panel,
                        "Failed to start local callback server on port " + port + ": " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    private static void exchangeCodeForToken(String code, String callbackUrl, String accessTokenUrl, String clientId,
            String clientSecret, String clientAuth, Object model, JPanel panel) {
        try {
            StringBuilder body = new StringBuilder();
            body.append("grant_type=authorization_code");
            body.append("&code=").append(URLEncoder.encode(code, StandardCharsets.UTF_8));
            if (callbackUrl != null && !callbackUrl.isBlank()) {
                body.append("&redirect_uri=").append(URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8));
            }

            if ("body".equals(clientAuth)) {
                body.append("&client_id=")
                        .append(URLEncoder.encode(clientId == null ? "" : clientId, StandardCharsets.UTF_8));
                body.append("&client_secret=")
                        .append(URLEncoder.encode(clientSecret == null ? "" : clientSecret, StandardCharsets.UTF_8));
            }

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(accessTokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()));

            if ("header".equals(clientAuth)) {
                String auth = (clientId == null ? "" : clientId) + ":" + (clientSecret == null ? "" : clientSecret);
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                reqBuilder.header("Authorization", "Basic " + encodedAuth);
            }

            HttpClient client = HttpClient.newBuilder().build();
            client.sendAsync(reqBuilder.build(), HttpResponse.BodyHandlers.ofString())
                    .thenAccept(res -> {
                        handleTokenResponse(res.body(), model, panel);
                    })
                    .exceptionally(ex -> {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(panel,
                                    "Failed to exchange code for token: " + ex.getMessage(), "OAuth2 Error",
                                    JOptionPane.ERROR_MESSAGE);
                        });
                        return null;
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleTokenResponse(String responseBody, Object model, JPanel panel) {
        // Simple regex to find "access_token":"..."
        String token = null;
        Matcher m = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"").matcher(responseBody);
        if (m.find()) {
            token = m.group(1);
        }

        if (token != null) {
            final String finalToken = token;
            SwingUtilities.invokeLater(() -> {
                if (model instanceof RequestModel req) {
                    req.setOauth2AccessToken(finalToken);
                    if (panel instanceof RequestPanel p) {
                        p.oauth2AccessTokenField.setText(finalToken);
                    }
                } else if (model instanceof CollectionModel col) {
                    col.setOauth2AccessToken(finalToken);
                    if (panel instanceof CollectionPanel p) {
                        p.oauth2AccessTokenField.setText(finalToken);
                    }
                }
                JOptionPane.showMessageDialog(panel, "Access Token successfully generated and saved!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            });
        } else {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(panel, "Could not extract access_token from response:\n" + responseBody,
                        "OAuth2 Error", JOptionPane.WARNING_MESSAGE);
            });
        }
    }
}

