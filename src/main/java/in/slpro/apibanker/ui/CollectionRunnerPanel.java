package in.slpro.apibanker.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import in.slpro.apibanker.http.HttpClientWrapper;
import in.slpro.apibanker.http.JmxHelper;
import in.slpro.apibanker.model.*;
import in.slpro.apibanker.storage.StorageManager;

import org.apache.poi.xssf.usermodel.XSSFSheet;

/**
 * CollectionRunnerPanel
 *
 * <p>
 * This panel is the core execution UI for the ApiBanker Collection Runner.
 * It provides a comprehensive interface for setting up, executing, and
 * monitoring
 * batch API requests. It supports configuring virtual users (VUsers),
 * iterations,
 * and request delays. It executes the requests in an isolated thread pool,
 * tracks
 * live metrics (Pass, Fail, Avg Response Time), renders a real-time scatter
 * plot
 * chart, and manages the exporting of extensive HTML/PDF reports and execution
 * logs.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.0.0-beta
 * @since 1.0.0
 */
public class CollectionRunnerPanel extends JPanel {
    private final MainFrame mainFrame;
    private final CollectionModel collection;
    private final RequestModel runnerModel;

    private DefaultTableModel requestSelectionModel;
    private JTable requestSelectionTable;
    private VisualChartPanel chartPanel;

    public static class RunSample {
        public final long timestamp; // epoch ms
        public final String name;
        public final String method;
        public final int statusCode;
        public final long duration;
        public final boolean success;

        public RunSample(long timestamp, String name, String method, int statusCode, long duration, boolean success) {
            this.timestamp = timestamp;
            this.name = name;
            this.method = method;
            this.statusCode = statusCode;
            this.duration = duration;
            this.success = success;
        }
    }

    private final List<RunSample> runSamples = new CopyOnWriteArrayList<>();

    // UI Components
    private JSpinner iterationsSpinner;
    private JSpinner delaySpinner;
    private JSpinner vusersSpinner;
    private JCheckBox saveLogsCheck;
    private JComboBox<String> envSelectCombo;
    private JButton runBtn;
    private JButton stopBtn;

    private JProgressBar progressBar;
    private JPanel metricsPanel;
    private JLabel statusLabel;
    private JLabel totalReqLabel;
    private JLabel passedLabel;
    private JLabel failedLabel;
    private JLabel avgTimeLabel;

    private DefaultTableModel resultsTableModel;
    private JTable resultsTable;

    private DefaultTableModel aggregateModel;
    private JTable aggregateTable;

    private ExecutorService executorService;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private boolean isInitializing = true;

    // Run statistics & metadata
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private File currentRunDir;

    // Aggregate statistics helper
    private static class RequestStats {
        final String name;
        final String method;
        final List<Long> latencies = new CopyOnWriteArrayList<>();
        final List<Long> sizes = new CopyOnWriteArrayList<>();
        int successCount = 0;
        int failCount = 0;
        final Set<Integer> statusCodes = ConcurrentHashMap.newKeySet();
        final Set<String> formats = ConcurrentHashMap.newKeySet();

        RequestStats(String name, String method) {
            this.name = name;
            this.method = method;
        }
    }

    private final Map<String, RequestStats> aggregateStatsMap = new ConcurrentHashMap<>();

