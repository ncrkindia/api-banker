package in.slpro.japi.ui;

import in.slpro.japi.model.EnvironmentModel;
import in.slpro.japi.model.RequestModel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ItemEvent;

public class CodeSnippetDialog extends JDialog {
    private final RequestModel requestModel;
    private final EnvironmentModel environment;
    private final RSyntaxTextArea codeArea;
    private final JComboBox<String> langCombo;
    private final JCheckBox resolveVarsCheck;
    private final JButton copyBtn;

    public CodeSnippetDialog(Frame parent, RequestModel requestModel, EnvironmentModel environment) {
        super(parent, "Generate Code Snippet", true);
        this.requestModel = requestModel;
        this.environment = environment;

        setSize(750, 500);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        // Top Control Panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        topPanel.setBackground(UIManager.getColor("Panel.background"));
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Workspace.borderColor")));

        JLabel langLabel = new JLabel("Language:");
        langLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        topPanel.add(langLabel);

        String[] languages = {"cURL", "JavaScript (fetch)", "Python (requests)", "Java (HttpClient)"};
        langCombo = new JComboBox<>(languages);
        langCombo.setPreferredSize(new Dimension(180, 28));
        topPanel.add(langCombo);

        resolveVarsCheck = new JCheckBox("Resolve Variables", true);
        resolveVarsCheck.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        topPanel.add(resolveVarsCheck);

        add(topPanel, BorderLayout.NORTH);

        // Center Code Editor Panel
        codeArea = new RSyntaxTextArea();
        codeArea.setEditable(false);
        codeArea.setCodeFoldingEnabled(false);
        codeArea.setAntiAliasingEnabled(true);
        codeArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        codeArea.setHighlightCurrentLine(false);
        codeArea.setBackground(UIManager.getColor("Workspace.background"));
        codeArea.setCaretColor(UIManager.getColor("Label.foreground"));

        RTextScrollPane scrollPane = new RTextScrollPane(codeArea);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom Action Panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottomPanel.setBackground(UIManager.getColor("Panel.background"));
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Workspace.borderColor")),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        copyBtn = new JButton("Copy to Clipboard");
        copyBtn.setPreferredSize(new Dimension(150, 30));
        Color accent = UIManager.getColor("AccentColor");
        copyBtn.setBackground(accent != null ? accent : new Color(52, 152, 219));
        copyBtn.setForeground(Color.WHITE);
        copyBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        copyBtn.addActionListener(e -> copyToClipboard());

        bottomPanel.add(copyBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        // Event Listeners
        langCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateCodeStyle();
                generateCode();
            }
        });

        resolveVarsCheck.addActionListener(e -> generateCode());

        // Initialize display
        updateCodeStyle();
        generateCode();
    }

    private void updateCodeStyle() {
        String lang = (String) langCombo.getSelectedItem();
        if (lang == null) return;
        switch (lang) {
            case "cURL" -> codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
            case "JavaScript (fetch)" -> codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
            case "Python (requests)" -> codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
            case "Java (HttpClient)" -> codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        }
    }

    private void generateCode() {
        String lang = (String) langCombo.getSelectedItem();
        if (lang == null) return;
        
        String key = switch (lang) {
            case "cURL" -> "curl";
            case "JavaScript (fetch)" -> "javascript";
            case "Python (requests)" -> "python";
            case "Java (HttpClient)" -> "java";
            default -> "curl";
        };

        boolean resolve = resolveVarsCheck.isSelected();
        String snippet = CodeSnippetGenerator.generate(key, requestModel, environment, resolve);
        codeArea.setText(snippet);
        codeArea.setCaretPosition(0);

        // Reset copy button status
        copyBtn.setText("Copy to Clipboard");
        copyBtn.setEnabled(true);
    }

    private void copyToClipboard() {
        String text = codeArea.getText();
        if (text == null || text.isEmpty()) return;

        StringSelection selection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);

        copyBtn.setText("Copied!");
        copyBtn.setEnabled(false);

        Timer timer = new Timer(2000, e -> {
            copyBtn.setText("Copy to Clipboard");
            copyBtn.setEnabled(true);
        });
        timer.setRepeats(false);
        timer.start();
    }
}
