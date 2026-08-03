package in.slpro.japi.model;

import java.util.List;
import java.util.Map;

/**
 * ResponseModel
 *
 * <p>
 * Core functionality and implementation logic for ResponseModel.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.1.0-beta
 * @since 1.0.0
 */
public class ResponseModel {
    private int statusCode;
    private String statusText;
    private long executionTimeMs;
    private long sizeBytes;
    private String body;
    private Map<String, List<String>> headers;
    private String actualUrl;
    private String sslDetails;
    private boolean sslValid;
    
    // Timing breakdown
    private long preRequestTimeMs;
    private long networkTimeMs;
    private long testScriptTimeMs;

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
    
    public String getSslDetails() { return sslDetails; }
    public void setSslDetails(String sslDetails) { this.sslDetails = sslDetails; }
    public boolean isSslValid() { return sslValid; }
    public void setSslValid(boolean sslValid) { this.sslValid = sslValid; }
    
    public long getPreRequestTimeMs() { return preRequestTimeMs; }
    public void setPreRequestTimeMs(long preRequestTimeMs) { this.preRequestTimeMs = preRequestTimeMs; }
    public long getNetworkTimeMs() { return networkTimeMs; }
    public void setNetworkTimeMs(long networkTimeMs) { this.networkTimeMs = networkTimeMs; }
    public long getTestScriptTimeMs() { return testScriptTimeMs; }
    public void setTestScriptTimeMs(long testScriptTimeMs) { this.testScriptTimeMs = testScriptTimeMs; }
}