    /**
     * Constructs the Collection Runner interface for a specific Collection.
     * <p>
     * Initializes the Left-Hand Side (LHS) configuration panel where users can
     * select
     * the target Environment, toggle individual requests to run, and configure load
     * parameters (Iterations, Delay, VUsers). Also sets up the Right-Hand Side
     * (RHS)
     * monitoring panel containing the real-time Visual Scatter Plot, aggregate
     * statistics
     * table, and detailed execution logs table.
     * </p>
     * 
     * @param mainFrame   The root application window (for navigating and reading
     *                    global state).
     * @param collection  The target Collection to execute.
     * @param runnerModel A persistent RequestModel (acting as a configuration
     *                    container)
     *                    where user-selected runner settings are stored and loaded
     *                    from.
     */
    public CollectionRunnerPanel(MainFrame mainFrame, CollectionModel collection, RequestModel runnerModel) {
        this.mainFrame = mainFrame;
        this.collection = collection;
        this.runnerModel = runnerModel;

        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));

        initUI();
        loadConfig();

        Font defaultFont = UIManager.getFont("defaultFont");
        if (defaultFont != null) {
            updateFontSize(defaultFont.getSize());
        } else {
            updateFontSize(16);
        }

        // Add auto-save listeners after config is loaded
        iterationsSpinner.addChangeListener(e -> autoSave());
        delaySpinner.addChangeListener(e -> autoSave());
        vusersSpinner.addChangeListener(e -> autoSave());
        saveLogsCheck.addActionListener(e -> autoSave());
        envSelectCombo.addActionListener(e -> autoSave());

        isInitializing = false;
    }

    private void initUI() {
        // Top config panel
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBackground(UIManager.getColor("Workspace.panelBackground"));
        configPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Workspace.borderColor")),
                new EmptyBorder(10, 15, 10, 15)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Iterations
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        configPanel.add(new JLabel("Iterations:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.2;
        iterationsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));
        configPanel.add(iterationsSpinner, gbc);

        // Delay ms
        gbc.gridx = 2;
        gbc.weightx = 0;
        configPanel.add(new JLabel("Delay (ms):"), gbc);
        gbc.gridx = 3;
        gbc.weightx = 0.2;
        delaySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 60000, 50));
        configPanel.add(delaySpinner, gbc);

        // Virtual Users / Concurrency
        gbc.gridx = 4;
        gbc.weightx = 0;
        configPanel.add(new JLabel("Concurrent Users:"), gbc);
        gbc.gridx = 5;
        gbc.weightx = 0.2;
        vusersSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        configPanel.add(vusersSpinner, gbc);

        // Environment
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        configPanel.add(new JLabel("Environment:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 0.5;
        envSelectCombo = new JComboBox<>();
        refreshEnvCombo();
        configPanel.add(envSelectCombo, gbc);

        // Save logs checkbox
        gbc.gridx = 4;
        gbc.gridwidth = 2;
        gbc.weightx = 0.3;
        saveLogsCheck = new JCheckBox("Save Execution Logs", true);
        saveLogsCheck.setOpaque(false);
        configPanel.add(saveLogsCheck, gbc);

        // Control Buttons
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controlPanel.setOpaque(false);

        JButton importJmxBtn = new JButton("Import JMeter (.jmx)");
        importJmxBtn.addActionListener(e -> importJmx());
        JButton exportJmxBtn = new JButton("Export JMeter (.jmx)");
        exportJmxBtn.addActionListener(e -> exportJmx());

        runBtn = new JButton("Run Collection");
        runBtn.setBackground(new Color(46, 204, 113));
        runBtn.setForeground(Color.WHITE);
        runBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        runBtn.addActionListener(e -> startRun());

        stopBtn = new JButton("Stop");
        stopBtn.setBackground(new Color(231, 76, 60));
        stopBtn.setForeground(Color.WHITE);
        stopBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(e -> stopRun());

        JButton reportBtn = new JButton("Export Metrics");
        reportBtn.addActionListener(e -> {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem excelItem = new JMenuItem("Export as Excel (.xlsx)");
            excelItem.addActionListener(evt -> exportReport("xlsx"));

            JMenuItem pdfItem = new JMenuItem("Export as PDF (.pdf)");
            pdfItem.addActionListener(evt -> exportReport("pdf"));

            JMenuItem htmlItem = new JMenuItem("Export as HTML (.html)");
            htmlItem.addActionListener(evt -> exportReport("html"));

            JMenuItem csvItem = new JMenuItem("Export as CSV (.csv)");
            csvItem.addActionListener(evt -> exportReport("csv"));

            menu.add(excelItem);
            menu.add(pdfItem);
            menu.addSeparator();
            menu.add(htmlItem);
            menu.add(csvItem);

            menu.addSeparator();
            JMenu logsMenu = new JMenu("Export Logs");
            JMenuItem sumLogItem = new JMenuItem("Summary Only");
            sumLogItem.addActionListener(evt -> exportLogs("summary"));
            JMenuItem dumpLogItem = new JMenuItem("Dump Only");
            dumpLogItem.addActionListener(evt -> exportLogs("dump"));
            JMenuItem bothLogItem = new JMenuItem("Summary + Dump");
            bothLogItem.addActionListener(evt -> exportLogs("full"));
            logsMenu.add(sumLogItem);
            logsMenu.add(dumpLogItem);
            logsMenu.add(bothLogItem);
            menu.add(logsMenu);
            menu.show(reportBtn, 0, reportBtn.getHeight());
        });

        controlPanel.add(importJmxBtn);
        controlPanel.add(exportJmxBtn);
        controlPanel.add(reportBtn);
        controlPanel.add(stopBtn);
        controlPanel.add(runBtn);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 6;
        gbc.weightx = 1.0;
        configPanel.add(controlPanel, gbc);

        add(configPanel, BorderLayout.NORTH);

        // Selection panel (Left)
        JPanel selectionPanel = new JPanel(new BorderLayout(5, 5));
        selectionPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, UIManager.getColor("Workspace.borderColor")),
                new EmptyBorder(10, 15, 10, 15)));
        selectionPanel.setBackground(UIManager.getColor("Panel.background"));

        JPanel selHeader = new JPanel(new BorderLayout());
        selHeader.setOpaque(false);
        JLabel selTitle = new JLabel("Select APIs to Run");
        selTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        selHeader.add(selTitle, BorderLayout.WEST);

        JPanel selectControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        selectControls.setOpaque(false);
        JButton selectAllBtn = new JButton("All");
        selectAllBtn.setMargin(new Insets(2, 6, 2, 6));
        selectAllBtn.addActionListener(e -> {
            for (int i = 0; i < requestSelectionModel.getRowCount(); i++) {
                requestSelectionModel.setValueAt(true, i, 0);
            }
            autoSave();
        });
        JButton deselectAllBtn = new JButton("None");
        deselectAllBtn.setMargin(new Insets(2, 6, 2, 6));
        deselectAllBtn.addActionListener(e -> {
            for (int i = 0; i < requestSelectionModel.getRowCount(); i++) {
                requestSelectionModel.setValueAt(false, i, 0);
            }
            autoSave();
        });
        selectControls.add(selectAllBtn);
        selectControls.add(deselectAllBtn);
        selHeader.add(selectControls, BorderLayout.EAST);
        selectionPanel.add(selHeader, BorderLayout.NORTH);

        requestSelectionModel = new DefaultTableModel(new String[] { "Run", "Method", "Request Name" }, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0)
                    return Boolean.class;
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0;
            }
        };

        requestSelectionModel.addTableModelListener(e -> {
            if (!isInitializing) {
                autoSave();
            }
        });

        requestSelectionTable = new JTable(requestSelectionModel);
        requestSelectionTable.setRowHeight(24);
        requestSelectionTable.getColumnModel().getColumn(0).setMaxWidth(50);
        requestSelectionTable.getColumnModel().getColumn(1).setMaxWidth(70);

        JScrollPane selectionScroll = new JScrollPane(requestSelectionTable);
        selectionPanel.add(selectionScroll, BorderLayout.CENTER);

        // Populate requests
        populateSelectionTable();

        // Right side: Metrics + Table (existing centerPanel)
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
        centerPanel.setBackground(UIManager.getColor("Panel.background"));

        // Summary Cards
        metricsPanel = new JPanel(new GridLayout(1, 5, 10, 0));
        metricsPanel.setBackground(UIManager.getColor("Panel.background"));
        metricsPanel.setPreferredSize(new Dimension(0, 65));

        totalReqLabel = createMetricCard(metricsPanel, "Total Requests", "0", new Color(52, 152, 219));
        passedLabel = createMetricCard(metricsPanel, "Passed", "0", new Color(39, 174, 96));
        failedLabel = createMetricCard(metricsPanel, "Failed", "0", new Color(231, 76, 60));
        avgTimeLabel = createMetricCard(metricsPanel, "Avg Duration", "0 ms", new Color(155, 89, 182));
        statusLabel = createMetricCard(metricsPanel, "Runner Status", "Idle", new Color(127, 140, 141));

        centerPanel.add(metricsPanel, BorderLayout.NORTH);

        // Tabs for Results Table vs Aggregate Report vs Visual Charts
        JTabbedPane runnerTabs = new JTabbedPane();
        runnerTabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // 1. Raw Execution Table
        resultsTableModel = new DefaultTableModel(
                new String[] { "#", "Iteration", "Request", "Method", "Status", "Duration (ms)", "URL" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        resultsTable = new JTable(resultsTableModel);
        resultsTable.setRowHeight(24);
        resultsTable.getColumnModel().getColumn(0).setMaxWidth(40);
        resultsTable.getColumnModel().getColumn(1).setMaxWidth(70);
        resultsTable.getColumnModel().getColumn(3).setMaxWidth(70);
        resultsTable.getColumnModel().getColumn(4).setMaxWidth(90);
        resultsTable.getColumnModel().getColumn(5).setMaxWidth(100);
        JScrollPane tableScroll = new JScrollPane(resultsTable);
        runnerTabs.addTab("Execution Log", tableScroll);

        // 2. Aggregate Report Table
        aggregateModel = new DefaultTableModel(new String[] {
                "API Name", "Method", "Samples", "StatusCodes", "Avg (ms)", "Min (ms)", "Max (ms)",
                "p75 (ms)", "p90 (ms)", "p95 (ms)", "p99 (ms)", "Avg Size(B)", "Pass", "Fail", "Success %", "Format"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        aggregateTable = new JTable(aggregateModel);
        aggregateTable.setRowHeight(24);
        JScrollPane aggregateScroll = new JScrollPane(aggregateTable);
        runnerTabs.addTab("Aggregate Summary", aggregateScroll);

        // 3. Visual Charts Tab
        chartPanel = new VisualChartPanel();
        runnerTabs.addTab("Visual Charts", chartPanel);

        centerPanel.add(runnerTabs, BorderLayout.CENTER);

        // Bottom progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(0, 22));
        centerPanel.add(progressBar, BorderLayout.SOUTH);

        // JSplitPane to host selection on left and centerPanel on right
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, selectionPanel, centerPanel);
        splitPane.setDividerLocation(260);
        splitPane.setBorder(null);
        add(splitPane, BorderLayout.CENTER);
    }

    private void populateSelectionTable() {
        requestSelectionModel.setRowCount(0);
        if (collection != null) {
            List<RequestModel> allReqs = new ArrayList<>();
            collectRequestsRecursive(collection, allReqs);
            for (RequestModel req : allReqs) {
                requestSelectionModel.addRow(new Object[] { true, req.getMethod(), req.getName() });
            }
        }
    }

    private class VisualChartPanel extends JPanel {
        private final JComboBox<String> yAxisCombo;
        private final JComboBox<String> timeWindowCombo;
        private final JPanel chartDrawArea;

        VisualChartPanel() {
            setLayout(new BorderLayout(5, 5));
            setBackground(UIManager.getColor("Panel.background"));

            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
            controls.setBackground(UIManager.getColor("Workspace.panelBackground"));
            controls.setBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Workspace.borderColor")));

            controls.add(new JLabel("Y-Axis Metric Group:"));
            yAxisCombo = new JComboBox<>(new String[] {
                    "HTTP Status Codes (2xx, 3xx, 4xx, 5xx)",
                    "Response Latency (Avg, Min, Max, p95)"
            });
            yAxisCombo.addActionListener(e -> repaint());
            controls.add(yAxisCombo);

            controls.add(new JLabel("Time Window (X-Axis Bin):"));
            timeWindowCombo = new JComboBox<>(new String[] {
                    "1 Second",
                    "5 Seconds",
                    "10 Seconds",
                    "30 Seconds",
                    "1 Minute",
                    "5 Minutes"
            });
            timeWindowCombo.addActionListener(e -> repaint());
            controls.add(timeWindowCombo);

            add(controls, BorderLayout.NORTH);

            chartDrawArea = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    drawChart((Graphics2D) g, getWidth(), getHeight(),
                            "HTTP Status Codes (2xx, 3xx, 4xx, 5xx)".equals(yAxisCombo.getSelectedItem()));
                }
            };
            chartDrawArea.setBackground(UIManager.getColor("Panel.background"));
            add(chartDrawArea, BorderLayout.CENTER);
        }

        public void drawChart(Graphics2D g2, int w, int h, boolean isStatusCodes) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int padding = 50;
            int labelPadding = 30;

            // Draw axes
            g2.setColor(UIManager.getColor("Label.foreground"));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));

            // X-axis line
            g2.drawLine(padding + labelPadding, h - padding - labelPadding, w - padding, h - padding - labelPadding);
            // Y-axis line
            g2.drawLine(padding + labelPadding, padding, padding + labelPadding, h - padding - labelPadding);

            List<RunSample> samplesCopy = new ArrayList<>(runSamples);
            if (samplesCopy.isEmpty()) {
                g2.drawString("No data available. Run the collection to view results.", w / 2 - 120, h / 2);
                return;
            }

            // Get start time
            long startMs = startTime != null
                    ? startTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    : samplesCopy.get(0).timestamp;

            // Determine bin size in ms
            long binSizeMs = 1000;
            String binStr = (String) timeWindowCombo.getSelectedItem();
            if ("5 Seconds".equals(binStr))
                binSizeMs = 5000;
            else if ("10 Seconds".equals(binStr))
                binSizeMs = 10000;
            else if ("30 Seconds".equals(binStr))
                binSizeMs = 30000;
            else if ("1 Minute".equals(binStr))
                binSizeMs = 60000;
            else if ("5 Minutes".equals(binStr))
                binSizeMs = 300000;

            // Group samples into bins
            long maxElapsed = System.currentTimeMillis() - startMs;
            if (!samplesCopy.isEmpty()) {
                long lastSampleTime = samplesCopy.get(samplesCopy.size() - 1).timestamp;
                maxElapsed = Math.max(maxElapsed, lastSampleTime - startMs);
            }
            int numBins = (int) (maxElapsed / binSizeMs) + 1;
            if (numBins < 5)
                numBins = 5; // minimum bins for visual spacing

            List<List<RunSample>> bins = new ArrayList<>();
            for (int i = 0; i < numBins; i++) {
                bins.add(new ArrayList<>());
            }

            for (RunSample s : samplesCopy) {
                long elapsed = s.timestamp - startMs;
                int binIdx = (int) (elapsed / binSizeMs);
                if (binIdx >= 0 && binIdx < numBins) {
                    bins.get(binIdx).add(s);
                }
            }

            // Calculate metric values per bin (4 lines)
            double[][] multiValues = new double[4][numBins];
            double maxValue = 1.0; // minimum scale

            for (int i = 0; i < numBins; i++) {
                List<RunSample> binSamples = bins.get(i);
                if (binSamples.isEmpty()) {
                    for (int line = 0; line < 4; line++) {
                        multiValues[line][i] = 0.0;
                    }
                    continue;
                }

                if (isStatusCodes) {
                    double count2xx = 0, count3xx = 0, count4xx = 0, count5xx = 0;
                    for (RunSample s : binSamples) {
                        int code = s.statusCode;
                        if (code >= 200 && code < 300)
                            count2xx++;
                        else if (code >= 300 && code < 400)
                            count3xx++;
                        else if (code >= 400 && code < 500)
                            count4xx++;
                        else
                            count5xx++;
                    }
                    multiValues[0][i] = count2xx;
                    multiValues[1][i] = count3xx;
                    multiValues[2][i] = count4xx;
                    multiValues[3][i] = count5xx;
                } else {
                    double min = Double.MAX_VALUE;
                    double max = -1.0;
                    double sum = 0.0;
                    List<Long> latencies = new ArrayList<>();
                    for (RunSample s : binSamples) {
                        sum += s.duration;
                        if (s.duration < min)
                            min = s.duration;
                        if (s.duration > max)
                            max = s.duration;
                        latencies.add(s.duration);
                    }
                    double avg = sum / binSamples.size();
                    double p95 = 0.0;
                    if (!latencies.isEmpty()) {
                        Collections.sort(latencies);
                        int idx = (int) (0.95 * latencies.size());
                        if (idx >= latencies.size())
                            idx = latencies.size() - 1;
                        p95 = latencies.get(idx);
                    }
                    multiValues[0][i] = avg;
                    multiValues[1][i] = min == Double.MAX_VALUE ? 0.0 : min;
                    multiValues[2][i] = max == -1.0 ? 0.0 : max;
                    multiValues[3][i] = p95;
                }

                for (int line = 0; line < 4; line++) {
                    if (multiValues[line][i] > maxValue) {
                        maxValue = multiValues[line][i];
                    }
                }
            }

            // Add some headroom to Y-axis
            maxValue = maxValue * 1.15;

            // X and Y scaling factors
            double xScale = ((double) w - 2 * padding - labelPadding) / (numBins - 1);
            double yScale = ((double) h - 2 * padding - labelPadding) / maxValue;

            // Draw grid lines and Y-axis labels
            g2.setColor(UIManager.getColor("Workspace.borderColor"));
            int yTicks = 5;
            for (int i = 0; i <= yTicks; i++) {
                double val = (maxValue / yTicks) * i;
                int yPos = (int) (h - padding - labelPadding - val * yScale);
                g2.drawLine(padding + labelPadding, yPos, w - padding, yPos);

                // Draw label
                g2.setColor(UIManager.getColor("Label.foreground"));
                String label = String.format("%.1f", val);
                if (val == (int) val)
                    label = String.valueOf((int) val);
                g2.drawString(label, padding - 5, yPos + 4);
                g2.setColor(UIManager.getColor("Workspace.borderColor"));
            }

            // Draw X-axis labels (time elapsed in bin scale)
            g2.setColor(UIManager.getColor("Label.foreground"));
            int xTicks = Math.min(numBins, 8);
            for (int i = 0; i < xTicks; i++) {
                int binIdx = (int) (((double) (numBins - 1) / (xTicks - 1)) * i);
                int xPos = (int) (padding + labelPadding + binIdx * xScale);

                long totalSec = (binIdx * binSizeMs) / 1000;
                String xLabel;
                if (totalSec < 60) {
                    xLabel = totalSec + "s";
                } else if (totalSec < 3600) {
                    xLabel = (totalSec / 60) + "m " + (totalSec % 60) + "s";
                } else {
                    xLabel = (totalSec / 3600) + "h " + ((totalSec % 3600) / 60) + "m";
                }

                g2.drawString(xLabel, xPos - 12, h - padding + 5);
            }

            // Set colors and labels
            Color[] colors = new Color[4];
            String[] names = new String[4];

            if (isStatusCodes) {
                colors[0] = new Color(46, 204, 113); // Green (2xx)
                colors[1] = new Color(52, 152, 219); // Blue (3xx)
                colors[2] = new Color(241, 196, 15); // Yellow (4xx)
                colors[3] = new Color(231, 76, 60); // Red (5xx)
                names[0] = "Success (2xx)";
                names[1] = "Redirect (3xx)";
                names[2] = "Client Error (4xx)";
                names[3] = "Server Error (5xx)";
            } else {
                colors[0] = new Color(52, 152, 219); // Blue (Avg)
                colors[1] = new Color(46, 204, 113); // Green (Min)
                colors[2] = new Color(231, 76, 60); // Red (Max)
                colors[3] = new Color(155, 89, 182); // Purple (p95)
                names[0] = "Avg Latency (ms)";
                names[1] = "Min Latency (ms)";
                names[2] = "Max Latency (ms)";
                names[3] = "p95 Latency (ms)";
            }

            // Draw Legend (top right)
            int legendX = w - padding - 190;
            int legendY = padding + 10;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));

            // Draw background for legend box
            g2.setColor(UIManager.getColor("Workspace.panelBackground"));
            g2.fillRect(legendX, legendY, 185, 95);
            g2.setColor(UIManager.getColor("Workspace.borderColor"));
            g2.drawRect(legendX, legendY, 185, 95);

            for (int line = 0; line < 4; line++) {
                int itemY = legendY + 15 + line * 20;

                // Draw color block
                g2.setColor(colors[line]);
                g2.fillRect(legendX + 15, itemY - 7, 12, 12);

                // Draw name label
                g2.setColor(UIManager.getColor("Label.foreground"));
                g2.drawString(names[line], legendX + 35, itemY + 3);
            }

            // Draw lines and dots for each of the 4 lines
            g2.setStroke(new BasicStroke(2f));
            for (int line = 0; line < 4; line++) {
                g2.setColor(colors[line]);

                // Draw line path
                for (int i = 0; i < numBins - 1; i++) {
                    int x1 = (int) (padding + labelPadding + i * xScale);
                    int y1 = (int) (h - padding - labelPadding - multiValues[line][i] * yScale);
                    int x2 = (int) (padding + labelPadding + (i + 1) * xScale);
                    int y2 = (int) (h - padding - labelPadding - multiValues[line][i + 1] * yScale);
                    g2.drawLine(x1, y1, x2, y2);
                }

                // Draw dots/markers
                for (int i = 0; i < numBins; i++) {
                    int x = (int) (padding + labelPadding + i * xScale);
                    int y = (int) (h - padding - labelPadding - multiValues[line][i] * yScale);
                    g2.fillOval(x - 4, y - 4, 8, 8);
                    g2.setColor(UIManager.getColor("Panel.background"));
                    g2.fillOval(x - 2, y - 2, 4, 4);
                    g2.setColor(colors[line]);
                }
            }
        }
    }

    private JLabel createMetricCard(JPanel parent, String title, String initialVal, Color color) {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBackground(UIManager.getColor("Workspace.metricCardBackground"));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, color),
                new EmptyBorder(6, 10, 6, 10)));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        titleLbl.setForeground(new Color(120, 120, 120));

        JLabel valLbl = new JLabel(initialVal);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        valLbl.setForeground(color);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valLbl, BorderLayout.CENTER);
        parent.add(card);
        return valLbl;
    }

    private void refreshEnvCombo() {
        envSelectCombo.removeAllItems();
        envSelectCombo.addItem("Use Active Environment");
        for (EnvironmentModel env : mainFrame.getEnvironments()) {
            envSelectCombo.addItem(env.getName());
        }
    }

    private void loadConfig() {
        if (runnerModel.getBodyRawContent() != null && !runnerModel.getBodyRawContent().isBlank()) {
            try {
                com.google.gson.JsonObject cfg = com.google.gson.JsonParser.parseString(runnerModel.getBodyRawContent())
                        .getAsJsonObject();
                if (cfg.has("iterations"))
                    iterationsSpinner.setValue(cfg.get("iterations").getAsInt());
                if (cfg.has("delay"))
                    delaySpinner.setValue(cfg.get("delay").getAsInt());
                if (cfg.has("vusers"))
                    vusersSpinner.setValue(cfg.get("vusers").getAsInt());
                if (cfg.has("saveLogs"))
                    saveLogsCheck.setSelected(cfg.get("saveLogs").getAsBoolean());
                if (cfg.has("environmentId")) {
                    String envId = cfg.get("environmentId").getAsString();
                    if (!envId.isEmpty()) {
                        for (int i = 0; i < mainFrame.getEnvironments().size(); i++) {
                            if (mainFrame.getEnvironments().get(i).getId().equals(envId)) {
                                envSelectCombo.setSelectedIndex(i + 1);
                                break;
                            }
                        }
                    }
                }
                if (cfg.has("selectedRequests") && requestSelectionModel != null) {
                    com.google.gson.JsonArray selectedList = cfg.getAsJsonArray("selectedRequests");
                    Set<String> selectedNames = new HashSet<>();
                    for (com.google.gson.JsonElement el : selectedList) {
                        selectedNames.add(el.getAsString());
                    }
                    for (int i = 0; i < requestSelectionModel.getRowCount(); i++) {
                        String reqName = (String) requestSelectionModel.getValueAt(i, 2);
                        requestSelectionModel.setValueAt(selectedNames.contains(reqName), i, 0);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    public void saveConfig() {
        com.google.gson.JsonObject cfg = new com.google.gson.JsonObject();
        cfg.addProperty("iterations", (Integer) iterationsSpinner.getValue());
        cfg.addProperty("delay", (Integer) delaySpinner.getValue());
        cfg.addProperty("vusers", (Integer) vusersSpinner.getValue());
        cfg.addProperty("saveLogs", saveLogsCheck.isSelected());

        int envIdx = envSelectCombo.getSelectedIndex();
        if (envIdx > 0 && envIdx - 1 < mainFrame.getEnvironments().size()) {
            cfg.addProperty("environmentId", mainFrame.getEnvironments().get(envIdx - 1).getId());
        } else {
            cfg.addProperty("environmentId", "");
        }

        com.google.gson.JsonArray selectedList = new com.google.gson.JsonArray();
        if (requestSelectionModel != null) {
            for (int i = 0; i < requestSelectionModel.getRowCount(); i++) {
                boolean checked = (Boolean) requestSelectionModel.getValueAt(i, 0);
                if (checked) {
                    String reqName = (String) requestSelectionModel.getValueAt(i, 2);
                    selectedList.add(reqName);
                }
            }
        }
        cfg.add("selectedRequests", selectedList);

        runnerModel.setBodyRawContent(cfg.toString());
    }

    private void autoSave() {
        if (!isInitializing) {
            saveConfig();
            mainFrame.saveCollections();
        }
    }

    private String sanitizeFilename(String name) {
        if (name == null)
            return "runner";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }

    private File getLogDir() {
        String logsDir = StorageManager.getInstance().getSettings().getLogsDirectory();
        File logsFolder = new File(logsDir);
        if (!logsFolder.exists())
            logsFolder.mkdirs();

        String cleanColl = sanitizeFilename(collection.getName());
        String cleanRunner = sanitizeFilename(runnerModel.getName());
        String dirName = cleanColl + "-" + cleanRunner;
        File runGroupDir = new File(logsFolder, dirName);
        if (!runGroupDir.exists()) {
            runGroupDir.mkdirs();
        }

        LocalDateTime start = (startTime != null) ? startTime : LocalDateTime.now();
        String fileStr = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS"));
        File specificRunDir = new File(runGroupDir, fileStr);
        if (!specificRunDir.exists()) {
            specificRunDir.mkdirs();
        }
        return specificRunDir;
    }

    private EnvironmentModel resolveSelectedEnvironment() {
        int idx = envSelectCombo.getSelectedIndex();
        if (idx > 0 && idx - 1 < mainFrame.getEnvironments().size()) {
            return mainFrame.getEnvironments().get(idx - 1);
        }
        return mainFrame.getActiveEnvironment();
    }

    private void startRun() {
        saveConfig();
        mainFrame.saveCollections();

        runSamples.clear();

        final List<RequestModel> requests;
        if (requestSelectionModel != null) {
            List<RequestModel> temp = new ArrayList<>();
            List<RequestModel> allReqs = new ArrayList<>();
            collectRequestsRecursive(collection, allReqs);
            for (int i = 0; i < requestSelectionModel.getRowCount(); i++) {
                boolean checked = (Boolean) requestSelectionModel.getValueAt(i, 0);
                if (checked) {
                    String reqName = (String) requestSelectionModel.getValueAt(i, 2);
                    for (RequestModel r : allReqs) {
                        if (reqName.equals(r.getName())) {
                            temp.add(r);
                            break;
                        }
                    }
                }
            }
            requests = temp;
        } else {
            List<RequestModel> allReqs = new ArrayList<>();
            collectRequestsRecursive(collection, allReqs);
            requests = allReqs;
        }

        if (requests.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No selected requests to run.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int iterations = (Integer) iterationsSpinner.getValue();
        int delay = (Integer) delaySpinner.getValue();
        int vusers = (Integer) vusersSpinner.getValue();
        boolean saveLogs = saveLogsCheck.isSelected() && StorageManager.getInstance().getSettings().isEnableLogging();
        EnvironmentModel env = resolveSelectedEnvironment();

        resultsTableModel.setRowCount(0);
        aggregateModel.setRowCount(0);
        aggregateStatsMap.clear();

        totalReqLabel.setText("0");
        passedLabel.setText("0");
        failedLabel.setText("0");
        avgTimeLabel.setText("0 ms");
        statusLabel.setText("Running...");
        statusLabel.setForeground(new Color(230, 126, 34));

        runBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        isRunning.set(true);

        startTime = LocalDateTime.now();
        int totalTotalReqs = requests.size() * iterations;
        progressBar.setMaximum(totalTotalReqs);
        progressBar.setValue(0);

        currentRunDir = saveLogs ? getLogDir() : null;
        File runDir = currentRunDir;

        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger passedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        AtomicLong totalDuration = new AtomicLong(0);

        executorService = Executors.newFixedThreadPool(vusers);

        SwingWorker<Void, Object[]> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                PrintWriter logWriter = null;
                PrintWriter dumpWriter = null;
                if (runDir != null) {
                    try {
                        logWriter = new PrintWriter(
                                new FileWriter(new File(runDir, "summary.log"), StandardCharsets.UTF_8, true));
                        dumpWriter = new PrintWriter(
                                new FileWriter(new File(runDir, "dump.log"), StandardCharsets.UTF_8, true));
                        String startedStr = startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        logWriter.println("=== Execution Log ===");
                        logWriter.println("Collection Name: " + collection.getName());
                        logWriter.println("Collection ID: " + collection.getId());
                        logWriter.println("Runner: " + runnerModel.getName());
                        logWriter.println("Started: " + startedStr);
                        logWriter.println("Environment: " + (env != null ? env.getName() : "None"));
                        logWriter.println("Load Profile: Fixed (Threads: " + vusers + ")");
                        logWriter.println("Limit: Iterations = " + iterations);
                        logWriter.println("=================================================");

                        dumpWriter.println("=== Full Request/Response Dump ===");
                        dumpWriter.println("Collection Name: " + collection.getName());
                        dumpWriter.println("Started: " + startedStr);
                        dumpWriter.println("=================================================");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                final PrintWriter finalWriter = logWriter;
                final PrintWriter finalDumpWriter = dumpWriter;
                HttpClientWrapper client = new HttpClientWrapper();
                client.setSilentMode(true);

                for (int iter = 1; iter <= iterations && isRunning.get(); iter++) {
                    final int currentIter = iter;
                    for (RequestModel req : requests) {
                        if (!isRunning.get())
                            break;

                        executorService.submit(() -> {
                            if (!isRunning.get())
                                return;

                            long startMs = System.currentTimeMillis();
                            ResponseModel response = client.execute(req, env);
                            long dur = System.currentTimeMillis() - startMs;

                            int count = completedCount.incrementAndGet();
                            boolean ok = response.getStatusCode() >= 200 && response.getStatusCode() < 400;
                            if (ok)
                                passedCount.incrementAndGet();
                            else
                                failedCount.incrementAndGet();
                            totalDuration.addAndGet(dur);

                            runSamples.add(new RunSample(System.currentTimeMillis(), req.getName(), req.getMethod(),
                                    response.getStatusCode(), dur, ok));

                            String statusStr = response.getStatusCode() + " " + response.getStatusText();
                            publish(new Object[] { count, currentIter, req.getName(), req.getMethod(), statusStr, dur,
                                    response.getActualUrl() != null ? response.getActualUrl() : req.getUrl() });

                            // Calculate aggregate stats
                            String key = req.getName() + " [" + req.getMethod() + "]";
                            RequestStats stats = aggregateStatsMap.computeIfAbsent(key,
                                    k -> new RequestStats(req.getName(), req.getMethod()));
                            synchronized (stats) {
                                stats.latencies.add(dur);
                                stats.sizes.add(response.getSizeBytes());
                                if (ok)
                                    stats.successCount++;
                                else
                                    stats.failCount++;
                                stats.statusCodes.add(response.getStatusCode());

                                // Check format
                                String format = "Text";
                                Map<String, List<String>> hdrs = response.getHeaders();
                                if (hdrs != null) {
                                    List<String> contentTypes = hdrs.get("content-type");
                                    if (contentTypes != null && !contentTypes.isEmpty()) {
                                        String ct = contentTypes.get(0).toLowerCase();
                                        if (ct.contains("json"))
                                            format = "JSON";
                                        else if (ct.contains("xml"))
                                            format = "XML";
                                    }
                                }
                                stats.formats.add(format);
                            }

                            if (finalWriter != null) {
                                synchronized (finalWriter) {
                                    String logTime = java.time.LocalDateTime.now().format(
                                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                    finalWriter.printf("%s | %s %s | Status: %d | Latency: %dms%n",
                                            logTime, req.getMethod(), req.getName(), response.getStatusCode(), dur);
                                }
                            }
                            if (finalDumpWriter != null) {
                                synchronized (finalDumpWriter) {
                                    String logTime = java.time.LocalDateTime.now().format(
                                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                    finalDumpWriter.println("-------------------------------------------------");
                                    finalDumpWriter.println("Time: " + logTime);
                                    finalDumpWriter.println("Request Name: " + req.getName());
                                    finalDumpWriter.println("URL: " + req.getMethod() + " "
                                            + (response.getActualUrl() != null ? response.getActualUrl()
                                                    : req.getUrl()));

                                    finalDumpWriter.println("\n[Request Headers]");
                                    if (req.getHeaders() != null) {
                                        for (KeyValueItem kv : req.getHeaders()) {
                                            if (kv.isEnabled() && kv.getKey() != null && !kv.getKey().isBlank()) {
                                                finalDumpWriter.println(kv.getKey() + ": "
                                                        + (kv.getValue() != null ? kv.getValue() : ""));
                                            }
                                        }
                                    }

                                    finalDumpWriter.println("\n[Authorization]");
                                    String authType = req.getAuthType();
                                    if ("inherit".equalsIgnoreCase(authType) || authType == null
                                            || authType.isEmpty()) {
                                        authType = collection.getAuthType();
                                        finalDumpWriter.println("Inherited from Collection:");
                                    }
                                    finalDumpWriter.println("Type: " + (authType != null ? authType : "none"));
                                    if ("bearer".equalsIgnoreCase(authType)) {
                                        finalDumpWriter.println(
                                                "Token: " + (req.getAuthToken() != null && !req.getAuthToken().isEmpty()
                                                        ? req.getAuthToken()
                                                        : collection.getAuthToken()));
                                    } else if ("basic".equalsIgnoreCase(authType)) {
                                        finalDumpWriter.println("Username: "
                                                + (req.getAuthUsername() != null && !req.getAuthUsername().isEmpty()
                                                        ? req.getAuthUsername()
                                                        : collection.getAuthUsername()));
                                        finalDumpWriter.println("Password: "
                                                + (req.getAuthPassword() != null && !req.getAuthPassword().isEmpty()
                                                        ? req.getAuthPassword()
                                                        : collection.getAuthPassword()));
                                    } else if ("apiKey".equalsIgnoreCase(authType)) {
                                        finalDumpWriter.println("Key Name: "
                                                + (req.getAuthApiKeyName() != null && !req.getAuthApiKeyName().isEmpty()
                                                        ? req.getAuthApiKeyName()
                                                        : collection.getAuthApiKeyName()));
                                        finalDumpWriter.println("Key Value: " + (req.getAuthApiKeyValue() != null
                                                && !req.getAuthApiKeyValue().isEmpty() ? req.getAuthApiKeyValue()
                                                        : collection.getAuthApiKeyValue()));
                                        finalDumpWriter.println("Add To: "
                                                + (req.getAuthApiKeyIn() != null && !req.getAuthApiKeyIn().isEmpty()
                                                        ? req.getAuthApiKeyIn()
                                                        : collection.getAuthApiKeyIn()));
                                    }

                                    finalDumpWriter.println("\n[Request Body]");
                                    finalDumpWriter.println("Type: " + req.getBodyType());
                                    if ("raw".equalsIgnoreCase(req.getBodyType())) {
                                        finalDumpWriter.println(req.getBodyRawContent());
                                    } else if ("form-data".equalsIgnoreCase(req.getBodyType())
                                            && req.getFormData() != null) {
                                        for (KeyValueItem kv : req.getFormData()) {
                                            if (kv.isEnabled())
                                                finalDumpWriter.println(kv.getKey() + "=" + kv.getValue() + " (type="
                                                        + kv.getType() + ")");
                                        }
                                    } else if (("form".equalsIgnoreCase(req.getBodyType())
                                            || "x-www-form-urlencoded".equalsIgnoreCase(req.getBodyType()))) {
                                        List<KeyValueItem> items = req.getUrlencodedData();
                                        if (items == null || items.isEmpty())
                                            items = req.getFormData();
                                        if (items != null) {
                                            for (KeyValueItem kv : items) {
                                                if (kv.isEnabled())
                                                    finalDumpWriter.println(kv.getKey() + "=" + kv.getValue());
                                            }
                                        }
                                    }

                                    if (response.getRedirects() != null && !response.getRedirects().isEmpty()) {
                                        finalDumpWriter.println("\n[Redirect History]");
                                        for (int i = 0; i < response.getRedirects().size(); i++) {
                                            ResponseModel rm = response.getRedirects().get(i);
                                            finalDumpWriter.println("  Redirect " + (i + 1) + ": " + rm.getStatusCode()
                                                    + " " + rm.getStatusText());
                                            finalDumpWriter.println("  URL: " + rm.getActualUrl());
                                            if (rm.getHeaders() != null) {
                                                for (Map.Entry<String, List<String>> entry : rm.getHeaders()
                                                        .entrySet()) {
                                                    finalDumpWriter.println("  " + entry.getKey() + ": "
                                                            + String.join(", ", entry.getValue()));
                                                }
                                            }
                                            finalDumpWriter.println();
                                        }
                                    }

                                    finalDumpWriter.println("\n[Final Response Status]");
                                    finalDumpWriter.println(response.getStatusCode() + " " + response.getStatusText());
                                    finalDumpWriter.println("Latency: " + dur + "ms");
                                    finalDumpWriter.println("Size: " + response.getSizeBytes() + " bytes");
                                    finalDumpWriter.println("\nResponse Headers:");
                                    if (response.getHeaders() != null) {
                                        for (Map.Entry<String, List<String>> entry : response.getHeaders().entrySet()) {
                                            finalDumpWriter.println(
                                                    entry.getKey() + ": " + String.join(", ", entry.getValue()));
                                        }
                                    }
                                    finalDumpWriter.println("\nResponse Body:");
                                    finalDumpWriter.println(response.getBody());
                                    finalDumpWriter.println("-------------------------------------------------\n");
                                    finalDumpWriter.flush();
                                }
                            }

                            if (delay > 0) {
                                try {
                                    Thread.sleep(delay);
                                } catch (InterruptedException ignored) {
                                }
                            }
                        });
                    }
                }

                executorService.shutdown();
                try {
                    executorService.awaitTermination(2, TimeUnit.HOURS);
                } catch (InterruptedException ignored) {
                }

                if (finalWriter != null) {
                    synchronized (finalWriter) {
                        finalWriter.println("=================================================");
                        finalWriter.println("Finished: "
                                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    }
                    finalWriter.close();
                }
                if (finalDumpWriter != null) {
                    synchronized (finalDumpWriter) {
                        finalDumpWriter.println("=================================================");
                        finalDumpWriter.println("Finished: "
                                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    }
                    finalDumpWriter.close();
                }
                return null;
            }

            @Override
            protected void process(List<Object[]> chunks) {
                for (Object[] row : chunks) {
                    resultsTableModel.addRow(row);
                    progressBar.setValue((Integer) row[0]);
                }
                int done = completedCount.get();
                totalReqLabel.setText(String.valueOf(done));
                passedLabel.setText(String.valueOf(passedCount.get()));
                failedLabel.setText(String.valueOf(failedCount.get()));
                long avg = done > 0 ? totalDuration.get() / done : 0;
                avgTimeLabel.setText(avg + " ms");
                if (chartPanel != null) {
                    chartPanel.repaint();
                }
            }

            @Override
            protected void done() {
                endTime = LocalDateTime.now();
                isRunning.set(false);
                runBtn.setEnabled(true);
                stopBtn.setEnabled(false);

                if (failedCount.get() > 0) {
                    statusLabel.setText("Completed with Failures");
                    statusLabel.setForeground(new Color(231, 76, 60));
                } else {
                    statusLabel.setText("Passed");
                    statusLabel.setForeground(new Color(39, 174, 96));
                }

                updateAggregateReport();
                if (chartPanel != null) {
                    chartPanel.repaint();
                }

                if (currentRunDir != null && currentRunDir.exists()) {
                    try {
                        exportCSV(new File(currentRunDir, "metrics.csv"));
                        exportHTML(new File(currentRunDir, "metrics.html"));
                        exportExcel(new File(currentRunDir, "metrics.xlsx"));
                        exportPDF(new File(currentRunDir, "metrics.pdf"));
                    } catch (Exception ignored) {
                    }
                }
            }
        };
        worker.execute();
    }

    private void updateAggregateReport() {
        aggregateModel.setRowCount(0);
        if (aggregateStatsMap.isEmpty())
            return;

        List<Long> allLatencies = new ArrayList<>();
        List<Long> allSizes = new ArrayList<>();
        int totalPass = 0;
        int totalFail = 0;
        Set<Integer> allStatusCodes = new HashSet<>();
        Set<String> allFormats = new HashSet<>();

        for (RequestStats stats : aggregateStatsMap.values()) {
            synchronized (stats) {
                if (stats.latencies.isEmpty())
                    continue;
                List<Long> lats = new ArrayList<>(stats.latencies);
                Collections.sort(lats);
                List<Long> szs = new ArrayList<>(stats.sizes);
                Collections.sort(szs);

                allLatencies.addAll(lats);
                allSizes.addAll(szs);
                totalPass += stats.successCount;
                totalFail += stats.failCount;
                allStatusCodes.addAll(stats.statusCodes);
                allFormats.addAll(stats.formats);

                int samples = lats.size();
                long min = lats.get(0);
                long max = lats.get(samples - 1);
                long sum = 0;
                for (long l : lats)
                    sum += l;
                long avg = sum / samples;

                long p75 = lats.get((int) (samples * 0.75));
                long p90 = lats.get((int) (samples * 0.90));
                long p95 = lats.get((int) (samples * 0.95));
                long p99 = lats.get((int) (samples * 0.99));

                long sizeSum = 0;
                for (long s : szs)
                    sizeSum += s;
                long avgSize = sizeSum / samples;

                double succPercent = (double) stats.successCount / samples * 100.0;

                aggregateModel.addRow(new Object[] {
                        stats.name,
                        stats.method,
                        samples,
                        stats.statusCodes.toString().replaceAll("[\\[\\]]", ""),
                        avg,
                        min,
                        max,
                        p75,
                        p90,
                        p95,
                        p99,
                        avgSize,
                        stats.successCount,
                        stats.failCount,
                        String.format("%.1f%%", succPercent),
                        String.join(", ", stats.formats)
                });
            }
        }

        // Add Grand Total Row
        if (!allLatencies.isEmpty()) {
            Collections.sort(allLatencies);
            Collections.sort(allSizes);
            int samples = allLatencies.size();
            long min = allLatencies.get(0);
            long max = allLatencies.get(samples - 1);
            long sum = 0;
            for (long l : allLatencies)
                sum += l;
            long avg = sum / samples;

            long p75 = allLatencies.get((int) (samples * 0.75));
            long p90 = allLatencies.get((int) (samples * 0.90));
            long p95 = allLatencies.get((int) (samples * 0.95));
            long p99 = allLatencies.get((int) (samples * 0.99));

            long sizeSum = 0;
            for (long s : allSizes)
                sizeSum += s;
            long avgSize = sizeSum / samples;

            double succPercent = (double) totalPass / samples * 100.0;

            aggregateModel.addRow(new Object[] {
                    "TOTAL",
                    "-",
                    samples,
                    allStatusCodes.toString().replaceAll("[\\[\\]]", ""),
                    avg,
                    min,
                    max,
                    p75,
                    p90,
                    p95,
                    p99,
                    avgSize,
                    totalPass,
                    totalFail,
                    String.format("%.1f%%", succPercent),
                    String.join(", ", allFormats)
            });
        }
    }

    private void stopRun() {
        isRunning.set(false);
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        statusLabel.setText("Stopped");
        statusLabel.setForeground(new Color(231, 76, 60));
        runBtn.setEnabled(true);
        stopBtn.setEnabled(false);
    }

    private void importJmx() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import JMeter .jmx Plan");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                List<RequestModel> imported = JmxHelper.importJmx(chooser.getSelectedFile());
                if (imported.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No HTTP Samplers found in this JMX file.", "Info",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                collection.getRequests().addAll(imported);
                mainFrame.saveCollections();
                mainFrame.refreshCollections(mainFrame.getCollections());
                showToast(this, "Successfully imported " + imported.size() + " request(s) into collection!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "JMX Import failed: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportJmx() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Collection to JMeter .jmx");
        chooser.setSelectedFile(new File(sanitizeFilename(collection.getName()) + ".jmx"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                JmxHelper.exportJmx(collection, chooser.getSelectedFile());
                showToast(this, "Successfully exported collection to JMeter plan!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "JMX Export failed: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportReport(String format) {
        if (resultsTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No execution results to export.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        String datetime = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS"));
        String safeName = sanitizeFilename(collection.getName()) + "-" + datetime + "." + format;
        chooser.setSelectedFile(new File(safeName));
        chooser.setDialogTitle("Export Execution Report (" + format.toUpperCase() + ")");

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        File file = chooser.getSelectedFile();

        try {
            if ("pdf".equalsIgnoreCase(format)) {
                exportPDF(file);
            } else if ("xlsx".equalsIgnoreCase(format)) {
                exportExcel(file);
            } else if ("html".equalsIgnoreCase(format)) {
                exportHTML(file);
            } else if ("csv".equalsIgnoreCase(format)) {
                exportCSV(file);
            }
            showToast(this, "Report exported successfully to: " + file.getName());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to export report: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportLogs(String type) {
        if (currentRunDir == null || !currentRunDir.exists()) {
            JOptionPane.showMessageDialog(this,
                    "No logs were saved for this run. Ensure 'Save Logs' is checked before running.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        String datetime = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS"));
        String safeName = sanitizeFilename(collection.getName()) + "-" + datetime + "-" + type + ".log";
        chooser.setSelectedFile(new File(safeName));
        chooser.setDialogTitle("Export Logs (" + type.toUpperCase() + ")");

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        File file = chooser.getSelectedFile();

        try (PrintWriter pw = new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            if ("summary".equals(type) || "full".equals(type)) {
                File summaryFile = new File(currentRunDir, "summary.log");
                if (summaryFile.exists()) {
                    pw.println("================= SUMMARY LOG =================");
                    java.nio.file.Files.lines(summaryFile.toPath()).forEach(pw::println);
                }
            }
            if ("dump".equals(type) || "full".equals(type)) {
                File dumpFile = new File(currentRunDir, "dump.log");
                if (dumpFile.exists()) {
                    pw.println("\n================= DUMP LOG =================");
                    java.nio.file.Files.lines(dumpFile.toPath()).forEach(pw::println);
                }
            }
            showToast(this, "Logs exported successfully to: " + file.getName());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to export logs: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private byte[] getChartImageBytes(boolean isStatusCodes) throws Exception {
        BufferedImage img = new BufferedImage(800, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        Color bg = UIManager.getColor("Panel.background");
        g2.setColor(bg != null ? bg : Color.WHITE);
        g2.fillRect(0, 0, 800, 400);
        chartPanel.drawChart(g2, 800, 400, isStatusCodes);
        g2.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private void exportPDF(File file) throws Exception {
        com.lowagie.text.Document document = new com.lowagie.text.Document();
        com.lowagie.text.pdf.PdfWriter writer = com.lowagie.text.pdf.PdfWriter.getInstance(document,
                new FileOutputStream(file));

        writer.setPageEvent(new PDFBrandingEvent());

        document.open();

        boolean isDark = UIManager.getBoolean("FlatLaf.dark");
        Color pdfFg = isDark ? new Color(220, 220, 220) : Color.BLACK;

        Color accentColor = UIManager.getColor("AccentColor");
        if (accentColor == null)
            accentColor = new Color(26, 115, 232);

        // Title block
        com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18,
                com.lowagie.text.Font.BOLD, accentColor);
        com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("ApiBanker Collection Performance Metrics",
                titleFont);
        title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        title.setSpacingAfter(15);
        document.add(title);

        // Metadata / Load Profile
        com.lowagie.text.Font metaFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10,
                com.lowagie.text.Font.NORMAL, pdfFg);
        document.add(new com.lowagie.text.Paragraph("Collection: " + collection.getName(), metaFont));
        document.add(new com.lowagie.text.Paragraph("Runner Model: " + runnerModel.getName(), metaFont));
        document.add(new com.lowagie.text.Paragraph("Run Started: " + (startTime != null ? startTime.toString() : "-")
                + " | Finished: " + (endTime != null ? endTime.toString() : "-"), metaFont));
        document.add(new com.lowagie.text.Paragraph("Load Profile: Iterations=" + iterationsSpinner.getValue()
                + ", Concurrency=" + vusersSpinner.getValue() + ", Delay=" + delaySpinner.getValue() + "ms", metaFont));

        EnvironmentModel env = resolveSelectedEnvironment();
        document.add(
                new com.lowagie.text.Paragraph("Environment: " + (env != null ? env.getName() : "None"), metaFont));
        document.add(new com.lowagie.text.Paragraph(
                "Generated by: " + System.getProperty("user.name") + " at " + LocalDateTime.now(), metaFont));
        document.add(new com.lowagie.text.Paragraph("\n"));

        // Overview Summary Metrics (Grid/Table)
        com.lowagie.text.pdf.PdfPTable summaryTable = new com.lowagie.text.pdf.PdfPTable(4);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingBefore(10);
        summaryTable.setSpacingAfter(20);

        com.lowagie.text.Font cellHeaderFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11,
                com.lowagie.text.Font.BOLD, Color.WHITE);
        com.lowagie.text.Font cellValFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12,
                com.lowagie.text.Font.BOLD, pdfFg);

        String[] headers = { "Total Requests", "Passed", "Failed", "Avg Duration" };
        String[] vals = { totalReqLabel.getText(), passedLabel.getText(), failedLabel.getText(),
                avgTimeLabel.getText() };
        Color[] bgColors = { new Color(52, 152, 219), new Color(46, 204, 113), new Color(231, 76, 60),
                new Color(155, 89, 182) };

        for (int i = 0; i < 4; i++) {
            com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Paragraph(headers[i], cellHeaderFont));
            cell.setBackgroundColor(bgColors[i]);
            cell.setBorderColor(isDark ? new Color(68, 68, 68) : Color.LIGHT_GRAY);
            cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            cell.setPadding(8);
            summaryTable.addCell(cell);
        }

        for (int i = 0; i < 4; i++) {
            com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Paragraph(vals[i], cellValFont));
            cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            cell.setBackgroundColor(isDark ? new Color(30, 31, 34) : Color.WHITE);
            cell.setBorderColor(isDark ? new Color(68, 68, 68) : Color.LIGHT_GRAY);
            cell.setPadding(10);
            summaryTable.addCell(cell);
        }
        document.add(summaryTable);

        // Add visual charts
        com.lowagie.text.Font sectionFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 14,
                com.lowagie.text.Font.BOLD, pdfFg);
        document.add(new com.lowagie.text.Paragraph("Performance Analytics Charts", sectionFont));
        document.add(new com.lowagie.text.Paragraph("\n"));

        // Chart 1: Status Codes
        byte[] codeChartBytes = getChartImageBytes(true);
        com.lowagie.text.Image codeImg = com.lowagie.text.Image.getInstance(codeChartBytes);
        codeImg.scalePercent(60);
        codeImg.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        document.add(new com.lowagie.text.Paragraph("HTTP Status Codes Distribution:", metaFont));
        document.add(codeImg);
        document.add(new com.lowagie.text.Paragraph("\n"));

        // Chart 2: Latencies
        byte[] latencyChartBytes = getChartImageBytes(false);
        com.lowagie.text.Image latImg = com.lowagie.text.Image.getInstance(latencyChartBytes);
        latImg.scalePercent(60);
        latImg.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        document.add(new com.lowagie.text.Paragraph("Response Latency Distribution:", metaFont));
        document.add(latImg);

        // Force new page for Aggregate Table
        document.newPage();

        document.add(new com.lowagie.text.Paragraph("Aggregate Execution Summary", sectionFont));
        document.add(new com.lowagie.text.Paragraph("\n"));

        com.lowagie.text.pdf.PdfPTable aggTable = new com.lowagie.text.pdf.PdfPTable(8);
        aggTable.setWidthPercentage(100);
        aggTable.setWidths(new float[] { 3f, 1f, 1.2f, 1.5f, 1.2f, 1f, 1f, 1.2f });

        com.lowagie.text.Font tableHeaderFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9,
                com.lowagie.text.Font.BOLD, pdfFg);
        com.lowagie.text.Font tableRowFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8,
                com.lowagie.text.Font.NORMAL, pdfFg);
        com.lowagie.text.Font totalRowFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8,
                com.lowagie.text.Font.BOLD, pdfFg);

        String[] colHeaders = { "Name", "Method", "Samples", "Codes", "Avg", "Min", "Max", "Success %" };
        for (String colHeader : colHeaders) {
            com.lowagie.text.pdf.PdfPCell hCell = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Paragraph(colHeader, tableHeaderFont));
            hCell.setBackgroundColor(isDark ? new Color(30, 31, 34) : new Color(240, 240, 240));
            hCell.setBorderColor(isDark ? new Color(68, 68, 68) : Color.LIGHT_GRAY);
            hCell.setPadding(5);
            aggTable.addCell(hCell);
        }

        for (int i = 0; i < aggregateModel.getRowCount(); i++) {
            boolean isTotal = "TOTAL".equals(aggregateModel.getValueAt(i, 0));
            com.lowagie.text.Font rowF = isTotal ? totalRowFont : tableRowFont;

            for (int j = 0; j < 8; j++) {
                int colIdx = j == 7 ? 14 : j; // Map success %
                com.lowagie.text.pdf.PdfPCell dCell = new com.lowagie.text.pdf.PdfPCell(
                        new com.lowagie.text.Paragraph(String.valueOf(aggregateModel.getValueAt(i, colIdx)), rowF));
                dCell.setBorderColor(isDark ? new Color(68, 68, 68) : Color.LIGHT_GRAY);
                if (isDark)
                    dCell.setBackgroundColor(new Color(43, 45, 49));
                aggTable.addCell(dCell);
            }
        }

        document.add(aggTable);
        document.close();
    }

    private void exportExcel(File file) throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook();

        XSSFSheet summarySheet = workbook.createSheet("Dashboard");
        summarySheet.setDisplayGridlines(true);

        Row titleRow = summarySheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("ApiBanker PERFORMANCE RUNNER REPORT");
        CellStyle titleStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        titleStyle.setFont(font);
        titleCell.setCellStyle(titleStyle);

        Row r1 = summarySheet.createRow(2);
        r1.createCell(0).setCellValue("Collection:");
        r1.createCell(1).setCellValue(collection.getName());

        Row r2 = summarySheet.createRow(3);
        r2.createCell(0).setCellValue("Runner Model:");
        r2.createCell(1).setCellValue(runnerModel.getName());

        Row r3 = summarySheet.createRow(4);
        r3.createCell(0).setCellValue("Run Started:");
        r3.createCell(1).setCellValue(startTime != null ? startTime.toString() : "-");
        r3.createCell(2).setCellValue("Run Finished:");
        r3.createCell(3).setCellValue(endTime != null ? endTime.toString() : "-");

        Row r4 = summarySheet.createRow(5);
        r4.createCell(0).setCellValue("Load Profile:");
        r4.createCell(1).setCellValue("Iterations=" + iterationsSpinner.getValue() + ", Concurrency="
                + vusersSpinner.getValue() + ", Delay=" + delaySpinner.getValue() + "ms");

        Row r5 = summarySheet.createRow(6);
        r5.createCell(0).setCellValue("Generated At:");
        r5.createCell(1).setCellValue(LocalDateTime.now().toString());
        r5.createCell(2).setCellValue("User:");
        r5.createCell(3).setCellValue(System.getProperty("user.name"));

        Row r6 = summarySheet.createRow(7);
        r6.createCell(0).setCellValue("Branding:");
        r6.createCell(1).setCellValue("SLPRO ApiBanker Studio (from SL Pro)");

        Row headerRow = summarySheet.createRow(9);
        Row valRow = summarySheet.createRow(10);

        String[] summaryHeaders = { "Total Requests", "Passed", "Failed", "Avg Duration" };
        String[] summaryVals = { totalReqLabel.getText(), passedLabel.getText(), failedLabel.getText(),
                avgTimeLabel.getText() };
        for (int i = 0; i < 4; i++) {
            headerRow.createCell(i).setCellValue(summaryHeaders[i]);
            valRow.createCell(i).setCellValue(summaryVals[i]);
        }

        byte[] codeChartBytes = getChartImageBytes(true);
        byte[] latencyChartBytes = getChartImageBytes(false);

        int codeIdx = workbook.addPicture(codeChartBytes, Workbook.PICTURE_TYPE_PNG);
        int latIdx = workbook.addPicture(latencyChartBytes, Workbook.PICTURE_TYPE_PNG);

        Drawing<?> drawing = summarySheet.createDrawingPatriarch();

        ClientAnchor codeAnchor = workbook.getCreationHelper().createClientAnchor();
        codeAnchor.setCol1(0);
        codeAnchor.setRow1(12);
        codeAnchor.setCol2(8);
        codeAnchor.setRow2(28);
        drawing.createPicture(codeAnchor, codeIdx);

        ClientAnchor latAnchor = workbook.getCreationHelper().createClientAnchor();
        latAnchor.setCol1(9);
        latAnchor.setRow1(12);
        latAnchor.setCol2(17);
        latAnchor.setRow2(28);
        drawing.createPicture(latAnchor, latIdx);

        XSSFSheet aggSheet = workbook.createSheet("Aggregate Summary");
        aggSheet.setDisplayGridlines(true);

        Row aggHeader = aggSheet.createRow(0);
        for (int j = 0; j < aggregateModel.getColumnCount(); j++) {
            aggHeader.createCell(j).setCellValue(aggregateModel.getColumnName(j));
        }

        for (int i = 0; i < aggregateModel.getRowCount(); i++) {
            Row row = aggSheet.createRow(i + 1);
            for (int j = 0; j < aggregateModel.getColumnCount(); j++) {
                Object val = aggregateModel.getValueAt(i, j);
                if (val instanceof Number) {
                    row.createCell(j).setCellValue(((Number) val).doubleValue());
                } else {
                    row.createCell(j).setCellValue(String.valueOf(val));
                }
            }
        }

        XSSFSheet logSheet = workbook.createSheet("Execution Log");
        logSheet.setDisplayGridlines(true);

        Row logHeader = logSheet.createRow(0);
        for (int j = 0; j < resultsTableModel.getColumnCount(); j++) {
            logHeader.createCell(j).setCellValue(resultsTableModel.getColumnName(j));
        }

        for (int i = 0; i < resultsTableModel.getRowCount(); i++) {
            Row row = logSheet.createRow(i + 1);
            for (int j = 0; j < resultsTableModel.getColumnCount(); j++) {
                Object val = resultsTableModel.getValueAt(i, j);
                if (val instanceof Number) {
                    row.createCell(j).setCellValue(((Number) val).doubleValue());
                } else {
                    row.createCell(j).setCellValue(String.valueOf(val));
                }
            }
        }

        for (int j = 0; j < aggregateModel.getColumnCount(); j++) {
            aggSheet.autoSizeColumn(j);
        }
        for (int j = 0; j < resultsTableModel.getColumnCount(); j++) {
            logSheet.autoSizeColumn(j);
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        }
        workbook.close();
    }

    private void exportHTML(File file) throws Exception {
        byte[] codeChart = getChartImageBytes(true);
        byte[] latencyChart = getChartImageBytes(false);
        String codeBase64 = java.util.Base64.getEncoder().encodeToString(codeChart);
        String latencyBase64 = java.util.Base64.getEncoder().encodeToString(latencyChart);

        try (PrintWriter pw = new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            EnvironmentModel env = resolveSelectedEnvironment();
            String sysUser = System.getProperty("user.name");
            String genAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            boolean isDark = UIManager.getBoolean("FlatLaf.dark");
            String bg = isDark ? "#2b2d31" : "#f8f9fa";
            String fg = isDark ? "#e0e0e0" : "#333";
            String cardBg = isDark ? "#313338" : "#fff";
            String thBg = isDark ? "#2b2d31" : "#f1f3f5";
            String totalBg = isDark ? "#404249" : "#eaeded";
            String border = isDark ? "#444" : "#eee";

            Color accentColor = UIManager.getColor("AccentColor");
            if (accentColor == null)
                accentColor = new Color(26, 115, 232);
            String accentHex = String.format("#%02x%02x%02x", accentColor.getRed(), accentColor.getGreen(),
                    accentColor.getBlue());

            pw.println(
                    "<!DOCTYPE html><html><head><title>ApiBanker Execution Report - " + collection.getName() + "</title>");
            pw.println("<style>body{font-family:'Segoe UI',sans-serif;margin:20px;background:" + bg + ";color:" + fg
                    + ";}");
            pw.println(
                    ".header{background:" + cardBg
                            + ";padding:20px;border-radius:8px;box-shadow:0 2px 4px rgba(0,0,0,0.1);margin-bottom:20px;border-top: 4px solid "
                            + accentHex + ";}");
            pw.println(".metrics{display:flex;gap:15px;margin-bottom:20px;}");
            pw.println(
                    ".card{background:" + cardBg
                            + ";padding:15px;border-radius:6px;flex:1;box-shadow:0 1px 3px rgba(0,0,0,0.1);}");
            pw.println(
                    "table{width:100%;border-collapse:collapse;background:" + cardBg
                            + ";box-shadow:0 1px 3px rgba(0,0,0,0.1);margin-bottom:30px;}");
            pw.println(
                    "th,td{padding:10px;text-align:left;border-bottom:1px solid " + border + ";}th{background:" + thBg
                            + ";} tr.total{font-weight:bold;background:" + totalBg + ";}</style></head><body>");

            pw.println(
                    "<div style='text-align:right; font-size:12px; color:#888; margin-bottom:10px;'>SLPRO ApiBanker Studio &copy; 2026</div>");

            pw.println("<div class='header'><h2 style='color:" + accentHex
                    + "; margin-top:0;'>ApiBanker Collection Performance Metrics</h2>");
            pw.println("<p><b>Collection:</b> " + collection.getName() + " | <b>Runner:</b> " + runnerModel.getName()
                    + " | <b>Run By:</b> " + sysUser + "</p>");
            pw.println("<p><b>Started At:</b> " + (startTime != null ? startTime : "-") + " | <b>Finished At:</b> "
                    + (endTime != null ? endTime : "-") + "</p>");
            pw.println("<p><b>Environment:</b> " + (env != null ? env.getName() : "None")
                    + " | <b>Report Generated At:</b> " + genAt + "</p>");
            pw.println("<p><b>Load Profile:</b> Iterations=" + iterationsSpinner.getValue() + ", Concurrent Users="
                    + vusersSpinner.getValue() + ", Delay=" + delaySpinner.getValue() + "ms</p></div>");

            pw.println("<div class='metrics'>");
            pw.println(
                    "<div class='card'><h4>Total Requests</h4><p style='font-size:20px;font-weight:bold;color:#3498db;'>"
                            + totalReqLabel.getText() + "</p></div>");
            pw.println("<div class='card'><h4>Passed</h4><p style='font-size:20px;font-weight:bold;color:#2ecc71;'>"
                    + passedLabel.getText() + "</p></div>");
            pw.println("<div class='card'><h4>Failed</h4><p style='font-size:20px;font-weight:bold;color:#e74c3c;'>"
                    + failedLabel.getText() + "</p></div>");
            pw.println(
                    "<div class='card'><h4>Avg Duration</h4><p style='font-size:20px;font-weight:bold;color:#9b59b6;'>"
                            + avgTimeLabel.getText() + "</p></div></div>");

            pw.println("<h3>Performance Analytics Charts</h3>");
            pw.println("<div style='display:flex; gap:15px; margin-bottom:20px;'>");
            pw.println(
                    "<div class='card' style='text-align:center;'><h4>HTTP Status Codes</h4><img src='data:image/png;base64,"
                            + codeBase64
                            + "' style='max-width:100%; border:1px solid #eee; border-radius:4px;'/></div>");
            pw.println(
                    "<div class='card' style='text-align:center;'><h4>Response Latency</h4><img src='data:image/png;base64,"
                            + latencyBase64
                            + "' style='max-width:100%; border:1px solid #eee; border-radius:4px;'/></div>");
            pw.println("</div>");

            pw.println("<h3>Aggregate Summary</h3>");
            pw.println(
                    "<table><thead><tr><th>API Name</th><th>Method</th><th>Samples</th><th>StatusCodes</th><th>Avg (ms)</th><th>Min (ms)</th><th>Max (ms)</th><th>p75</th><th>p90</th><th>p95</th><th>p99</th><th>Avg Size(B)</th><th>Pass</th><th>Fail</th><th>Success %</th><th>Format</th></tr></thead><tbody>");
            for (int i = 0; i < aggregateModel.getRowCount(); i++) {
                boolean isTotal = "TOTAL".equals(aggregateModel.getValueAt(i, 0));
                pw.println(isTotal ? "<tr class='total'>" : "<tr>");
                for (int j = 0; j < aggregateModel.getColumnCount(); j++) {
                    pw.println("<td>" + aggregateModel.getValueAt(i, j) + "</td>");
                }
                pw.println("</tr>");
            }
            pw.println("</tbody></table>");

            pw.println("<h3>Execution Log Detail</h3>");
            pw.println(
                    "<table><thead><tr><th>#</th><th>Iteration</th><th>Request</th><th>Method</th><th>Status</th><th>Duration (ms)</th><th>URL</th></tr></thead><tbody>");
            for (int i = 0; i < resultsTableModel.getRowCount(); i++) {
                pw.println("<tr>");
                for (int j = 0; j < resultsTableModel.getColumnCount(); j++) {
                    pw.println("<td>" + resultsTableModel.getValueAt(i, j) + "</td>");
                }
                pw.println("</tr>");
            }
            pw.println("</tbody></table>");

            pw.println(
                    "<div style='text-align:center; font-size:12px; color:#888; margin-top:30px; padding-top:15px; border-top:1px solid #eee;'>Confidential - Generated by ApiBanker - from SL Pro</div>");
            pw.println("</body></html>");
        }
    }

    private void exportCSV(File file) throws Exception {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            pw.println("\"ApiBanker Performance Runner Report - from SL Pro\"");
            pw.println("\"Collection:\",\"" + collection.getName() + "\"");
            pw.println("\"Runner Model:\",\"" + runnerModel.getName() + "\"");
            pw.println("\"Generated At:\",\"" + LocalDateTime.now().toString() + "\"");
            pw.println();
            pw.println(
                    "API Name,Method,Samples,StatusCodes,Avg (ms),Min (ms),Max (ms),p75,p90,p95,p99,Avg Size(B),Pass,Fail,Success %,Format");
            for (int i = 0; i < aggregateModel.getRowCount(); i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < aggregateModel.getColumnCount(); j++) {
                    if (j > 0)
                        sb.append(",");
                    sb.append("\"").append(aggregateModel.getValueAt(i, j)).append("\"");
                }
                pw.println(sb.toString());
            }
        }
    }

    private static class PDFBrandingEvent extends com.lowagie.text.pdf.PdfPageEventHelper {
        @Override
        public void onStartPage(com.lowagie.text.pdf.PdfWriter writer, com.lowagie.text.Document document) {
            boolean isDark = UIManager.getBoolean("FlatLaf.dark");
            if (isDark) {
                com.lowagie.text.pdf.PdfContentByte cb = writer.getDirectContentUnder();
                cb.saveState();
                cb.setColorFill(new Color(43, 45, 49));
                cb.rectangle(0, 0, document.getPageSize().getWidth(), document.getPageSize().getHeight());
                cb.fill();
                cb.restoreState();
            }
        }

        @Override
        public void onEndPage(com.lowagie.text.pdf.PdfWriter writer, com.lowagie.text.Document document) {
            com.lowagie.text.pdf.PdfContentByte cb = writer.getDirectContent();
            cb.saveState();

            cb.beginText();
            try {
                com.lowagie.text.pdf.BaseFont bf = com.lowagie.text.pdf.BaseFont.createFont(
                        com.lowagie.text.pdf.BaseFont.HELVETICA_BOLD, com.lowagie.text.pdf.BaseFont.CP1252,
                        com.lowagie.text.pdf.BaseFont.NOT_EMBEDDED);
                cb.setFontAndSize(bf, 8);
                Color accentColor = UIManager.getColor("AccentColor");
                cb.setColorFill((accentColor != null) ? accentColor : new Color(26, 115, 232));
                cb.showTextAligned(com.lowagie.text.pdf.PdfContentByte.ALIGN_LEFT, "ApiBanker Performance Metrics",
                        document.left(), document.top() + 10, 0);

                com.lowagie.text.pdf.BaseFont bf2 = com.lowagie.text.pdf.BaseFont.createFont(
                        com.lowagie.text.pdf.BaseFont.HELVETICA, com.lowagie.text.pdf.BaseFont.CP1252,
                        com.lowagie.text.pdf.BaseFont.NOT_EMBEDDED);
                cb.setFontAndSize(bf2, 8);
                cb.setColorFill(Color.GRAY);
                cb.showTextAligned(com.lowagie.text.pdf.PdfContentByte.ALIGN_RIGHT, "SL Pro", document.right(),
                        document.top() + 10, 0);
            } catch (Exception ignored) {
            }
            cb.endText();

            cb.setLineWidth(0.5f);
            cb.setColorStroke(Color.LIGHT_GRAY);
            cb.moveTo(document.left(), document.top() + 5);
            cb.lineTo(document.right(), document.top() + 5);
            cb.stroke();

            cb.setLineWidth(0.5f);
            cb.moveTo(document.left(), document.bottom() - 5);
            cb.lineTo(document.right(), document.bottom() - 5);
            cb.stroke();

            cb.beginText();
            try {
                com.lowagie.text.pdf.BaseFont bf = com.lowagie.text.pdf.BaseFont.createFont(
                        com.lowagie.text.pdf.BaseFont.HELVETICA, com.lowagie.text.pdf.BaseFont.CP1252,
                        com.lowagie.text.pdf.BaseFont.NOT_EMBEDDED);
                cb.setFontAndSize(bf, 8);
                cb.setColorFill(Color.GRAY);
                cb.showTextAligned(com.lowagie.text.pdf.PdfContentByte.ALIGN_LEFT,
                        "Confidential - Generated by ApiBanker - from SL Pro", document.left(), document.bottom() - 15, 0);
                cb.showTextAligned(com.lowagie.text.pdf.PdfContentByte.ALIGN_RIGHT, "Page " + writer.getPageNumber(),
                        document.right(), document.bottom() - 15, 0);
            } catch (Exception ignored) {
            }
            cb.endText();

            cb.restoreState();
        }
    }

    public void updateFontSize(int size) {
        if (metricsPanel != null) {
            metricsPanel.setPreferredSize(new Dimension(0, Math.max(65, size * 4 + 10)));
        }
        if (progressBar != null) {
            progressBar.setPreferredSize(new Dimension(0, Math.max(22, size + 8)));
        }
        FontScaleHelper.scaleFonts(this, size);
        revalidate();
        repaint();
    }

    public static void showToast(Component parent, String message) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        if (window == null)
            return;

        JWindow toast = new JWindow(window);
        toast.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(33, 33, 33, 220)); // Semi-transparent dark background
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel label = new JLabel(message);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(label, BorderLayout.CENTER);
        toast.add(panel);
        toast.pack();

        Point parentPos = window.getLocationOnScreen();
        int x = parentPos.x + (window.getWidth() - toast.getWidth()) / 2;
        int y = parentPos.y + window.getHeight() - toast.getHeight() - 80;
        toast.setLocation(x, y);

        toast.setVisible(true);

        new Thread(() -> {
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
            }
            SwingUtilities.invokeLater(() -> {
                toast.dispose();
            });
        }).start();
    }

    private void collectRequestsRecursive(CollectionModel col, List<RequestModel> list) {
        if (col.getRequests() != null) {
            for (RequestModel req : col.getRequests()) {
                if (req.getType() == null || "request".equalsIgnoreCase(req.getType())) {
                    list.add(req);
                }
            }
        }
        if (col.getFolders() != null) {
            for (CollectionModel folder : col.getFolders()) {
                collectRequestsRecursive(folder, list);
            }
        }
    }

    public RequestModel getRequestModel() {
        saveConfig();
        return runnerModel;
    }
}


