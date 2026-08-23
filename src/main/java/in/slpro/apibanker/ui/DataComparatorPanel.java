package in.slpro.apibanker.ui;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import com.google.gson.*;

import in.slpro.apibanker.model.RequestModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * DataComparatorPanel
 *
 * <p>
 * This panel provides a powerful split-view utility for A/B testing API
 * responses or
 * arbitrary text payloads. It supports structural node-by-node comparisons for
 * JSON
 * and XML formats (by alphabetically sorting nodes and keys before diffing),
 * rendering an inline HTML diff view using Google's diff-match-patch logic.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 2.0.0
 * @since 1.0.0
 */
public class DataComparatorPanel extends JPanel {
    private final RequestModel requestModel;
    private final RSyntaxTextArea leftArea;
    private final RSyntaxTextArea rightArea;
    private final JTextPane diffPane;
    private final JComboBox<String> compareTypeCombo;
    private int currentFontSize = 12;

    /**
     * Constructs the Data Comparator utility interface.
     * <p>
     * Initializes the dual {@link RSyntaxTextArea} text inputs (Text A vs Text B),
     * the format selector (Raw, JSON, XML), and the diff-rendered HTML output pane.
     * Binds the current text states and format mode to a persistent RequestModel
     * for
     * workspace state recovery.
     * </p>
     * 
     * @param mainFrame    The root application window.
     * @param requestModel The persistent state container where the comparator's
     *                     inputs are stored.
     */
    public DataComparatorPanel(MainFrame mainFrame, RequestModel requestModel) {
        this.requestModel = requestModel;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(UIManager.getColor("Panel.background"));

        leftArea = new RSyntaxTextArea();
        leftArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        leftArea.setLineWrap(true);
        leftArea.setAntiAliasingEnabled(true);
        leftArea.setHighlightCurrentLine(false);

        rightArea = new RSyntaxTextArea();
        rightArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        rightArea.setLineWrap(true);
        rightArea.setAntiAliasingEnabled(true);
        rightArea.setHighlightCurrentLine(false);

        diffPane = new JTextPane();
        diffPane.setContentType("text/html");
        diffPane.setEditable(false);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topBar.setBackground(UIManager.getColor("Panel.background"));

        JButton compareBtn = new JButton("Compare");
        Color accent = UIManager.getColor("AccentColor");
        compareBtn.setBackground(accent != null ? accent : new Color(52, 152, 219));
        compareBtn.setForeground(Color.WHITE);
        compareBtn.addActionListener(e -> compare());

        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> {
            leftArea.setText("");
            rightArea.setText("");
            diffPane.setText("");
            updateModel();
        });

        compareTypeCombo = new JComboBox<>(new String[] {
                "Raw Text (Line by Line)",
                "JSON (Node by Node)",
                "XML (Node by Node)"
        });

        compareTypeCombo.addActionListener(e -> {
            int idx = compareTypeCombo.getSelectedIndex();
            if (idx == 1) {
                leftArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
                rightArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
            } else if (idx == 2) {
                leftArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
                rightArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
            } else {
                leftArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
                rightArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
            }
            updateModel();
        });

        topBar.add(new JLabel("Compare Mode:"));
        topBar.add(compareTypeCombo);
        topBar.add(compareBtn);
        topBar.add(clearBtn);
        topBar.add(mainFrame.createInfoBadge("sec-tools-suite", "View Data Comparator Guide"));
        add(topBar, BorderLayout.NORTH);

        JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        topSplit.setResizeWeight(0.5);

        RTextScrollPane leftScroll = new RTextScrollPane(leftArea);
        leftScroll.setBorder(BorderFactory.createTitledBorder("Text A (Original)"));

        RTextScrollPane rightScroll = new RTextScrollPane(rightArea);
        rightScroll.setBorder(BorderFactory.createTitledBorder("Text B (Revised)"));

        topSplit.setLeftComponent(leftScroll);
        topSplit.setRightComponent(rightScroll);

        JScrollPane diffScroll = new JScrollPane(diffPane);
        diffScroll.setBorder(BorderFactory.createTitledBorder("Diff Result"));

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplit.setResizeWeight(0.4);
        mainSplit.setTopComponent(topSplit);
        mainSplit.setBottomComponent(diffScroll);

