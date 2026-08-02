package in.slpro.japi.model;

import java.util.List;
import java.util.Map;

public class ResponseModel {
    private int statusCode;
    private String statusText;
    private long executionTimeMs;
    private long sizeBytes;
    private String body;
    private Map<String, List<String>> headers;
    private String actualUrl;

    public ResponseModel(int statusCode, String statusText, long executionTimeMs,
                         long sizeBytes, String body, Map<String, List<String>> headers) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.executionTimeMs = executionTimeMs;
        this.sizeBytes = sizeBytes;
        this.body = body;
        this.headers = headers;
    }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Map<String, List<String>> getHeaders() { return headers; }
    public void setHeaders(Map<String, List<String>> headers) { this.headers = headers; }
    public String getActualUrl() { return actualUrl; }
    public void setActualUrl(String actualUrl) { this.actualUrl = actualUrl; }
    
    private List<ResponseModel> redirects = new java.util.ArrayList<>();
    public List<ResponseModel> getRedirects() { return redirects; }
    public void setRedirects(List<ResponseModel> redirects) { this.redirects = redirects; }
}
