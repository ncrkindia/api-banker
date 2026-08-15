package in.slpro.apibanker.logger;

import in.slpro.apibanker.storage.StorageManager;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;

/**
 * ActionAuditLogger
 *
 * <p>
 * Core singleton logger responsible for tracking and recording user and system actions
 * across the application. Implements an auto-rotating daily file strategy with a
 * strict 10MB size limit per file, saving output into the workspace's designated log directory.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.6.0-beta
 * @since 1.4.0
 */
public class ActionAuditLogger {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final ActionAuditLogger INSTANCE = new ActionAuditLogger();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private ActionAuditLogger() {}

    /**
     * Retrieves the singleton instance of the ActionAuditLogger.
     *
     * @return The active ActionAuditLogger instance.
     */
    public static ActionAuditLogger getInstance() {
        return INSTANCE;
    }

    /**
     * Logs a standardized application action to the current daily audit file.
     * The action is conditionally logged based on user configuration.
     *
     * @param actionName  The categorized name of the action (e.g., "IMPORT_COLLECTION").
     * @param requestedBy The initiator of the action (e.g., "User", "System").
     * @param details     Comprehensive metadata describing the action's outcome and context.
     */
    public synchronized void logAction(String actionName, String requestedBy, String details) {
        try {
            if (!StorageManager.getInstance().getSettings().isEnableActionAuditLog()) {
                return;
            }
            
            File logDir = new File(StorageManager.getInstance().getSettings().getLogsDirectory());
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            String currentDate = LocalDateTime.now().format(DATE_FORMAT);
            File logFile = getLogFile(logDir, currentDate);
            
            try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, StandardCharsets.UTF_8, true))) {
                String timestamp = LocalDateTime.now().format(DATETIME_FORMAT);
                pw.printf("[%s] [Action: %s] [Requested By: %s] - %s%n", timestamp, actionName, requestedBy, details);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Resolves the target audit log file for writing, dynamically appending a rolling index
     * if the primary daily file exceeds the 10MB capacity constraint.
     *
     * @param logDir  The base directory for storing log files.
     * @param dateStr The formatted current date string to serve as the file prefix.
     * @return The optimal File descriptor ready for appending log data.
     */
    private File getLogFile(File logDir, String dateStr) {
        int index = 0;
        File file;
        while (true) {
            String suffix = index == 0 ? "" : "_" + index;
            file = new File(logDir, "action_audit_" + dateStr + suffix + ".log");
            if (!file.exists() || file.length() < MAX_FILE_SIZE) {
                return file;
            }
            index++;
        }
    }
}
