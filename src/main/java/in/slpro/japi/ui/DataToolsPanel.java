package in.slpro.japi.ui;

import com.google.gson.*;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * DataToolsPanel
 *
 * <p>
 * Core functionality and implementation logic for DataToolsPanel.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.1.0-beta
 * @since 1.0.0
 */
public class DataToolsPanel extends JPanel {

    public DataToolsPanel(MainFrame mainFrame) {
        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));

        JTabbedPane toolsTab = new JTabbedPane();
        toolsTab.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        toolsTab.addTab("Schema Validator", buildSchemaValidator());
        toolsTab.addTab("Data Masker & Anonymizer", buildDataMasker());
        toolsTab.addTab("Data Generator", buildDataGenerator());
        toolsTab.addTab("Data Transformer", buildDataTransformer());
        toolsTab.addTab("Format & Validate", buildFormatValidate());

        add(toolsTab, BorderLayout.CENTER);
    }

    // ─── 1. Schema Validator ───────────────────────────────────────────────
    private JPanel buildSchemaValidator() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(UIManager.getColor("Panel.background"));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.setBackground(UIManager.getColor("Panel.background"));
        JComboBox<String> schemaType = new JComboBox<>(new String[]{"JSON Schema (Draft 4/7 basic)", "XML XSD (Basic Check)"});
        JButton validateBtn = new JButton("Validate Against Schema");
        Color accent = UIManager.getColor("AccentColor");
        validateBtn.setBackground(accent != null ? accent : new Color(52, 152, 219));
        validateBtn.setForeground(Color.WHITE);
        topBar.add(new JLabel("Type:"));
        topBar.add(schemaType);
        topBar.add(validateBtn);
        panel.add(topBar, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.5);

        RSyntaxTextArea dataArea = new RSyntaxTextArea();
        dataArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        dataArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        dataArea.setText("{\n  \"id\": 101,\n  \"name\": \"John Doe\",\n  \"email\": \"john@example.com\"\n}");
        dataArea.setLineWrap(true);
        dataArea.setAntiAliasingEnabled(true);
        dataArea.setHighlightCurrentLine(false);
        RTextScrollPane dataScroll = new RTextScrollPane(dataArea);
        dataScroll.setBorder(BorderFactory.createTitledBorder("Payload (JSON or XML)"));

        RSyntaxTextArea schemaArea = new RSyntaxTextArea();
        schemaArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        schemaArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        schemaArea.setText("{\n  \"required\": [\"id\", \"name\", \"email\"],\n  \"properties\": {\n    \"id\": {\"type\": \"number\"},\n    \"name\": {\"type\": \"string\"},\n    \"email\": {\"type\": \"string\"}\n  }\n}");
        schemaArea.setLineWrap(true);
        schemaArea.setAntiAliasingEnabled(true);
        schemaArea.setHighlightCurrentLine(false);
        RTextScrollPane schemaScroll = new RTextScrollPane(schemaArea);
        schemaScroll.setBorder(BorderFactory.createTitledBorder("Schema (JSON Schema or XSD rules)"));

        schemaType.addActionListener(e -> {
            if (schemaType.getSelectedIndex() == 0) {
                dataArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
                schemaArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
            } else {
                dataArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
                schemaArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
            }
        });

        split.setLeftComponent(dataScroll);
        split.setRightComponent(schemaScroll);
        panel.add(split, BorderLayout.CENTER);

        JLabel resultLabel = new JLabel("Status: Awaiting Validation");
        resultLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        resultLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.add(resultLabel, BorderLayout.SOUTH);

        validateBtn.addActionListener(e -> {
            String dataText = dataArea.getText().trim();
            String schemaText = schemaArea.getText().trim();
            if (dataText.isEmpty() || schemaText.isEmpty()) {
                resultLabel.setText("Status: Error - Input fields cannot be empty");
                resultLabel.setForeground(Color.RED);
                return;
            }

            if (schemaType.getSelectedIndex() == 0) {
                // Basic JSON Schema validation
                try {
                    JsonObject dataObj = JsonParser.parseString(dataText).getAsJsonObject();
                    JsonObject schemaObj = JsonParser.parseString(schemaText).getAsJsonObject();

                    StringBuilder errors = new StringBuilder();
                    if (schemaObj.has("required")) {
                        JsonArray req = schemaObj.getAsJsonArray("required");
                        for (JsonElement el : req) {
                            String key = el.getAsString();
                            if (!dataObj.has(key)) {
                                errors.append("Missing required field: '").append(key).append("'. ");
                            }
                        }
                    }

                    if (schemaObj.has("properties")) {
                        JsonObject props = schemaObj.getAsJsonObject("properties");
                        for (String key : props.keySet()) {
                            if (dataObj.has(key)) {
                                JsonObject rules = props.getAsJsonObject(key);
                                if (rules.has("type")) {
                                    String expectedType = rules.get("type").getAsString();
                                    JsonElement val = dataObj.get(key);
                                    boolean valid = false;
                                    if ("string".equals(expectedType) && val.isJsonPrimitive() && val.getAsJsonPrimitive().isString()) valid = true;
                                    else if ("number".equals(expectedType) && val.isJsonPrimitive() && val.getAsJsonPrimitive().isNumber()) valid = true;
                                    else if ("boolean".equals(expectedType) && val.isJsonPrimitive() && val.getAsJsonPrimitive().isBoolean()) valid = true;
                                    else if ("object".equals(expectedType) && val.isJsonObject()) valid = true;
                                    else if ("array".equals(expectedType) && val.isJsonArray()) valid = true;

                                    if (!valid) {
                                        errors.append("Field '").append(key).append("' expected type '").append(expectedType).append("'. ");
                                    }
                                }
                            }
                        }
                    }

                    if (errors.length() == 0) {
                        resultLabel.setText("✓ Schema Validation Succeeded!");
                        resultLabel.setForeground(new Color(39, 174, 96));
                    } else {
                        resultLabel.setText("✗ Validation Failed: " + errors.toString());
                        resultLabel.setForeground(Color.RED);
                    }
                } catch (Exception ex) {
                    resultLabel.setText("✗ Validation Error: " + ex.getMessage());
                    resultLabel.setForeground(Color.RED);
                }
            } else {
                // XML/XSD placeholder check
                try {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    builder.parse(new InputSource(new StringReader(dataText)));
                    resultLabel.setText("✓ XML parsing check Succeeded!");
                    resultLabel.setForeground(new Color(39, 174, 96));
                } catch (Exception ex) {
                    resultLabel.setText("✗ XML parsing failed: " + ex.getMessage());
                    resultLabel.setForeground(Color.RED);
                }
            }
        });

        return panel;
    }

    // ─── 2. Data Masker & Anonymizer ───────────────────────────────────────
    private JPanel buildDataMasker() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(UIManager.getColor("Panel.background"));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topBar.setBackground(UIManager.getColor("Panel.background"));

        JTextField maskKeysField = new JTextField("password,email,card,phone,ssn,secret", 25);
        JButton maskBtn = new JButton("Mask Data");
        maskBtn.setBackground(new Color(46, 204, 113));
        maskBtn.setForeground(Color.WHITE);

        JButton anonymizeBtn = new JButton("Anonymize Data");
        anonymizeBtn.setBackground(new Color(155, 89, 182));
        anonymizeBtn.setForeground(Color.WHITE);

        topBar.add(new JLabel("Keys to Mask/Anonymize:"));
        topBar.add(maskKeysField);
        topBar.add(maskBtn);
        topBar.add(anonymizeBtn);
        panel.add(topBar, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.5);

        RSyntaxTextArea inputArea = new RSyntaxTextArea();
        inputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        inputArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        inputArea.setText("{\n  \"username\": \"admin\",\n  \"password\": \"superSecret123!\",\n  \"email\": \"user@company.com\",\n  \"phone\": \"+1-555-0199\",\n  \"secretCode\": \"X-992-K\"\n}");
        inputArea.setLineWrap(true);
        inputArea.setAntiAliasingEnabled(true);
        inputArea.setHighlightCurrentLine(false);
        RTextScrollPane inScroll = new RTextScrollPane(inputArea);
        inScroll.setBorder(BorderFactory.createTitledBorder("Input JSON / XML"));

        RSyntaxTextArea outputArea = new RSyntaxTextArea();
        outputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        outputArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        outputArea.setEditable(false);
        outputArea.setBackground(UIManager.getColor("Workspace.panelBackground"));
        outputArea.setLineWrap(true);
        outputArea.setCodeFoldingEnabled(true);
        outputArea.setAntiAliasingEnabled(true);
        outputArea.setHighlightCurrentLine(false);
        RTextScrollPane outScroll = new RTextScrollPane(outputArea);
        outScroll.setBorder(BorderFactory.createTitledBorder("Masked / Anonymized Output"));

        split.setLeftComponent(inScroll);
        split.setRightComponent(outScroll);
        panel.add(split, BorderLayout.CENTER);

        maskBtn.addActionListener(e -> {
            String text = inputArea.getText().trim();
            String[] keys = maskKeysField.getText().split(",");
            boolean isXml = text.startsWith("<");
            if (isXml) {
                inputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
                outputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
            } else {
                inputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
                outputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
            }
            try {
                JsonElement elem = JsonParser.parseString(text);
                maskJsonElement(elem, keys, false);
                outputArea.setText(new GsonBuilder().setPrettyPrinting().create().toJson(elem));
            } catch (Exception ex) {
                String masked = text;
                for (String key : keys) {
                    Pattern p = Pattern.compile("(\"" + key.trim() + "\"\\s*:\\s*\")([^\"]+)(\")", Pattern.CASE_INSENSITIVE);
                    Matcher m = p.matcher(masked);
                    masked = m.replaceAll("$1******$3");
                }
                outputArea.setText(masked);
            }
        });

        anonymizeBtn.addActionListener(e -> {
            String text = inputArea.getText().trim();
            String[] keys = maskKeysField.getText().split(",");
            boolean isXml = text.startsWith("<");
            if (isXml) {
                inputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
                outputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
            } else {
                inputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
                outputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
            }
            try {
                JsonElement elem = JsonParser.parseString(text);
                maskJsonElement(elem, keys, true);
                outputArea.setText(new GsonBuilder().setPrettyPrinting().create().toJson(elem));
            } catch (Exception ex) {
                outputArea.setText("Error anonymizing JSON: " + ex.getMessage());
            }
        });

        return panel;
    }

    private void maskJsonElement(JsonElement elem, String[] keys, boolean anonymize) {
        if (elem.isJsonObject()) {
            JsonObject obj = elem.getAsJsonObject();
            for (String key : obj.keySet()) {
                JsonElement child = obj.get(key);
                boolean matches = false;
                for (String k : keys) {
                    if (key.equalsIgnoreCase(k.trim())) {
                        matches = true;
                        break;
                    }
                }
                if (matches && child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                    if (anonymize) {
                        obj.addProperty(key, "anon_" + UUID.randomUUID().toString().substring(0, 8));
                    } else {
                        obj.addProperty(key, "********");
                    }
                } else {
                    maskJsonElement(child, keys, anonymize);
                }
            }
        } else if (elem.isJsonArray()) {
            JsonArray arr = elem.getAsJsonArray();
            for (JsonElement child : arr) {
                maskJsonElement(child, keys, anonymize);
            }
        }
    }

    // ─── 3. Data Generator ─────────────────────────────────────────────────
    private JPanel buildDataGenerator() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(UIManager.getColor("Panel.background"));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
        topBar.setBackground(UIManager.getColor("Panel.background"));

        JComboBox<String> schemaTemplate = new JComboBox<>(new String[]{"Users List", "Products Catalog", "Transactions", "Custom Config"});
        JTextField countField = new JTextField("10", 4);
        JButton generateBtn = new JButton("Generate Data");
        Color accent = UIManager.getColor("AccentColor");
        generateBtn.setBackground(accent != null ? accent : new Color(52, 152, 219));
        generateBtn.setForeground(Color.WHITE);

        topBar.add(new JLabel("Template:"));
        topBar.add(schemaTemplate);
        topBar.add(new JLabel("Count:"));
        topBar.add(countField);
        topBar.add(generateBtn);
        panel.add(topBar, BorderLayout.NORTH);

        RSyntaxTextArea outputArea = new RSyntaxTextArea();
        outputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        outputArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        outputArea.setEditable(false);
        outputArea.setBackground(UIManager.getColor("Workspace.panelBackground"));
        outputArea.setLineWrap(true);
        outputArea.setCodeFoldingEnabled(true);
        outputArea.setAntiAliasingEnabled(true);
        outputArea.setHighlightCurrentLine(false);
        RTextScrollPane scroll = new RTextScrollPane(outputArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Generated Mock Output (JSON)"));
        panel.add(scroll, BorderLayout.CENTER);

        generateBtn.addActionListener(e -> {
            int count = 10;
            try {
                count = Integer.parseInt(countField.getText().trim());
            } catch (NumberFormatException ignored) {}

            String selected = (String) schemaTemplate.getSelectedItem();
            JsonArray arr = new JsonArray();
            Random r = new Random();

            String[] names = {"Alice Smith", "Bob Jones", "Charlie Brown", "David Green", "Eva White", "Frank Miller", "Grace Davis", "Henry Wilson"};
            String[] domains = {"gmail.com", "yahoo.com", "company.org", "outlook.com", "slpro.in"};
            String[] products = {"Laptop Pro", "Wireless Mouse", "Mechanical Keyboard", "USB-C Hub", "HD Monitor", "Bluetooth Headset"};
            String[] categories = {"Electronics", "Office Accessories", "Hardware", "Audio"};

            for (int i = 0; i < count; i++) {
                JsonObject item = new JsonObject();
                if ("Users List".equals(selected)) {
                    String name = names[r.nextInt(names.length)];
                    item.addProperty("id", i + 1000);
                    item.addProperty("name", name);
                    item.addProperty("email", name.toLowerCase().replace(" ", ".") + "@" + domains[r.nextInt(domains.length)]);
                    item.addProperty("isActive", r.nextBoolean());
                    item.addProperty("role", r.nextBoolean() ? "ADMIN" : "USER");
                } else if ("Products Catalog".equals(selected)) {
                    item.addProperty("sku", "SKU-" + (r.nextInt(90000) + 10000));
                    item.addProperty("name", products[r.nextInt(products.length)]);
                    item.addProperty("price", Math.round((10.0 + r.nextDouble() * 490.0) * 100.0) / 100.0);
                    item.addProperty("category", categories[r.nextInt(categories.length)]);
                    item.addProperty("stock", r.nextInt(150));
                } else {
                    item.addProperty("transactionId", "TXN-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase());
                    item.addProperty("amount", Math.round((1.0 + r.nextDouble() * 2000.0) * 100.0) / 100.0);
                    item.addProperty("currency", r.nextBoolean() ? "USD" : "INR");
                    item.addProperty("status", r.nextBoolean() ? "SUCCESS" : "PENDING");
                }
                arr.add(item);
            }
            outputArea.setText(new GsonBuilder().setPrettyPrinting().create().toJson(arr));
        });

        return panel;
    }

    // ─── 4. Data Transformer ───────────────────────────────────────────────
    private JPanel buildDataTransformer() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(UIManager.getColor("Panel.background"));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        topBar.setBackground(UIManager.getColor("Panel.background"));

        JButton jsonToXmlBtn = new JButton("Transform JSON to XML");
        jsonToXmlBtn.setBackground(new Color(230, 126, 34));
        jsonToXmlBtn.setForeground(Color.WHITE);

        JButton xmlToJsonBtn = new JButton("Transform XML to JSON");
        xmlToJsonBtn.setBackground(new Color(142, 68, 173));
        xmlToJsonBtn.setForeground(Color.WHITE);

        topBar.add(jsonToXmlBtn);
        topBar.add(xmlToJsonBtn);
        panel.add(topBar, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.5);

        RSyntaxTextArea inputArea = new RSyntaxTextArea();
        inputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        inputArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        inputArea.setText("{\n  \"user\": {\n    \"id\": 12,\n    \"name\": \"Nitesh\",\n    \"role\": \"developer\"\n  }\n}");
        inputArea.setLineWrap(true);
        inputArea.setAntiAliasingEnabled(true);
        inputArea.setHighlightCurrentLine(false);
        RTextScrollPane inScroll = new RTextScrollPane(inputArea);
        inScroll.setBorder(BorderFactory.createTitledBorder("Input Format"));

        RSyntaxTextArea outputArea = new RSyntaxTextArea();
        outputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        outputArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        outputArea.setEditable(false);
        outputArea.setBackground(UIManager.getColor("Workspace.panelBackground"));
        outputArea.setLineWrap(true);
        outputArea.setCodeFoldingEnabled(true);
        outputArea.setAntiAliasingEnabled(true);
        outputArea.setHighlightCurrentLine(false);
        RTextScrollPane outScroll = new RTextScrollPane(outputArea);
        outScroll.setBorder(BorderFactory.createTitledBorder("Transformed Output"));

        split.setLeftComponent(inScroll);
        split.setRightComponent(outScroll);
        panel.add(split, BorderLayout.CENTER);

        jsonToXmlBtn.addActionListener(e -> {
            String text = inputArea.getText().trim();
            inputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
            outputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
            try {
                JsonElement elem = JsonParser.parseString(text);
                StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root>\n");
                jsonToXmlBuilder(elem, xml, 2);
                xml.append("</root>");
                outputArea.setText(xml.toString());
            } catch (Exception ex) {
                outputArea.setText("Error: " + ex.getMessage());
            }
        });

        xmlToJsonBtn.addActionListener(e -> {
            String text = inputArea.getText().trim();
            inputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
            outputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
            try {
                JsonObject obj = new JsonObject();
                Pattern tagPattern = Pattern.compile("<([^>]+)>([^<]*)</\\1>");
                Matcher m = tagPattern.matcher(text);
                while (m.find()) {
                    obj.addProperty(m.group(1), m.group(2).trim());
                }
                if (obj.size() == 0) {
                    outputArea.setText("{\n  \"note\": \"Simple XML elements not found. Please verify XML tags structure.\"\n}");
                } else {
                    outputArea.setText(new GsonBuilder().setPrettyPrinting().create().toJson(obj));
                }
            } catch (Exception ex) {
                outputArea.setText("Error: " + ex.getMessage());
            }
        });

        return panel;
    }

    private void jsonToXmlBuilder(JsonElement elem, StringBuilder xml, int indent) {
        String spaces = " ".repeat(indent);
        if (elem.isJsonObject()) {
            JsonObject obj = elem.getAsJsonObject();
            for (String key : obj.keySet()) {
                JsonElement child = obj.get(key);
                if (child.isJsonPrimitive()) {
                    xml.append(spaces).append("<").append(key).append(">")
                            .append(child.getAsString())
                            .append("</").append(key).append(">\n");
                } else {
                    xml.append(spaces).append("<").append(key).append(">\n");
                    jsonToXmlBuilder(child, xml, indent + 2);
                    xml.append(spaces).append("</").append(key).append(">\n");
                }
            }
        } else if (elem.isJsonArray()) {
            JsonArray arr = elem.getAsJsonArray();
            for (JsonElement child : arr) {
                xml.append(spaces).append("<item>\n");
                jsonToXmlBuilder(child, xml, indent + 2);
                xml.append(spaces).append("</item>\n");
            }
        }
    }

    // ─── 5. Format & Validate ──────────────────────────────────────────────
    private JPanel buildFormatValidate() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(UIManager.getColor("Panel.background"));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        topBar.setBackground(UIManager.getColor("Panel.background"));

        JButton formatJsonBtn = new JButton("Format JSON");
        Color accent = UIManager.getColor("AccentColor");
        formatJsonBtn.setBackground(accent != null ? accent : new Color(52, 152, 219));
        formatJsonBtn.setForeground(Color.WHITE);

        JButton formatXmlBtn = new JButton("Format XML");
        formatXmlBtn.setBackground(new Color(230, 126, 34));
        formatXmlBtn.setForeground(Color.WHITE);

        topBar.add(formatJsonBtn);
        topBar.add(formatXmlBtn);
        panel.add(topBar, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.5);

        RSyntaxTextArea inputArea = new RSyntaxTextArea();
        inputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        inputArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        inputArea.setText("{\"user\":\"test\",\"role\":\"admin\",\"tags\":[\"dev\",\"qa\"]}");
        inputArea.setLineWrap(true);
        inputArea.setAntiAliasingEnabled(true);
        inputArea.setHighlightCurrentLine(false);
        RTextScrollPane inScroll = new RTextScrollPane(inputArea);
        inScroll.setBorder(BorderFactory.createTitledBorder("Raw Text"));

        RSyntaxTextArea outputArea = new RSyntaxTextArea();
        outputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        outputArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        outputArea.setEditable(false);
        outputArea.setBackground(UIManager.getColor("Workspace.panelBackground"));
        outputArea.setLineWrap(true);
        outputArea.setCodeFoldingEnabled(true);
        outputArea.setAntiAliasingEnabled(true);
        outputArea.setHighlightCurrentLine(false);
        RTextScrollPane outScroll = new RTextScrollPane(outputArea);
        outScroll.setBorder(BorderFactory.createTitledBorder("Formatted View"));

        split.setLeftComponent(inScroll);
        split.setRightComponent(outScroll);
        panel.add(split, BorderLayout.CENTER);

        formatJsonBtn.addActionListener(e -> {
            inputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
            outputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
            try {
                JsonElement elem = JsonParser.parseString(inputArea.getText());
                outputArea.setText(new GsonBuilder().setPrettyPrinting().create().toJson(elem));
            } catch (Exception ex) {
                outputArea.setText("Invalid JSON: " + ex.getMessage());
            }
        });

        formatXmlBtn.addActionListener(e -> {
            inputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
            outputArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
            try {
                String xml = inputArea.getText().trim();
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(new InputSource(new StringReader(xml)));

                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

                StringWriter sw = new StringWriter();
                transformer.transform(new DOMSource(doc), new StreamResult(sw));
                outputArea.setText(sw.toString());
            } catch (Exception ex) {
                outputArea.setText("Invalid XML: " + ex.getMessage());
            }
        });

        return panel;
    }
}
