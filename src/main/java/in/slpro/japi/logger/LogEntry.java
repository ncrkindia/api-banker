package in.slpro.japi.logger;

import java.util.List;
import java.util.Map;

/**
 * LogEntry
 *
 * <p>
 * Core functionality and implementation logic for LogEntry.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.1.0-beta
 * @since 1.0.0
 */
public class LogEntry {
    public enum Level { REQUEST, RESPONSE, ERROR, INFO }

    private final Level level;
    private final String message;
    private final long timestamp;
    private final int statusCode;
    private final long durationMs;
    private final String method;
    private final String url;
    private final long responseSize;
    private final String source; // e.g., "Request", "Pre-request", "Test", "System"

    // Raw HTTP details for Request/Response
    private final Map<String, List<String>> requestHeaders;
    private final String requestBody;
    private final Map<String, List<String>> responseHeaders;
    private final String responseBody;

    public LogEntry(Level level, String message, int statusCode, long durationMs, String method, String url, long responseSize, String source,
                    Map<String, List<String>> requestHeaders, String requestBody,
                    Map<String, List<String>> responseHeaders, String responseBody) {
        this.level = level;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.statusCode = statusCode;
        this.durationMs = durationMs;
        this.method = method;
        this.url = url;
        this.responseSize = responseSize;
        this.source = source != null ? source : "System";
        this.requestHeaders = requestHeaders;
        this.requestBody = requestBody;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
    }

    public LogEntry(Level level, String message, int statusCode, long durationMs, String method, String url, long responseSize, String source) {
        this(level, message, statusCode, durationMs, method, url, responseSize, source, null, null, null, null);
    }

    public LogEntry(Level level, String message, int statusCode, long durationMs, String method, String url, long responseSize) {
        this(level, message, statusCode, durationMs, method, url, responseSize, (url != null) ? "Request" : "System");
    }

    public Level getLevel() { return level; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public int getStatusCode() { return statusCode; }
    public long getDurationMs() { return durationMs; }
    public String getMethod() { return method; }
    public String getUrl() { return url; }
    public long getResponseSize() { return responseSize; }
    public String getSource() { return source; }

    public Map<String, List<String>> getRequestHeaders() { return requestHeaders; }
    public String getRequestBody() { return requestBody; }
    public Map<String, List<String>> getResponseHeaders() { return responseHeaders; }
    public String getResponseBody() { return responseBody; }
}
