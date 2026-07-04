package in.slpro.japi.ui;

import in.slpro.japi.http.HttpClientWrapper;
import in.slpro.japi.http.JmxHelper;
import in.slpro.japi.model.*;
import in.slpro.japi.storage.StorageManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
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

public class CollectionRunnerPanel extends JPanel {
    private final MainFrame mainFrame;
    private final CollectionModel collection;
    private final RequestModel runnerModel;

    // UI Components
    private JSpinner iterationsSpinner;
    private JSpinner delaySpinner;
    private JSpinner vusersSpinner;
    private JCheckBox saveLogsCheck;
    private JComboBox<String> envSelectCombo;
    private JButton runBtn;
    private JButton stopBtn;

    private JProgressBar progressBar;
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

    public CollectionRunnerPanel(MainFrame mainFrame, CollectionModel collection, RequestModel runnerModel) {
        this.mainFrame = mainFrame;
        this.collection = collection;
        this.runnerModel = runnerModel;

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        initUI();
        loadConfig();

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
        configPanel.setBackground(new Color(248, 249, 250));
        configPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
                new EmptyBorder(10, 15, 10, 15)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Iterations
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        configPanel.add(new JLabel("Iterations:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.2;
        iterationsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));
        configPanel.add(iterationsSpinner, gbc);

        // Delay ms
        gbc.gridx = 2; gbc.weightx = 0;
        configPanel.add(new JLabel("Delay (ms):"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.2;
        delaySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 60000, 50));
        configPanel.add(delaySpinner, gbc);

        // Virtual Users / Concurrency
        gbc.gridx = 4; gbc.weightx = 0;
        configPanel.add(new JLabel("Concurrent Users:"), gbc);
        gbc.gridx = 5; gbc.weightx = 0.2;
        vusersSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        configPanel.add(vusersSpinner, gbc);

        // Environment
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        configPanel.add(new JLabel("Environment:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 0.5;
        envSelectCombo = new JComboBox<>();
        refreshEnvCombo();
        configPanel.add(envSelectCombo, gbc);

        // Save logs checkbox
        gbc.gridx = 4; gbc.gridwidth = 2; gbc.weightx = 0.3;
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

        JButton reportBtn = new JButton("Export Report");
        reportBtn.addActionListener(e -> exportReport());

        controlPanel.add(importJmxBtn);
        controlPanel.add(exportJmxBtn);
        controlPanel.add(reportBtn);
        controlPanel.add(stopBtn);
        controlPanel.add(runBtn);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 6; gbc.weightx = 1.0;
        configPanel.add(controlPanel, gbc);

        add(configPanel, BorderLayout.NORTH);

        // Center: Metrics + Table
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
        centerPanel.setBackground(Color.WHITE);

        // Summary Cards
        JPanel metricsPanel = new JPanel(new GridLayout(1, 5, 10, 0));
        metricsPanel.setBackground(Color.WHITE);
        metricsPanel.setPreferredSize(new Dimension(0, 65));

        totalReqLabel = createMetricCard(metricsPanel, "Total Requests", "0", new Color(52, 152, 219));
        passedLabel = createMetricCard(metricsPanel, "Passed", "0", new Color(39, 174, 96));
        failedLabel = createMetricCard(metricsPanel, "Failed", "0", new Color(231, 76, 60));
        avgTimeLabel = createMetricCard(metricsPanel, "Avg Duration", "0 ms", new Color(155, 89, 182));
        statusLabel = createMetricCard(metricsPanel, "Runner Status", "Idle", new Color(127, 140, 141));

        centerPanel.add(metricsPanel, BorderLayout.NORTH);

        // Tabs for Results Table vs Aggregate Report
        JTabbedPane runnerTabs = new JTabbedPane();
        runnerTabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // 1. Raw Execution Table
        resultsTableModel = new DefaultTableModel(new String[]{"#", "Iteration", "Request", "Method", "Status", "Duration (ms)", "URL"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
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
        aggregateModel = new DefaultTableModel(new String[]{
                "API Name", "Method", "Samples", "StatusCodes", "Avg (ms)", "Min (ms)", "Max (ms)",
                "p75 (ms)", "p90 (ms)", "p95 (ms)", "p99 (ms)", "Avg Size(B)", "Pass", "Fail", "Success %", "Format"
        }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        aggregateTable = new JTable(aggregateModel);
        aggregateTable.setRowHeight(24);
        JScrollPane aggregateScroll = new JScrollPane(aggregateTable);
        runnerTabs.addTab("Aggregate Summary", aggregateScroll);

        centerPanel.add(runnerTabs, BorderLayout.CENTER);

        // Bottom progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(0, 22));
        centerPanel.add(progressBar, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);
    }

    private JLabel createMetricCard(JPanel parent, String title, String initialVal, Color color) {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBackground(new Color(245, 247, 250));
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
                com.google.gson.JsonObject cfg = com.google.gson.JsonParser.parseString(runnerModel.getBodyRawContent()).getAsJsonObject();
                if (cfg.has("iterations")) iterationsSpinner.setValue(cfg.get("iterations").getAsInt());
                if (cfg.has("delay")) delaySpinner.setValue(cfg.get("delay").getAsInt());
                if (cfg.has("vusers")) vusersSpinner.setValue(cfg.get("vusers").getAsInt());
                if (cfg.has("saveLogs")) saveLogsCheck.setSelected(cfg.get("saveLogs").getAsBoolean());
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
            } catch (Exception ignored) {}
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
        
        runnerModel.setBodyRawContent(cfg.toString());
    }

    private void autoSave() {
        if (!isInitializing) {
            saveConfig();
            mainFrame.saveCollections();
        }
    }

    private String sanitizeFilename(String name) {
        if (name == null) return "runner";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }

    private File getLogFile() {
        String dataDir = StorageManager.getInstance().getSettings().getDataDirectory();
        File logsFolder = new File(dataDir, "logs");
        if (!logsFolder.exists()) logsFolder.mkdirs();

        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String cleanColl = sanitizeFilename(collection.getName());
        String cleanRunner = sanitizeFilename(runnerModel.getName());

        int inc = 1;
        File file;
        do {
            file = new File(logsFolder, String.format("%s-%s-%s-%d.log", dateStr, cleanColl, cleanRunner, inc));
            inc++;
        } while (file.exists());

        return file;
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

        List<RequestModel> requests = collection.getRequests().stream()
                .filter(r -> !"runner".equalsIgnoreCase(r.getType()))
                .toList();

        if (requests.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No executable requests in collection.", "Warning", JOptionPane.WARNING_MESSAGE);
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

        File logFile = saveLogs ? getLogFile() : null;

        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger passedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        AtomicLong totalDuration = new AtomicLong(0);

        executorService = Executors.newFixedThreadPool(vusers);

        SwingWorker<Void, Object[]> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                PrintWriter logWriter = null;
                if (logFile != null) {
                    try {
                        logWriter = new PrintWriter(new FileWriter(logFile, StandardCharsets.UTF_8, true));
                        String startedStr = startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        logWriter.println("=== Execution Log ===");
                        logWriter.println("Collection: " + collection.getName());
                        logWriter.println("Runner: " + runnerModel.getName());
                        logWriter.println("Started: " + startedStr);
                        logWriter.println("Environment: " + (env != null ? env.getName() : "None"));
                        logWriter.println("Load Profile: Fixed (Threads: " + vusers + ")");
                        logWriter.println("Limit: Iterations = " + iterations);
                        logWriter.println("=================================================");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                final PrintWriter finalWriter = logWriter;
                HttpClientWrapper client = new HttpClientWrapper();
                client.setSilentMode(true);

                for (int iter = 1; iter <= iterations && isRunning.get(); iter++) {
                    final int currentIter = iter;
                    for (RequestModel req : requests) {
                        if (!isRunning.get()) break;

                        executorService.submit(() -> {
                            if (!isRunning.get()) return;

                            long startMs = System.currentTimeMillis();
                            ResponseModel response = client.execute(req, env);
                            long dur = System.currentTimeMillis() - startMs;

                            int count = completedCount.incrementAndGet();
                            boolean ok = response.getStatusCode() >= 200 && response.getStatusCode() < 400;
                            if (ok) passedCount.incrementAndGet(); else failedCount.incrementAndGet();
                            totalDuration.addAndGet(dur);

                            String statusStr = response.getStatusCode() + " " + response.getStatusText();
                            publish(new Object[]{count, currentIter, req.getName(), req.getMethod(), statusStr, dur, response.getActualUrl() != null ? response.getActualUrl() : req.getUrl()});

                            // Calculate aggregate stats
                            String key = req.getName() + " [" + req.getMethod() + "]";
                            RequestStats stats = aggregateStatsMap.computeIfAbsent(key, k -> new RequestStats(req.getName(), req.getMethod()));
                            synchronized (stats) {
                                stats.latencies.add(dur);
                                stats.sizes.add(response.getSizeBytes());
                                if (ok) stats.successCount++; else stats.failCount++;
                                stats.statusCodes.add(response.getStatusCode());

                                // Check format
                                String format = "Text";
                                Map<String, List<String>> hdrs = response.getHeaders();
                                if (hdrs != null) {
                                    List<String> contentTypes = hdrs.get("content-type");
                                    if (contentTypes != null && !contentTypes.isEmpty()) {
                                        String ct = contentTypes.get(0).toLowerCase();
                                        if (ct.contains("json")) format = "JSON";
                                        else if (ct.contains("xml")) format = "XML";
                                    }
                                }
                                stats.formats.add(format);
                            }

                            if (finalWriter != null) {
                                synchronized (finalWriter) {
                                    String logTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                    finalWriter.printf("%s | %s %s | Status: %d | Latency: %dms%n",
                                            logTime, req.getMethod(), req.getName(), response.getStatusCode(), dur);
                                }
                            }

                            if (delay > 0) {
                                try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
                            }
                        });
                    }
                }

                executorService.shutdown();
                try {
                    executorService.awaitTermination(2, TimeUnit.HOURS);
                } catch (InterruptedException ignored) {}

                if (finalWriter != null) {
                    synchronized (finalWriter) {
                        finalWriter.println("=================================================");
                        finalWriter.println("Finished: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    }
                    finalWriter.close();
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
            }
        };
        worker.execute();
    }

    private void updateAggregateReport() {
        aggregateModel.setRowCount(0);
        if (aggregateStatsMap.isEmpty()) return;

        List<Long> allLatencies = new ArrayList<>();
        List<Long> allSizes = new ArrayList<>();
        int totalPass = 0;
        int totalFail = 0;
        Set<Integer> allStatusCodes = new HashSet<>();
        Set<String> allFormats = new HashSet<>();

        for (RequestStats stats : aggregateStatsMap.values()) {
            synchronized (stats) {
                if (stats.latencies.isEmpty()) continue;
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
                for (long l : lats) sum += l;
                long avg = sum / samples;

                long p75 = lats.get((int) (samples * 0.75));
                long p90 = lats.get((int) (samples * 0.90));
                long p95 = lats.get((int) (samples * 0.95));
                long p99 = lats.get((int) (samples * 0.99));

                long sizeSum = 0;
                for (long s : szs) sizeSum += s;
                long avgSize = sizeSum / samples;

                double succPercent = (double) stats.successCount / samples * 100.0;

                aggregateModel.addRow(new Object[]{
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
            for (long l : allLatencies) sum += l;
            long avg = sum / samples;

            long p75 = allLatencies.get((int) (samples * 0.75));
            long p90 = allLatencies.get((int) (samples * 0.90));
            long p95 = allLatencies.get((int) (samples * 0.95));
            long p99 = allLatencies.get((int) (samples * 0.99));

            long sizeSum = 0;
            for (long s : allSizes) sizeSum += s;
            long avgSize = sizeSum / samples;

            double succPercent = (double) totalPass / samples * 100.0;

            aggregateModel.addRow(new Object[]{
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
                    JOptionPane.showMessageDialog(this, "No HTTP Samplers found in this JMX file.", "Info", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                collection.getRequests().addAll(imported);
                mainFrame.saveCollections();
                mainFrame.refreshCollections(mainFrame.getCollections());
                JOptionPane.showMessageDialog(this, "Successfully imported " + imported.size() + " request(s) into collection!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "JMX Import failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
                JOptionPane.showMessageDialog(this, "Successfully exported collection to JMeter plan!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "JMX Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportReport() {
        if (resultsTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No execution results to export.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Object[] options = {"HTML Report", "CSV Report"};
        int choice = JOptionPane.showOptionDialog(this, "Choose export format:", "Export Report",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (choice == JOptionPane.CLOSED_OPTION) return;

        JFileChooser chooser = new JFileChooser();
        String ext = (choice == 0) ? "html" : "csv";
        String safeName = sanitizeFilename(collection.getName()) + "-report." + ext;
        chooser.setSelectedFile(new File(safeName));
        chooser.setDialogTitle("Export Execution Report");

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        try (PrintWriter pw = new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            if (choice == 0) {
                // HTML format with Aggregate summary
                EnvironmentModel env = resolveSelectedEnvironment();
                String sysUser = System.getProperty("user.name");
                String genAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                pw.println("<!DOCTYPE html><html><head><title>JAPI Execution Report - " + collection.getName() + "</title>");
                pw.println("<style>body{font-family:'Segoe UI',sans-serif;margin:20px;background:#f8f9fa;color:#333;}");
                pw.println(".header{background:#fff;padding:20px;border-radius:8px;box-shadow:0 2px 4px rgba(0,0,0,0.05);margin-bottom:20px;}");
                pw.println(".metrics{display:flex;gap:15px;margin-bottom:20px;}");
                pw.println(".card{background:#fff;padding:15px;border-radius:6px;flex:1;box-shadow:0 1px 3px rgba(0,0,0,0.05);}");
                pw.println("table{width:100%;border-collapse:collapse;background:#fff;box-shadow:0 1px 3px rgba(0,0,0,0.05);margin-bottom:30px;}");
                pw.println("th,td{padding:10px;text-align:left;border-bottom:1px solid #eee;}th{background:#f1f3f5;} tr.total{font-weight:bold;background:#eaeded;}</style></head><body>");

                pw.println("<div class='header'><h2>Execution Report: " + collection.getName() + "</h2>");
                pw.println("<p><b>Runner:</b> " + runnerModel.getName() + " | <b>Run By:</b> " + sysUser + "</p>");
                pw.println("<p><b>Started At:</b> " + (startTime != null ? startTime : "-") + " | <b>Finished At:</b> " + (endTime != null ? endTime : "-") + "</p>");
                pw.println("<p><b>Environment:</b> " + (env != null ? env.getName() : "None") + " | <b>Report Generated At:</b> " + genAt + "</p>");
                pw.println("<p><b>Load Profile:</b> Iterations=" + iterationsSpinner.getValue() + ", Concurrent Users=" + vusersSpinner.getValue() + ", Delay=" + delaySpinner.getValue() + "ms</p></div>");

                pw.println("<div class='metrics'>");
                pw.println("<div class='card'><h4>Total Requests</h4><p style='font-size:20px;font-weight:bold;color:#3498db;'>" + totalReqLabel.getText() + "</p></div>");
                pw.println("<div class='card'><h4>Passed</h4><p style='font-size:20px;font-weight:bold;color:#2ecc71;'>" + passedLabel.getText() + "</p></div>");
                pw.println("<div class='card'><h4>Failed</h4><p style='font-size:20px;font-weight:bold;color:#e74c3c;'>" + failedLabel.getText() + "</p></div>");
                pw.println("<div class='card'><h4>Avg Duration</h4><p style='font-size:20px;font-weight:bold;color:#9b59b6;'>" + avgTimeLabel.getText() + "</p></div></div>");

                // Aggregate Report Table
                pw.println("<h3>Aggregate Summary</h3>");
                pw.println("<table><thead><tr><th>API Name</th><th>Method</th><th>Samples</th><th>StatusCodes</th><th>Avg (ms)</th><th>Min (ms)</th><th>Max (ms)</th><th>p75</th><th>p90</th><th>p95</th><th>p99</th><th>Avg Size(B)</th><th>Pass</th><th>Fail</th><th>Success %</th><th>Format</th></tr></thead><tbody>");
                for (int i = 0; i < aggregateModel.getRowCount(); i++) {
                    boolean isTotal = "TOTAL".equals(aggregateModel.getValueAt(i, 0));
                    pw.println(isTotal ? "<tr class='total'>" : "<tr>");
                    for (int j = 0; j < aggregateModel.getColumnCount(); j++) {
                        pw.println("<td>" + aggregateModel.getValueAt(i, j) + "</td>");
                    }
                    pw.println("</tr>");
                }
                pw.println("</tbody></table>");

                // Raw Log Table
                pw.println("<h3>Execution Log Detail</h3>");
                pw.println("<table><thead><tr><th>#</th><th>Iteration</th><th>Request</th><th>Method</th><th>Status</th><th>Duration (ms)</th><th>URL</th></tr></thead><tbody>");
                for (int i = 0; i < resultsTableModel.getRowCount(); i++) {
                    pw.println("<tr>");
                    for (int j = 0; j < resultsTableModel.getColumnCount(); j++) {
                        pw.println("<td>" + resultsTableModel.getValueAt(i, j) + "</td>");
                    }
                    pw.println("</tr>");
                }
                pw.println("</tbody></table></body></html>");
            } else {
                // CSV export of aggregate statistics
                pw.println("API Name,Method,Samples,StatusCodes,Avg (ms),Min (ms),Max (ms),p75,p90,p95,p99,Avg Size(B),Pass,Fail,Success %,Format");
                for (int i = 0; i < aggregateModel.getRowCount(); i++) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < aggregateModel.getColumnCount(); j++) {
                        if (j > 0) sb.append(",");
                        sb.append("\"").append(aggregateModel.getValueAt(i, j)).append("\"");
                    }
                    pw.println(sb.toString());
                }
            }
            JOptionPane.showMessageDialog(this, "Report exported successfully.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to export report: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void updateFontSize(int size) {
        FontScaleHelper.scaleFonts(this, size);
        repaint();
    }

    public RequestModel getRequestModel() {
        saveConfig();
        return runnerModel;
    }
}
