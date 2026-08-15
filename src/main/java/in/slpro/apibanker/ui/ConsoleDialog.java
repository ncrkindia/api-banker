package in.slpro.apibanker.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import in.slpro.apibanker.logger.ConsoleLogger;
import in.slpro.apibanker.logger.LogEntry;
import in.slpro.apibanker.model.AppSettings;
import in.slpro.apibanker.storage.StorageManager;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * ConsoleDialog
 *
 * <p>
 * Core functionality and implementation logic for ConsoleDialog.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.6.0-beta
 * @since 1.0.0
 */
public class ConsoleDialog extends JFrame implements ConsoleLogger.LogListener {
    private static ConsoleDialog instance = null;

    public static synchronized void showConsole(JFrame owner) {
        if (instance != null && instance.isDisplayable()) {
            if (instance.getState() == Frame.ICONIFIED) {
                instance.setState(Frame.NORMAL);
            }
            instance.toFront();
            instance.requestFocus();
        } else {
            instance = new ConsoleDialog(owner);
            instance.setVisible(true);
        }
    }

    private final JPanel logPanel;
    private final JScrollPane scrollPane;
    private final JTextField searchField;
    private final JComboBox<String> levelCombo;
    private final JCheckBox autoScrollCheck;

    // --- Dynamic Styles & Theme Colors ---
    private final boolean isDark;
    private final int fontSize;
    private final Font uiFont;
    private final Font uiFontBold;
    private final Font monoFont;

    private final Color colorBackground;
    private final Color colorText;
    private final Color colorToolbarBg;
    private final Color colorToolbarBorder;
    private final Color colorInputBg;
    private final Color colorInputBorder;
    private final Color colorInputFg;
    private final Color colorScriptBg;
    private final Color colorScriptBgError;
    private final Color colorScriptBorder;
    private final Color colorReqHeaderBg;
    private final Color colorReqHeaderBorder;
    private final Color colorReqDetailBg;
    private final Color colorErrorText;

