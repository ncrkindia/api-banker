package in.slpro.japi.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import in.slpro.japi.model.CookieModel;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * CookieJar
 *
 * <p>
 * This class acts as the centralized persistent Cookie store for the JAPI HTTP Client.
 * It intercepts incoming HTTP `Set-Cookie` headers from responses, parses their attributes 
 * (Domain, Path, Max-Age, Secure), and automatically injects them into subsequent outgoing 
 * requests that match the domain and path requirements. It persists cookies locally to `~/.japi/cookies.json`.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.1.0-beta
 * @since 1.0.0
 */
public class CookieJar {
    private static CookieJar instance;
    private final List<CookieModel> cookies = new ArrayList<>();
    private final File cookiesFile;
    private final Gson gson;

    private CookieJar() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        String userHome = System.getProperty("user.home");
        File bootstrapDir = new File(userHome, ".japi");
        if (!bootstrapDir.exists()) {
            bootstrapDir.mkdirs();
        }
        this.cookiesFile = new File(bootstrapDir, "cookies.json");
        loadCookies();
    }

    /**
     * @return The thread-safe singleton instance of the CookieJar.
     */
    public static synchronized CookieJar getInstance() {
        if (instance == null) {
            instance = new CookieJar();
        }
        return instance;
    }

    public synchronized List<CookieModel> getCookies() {
        return new ArrayList<>(cookies);
    }

    public synchronized void setCookies(List<CookieModel> newCookies) {
        cookies.clear();
        cookies.addAll(newCookies);
        saveCookies();
    }

    public synchronized void addCookie(CookieModel cookie) {
        cookies.removeIf(c -> c.getName().equalsIgnoreCase(cookie.getName()) 
                && c.getDomain().equalsIgnoreCase(cookie.getDomain()) 
                && c.getPath().equalsIgnoreCase(cookie.getPath()));
        cookies.add(cookie);
        saveCookies();
    }

    public synchronized void removeCookie(CookieModel cookie) {
        cookies.removeIf(c -> c.getName().equalsIgnoreCase(cookie.getName()) 
                && c.getDomain().equalsIgnoreCase(cookie.getDomain()) 
                && c.getPath().equalsIgnoreCase(cookie.getPath()));
        saveCookies();
    }

    public synchronized void clear() {
        cookies.clear();
        saveCookies();
    }

    private void loadCookies() {
        if (cookiesFile.exists()) {
            try (Reader reader = new FileReader(cookiesFile, StandardCharsets.UTF_8)) {
                List<CookieModel> loaded = gson.fromJson(reader, new TypeToken<List<CookieModel>>(){}.getType());
                if (loaded != null) {
                    cookies.addAll(loaded);
                }
            } catch (Exception e) {
                System.err.println("Failed to load cookies: " + e.getMessage());
            }
        }
    }

    private void saveCookies() {
        try (Writer writer = new FileWriter(cookiesFile, StandardCharsets.UTF_8)) {
            gson.toJson(cookies, writer);
        } catch (Exception e) {
            System.err.println("Failed to save cookies: " + e.getMessage());
        }
    }

    /**
     * Evaluates all stored cookies against the target URL and constructs a standard
     * HTTP `Cookie` header string containing all valid, non-expired cookies matching 
     * the domain and path.
     * 
     * @param urlString The target URL being requested.
     * @return A formatted `Cookie` header string (e.g., "session=123; user=abc"), or null if no matches.
     */
    public synchronized String getCookieHeaderForUrl(String urlString) {
        try {
            URI uri = new URI(urlString);
            String host = uri.getHost();
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                path = "/";
            }

            if (host == null) return null;

            StringBuilder sb = new StringBuilder();
            for (CookieModel cookie : cookies) {
                if (cookie.matches(host, path)) {
                    if (sb.length() > 0) {
                        sb.append("; ");
                    }
                    sb.append(cookie.getName()).append("=").append(cookie.getValue());
                }
            }
            return sb.length() > 0 ? sb.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses incoming `Set-Cookie` headers from an HTTP response and stores them
     * in the persistent jar, respecting domain and path scoping rules.
     * 
     * @param urlString The URL of the server that returned the cookies.
     * @param setCookieHeaders The list of raw `Set-Cookie` header strings.
     */
    public synchronized void parseAndStoreCookies(String urlString, List<String> setCookieHeaders) {
        if (setCookieHeaders == null || setCookieHeaders.isEmpty()) return;

        try {
            URI uri = new URI(urlString);
            String defaultHost = uri.getHost();
            String defaultPath = uri.getPath();
            if (defaultPath == null || defaultPath.isBlank()) {
                defaultPath = "/";
            }

            for (String header : setCookieHeaders) {
                try {
                    CookieModel cookie = parseSetCookie(header, defaultHost, defaultPath);
                    if (cookie != null) {
                        addCookie(cookie);
                    }
                } catch (Exception ex) {
                    System.err.println("Error parsing cookie: " + header + " - " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Invalid URL for cookie parsing: " + urlString);
        }
    }

    private CookieModel parseSetCookie(String header, String defaultHost, String defaultPath) {
        String[] parts = header.split(";");
        if (parts.length == 0) return null;

        String nameValPart = parts[0].trim();
        int eqIdx = nameValPart.indexOf('=');
        if (eqIdx == -1) return null;

        String name = nameValPart.substring(0, eqIdx).trim();
        String value = nameValPart.substring(eqIdx + 1).trim();

        CookieModel cookie = new CookieModel(name, value, defaultHost, defaultPath);

        for (int i = 1; i < parts.length; i++) {
            String attributePart = parts[i].trim();
            int attrEqIdx = attributePart.indexOf('=');
            String attrName = attrEqIdx == -1 ? attributePart : attributePart.substring(0, attrEqIdx).trim();
            String attrVal = attrEqIdx == -1 ? "" : attributePart.substring(attrEqIdx + 1).trim();

            if (attrName.equalsIgnoreCase("Domain")) {
                if (!attrVal.isEmpty()) {
                    cookie.setDomain(attrVal);
                }
            } else if (attrName.equalsIgnoreCase("Path")) {
                if (!attrVal.isEmpty()) {
                    cookie.setPath(attrVal);
                }
            } else if (attrName.equalsIgnoreCase("Max-Age")) {
                try {
                    long maxAgeSec = Long.parseLong(attrVal);
                    cookie.setExpiry(System.currentTimeMillis() + (maxAgeSec * 1000));
                } catch (NumberFormatException ignored) {}
            } else if (attrName.equalsIgnoreCase("Expires")) {
                try {
                    // Try parsing HTTP date formats
                    SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
                    cookie.setExpiry(df.parse(attrVal).getTime());
                } catch (Exception ignored) {
                    try {
                        SimpleDateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz", Locale.US);
                        cookie.setExpiry(df.parse(attrVal).getTime());
                    } catch (Exception secondIgnored) {}
                }
            } else if (attrName.equalsIgnoreCase("Secure")) {
                cookie.setSecure(true);
            } else if (attrName.equalsIgnoreCase("HttpOnly")) {
                cookie.setHttpOnly(true);
            }
        }
        return cookie;
    }
}
