package in.slpro.japi.ui;

import in.slpro.japi.model.RequestModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Base64;

public class JwtDecoderPanel extends JPanel {
    private final RequestModel requestModel;
    private final JTextArea inputArea;
    private final JTextArea headerArea;
    private final JTextArea payloadArea;
    private final JTextArea signatureArea;

    public JwtDecoderPanel(MainFrame mainFrame, RequestModel requestModel) {
        this.requestModel = requestModel;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(Color.WHITE);

        // Input area
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createTitledBorder("JWT Token"));

        inputArea = new JTextArea(4, 0);
        inputArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);

        JButton decodeBtn = new JButton("Decode");
        decodeBtn.setBackground(new Color(52, 152, 219));
        decodeBtn.setForeground(Color.WHITE);
        decodeBtn.addActionListener(e -> decode());
        inputPanel.add(decodeBtn, BorderLayout.EAST);

        add(inputPanel, BorderLayout.NORTH);

        // Output sections
        JPanel outputPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        outputPanel.setBackground(Color.WHITE);

        headerArea = createOutputArea("Header");
        payloadArea = createOutputArea("Payload");
        signatureArea = createOutputArea("Signature");

        outputPanel.add(wrapInBorder(headerArea, "Header"));
        outputPanel.add(wrapInBorder(payloadArea, "Payload"));
        outputPanel.add(wrapInBorder(signatureArea, "Signature"));

        add(outputPanel, BorderLayout.CENTER);

        // Load saved token
        if (requestModel.getBodyRawContent() != null && !requestModel.getBodyRawContent().isBlank()) {
            inputArea.setText(requestModel.getBodyRawContent());
            decode();
        }
    }

    private JTextArea createOutputArea(String name) {
        JTextArea area = new JTextArea();
        area.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        area.setEditable(false);
        area.setBackground(new Color(248, 249, 250));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    private JScrollPane wrapInBorder(JTextArea area, String title) {
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createTitledBorder(title));
        return scroll;
    }

    private void decode() {
        String token = inputArea.getText().trim();
        if (token.isEmpty()) return;

        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                JOptionPane.showMessageDialog(this, "Invalid JWT format. Expected 3 parts separated by '.'", "Invalid JWT", JOptionPane.ERROR_MESSAGE);
                return;
            }

            headerArea.setText(prettyJson(decodeBase64(parts[0])));
            payloadArea.setText(prettyJson(decodeBase64(parts[1])));
            signatureArea.setText(parts.length > 2 ? parts[2] : "(no signature)");

            // Save token
            requestModel.setBodyRawContent(token);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error decoding: " + e.getMessage(), "Decode Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String decodeBase64(String encoded) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encoded.replaceAll("=+$", ""));
            return new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "(decode error: " + e.getMessage() + ")";
        }
    }

    private String prettyJson(String json) {
        try {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            com.google.gson.JsonElement elem = com.google.gson.JsonParser.parseString(json);
            return gson.toJson(elem);
        } catch (Exception e) {
            return json;
        }
    }

    public void updateFontSize(int size) {
        FontScaleHelper.scaleFonts(this, size);
    }

    public RequestModel getRequestModel() {
        return requestModel;
    }
}
