package in.slpro.japi.model;

import java.util.ArrayList;
import java.util.List;

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
    private String type = "request"; // request, runner
    // History metadata
    private Long timestamp;
    private Integer responseStatus;
    private String actualUrl;
    // Comparator metadata
    private String comparatorTextA = "";
    private String comparatorTextB = "";
    private int comparatorMode = 0;

    public RequestModel() {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = "New Request";
        this.headers.add(new KeyValueItem("User-Agent", "JAPI API Client", true));
        this.headers.add(new KeyValueItem("Accept", "*/*", true));
        this.headers.add(new KeyValueItem("Accept-Encoding", "gzip, deflate, br", true));
        this.headers.add(new KeyValueItem("Connection", "keep-alive", true));
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public List<KeyValueItem> getHeaders() { return headers; }
    public void setHeaders(List<KeyValueItem> headers) { this.headers = headers; }
    public List<KeyValueItem> getParams() { return params; }
    public void setParams(List<KeyValueItem> params) { this.params = params; }
    public String getBodyType() { return bodyType; }
    public void setBodyType(String bodyType) { this.bodyType = bodyType; }
    public String getBodyRawType() { return bodyRawType; }
    public void setBodyRawType(String bodyRawType) { this.bodyRawType = bodyRawType; }
    public String getBodyRawContent() { return bodyRawContent; }
    public void setBodyRawContent(String bodyRawContent) { this.bodyRawContent = bodyRawContent; }
    public List<KeyValueItem> getFormData() { return formData; }
    public void setFormData(List<KeyValueItem> formData) { this.formData = formData; }
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
    public String getPreRequestScript() { return preRequestScript; }
    public void setPreRequestScript(String preRequestScript) { this.preRequestScript = preRequestScript; }
    public String getPostRequestScript() { return postRequestScript; }
    public void setPostRequestScript(String postRequestScript) { this.postRequestScript = postRequestScript; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
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

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
