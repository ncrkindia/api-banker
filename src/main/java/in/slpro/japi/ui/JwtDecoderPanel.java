package in.slpro.japi.ui;

import in.slpro.japi.model.RequestModel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import com.google.gson.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Base64;

/**
 * JwtDecoderPanel
 *
 * <p>
 * Core functionality and implementation logic for JwtDecoderPanel.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.1.0-beta
 * @since 1.0.0
 */
public class JwtDecoderPanel extends JPanel {
    private final RequestModel requestModel;
    private final RSyntaxTextArea inputArea;
    private final RSyntaxTextArea headerArea;
    private final RSyntaxTextArea payloadArea;
    private final RSyntaxTextArea signatureArea;

    public JwtDecoderPanel(MainFrame mainFrame, RequestModel requestModel) {
        this.requestModel = requestModel;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(UIManager.getColor("Panel.background"));

        // Input area
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBackground(UIManager.getColor("Panel.background"));
        inputPanel.setBorder(BorderFactory.createTitledBorder("JWT Token"));

        inputArea = new RSyntaxTextArea(4, 0);
        inputArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setAntiAliasingEnabled(true);
        inputArea.setHighlightCurrentLine(false);
        inputPanel.add(new RTextScrollPane(inputArea), BorderLayout.CENTER);

        JButton decodeBtn = new JButton("Decode");
        Color accent = UIManager.getColor("AccentColor");
        decodeBtn.setBackground(accent != null ? accent : new Color(52, 152, 219));
        decodeBtn.setForeground(Color.WHITE);
        decodeBtn.addActionListener(e -> decode());
        inputPanel.add(decodeBtn, BorderLayout.EAST);

        add(inputPanel, BorderLayout.NORTH);

        // Output sections
        JPanel outputPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        outputPanel.setBackground(UIManager.getColor("Panel.background"));

        headerArea = createOutputArea(SyntaxConstants.SYNTAX_STYLE_JSON);
        payloadArea = createOutputArea(SyntaxConstants.SYNTAX_STYLE_JSON);
        signatureArea = createOutputArea(SyntaxConstants.SYNTAX_STYLE_NONE);

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

    private RSyntaxTextArea createOutputArea(String syntaxStyle) {
        RSyntaxTextArea area = new RSyntaxTextArea();
        area.setSyntaxEditingStyle(syntaxStyle);
        area.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        area.setEditable(false);
        area.setBackground(UIManager.getColor("Workspace.panelBackground"));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCodeFoldingEnabled(true);
        area.setAntiAliasingEnabled(true);
        area.setHighlightCurrentLine(false);
        return area;
    }

    private RTextScrollPane wrapInBorder(RSyntaxTextArea area, String title) {
        RTextScrollPane scroll = new RTextScrollPane(area);
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

            headerArea.setText(beautifyJson(decodeBase64(parts[0])));
            payloadArea.setText(beautifyJson(decodeBase64(parts[1])));
            
            String sig = parts.length > 2 ? parts[2] : "(no signature)";
            signatureArea.setText(sig);

            // Save token
            requestModel.setBodyRawContent(token);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error decoding: " + e.getMessage(), "Decode Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String beautifyJson(String rawJson) {
        try {
            JsonElement je = JsonParser.parseString(rawJson);
            return new GsonBuilder().setPrettyPrinting().create().toJson(je);
        } catch (Exception e) {
            return rawJson;
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

    public void updateFontSize(int size) {
        FontScaleHelper.scaleFonts(this, size);
    }

    public RequestModel getRequestModel() {
        return requestModel;
    }
}
