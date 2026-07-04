package in.slpro.japi.logger;

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

    public LogEntry(Level level, String message, int statusCode, long durationMs, String method, String url, long responseSize) {
        this.level = level;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.statusCode = statusCode;
        this.durationMs = durationMs;
        this.method = method;
        this.url = url;
        this.responseSize = responseSize;
    }

    public Level getLevel() { return level; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public int getStatusCode() { return statusCode; }
    public long getDurationMs() { return durationMs; }
    public String getMethod() { return method; }
    public String getUrl() { return url; }
    public long getResponseSize() { return responseSize; }
}
