package in.slpro.japi.ui;

import in.slpro.japi.model.RequestModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class JsonToolPanel extends JPanel {
    private final RequestModel requestModel;
    private final JTextArea inputArea;
    private final JTextArea outputArea;

    public JsonToolPanel(MainFrame mainFrame, RequestModel requestModel) {
        this.requestModel = requestModel;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(Color.WHITE);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topBar.setBackground(Color.WHITE);

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

        inputArea = new JTextArea();
        inputArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        inputArea.setLineWrap(true);
        JScrollPane inScroll = new JScrollPane(inputArea);
        inScroll.setBorder(BorderFactory.createTitledBorder("Input JSON"));

        outputArea = new JTextArea();
        outputArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        outputArea.setEditable(false);
        outputArea.setBackground(new Color(248, 249, 250));
        outputArea.setLineWrap(true);
        JScrollPane outScroll = new JScrollPane(outputArea);
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
            outputArea.setForeground(new Color(33, 33, 33));
        } catch (Exception e) {
            outputArea.setText("Error: " + e.getMessage());
            outputArea.setForeground(new Color(192, 57, 43));
        }
    }

    private void minify() {
        try {
            com.google.gson.JsonElement elem = com.google.gson.JsonParser.parseString(inputArea.getText());
            outputArea.setText(elem.toString());
            outputArea.setForeground(new Color(33, 33, 33));
        } catch (Exception e) {
            outputArea.setText("Error: " + e.getMessage());
            outputArea.setForeground(new Color(192, 57, 43));
        }
    }

    private void validateJson() {
        try {
            com.google.gson.JsonParser.parseString(inputArea.getText());
            outputArea.setText("✓ Valid JSON");
            outputArea.setForeground(new Color(39, 174, 96));
        } catch (Exception e) {
            outputArea.setText("✗ Invalid JSON: " + e.getMessage());
            outputArea.setForeground(new Color(192, 57, 43));
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
