package in.slpro.japi.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ConsoleLogger
 *
 * <p>
 * Core functionality and implementation logic for ConsoleLogger.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.1.0-beta
 * @since 1.0.0
 */
public class ConsoleLogger {
    private static ConsoleLogger instance;
    private final List<LogEntry> entries = new CopyOnWriteArrayList<>();
    private final List<LogListener> listeners = new CopyOnWriteArrayList<>();
    private File logsDir;
    private boolean enableLogging = true;

    public interface LogListener {
        void onLogEntry(LogEntry entry);
    }

    private ConsoleLogger() {}

    public static synchronized ConsoleLogger getInstance() {
        if (instance == null) {
            instance = new ConsoleLogger();
        }
        return instance;
    }

    public void setLogsDirectory(File dir) {
        this.logsDir = dir;
        if (!dir.exists()) dir.mkdirs();
    }

    public boolean isEnableLogging() {
        return enableLogging;
    }

    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }

    public void addListener(LogListener listener) {
        listeners.add(listener);
    }

    public void removeListener(LogListener listener) {
        listeners.remove(listener);
    }

    public void logRequest(String method, String url, int statusCode, long durationMs,
                           java.util.Map<String, List<String>> requestHeaders, String requestBody,
                           java.util.Map<String, List<String>> responseHeaders, String responseBody) {
        StringBuilder sb = new StringBuilder();
        sb.append(">>> ").append(method).append(" ").append(url).append("\n");
        if (requestHeaders != null) {
            requestHeaders.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(String.join(", ", v)).append("\n"));
        }
        if (requestBody != null && !requestBody.isEmpty()) {
            sb.append("Body: ").append(requestBody.length() > 500 ? requestBody.substring(0, 500) + "..." : requestBody).append("\n");
        }
        sb.append("\n<<< ").append(statusCode).append(" (").append(durationMs).append("ms)\n");
        if (responseHeaders != null) {
            responseHeaders.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(String.join(", ", v)).append("\n"));
        }
        if (responseBody != null && !responseBody.isEmpty()) {
            sb.append("Body: ").append(responseBody.length() > 1000 ? responseBody.substring(0, 1000) + "..." : responseBody);
        }

        LogEntry.Level level = (statusCode >= 400 || statusCode == 0) ? LogEntry.Level.ERROR : LogEntry.Level.REQUEST;
        long responseSize = responseBody != null ? responseBody.getBytes(java.nio.charset.StandardCharsets.UTF_8).length : 0;
        LogEntry entry = new LogEntry(level, sb.toString(), statusCode, durationMs, method, url, responseSize, "Request",
                requestHeaders, requestBody, responseHeaders, responseBody);
        entries.add(entry);
        listeners.forEach(l -> l.onLogEntry(entry));

        if (logsDir != null && enableLogging) {
            try {
                String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                File logFile = new File(logsDir, "japi_" + dateStr + ".log");
                try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, java.nio.charset.StandardCharsets.UTF_8, true))) {
                    pw.println("[" + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] " + entry.getMessage());
                    pw.println("---");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void logMessage(LogEntry.Level level, String message) {
        logMessage(level, message, "System");
    }

    public void logMessage(LogEntry.Level level, String message, String source) {
        LogEntry entry = new LogEntry(level, message, 0, 0, null, null, 0, source);
        entries.add(entry);
        listeners.forEach(l -> l.onLogEntry(entry));

        if (logsDir != null && enableLogging) {
            try {
                String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                File logFile = new File(logsDir, "japi_" + dateStr + ".log");
                try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, java.nio.charset.StandardCharsets.UTF_8, true))) {
                    pw.println("[" + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] [" + level + "] [" + source + "] " + entry.getMessage());
                    pw.println("---");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public List<LogEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    public void clear() {
        entries.clear();
    }
}
