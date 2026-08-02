package in.slpro.japi.model;

import java.util.ArrayList;
import java.util.List;

/**
 * RequestModel
 *
 * <p>
 * Represents a single HTTP Request entity within the JAPI workspace.
 * This model encapsulates all configurable parameters of a request, including
 * URL, HTTP method, headers, query parameters, complex body payloads (raw, form-data),
 * authentication mechanisms, and associated pre/post-request scripts.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.1.0-beta
 * @since 1.0.0
 */
public class RequestModel {
    private String id;
    private String name;
    private String method = "GET";
    private String url = "";
    private List<KeyValueItem> headers = new ArrayList<>();
    private List<KeyValueItem> params = new ArrayList<>();
    private String bodyType = "none"; // none, raw, form, urlencoded
    private String bodyRawType = "JSON"; // JSON, Text, HTML, XML, JavaScript
    private String bodyRawContent = "";
    private List<KeyValueItem> formData = new ArrayList<>();
    private String authType = "none"; // none, bearer, basic, apiKey
    private String authToken = "";
    private String authUsername = "";
    private String authPassword = "";
    private String authApiKeyName = "";
    private String authApiKeyValue = "";
    private String authApiKeyIn = "header"; // header or query
    private String preRequestScript = "";
    private String postRequestScript = "";
    private List<KeyValueItem> urlencodedData = new ArrayList<>();
    private String type = "request"; // request, runner
    private boolean sslVerification = true;
    private String sslSetting = "INHERIT"; // INHERIT, VERIFY, NO_VERIFY
    private String redirectSetting = "INHERIT"; // INHERIT, YES, NO
    // History metadata
    private Long timestamp;
    private Integer responseStatus;
    private String actualUrl;
    // Comparator metadata
    private String comparatorTextA = "";
    private String comparatorTextB = "";
    private int comparatorMode = 0;

    /**
     * Constructs a new, blank RequestModel.
     * <p>
     * Automatically assigns a new random UUID, sets the default name to "New Request",
     * and injects standard HTTP headers (User-Agent, Accept, Accept-Encoding, Connection)
     * as active default key-value pairs.
     * </p>
     */
    public RequestModel() {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = "New Request";
        this.headers.add(new KeyValueItem("User-Agent", "JAPI API Client", true));
        this.headers.add(new KeyValueItem("Accept", "*/*", true));
        this.headers.add(new KeyValueItem("Accept-Encoding", "gzip, deflate, br", true));
        this.headers.add(new KeyValueItem("Connection", "keep-alive", true));
    }

    /** @return Form URL-Encoded data parameters. */
    public List<KeyValueItem> getUrlencodedData() { return urlencodedData; }
    public void setUrlencodedData(List<KeyValueItem> urlencodedData) { this.urlencodedData = urlencodedData; }

    /** @return The unique UUID of the request. */
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    /** @return The display name of the request. */
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    /** @return The HTTP Method (e.g. GET, POST, PUT, DELETE). */
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    
    /** @return The target URL (which may contain {{variables}}). */
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    /** @return The list of HTTP headers to send. */
    public List<KeyValueItem> getHeaders() { return headers; }
    public void setHeaders(List<KeyValueItem> headers) { this.headers = headers; }
    
    /** @return The list of Query string parameters appended to the URL. */
    public List<KeyValueItem> getParams() { return params; }
    public void setParams(List<KeyValueItem> params) { this.params = params; }
    /** @return The type of body payload (none, raw, form, urlencoded, graphql). */
    public String getBodyType() { return bodyType; }
    public void setBodyType(String bodyType) { this.bodyType = bodyType; }
    
    /** @return The explicit sub-type of a 'raw' body (JSON, XML, HTML, Text). */
    public String getBodyRawType() { return bodyRawType; }
    public void setBodyRawType(String bodyRawType) { this.bodyRawType = bodyRawType; }
    
    /** @return The raw string content of the payload. */
    public String getBodyRawContent() { return bodyRawContent; }
    public void setBodyRawContent(String bodyRawContent) { this.bodyRawContent = bodyRawContent; }
    
    /** @return The multi-part form data parameters. */
    public List<KeyValueItem> getFormData() { return formData; }
    public void setFormData(List<KeyValueItem> formData) { this.formData = formData; }
    
    /** @return The explicit authorization scheme for this request. Defaults to 'inherit'. */
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    
    public String getAuthToken() { return authToken; }
    public void setAuthToken(String authToken) { this.authToken = authToken; }
    
    public String getAuthUsername() { return authUsername; }
    public void setAuthUsername(String authUsername) { this.authUsername = authUsername; }
    
    public String getAuthPassword() { return authPassword; }
    public void setAuthPassword(String authPassword) { this.authPassword = authPassword; }
    
    public String getAuthApiKeyName() { return authApiKeyName; }
    public void setAuthApiKeyName(String authApiKeyName) { this.authApiKeyName = authApiKeyName; }
    
    public String getAuthApiKeyValue() { return authApiKeyValue; }
    public void setAuthApiKeyValue(String authApiKeyValue) { this.authApiKeyValue = authApiKeyValue; }
    
    public String getAuthApiKeyIn() { return authApiKeyIn; }
    public void setAuthApiKeyIn(String authApiKeyIn) { this.authApiKeyIn = authApiKeyIn; }
    
    /** @return The JavaScript code executed before the network request is triggered. */
    public String getPreRequestScript() { return preRequestScript; }
    public void setPreRequestScript(String preRequestScript) { this.preRequestScript = preRequestScript; }
    
    /** @return The JavaScript code executed after the response is received (Test scripts). */
    public String getPostRequestScript() { return postRequestScript; }
    public void setPostRequestScript(String postRequestScript) { this.postRequestScript = postRequestScript; }
    /** @return The node type. Regular requests are 'request', special requests are 'runner' (or similar). */
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    /** @return The timestamp of the last execution (if saved in history). */
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    
    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }
    
    public String getActualUrl() { return actualUrl; }
    public void setActualUrl(String actualUrl) { this.actualUrl = actualUrl; }

    public String getComparatorTextA() { return comparatorTextA; }
    public void setComparatorTextA(String comparatorTextA) { this.comparatorTextA = comparatorTextA; }
    
    public String getComparatorTextB() { return comparatorTextB; }
    public void setComparatorTextB(String comparatorTextB) { this.comparatorTextB = comparatorTextB; }
    
    public int getComparatorMode() { return comparatorMode; }
    public void setComparatorMode(int comparatorMode) { this.comparatorMode = comparatorMode; }
    
    /**
     * @deprecated Used by older versions. Replaced by `sslSetting`.
     */
    @Deprecated
    public boolean isSslVerification() { return sslVerification; }
    @Deprecated
    public void setSslVerification(boolean sslVerification) { this.sslVerification = sslVerification; }
    
    /** @return The SSL strategy: INHERIT, VERIFY, NO_VERIFY. Handles legacy boolean migration. */
    public String getSslSetting() {
        if (sslSetting == null || sslSetting.isBlank()) {
            if (!sslVerification) {
                sslSetting = "NO_VERIFY";
            } else {
                sslSetting = "INHERIT";
            }
        }
        return sslSetting;
    }
    public void setSslSetting(String sslSetting) { this.sslSetting = sslSetting; }
    
    /** @return The Redirect strategy: INHERIT, YES, NO. */
    public String getRedirectSetting() {
        if (redirectSetting == null || redirectSetting.isBlank()) {
            redirectSetting = "INHERIT";
        }
        return redirectSetting;
    }
    public void setRedirectSetting(String redirectSetting) { this.redirectSetting = redirectSetting; }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