    public ConsoleDialog(JFrame owner) {
        super("ApiBanker Console");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        if (owner != null) {
            setIconImage(owner.getIconImage());
        }

        // --- Theme & Font Settings ---
        this.isDark = com.formdev.flatlaf.FlatLaf.isLafDark();
        AppSettings settings = StorageManager.getInstance().getSettings();
        int savedFontSize = settings.getFontSize();
        this.fontSize = (savedFontSize >= 10 && savedFontSize <= 30) ? savedFontSize : 14;

        this.uiFont = new Font("Segoe UI", Font.PLAIN, fontSize - 2);
        this.uiFontBold = new Font("Segoe UI", Font.BOLD, fontSize - 2);
        this.monoFont = new Font("JetBrains Mono", Font.PLAIN, fontSize - 2);

        // Define color palette based on active theme
        if (isDark) {
            colorBackground = new Color(18, 18, 24);
            colorText = new Color(210, 215, 225);
            colorToolbarBg = new Color(30, 31, 37);
            colorToolbarBorder = new Color(48, 49, 54);
            colorInputBg = new Color(20, 20, 24);
            colorInputBorder = new Color(60, 61, 67);
            colorInputFg = Color.WHITE;
            colorScriptBg = new Color(22, 23, 28);
            colorScriptBgError = new Color(38, 20, 22);
            colorScriptBorder = new Color(35, 36, 42);
            colorReqHeaderBg = new Color(28, 29, 36);
            colorReqHeaderBorder = new Color(42, 43, 50);
            colorReqDetailBg = new Color(15, 16, 20);
            colorErrorText = new Color(255, 100, 100);
        } else {
            colorBackground = new Color(245, 246, 248);
            colorText = new Color(40, 40, 40);
            colorToolbarBg = new Color(230, 232, 238);
            colorToolbarBorder = new Color(210, 212, 218);
            colorInputBg = Color.WHITE;
            colorInputBorder = new Color(180, 182, 188);
            colorInputFg = Color.BLACK;
            colorScriptBg = Color.WHITE;
            colorScriptBgError = new Color(255, 235, 235);
            colorScriptBorder = new Color(220, 222, 228);
            colorReqHeaderBg = new Color(238, 240, 245);
            colorReqHeaderBorder = new Color(220, 222, 228);
            colorReqDetailBg = new Color(250, 251, 252);
            colorErrorText = new Color(180, 20, 20);
        }

        // Restore window size and position from settings
        int savedW = settings.getConsoleWidth();
        int savedH = settings.getConsoleHeight();
        int savedX = settings.getConsoleX();
        int savedY = settings.getConsoleY();

        if (savedW > 100 && savedH > 100) {
            setSize(savedW, savedH);
        } else {
            setSize(950, 600);
        }

        if (savedX >= 0 && savedY >= 0) {
            setLocation(savedX, savedY);
        } else {
            setLocationRelativeTo(owner);
        }

        // Save position/size on dialog changes
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveLayout();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                synchronized (ConsoleDialog.class) {
                    if (instance == ConsoleDialog.this) {
                        instance = null;
                    }
                }
            }
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                saveLayout();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                saveLayout();
            }
        });

        // --- TOP TOOLBAR PANEL ---
        JPanel toolbarPanel = new JPanel(new GridBagLayout());
        toolbarPanel.setBackground(colorToolbarBg);
        toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, colorToolbarBorder),
                new EmptyBorder(8, 12, 8, 12)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 4, 0, 4);

        // Filter Label
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel searchLabel = new JLabel("Filter:");
        searchLabel.setForeground(colorText);
        searchLabel.setFont(uiFontBold);
        toolbarPanel.add(searchLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        searchField = new JTextField();
        searchField.setBackground(colorInputBg);
        searchField.setForeground(colorInputFg);
        searchField.setCaretColor(colorInputFg);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(colorInputBorder, 1),
                new EmptyBorder(4, 6, 4, 6)));
        searchField.setFont(uiFont);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterLogs();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterLogs();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterLogs();
            }
        });
        toolbarPanel.add(searchField, gbc);

        // Level Select Combo
        gbc.gridx = 2;
        gbc.weightx = 0;
        levelCombo = new JComboBox<>(new String[] { "All Levels", "Logs/Info", "Errors", "Network Requests" });
        levelCombo.setFont(uiFont);
        levelCombo.addActionListener(e -> filterLogs());
        toolbarPanel.add(levelCombo, gbc);

        // Auto Scroll Checkbox
        gbc.gridx = 3;
        gbc.weightx = 0;
        autoScrollCheck = new JCheckBox("Auto-scroll", true);
        autoScrollCheck.setFont(uiFont);
        autoScrollCheck.setForeground(colorText);
        autoScrollCheck.setOpaque(false);
        toolbarPanel.add(autoScrollCheck, gbc);

        // Download Logs Button
        gbc.gridx = 4;
        gbc.weightx = 0;
        JButton downloadBtn = new JButton("Download Logs");
        downloadBtn.setFont(uiFontBold);
        Color accent = UIManager.getColor("AccentColor");
        downloadBtn.setBackground(accent != null ? accent : new Color(52, 152, 219));
        downloadBtn.setForeground(Color.WHITE);
        downloadBtn.addActionListener(e -> downloadFilteredLogs());
        toolbarPanel.add(downloadBtn, gbc);

        // Clear Button
        gbc.gridx = 5;
        gbc.weightx = 0;
        JButton clearBtn = new JButton("Clear Console");
        clearBtn.setFont(uiFontBold);
        clearBtn.setBackground(new Color(192, 57, 43));
        clearBtn.setForeground(Color.WHITE);
        clearBtn.addActionListener(e -> {
            ConsoleLogger.getInstance().clear();
            filterLogs();
        });
        toolbarPanel.add(clearBtn, gbc);

        if (owner instanceof MainFrame mainFrame) {
            gbc.gridx = 6;
            gbc.weightx = 0;
            toolbarPanel.add(mainFrame.createInfoBadge("sec-console-logs", "View Console Guide"), gbc);
        }

        // --- CENTER CONSOLE AREA ---
        logPanel = new JPanel();
        logPanel.setLayout(new BoxLayout(logPanel, BoxLayout.Y_AXIS));
        logPanel.setBackground(colorBackground);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(colorBackground);
        wrapper.add(logPanel, BorderLayout.NORTH);

        scrollPane = new JScrollPane(wrapper);
        scrollPane.setBorder(null);
        scrollPane.setBackground(colorBackground);
        scrollPane.getViewport().setBackground(colorBackground);

        add(toolbarPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        ConsoleLogger.getInstance().addListener(this);
        filterLogs();
    }

    private void saveLayout() {
        AppSettings settings = StorageManager.getInstance().getSettings();
        settings.setConsoleWidth(getWidth());
        settings.setConsoleHeight(getHeight());
        settings.setConsoleX(getX());
        settings.setConsoleY(getY());
        StorageManager.getInstance().saveSettings();
    }

    private void filterLogs() {
        logPanel.removeAll();
        String search = searchField.getText().toLowerCase().trim();
        String filterLevel = (String) levelCombo.getSelectedItem();

        for (LogEntry entry : ConsoleLogger.getInstance().getEntries()) {
            // Apply level filter
            if ("Logs/Info".equals(filterLevel)) {
                if (entry.getUrl() != null || entry.getLevel() == LogEntry.Level.ERROR)
                    continue;
            } else if ("Errors".equals(filterLevel)) {
                if (entry.getLevel() != LogEntry.Level.ERROR)
                    continue;
            } else if ("Network Requests".equals(filterLevel)) {
                if (entry.getUrl() == null)
                    continue;
            }

            // Apply search filter
            if (!search.isEmpty()) {
                boolean matchMsg = entry.getMessage() != null && entry.getMessage().toLowerCase().contains(search);
                boolean matchUrl = entry.getUrl() != null && entry.getUrl().toLowerCase().contains(search);
                boolean matchMethod = entry.getMethod() != null && entry.getMethod().toLowerCase().contains(search);
                boolean matchSource = entry.getSource() != null && entry.getSource().toLowerCase().contains(search);
                if (!matchMsg && !matchUrl && matchMethod && !matchSource)
                    continue;
            }

            addEntryPanel(entry);
        }
        logPanel.revalidate();
        logPanel.repaint();

        if (autoScrollCheck.isSelected()) {
            scrollToTop();
        }
    }

    private void scrollToTop() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(0);
        });
    }

    private void addEntryPanel(LogEntry entry) {
        if (entry.getUrl() != null && !entry.getUrl().isEmpty()) {
            // Collapsible Network Request Panel
            logPanel.add(createCollapsibleRequestPanel(entry), 0);
        } else {
            // Simple Script / System console log panel
            logPanel.add(createScriptLogPanel(entry), 0);
        }
        logPanel.add(Box.createVerticalStrut(1), 1);
    }

    private JPanel createScriptLogPanel(LogEntry entry) {
        boolean isError = entry.getLevel() == LogEntry.Level.ERROR;
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setBackground(isError ? colorScriptBgError : colorScriptBg);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, colorScriptBorder),
                new EmptyBorder(8, 12, 8, 12)));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Metadata panel (Time + Source Badge + Log Type Badge)
        JPanel metaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        metaPanel.setOpaque(false);

        // Time Label
        String time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(entry.getTimestamp()));
        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(monoFont);
        timeLabel.setForeground(new Color(110, 115, 125));
        metaPanel.add(timeLabel);

        // Source badge
        JLabel sourceBadge = createBadgeLabel(entry.getSource(), getSourceColor(entry.getSource()));
        metaPanel.add(sourceBadge);

        // Info/Error tag (LOG badge removed, only show ERROR)
        if (isError) {
            JLabel badge = createBadgeLabel("ERROR", new Color(192, 57, 43));
            metaPanel.add(badge);
        }

        panel.add(metaPanel, BorderLayout.WEST);

        // Message text area
        JTextArea textArea = new JTextArea(entry.getMessage());
        textArea.setFont(monoFont);
        textArea.setForeground(isError ? colorErrorText : colorText);
        textArea.setBackground(panel.getBackground());
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBorder(null);

        panel.add(textArea, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCollapsibleRequestPanel(LogEntry entry) {
        JPanel requestContainer = new JPanel(new BorderLayout());
        requestContainer.setBackground(colorReqHeaderBg);
        requestContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        requestContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        boolean isError = entry.getStatusCode() == 0 || entry.getStatusCode() >= 400;

        // --- Header Panel ---
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setBackground(colorReqHeaderBg);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, colorReqHeaderBorder),
                new EmptyBorder(8, 12, 8, 12)));
        headerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Arrow and method badge
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setOpaque(false);

        JLabel arrowLabel = new JLabel("▸");
        arrowLabel.setFont(uiFontBold);
        arrowLabel.setForeground(new Color(150, 155, 165));
        leftPanel.add(arrowLabel);

        String time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(entry.getTimestamp()));
        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(monoFont);
        timeLabel.setForeground(new Color(110, 115, 125));
        leftPanel.add(timeLabel);

        // Source badge for Requests
        JLabel sourceBadge = createBadgeLabel(entry.getSource(), getSourceColor(entry.getSource()));
        leftPanel.add(sourceBadge);

        // HTTP Method Badge
        String method = entry.getMethod() != null ? entry.getMethod().toUpperCase() : "GET";
        JLabel methodLabel = createBadgeLabel(method, getMethodColor(method));
        leftPanel.add(methodLabel);

        // URL label
        JLabel urlLabel = new JLabel(entry.getUrl());
        urlLabel.setFont(monoFont);
        urlLabel.setForeground(colorText);
        leftPanel.add(urlLabel);

        headerPanel.add(leftPanel, BorderLayout.WEST);

        // Status code and latency
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        JLabel statusLabel = new JLabel(
                entry.getStatusCode() == 0 ? "Network Error" : String.valueOf(entry.getStatusCode()));
        statusLabel.setFont(uiFontBold);
        statusLabel.setForeground(isError ? new Color(255, 90, 90) : new Color(46, 204, 113));
        rightPanel.add(statusLabel);

        JLabel durationLabel = new JLabel(entry.getDurationMs() + " ms");
        durationLabel.setFont(uiFont);
        durationLabel.setForeground(new Color(140, 145, 155));
        rightPanel.add(durationLabel);

        headerPanel.add(rightPanel, BorderLayout.EAST);

        // --- Detail Panel ---
        JPanel detailPanel = new JPanel(new BorderLayout(0, 8));
        detailPanel.setBackground(colorReqDetailBg);
        detailPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, colorReqHeaderBorder),
                new EmptyBorder(8, 20, 8, 20)));
        detailPanel.setVisible(false);

        // --- View Mode Selector Toolbar ---
        JPanel detailHeader = new JPanel(new BorderLayout());
        detailHeader.setOpaque(false);

        JPanel modeTogglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        modeTogglePanel.setOpaque(false);

        JRadioButton formattedRadio = new JRadioButton("Formatted", true);
        formattedRadio.setFont(uiFontBold);
        formattedRadio.setForeground(colorText);
        formattedRadio.setOpaque(false);
        formattedRadio.setFocusable(false);

        JRadioButton rawRadio = new JRadioButton("Raw", false);
        rawRadio.setFont(uiFontBold);
        rawRadio.setForeground(colorText);
        rawRadio.setOpaque(false);
        rawRadio.setFocusable(false);

        ButtonGroup group = new ButtonGroup();
        group.add(formattedRadio);
        group.add(rawRadio);

        modeTogglePanel.add(formattedRadio);
        modeTogglePanel.add(rawRadio);
        detailHeader.add(modeTogglePanel, BorderLayout.WEST);

        // Details copy button
        JPanel detailActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        detailActions.setOpaque(false);
        JButton copyBtn = new JButton("Copy Full Log");
        copyBtn.setFont(uiFontBold);
        copyBtn.setBackground(new Color(127, 140, 141));
        copyBtn.setForeground(Color.WHITE);
        copyBtn.setFocusable(false);
        copyBtn.addActionListener(e -> {
            StringSelection selection = new StringSelection(entry.getMessage());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        });
        detailActions.add(copyBtn);
        detailHeader.add(detailActions, BorderLayout.EAST);

        // --- Content Panel (CardLayout) ---
        CardLayout cardLayout = new CardLayout();
        JPanel contentCards = new JPanel(cardLayout);
        contentCards.setOpaque(false);

        // 1. Formatted Panel
        JPanel formattedPanel = createFormattedRequestPanel(entry);
        contentCards.add(formattedPanel, "formatted");

        // 2. Raw Panel (Single Text Area)
        JTextArea detailTextArea = new JTextArea(entry.getMessage());
        detailTextArea.setFont(monoFont);
        detailTextArea.setForeground(colorText);
        detailTextArea.setBackground(colorReqDetailBg);
        detailTextArea.setEditable(false);
        detailTextArea.setLineWrap(true);
        detailTextArea.setWrapStyleWord(true);
        detailTextArea.setBorder(new EmptyBorder(8, 0, 8, 0));
        contentCards.add(detailTextArea, "raw");

        formattedRadio.addActionListener(ev -> cardLayout.show(contentCards, "formatted"));
        rawRadio.addActionListener(ev -> cardLayout.show(contentCards, "raw"));

        detailPanel.add(detailHeader, BorderLayout.NORTH);
        detailPanel.add(contentCards, BorderLayout.CENTER);

        // Toggle Expand Logic
        headerPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean visible = !detailPanel.isVisible();
                detailPanel.setVisible(visible);
                arrowLabel.setText(visible ? "▾" : "▸");
                requestContainer.revalidate();
                requestContainer.repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (isDark) {
                    headerPanel.setBackground(new Color(36, 37, 46));
                } else {
                    headerPanel.setBackground(new Color(225, 227, 233));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                headerPanel.setBackground(colorReqHeaderBg);
            }
        });

        requestContainer.add(headerPanel, BorderLayout.NORTH);
        requestContainer.add(detailPanel, BorderLayout.CENTER);
        return requestContainer;
    }

    private JPanel createFormattedRequestPanel(LogEntry entry) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        // --- REQUEST SECTION ---
        JLabel reqTitle = new JLabel("Request");
        reqTitle.setFont(uiFontBold);
        reqTitle.setForeground(isDark ? new Color(110, 180, 255) : new Color(0, 100, 200));
        panel.add(reqTitle);
        panel.add(Box.createVerticalStrut(4));

        // Request Line (Request)
        String reqLine = entry.getMethod() + " " + entry.getUrl();
        JTextArea reqLineArea = createMonoTextArea(reqLine);
        panel.add(reqLineArea);
        panel.add(Box.createVerticalStrut(8));

        // Request Headers
        JLabel reqHeadersTitle = new JLabel("Headers");
        reqHeadersTitle.setFont(uiFont);
        reqHeadersTitle.setForeground(new Color(150, 155, 165));
        panel.add(reqHeadersTitle);
        panel.add(Box.createVerticalStrut(2));

        JTextArea reqHeadersArea = createMonoTextArea(formatHeaders(entry.getRequestHeaders()));
        panel.add(reqHeadersArea);
        panel.add(Box.createVerticalStrut(8));

        // Request Body
        JLabel reqBodyTitle = new JLabel("Body");
        reqBodyTitle.setFont(uiFont);
        reqBodyTitle.setForeground(new Color(150, 155, 165));
        panel.add(reqBodyTitle);
        panel.add(Box.createVerticalStrut(2));

        JTextArea reqBodyArea = createMonoTextArea(formatBody(entry.getRequestBody()));
        panel.add(reqBodyArea);
        panel.add(Box.createVerticalStrut(16));

        // --- RESPONSE SECTION ---
        JLabel resTitle = new JLabel("Response");
        resTitle.setFont(uiFontBold);
        resTitle.setForeground(isDark ? new Color(110, 220, 150) : new Color(0, 150, 50));
        panel.add(resTitle);
        panel.add(Box.createVerticalStrut(4));

        // Response Status Line
        String resLine = "Status: " + (entry.getStatusCode() == 0 ? "Network Error"
                : entry.getStatusCode() + " (" + entry.getDurationMs() + " ms)");
        JTextArea resLineArea = createMonoTextArea(resLine);
        panel.add(resLineArea);
        panel.add(Box.createVerticalStrut(8));

        // Response Headers
        JLabel resHeadersTitle = new JLabel("Headers");
        resHeadersTitle.setFont(uiFont);
        resHeadersTitle.setForeground(new Color(150, 155, 165));
        panel.add(resHeadersTitle);
        panel.add(Box.createVerticalStrut(2));

        JTextArea resHeadersArea = createMonoTextArea(formatHeaders(entry.getResponseHeaders()));
        panel.add(resHeadersArea);
        panel.add(Box.createVerticalStrut(8));

        // Response Body
        JLabel resBodyTitle = new JLabel("Body");
        resBodyTitle.setFont(uiFont);
        resBodyTitle.setForeground(new Color(150, 155, 165));
        panel.add(resBodyTitle);
        panel.add(Box.createVerticalStrut(2));

        JTextArea resBodyArea = createMonoTextArea(formatBody(entry.getResponseBody()));
        panel.add(resBodyArea);

        return panel;
    }

    private JTextArea createMonoTextArea(String text) {
        JTextArea textArea = new JTextArea(text);
        textArea.setFont(monoFont);
        textArea.setForeground(colorText);
        textArea.setBackground(isDark ? new Color(22, 23, 28) : new Color(240, 241, 244));
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, new Color(100, 105, 115)),
                new EmptyBorder(6, 10, 6, 10)));
        return textArea;
    }

    private String formatHeaders(Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return "No headers";
        }
        StringBuilder sb = new StringBuilder();
        headers.forEach((k, v) -> sb.append(k).append(": ").append(String.join(", ", v)).append("\n"));
        return sb.toString().trim();
    }

    private String formatBody(String body) {
        if (body == null || body.trim().isEmpty()) {
            return "No body";
        }
        return prettyPrintJson(body);
    }

    private String prettyPrintJson(String json) {
        if (json == null || json.trim().isEmpty())
            return json;
        try {
            com.google.gson.JsonElement je = com.google.gson.JsonParser.parseString(json);
            return new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(je);
        } catch (Exception e) {
            return json; // Fallback to raw if not valid json
        }
    }

    private JLabel createBadgeLabel(String text, Color bg) {
        JLabel badge = new JLabel(" " + text + " ");
        badge.setFont(new Font("Segoe UI", Font.BOLD, fontSize - 4));
        badge.setForeground(Color.WHITE);
        badge.setOpaque(true);
        badge.setBackground(bg);
        return badge;
    }

    private Color getMethodColor(String method) {
        return switch (method.toUpperCase()) {
            case "GET" -> new Color(39, 174, 96);
            case "POST" -> new Color(41, 128, 185);
            case "PUT", "PATCH" -> new Color(230, 126, 34);
            case "DELETE" -> new Color(192, 57, 43);
            default -> new Color(110, 115, 125);
        };
    }

    private Color getSourceColor(String source) {
        if (source == null)
            return new Color(127, 140, 141);
        return switch (source) {
            case "Request" -> new Color(52, 152, 219);
            case "Pre-request" -> new Color(155, 89, 182);
            case "Test" -> new Color(241, 196, 15);
            default -> new Color(127, 140, 141);
        };
    }

    @Override
    public void onLogEntry(LogEntry entry) {
        SwingUtilities.invokeLater(() -> {
            String search = searchField.getText().toLowerCase().trim();
            String filterLevel = (String) levelCombo.getSelectedItem();

            // Check if level matches
            if ("Logs/Info".equals(filterLevel)) {
                if (entry.getUrl() != null || entry.getLevel() == LogEntry.Level.ERROR)
                    return;
            } else if ("Errors".equals(filterLevel)) {
                if (entry.getLevel() != LogEntry.Level.ERROR)
                    return;
            } else if ("Network Requests".equals(filterLevel)) {
                if (entry.getUrl() == null)
                    return;
            }

            // Check if search matches
            if (!search.isEmpty()) {
                boolean matchMsg = entry.getMessage() != null && entry.getMessage().toLowerCase().contains(search);
                boolean matchUrl = entry.getUrl() != null && entry.getUrl().toLowerCase().contains(search);
                boolean matchMethod = entry.getMethod() != null && entry.getMethod().toLowerCase().contains(search);
                boolean matchSource = entry.getSource() != null && entry.getSource().toLowerCase().contains(search);
                if (!matchMsg && !matchUrl && !matchMethod && !matchSource)
                    return;
            }

            JScrollBar bar = scrollPane.getVerticalScrollBar();
            int oldVal = bar.getValue();
            int oldHeight = logPanel.getPreferredSize().height;

            addEntryPanel(entry);
            logPanel.revalidate();
            logPanel.repaint();

            if (autoScrollCheck.isSelected()) {
                scrollToTop();
            } else {
                SwingUtilities.invokeLater(() -> {
                    int newHeight = logPanel.getPreferredSize().height;
                    int diff = newHeight - oldHeight;
                    bar.setValue(oldVal + diff);
                });
            }
        });
    }

    private void downloadFilteredLogs() {
        String search = searchField.getText().toLowerCase().trim();
        String filterLevel = (String) levelCombo.getSelectedItem();
        List<LogEntry> matchingEntries = new ArrayList<>();

        for (LogEntry entry : ConsoleLogger.getInstance().getEntries()) {
            if ("Logs/Info".equals(filterLevel)) {
                if (entry.getUrl() != null || entry.getLevel() == LogEntry.Level.ERROR)
                    continue;
            } else if ("Errors".equals(filterLevel)) {
                if (entry.getLevel() != LogEntry.Level.ERROR)
                    continue;
            } else if ("Network Requests".equals(filterLevel)) {
                if (entry.getUrl() == null)
                    continue;
            }

            if (!search.isEmpty()) {
                boolean matchMsg = entry.getMessage() != null && entry.getMessage().toLowerCase().contains(search);
                boolean matchUrl = entry.getUrl() != null && entry.getUrl().toLowerCase().contains(search);
                boolean matchMethod = entry.getMethod() != null && entry.getMethod().toLowerCase().contains(search);
                boolean matchSource = entry.getSource() != null && entry.getSource().toLowerCase().contains(search);
                if (!matchMsg && !matchUrl && !matchMethod && !matchSource)
                    continue;
            }
            matchingEntries.add(entry);
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
        String suggestedName = "apibanker-live-log-" + timestamp + ".log";

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Download Filtered Live Logs");
        fileChooser.setSelectedFile(new File(suggestedName));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dest = fileChooser.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(dest, StandardCharsets.UTF_8)) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                pw.println("================================================================================");
                pw.println(" ApiBanker Live Log Export - " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                pw.println(" Active Filter: Level=[" + filterLevel + "], Search=[" + (search.isEmpty() ? "None" : search) + "]");
                pw.println(" Total Log Entries: " + matchingEntries.size());
                pw.println("================================================================================\n");

                for (LogEntry entry : matchingEntries) {
                    String timeStr = sdf.format(new Date(entry.getTimestamp()));
                    pw.println("[" + timeStr + "] [" + (entry.getSource() != null ? entry.getSource() : "System") + "] "
                            + (entry.getLevel() == LogEntry.Level.ERROR ? "[ERROR] " : "[INFO] ")
                            + (entry.getMethod() != null ? entry.getMethod() + " " : "")
                            + (entry.getUrl() != null ? entry.getUrl() : ""));

                    if (entry.getStatusCode() != 0 || entry.getDurationMs() > 0) {
                        pw.println("Status: " + (entry.getStatusCode() == 0 ? "Network Error" : entry.getStatusCode())
                                + " | Latency: " + entry.getDurationMs() + " ms"
                                + " | Response Size: " + entry.getResponseSize() + " bytes");
                    }

                    if (entry.getRequestHeaders() != null && !entry.getRequestHeaders().isEmpty()) {
                        pw.println("--- Request Headers ---");
                        pw.println(formatHeaders(entry.getRequestHeaders()));
                    }
                    if (entry.getRequestBody() != null && !entry.getRequestBody().isBlank()) {
                        pw.println("--- Request Body ---");
                        pw.println(entry.getRequestBody());
                    }
                    if (entry.getResponseHeaders() != null && !entry.getResponseHeaders().isEmpty()) {
                        pw.println("--- Response Headers ---");
                        pw.println(formatHeaders(entry.getResponseHeaders()));
                    }
                    if (entry.getResponseBody() != null && !entry.getResponseBody().isBlank()) {
                        pw.println("--- Response Body ---");
                        pw.println(entry.getResponseBody());
                    }
                    if (entry.getMessage() != null && !entry.getMessage().isBlank()) {
                        pw.println("--- Message / Trace ---");
                        pw.println(entry.getMessage());
                    }
                    pw.println("--------------------------------------------------------------------------------\n");
                }
                JOptionPane.showMessageDialog(this,
                        "Successfully exported " + matchingEntries.size() + " log entries to:\n" + dest.getAbsolutePath(),
                        "Download Live Logs", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error saving log file: " + ex.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void dispose() {
        ConsoleLogger.getInstance().removeListener(this);
        synchronized (ConsoleDialog.class) {
            if (instance == this) {
                instance = null;
            }
        }
        super.dispose();
    }
}


