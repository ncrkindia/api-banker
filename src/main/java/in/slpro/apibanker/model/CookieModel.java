package in.slpro.apibanker.model;

/**
 * CookieModel
 *
 * <p>
 * Core functionality and implementation logic for CookieModel.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.0.0-beta
 * @since 1.0.0
 */
public class CookieModel {
    private String name;
    private String value;
    private String domain;
    private String path = "/";
    private Long expiry; // timestamp in ms
    private boolean secure;
    private boolean httpOnly;

    public CookieModel() {
    }

    public CookieModel(String name, String value, String domain, String path) {
        this.name = name;
        this.value = value;
        this.domain = domain;
        this.path = path != null ? path : "/";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getExpiry() {
        return expiry;
    }

    public void setExpiry(Long expiry) {
        this.expiry = expiry;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    public boolean matches(String requestHost, String requestPath) {
        if (domain == null || requestHost == null)
            return false;

        String cookieDomain = domain.toLowerCase();
        String host = requestHost.toLowerCase();

        if (cookieDomain.startsWith(".")) {
            cookieDomain = cookieDomain.substring(1);
        }

        if (!host.endsWith(cookieDomain)) {
            return false;
        }

        if (host.length() > cookieDomain.length()) {
            char prefixChar = host.charAt(host.length() - cookieDomain.length() - 1);
            if (prefixChar != '.') {
                return false;
            }
        }

        String cookiePath = path != null ? path : "/";
        String rPath = requestPath != null ? requestPath : "/";
        if (!rPath.startsWith(cookiePath)) {
            return false;
        }
        if (cookiePath.length() > 1 && !cookiePath.endsWith("/") && rPath.length() > cookiePath.length()
                && rPath.charAt(cookiePath.length()) != '/') {
            return false;
        }

        if (expiry != null && System.currentTimeMillis() > expiry) {
            return false;
        }

        return true;
    }
}

