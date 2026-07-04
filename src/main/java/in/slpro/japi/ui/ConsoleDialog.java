package in.slpro.japi.ui;

import in.slpro.japi.logger.ConsoleLogger;
import in.slpro.japi.logger.LogEntry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ConsoleDialog extends JDialog implements ConsoleLogger.LogListener {
    private final JPanel logPanel;
    private final JScrollPane scrollPane;

    public ConsoleDialog(JFrame owner) {
        super(owner, "JAPI Console", false);
        setSize(800, 500);
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        setLocationRelativeTo(owner);

        logPanel = new JPanel();
        logPanel.setLayout(new BoxLayout(logPanel, BoxLayout.Y_AXIS));
        logPanel.setBackground(new Color(20, 20, 30));

        scrollPane = new JScrollPane(logPanel);
        scrollPane.setBorder(null);
        scrollPane.setBackground(new Color(20, 20, 30));
        scrollPane.getViewport().setBackground(new Color(20, 20, 30));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.setBackground(new Color(30, 30, 40));
        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> {
            logPanel.removeAll();
            logPanel.revalidate();
            logPanel.repaint();
            ConsoleLogger.getInstance().clear();
        });
        topBar.add(clearBtn);

        add(topBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        ConsoleLogger.getInstance().addListener(this);

        // Load existing entries
        for (LogEntry entry : ConsoleLogger.getInstance().getEntries()) {
            addEntryPanel(entry);
        }
    }

    @Override
    public void onLogEntry(LogEntry entry) {
        SwingUtilities.invokeLater(() -> addEntryPanel(entry));
    }

    private void addEntryPanel(LogEntry entry) {
        JPanel entryPanel = new JPanel(new BorderLayout());
        entryPanel.setBackground(entry.getLevel() == LogEntry.Level.ERROR ? new Color(40, 20, 20) : new Color(25, 25, 35));
        entryPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(40, 40, 55)),
                new EmptyBorder(6, 10, 6, 10)));
        entryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        entryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(entry.getTimestamp()));
        JLabel timeLabel = new JLabel(time + "  ");
        timeLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        timeLabel.setForeground(new Color(100, 100, 130));

        JTextArea textArea = new JTextArea(entry.getMessage());
        textArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        textArea.setForeground(entry.getLevel() == LogEntry.Level.ERROR ? new Color(255, 100, 100) : new Color(180, 220, 180));
        textArea.setBackground(entryPanel.getBackground());
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBorder(null);

        entryPanel.add(timeLabel, BorderLayout.WEST);
        entryPanel.add(textArea, BorderLayout.CENTER);

        logPanel.add(entryPanel);
        logPanel.revalidate();

        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    @Override
    public void dispose() {
        ConsoleLogger.getInstance().removeListener(this);
        super.dispose();
    }
}