        add(mainSplit, BorderLayout.CENTER);

        // Load model values
        if (requestModel.getComparatorTextA() != null) {
            leftArea.setText(requestModel.getComparatorTextA());
        }
        if (requestModel.getComparatorTextB() != null) {
            rightArea.setText(requestModel.getComparatorTextB());
        }
        compareTypeCombo.setSelectedIndex(requestModel.getComparatorMode());
        int initIdx = compareTypeCombo.getSelectedIndex();
        if (initIdx == 1) {
            leftArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
            rightArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        } else if (initIdx == 2) {
            leftArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
            rightArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        } else {
            leftArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
            rightArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        }

        // Listen for changes
        leftArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateModel();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateModel();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateModel();
            }
        });
        rightArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateModel();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateModel();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateModel();
            }
        });
    }

    private void compare() {
        String leftText = leftArea.getText();
        String rightText = rightArea.getText();

        String finalLeft = leftText;
        String finalRight = rightText;

        int selectedMode = compareTypeCombo.getSelectedIndex();
        if (selectedMode == 1) { // JSON
            try {
                JsonElement jsonA = JsonParser.parseString(leftText.trim());
                JsonElement jsonB = JsonParser.parseString(rightText.trim());

                JsonElement sortedA = sortJson(jsonA);
                JsonElement sortedB = sortJson(jsonB);

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                finalLeft = gson.toJson(sortedA);
                finalRight = gson.toJson(sortedB);

                leftArea.setText(finalLeft);
                rightArea.setText(finalRight);
            } catch (Exception ex) {
                MainFrame.showToast(this, "JSON parse error, using Raw comparison: " + ex.getMessage());
            }
        } else if (selectedMode == 2) { // XML
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setIgnoringElementContentWhitespace(true);
                dbf.setIgnoringComments(true);
                DocumentBuilder db = dbf.newDocumentBuilder();

                Document docA = db.parse(new InputSource(new StringReader(leftText.trim())));
                Document docB = db.parse(new InputSource(new StringReader(rightText.trim())));

                Document sortedA = db.newDocument();
                sortedA.appendChild(sortXmlElement(sortedA, docA.getDocumentElement()));

                Document sortedB = db.newDocument();
                sortedB.appendChild(sortXmlElement(sortedB, docB.getDocumentElement()));

                finalLeft = formatXml(sortedA);
                finalRight = formatXml(sortedB);

                leftArea.setText(finalLeft);
                rightArea.setText(finalRight);
            } catch (Exception ex) {
                MainFrame.showToast(this, "XML parse error, using Raw comparison: " + ex.getMessage());
            }
        }

        List<String> leftLines = Arrays.asList(finalLeft.split("\n", -1));
        List<String> rightLines = Arrays.asList(finalRight.split("\n", -1));
        Patch<String> patch = DiffUtils.diff(leftLines, rightLines);

        boolean isDark = "dark"
                .equals(in.slpro.apibanker.storage.StorageManager.getInstance().getSettings().getTheme());
        String textBg = isDark ? "#1e1e1e" : "#ffffff";
        String textColor = isDark ? "#d4d4d4" : "#212121";
        String delBg = isDark ? "#482323" : "#ffeef0";
        String delText = isDark ? "#f17c7c" : "#c0392b";
        String addBg = isDark ? "#1b3c22" : "#e6ffed";
        String addText = isDark ? "#7cf193" : "#27ae60";

        StringBuilder html = new StringBuilder("<html><body style='font-family:JetBrains Mono,monospace;font-size:"
                + currentFontSize + "px;background:" + textBg + ";color:" + textColor + ";'>");
        int leftIdx = 0;
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            while (leftIdx < delta.getSource().getPosition()) {
                html.append("<div style='background:").append(textBg).append(";padding:1px 4px;'>")
                        .append(escapeHtml(leftLines.get(leftIdx))).append("</div>");
                leftIdx++;
            }
            for (String line : delta.getSource().getLines()) {
                html.append("<div style='background:").append(delBg).append(";padding:1px 4px;color:").append(delText)
                        .append(";'>- ").append(escapeHtml(line)).append("</div>");
                leftIdx++;
            }
            for (String line : delta.getTarget().getLines()) {
                html.append("<div style='background:").append(addBg).append(";padding:1px 4px;color:").append(addText)
                        .append(";'>+ ").append(escapeHtml(line)).append("</div>");
            }
        }
        while (leftIdx < leftLines.size()) {
            html.append("<div style='background:").append(textBg).append(";padding:1px 4px;'>")
                    .append(escapeHtml(leftLines.get(leftIdx))).append("</div>");
            leftIdx++;
        }
        html.append("</body></html>");
        diffPane.setText(html.toString());
        diffPane.setCaretPosition(0);
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace(" ", "&nbsp;");
    }

    // ─── JSON Sorting Helper ──────────────────────────────────────────
    private JsonElement sortJson(JsonElement elem) {
        if (elem == null)
            return null;
        if (elem.isJsonObject()) {
            JsonObject obj = elem.getAsJsonObject();
            JsonObject sorted = new JsonObject();
            String[] keys = obj.keySet().toArray(new String[0]);
            Arrays.sort(keys);
            for (String key : keys) {
                sorted.add(key, sortJson(obj.get(key)));
            }
            return sorted;
        } else if (elem.isJsonArray()) {
            JsonArray arr = elem.getAsJsonArray();
            JsonArray sorted = new JsonArray();
            for (JsonElement item : arr) {
                sorted.add(sortJson(item));
            }
            return sorted;
        }
        return elem;
    }

    // ─── XML Sorting Helper ───────────────────────────────────────────
    private Element sortXmlElement(Document destDoc, Element src) {
        if (src == null)
            return null;
        Element result = destDoc.createElement(src.getTagName());

        // Sort attributes
        LinkedHashMap<String, String> attrs = getAttributes(src);
        String[] attrNames = attrs.keySet().toArray(new String[0]);
        Arrays.sort(attrNames);
        for (String name : attrNames) {
            result.setAttribute(name, attrs.get(name));
        }

        // Sort children
        List<Element> children = getElementChildren(src);
        if (children.isEmpty()) {
            result.setTextContent(src.getTextContent().trim());
        } else {
            children.sort((e1, e2) -> {
                int cmp = e1.getTagName().compareTo(e2.getTagName());
                if (cmp != 0)
                    return cmp;
                // secondary sort by id attribute
                String id1 = e1.getAttribute("id");
                String id2 = e2.getAttribute("id");
                int idCmp = id1.compareTo(id2);
                if (idCmp != 0)
                    return idCmp;
                // tertiary sort by name attribute
                String name1 = e1.getAttribute("name");
                String name2 = e2.getAttribute("name");
                return name1.compareTo(name2);
            });

            for (Element child : children) {
                result.appendChild(sortXmlElement(destDoc, child));
            }
        }
        return result;
    }

    private LinkedHashMap<String, String> getAttributes(Element el) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        if (el == null)
            return map;
        org.w3c.dom.NamedNodeMap attrs = el.getAttributes();
        if (attrs != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
                Node node = attrs.item(i);
                map.put(node.getNodeName(), node.getNodeValue());
            }
        }
        return map;
    }

    private List<Element> getElementChildren(Element el) {
        List<Element> list = new ArrayList<>();
        if (el == null)
            return list;
        NodeList nl = el.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                list.add((Element) node);
            }
        }
        return list;
    }

    private String formatXml(Document doc) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(sw));
        return sw.toString().trim();
    }

    public void updateFontSize(int size) {
        this.currentFontSize = size;
        FontScaleHelper.scaleFonts(this, size);
        if (diffPane.getText() != null && !diffPane.getText().isEmpty()) {
            compare();
        }
    }

    public RequestModel getRequestModel() {
        updateModel();
        return requestModel;
    }

    private void updateModel() {
        if (requestModel != null) {
            requestModel.setComparatorTextA(leftArea != null ? leftArea.getText() : "");
            requestModel.setComparatorTextB(rightArea != null ? rightArea.getText() : "");
            requestModel.setComparatorMode(compareTypeCombo != null ? compareTypeCombo.getSelectedIndex() : 0);
        }
    }
}

