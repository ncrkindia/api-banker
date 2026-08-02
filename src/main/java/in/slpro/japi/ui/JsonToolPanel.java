package in.slpro.japi.ui;

import in.slpro.japi.model.RequestModel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * JsonToolPanel
 *
 * <p>
 * Core functionality and implementation logic for JsonToolPanel.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.1.0-beta
 * @since 1.0.0
 */
public class JsonToolPanel extends JPanel {
    private final RequestModel requestModel;
    private final RSyntaxTextArea inputArea;
    private final RSyntaxTextArea outputArea;

    public JsonToolPanel(MainFrame mainFrame, RequestModel requestModel) {
        this.requestModel = requestModel;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(UIManager.getColor("Panel.background"));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topBar.setBackground(UIManager.getColor("Panel.background"));

        JButton formatBtn = new JButton("Format / Prettify");
        JButton minifyBtn = new JButton("Minify");
        JButton validateBtn = new JButton("Validate");

        formatBtn.addActionListener(e -> format());
        minifyBtn.addActionListener(e -> minify());
        validateBtn.addActionListener(e -> validateJson());

        topBar.add(formatBtn);
        topBar.add(minifyBtn);
        topBar.add(validateBtn);

        add(topBar, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.5);

        inputArea = new RSyntaxTextArea();
        inputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        inputArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        inputArea.setLineWrap(true);
        inputArea.setCodeFoldingEnabled(true);
        inputArea.setAntiAliasingEnabled(true);
        inputArea.setHighlightCurrentLine(false);
        RTextScrollPane inScroll = new RTextScrollPane(inputArea);
        inScroll.setBorder(BorderFactory.createTitledBorder("Input JSON"));

        outputArea = new RSyntaxTextArea();
        outputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        outputArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        outputArea.setEditable(false);
        outputArea.setBackground(UIManager.getColor("Workspace.panelBackground"));
        outputArea.setLineWrap(true);
        outputArea.setCodeFoldingEnabled(true);
        outputArea.setAntiAliasingEnabled(true);
        outputArea.setHighlightCurrentLine(false);
        RTextScrollPane outScroll = new RTextScrollPane(outputArea);
        outScroll.setBorder(BorderFactory.createTitledBorder("Output"));

        split.setLeftComponent(inScroll);
        split.setRightComponent(outScroll);
        add(split, BorderLayout.CENTER);

        if (requestModel.getBodyRawContent() != null) {
            inputArea.setText(requestModel.getBodyRawContent());
        }
    }

    private void format() {
        try {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            com.google.gson.JsonElement elem = com.google.gson.JsonParser.parseString(inputArea.getText());
            outputArea.setText(gson.toJson(elem));
        } catch (Exception e) {
            outputArea.setText("Error: " + e.getMessage());
        }
    }

    private void minify() {
        try {
            com.google.gson.JsonElement elem = com.google.gson.JsonParser.parseString(inputArea.getText());
            outputArea.setText(elem.toString());
        } catch (Exception e) {
            outputArea.setText("Error: " + e.getMessage());
        }
    }

    private void validateJson() {
        try {
            com.google.gson.JsonParser.parseString(inputArea.getText());
            outputArea.setText("✓ Valid JSON");
        } catch (Exception e) {
            outputArea.setText("✗ Invalid JSON: " + e.getMessage());
        }
    }

    public void updateFontSize(int size) {
        FontScaleHelper.scaleFonts(this, size);
    }

    public RequestModel getRequestModel() {
        requestModel.setBodyRawContent(inputArea.getText());
        return requestModel;
    }
}
