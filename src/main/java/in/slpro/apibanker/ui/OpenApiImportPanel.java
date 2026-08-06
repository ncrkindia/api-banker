package in.slpro.apibanker.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;

import in.slpro.apibanker.model.CollectionModel;
import in.slpro.apibanker.model.RequestModel;
import in.slpro.apibanker.model.KeyValueItem;

/**
 * OpenApiImportPanel
 *
 * <p>
 * Workspace Tab panel for importing OpenAPI (Swagger) specifications.
 * Allows the user to select a file, analyze its contents, pick specific APIs
 * to import, and configure how the Base URL is stored.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.0.0-beta
 * @since 1.0.0
 */
public class OpenApiImportPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JTextField fileField;
    private final DefaultTableModel tableModel;
    private final JComboBox<String> baseUrlOption;
    private OpenAPI currentOpenAPI;
    private String collectionName = "OpenAPI Import";
    private final JTable table;

    public OpenApiImportPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));

        JPanel filePanel = new JPanel(new BorderLayout(5, 0));
        filePanel.add(new JLabel("OpenAPI File (JSON/YAML):"), BorderLayout.WEST);
        fileField = new JTextField();
        filePanel.add(fileField, BorderLayout.CENTER);
        JButton browseBtn = new JButton("Browse");
        filePanel.add(browseBtn, BorderLayout.EAST);

        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        configPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        configPanel.add(new JLabel("Import Base URL as:"));
        baseUrlOption = new JComboBox<>(
                new String[] { "Directly in Request URL", "Collection Variable ({{baseUrl}})" });
        configPanel.add(baseUrlOption);

        JButton analyzeBtn = new JButton("Analyze Spec");
        configPanel.add(analyzeBtn);

        topPanel.add(filePanel, BorderLayout.NORTH);
        topPanel.add(configPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // Table for APIs
        tableModel = new DefaultTableModel(new Object[] { "Import", "Method", "Path", "Summary" }, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };
        table = new JTable(tableModel);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setMaxWidth(80);
        table.setRowHeight(25);

        table.getColumnModel().getColumn(1).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus,
                    int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if (value instanceof String method) {
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                    if (!isSelected) {
                        c.setForeground(getMethodColor(method));
                    }
                }
                return c;
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom Actions
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton selectAllBtn = new JButton("Select All");
        JButton deselectAllBtn = new JButton("Deselect All");
        JButton importBtn = new JButton("Import Selected");
        importBtn.setBackground(new Color(46, 204, 113));
        importBtn.setForeground(Color.WHITE);

        bottomPanel.add(selectAllBtn);
        bottomPanel.add(deselectAllBtn);
        bottomPanel.add(importBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        // Actions
        browseBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(mainFrame.getLastFileChooserDirectory());
            chooser.setFileFilter(new FileNameExtensionFilter("OpenAPI Spec (JSON, YAML)", "json", "yaml", "yml"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                fileField.setText(file.getAbsolutePath());
                mainFrame.setLastFileChooserDirectory(file.getParentFile());
            }
        });

        analyzeBtn.addActionListener(e -> analyzeFile());

        selectAllBtn.addActionListener(e -> setAllSelection(true));
        deselectAllBtn.addActionListener(e -> setAllSelection(false));

        importBtn.addActionListener(e -> importSelected());
    }

    private void analyzeFile() {
        String path = fileField.getText().trim();
        if (path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a file first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            currentOpenAPI = new OpenAPIV3Parser().read(path);
            if (currentOpenAPI == null) {
                JOptionPane.showMessageDialog(this, "Failed to parse OpenAPI spec. It might be invalid.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            tableModel.setRowCount(0);

            if (currentOpenAPI.getInfo() != null && currentOpenAPI.getInfo().getTitle() != null) {
                collectionName = currentOpenAPI.getInfo().getTitle();
            } else {
                collectionName = new File(path).getName();
            }

            if (currentOpenAPI.getPaths() != null) {
                for (Map.Entry<String, PathItem> entry : currentOpenAPI.getPaths().entrySet()) {
                    String urlPath = entry.getKey();
                    PathItem pathItem = entry.getValue();

                    addOperationToTable("GET", urlPath, pathItem.getGet());
                    addOperationToTable("POST", urlPath, pathItem.getPost());
                    addOperationToTable("PUT", urlPath, pathItem.getPut());
                    addOperationToTable("DELETE", urlPath, pathItem.getDelete());
                    addOperationToTable("PATCH", urlPath, pathItem.getPatch());
                }
            }

            if (tableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No valid API paths found in the spec.", "Info",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error analyzing file: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addOperationToTable(String method, String path, Operation op) {
        if (op != null) {
            String summary = op.getSummary() != null ? op.getSummary() : "";
            tableModel.addRow(new Object[] { true, method, path, summary });
        }
    }

    private void setAllSelection(boolean selected) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(selected, i, 0);
        }
    }

    private void importSelected() {
        if (currentOpenAPI == null) {
            JOptionPane.showMessageDialog(this, "Please analyze a file first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<RequestModel> selectedRequests = new ArrayList<>();

        List<String> serverUrls = new ArrayList<>();
        if (currentOpenAPI.getServers() != null && !currentOpenAPI.getServers().isEmpty()) {
            for (Server server : currentOpenAPI.getServers()) {
                String sUrl = server.getUrl();
                if (sUrl != null) {
                    if (sUrl.endsWith("/")) {
                        sUrl = sUrl.substring(0, sUrl.length() - 1);
                    }
                    if (!sUrl.isEmpty()) {
                        serverUrls.add(sUrl);
                    }
                }
            }
        }

        String baseUrl = serverUrls.isEmpty() ? "http://localhost" : serverUrls.get(0);

        boolean useCollectionVar = baseUrlOption.getSelectedIndex() == 1;
        String prefixUrl = useCollectionVar ? "{{baseUrl}}" : baseUrl;

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            boolean isSelected = (Boolean) tableModel.getValueAt(i, 0);
            if (isSelected) {
                String method = (String) tableModel.getValueAt(i, 1);
                String path = (String) tableModel.getValueAt(i, 2);
                String summary = (String) tableModel.getValueAt(i, 3);

                RequestModel req = new RequestModel();
                req.setId(UUID.randomUUID().toString());
                req.setName(summary.isEmpty() ? path : summary);
                req.setMethod(method);

                // Construct URL
                String finalUrl = prefixUrl + path;
                req.setUrl(finalUrl);

                // Add basic default headers
                req.setHeaders(new ArrayList<>());
                req.getHeaders().add(new KeyValueItem("Accept", "application/json", true));
                req.setAuthType("inherit");

                selectedRequests.add(req);
            }
        }

        if (selectedRequests.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No APIs selected for import.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        CollectionModel collection = new CollectionModel();
        collection.setId(UUID.randomUUID().toString());
        collection.setName(collectionName);
        collection.setRequests(new ArrayList<>(selectedRequests));

        if (useCollectionVar) {
            List<KeyValueItem> colVars = new ArrayList<>();
            if (serverUrls.isEmpty()) {
                colVars.add(new KeyValueItem("baseUrl", "http://localhost", true));
            } else {
                for (int i = 0; i < serverUrls.size(); i++) {
                    String varName = i == 0 ? "baseUrl" : "baseUrl_" + i;
                    colVars.add(new KeyValueItem(varName, serverUrls.get(i), true));
                }
            }
            collection.setVariables(colVars);
        }

        // Map Security
        if (currentOpenAPI.getComponents() != null && currentOpenAPI.getComponents().getSecuritySchemes() != null) {
            for (Map.Entry<String, SecurityScheme> entry : currentOpenAPI.getComponents().getSecuritySchemes()
                    .entrySet()) {
                SecurityScheme scheme = entry.getValue();
                if (scheme.getType() == SecurityScheme.Type.HTTP) {
                    if ("bearer".equalsIgnoreCase(scheme.getScheme())) {
                        collection.setAuthType("bearer");
                        collection.setAuthToken("");
                        break;
                    } else if ("basic".equalsIgnoreCase(scheme.getScheme())) {
                        collection.setAuthType("basic");
                        collection.setAuthUsername("");
                        collection.setAuthPassword("");
                        break;
                    }
                } else if (scheme.getType() == SecurityScheme.Type.APIKEY) {
                    collection.setAuthType("apiKey");
                    collection.setAuthApiKeyName(scheme.getName());
                    collection.setAuthApiKeyIn(
                            scheme.getIn() != null ? scheme.getIn().toString().toLowerCase() : "header");
                    collection.setAuthApiKeyValue("");
                    break;
                } else if (scheme.getType() == SecurityScheme.Type.OAUTH2) {
                    collection.setAuthType("oauth2");
                    break;
                }
            }
        }

        collection.setReadme(generateReadme(currentOpenAPI, serverUrls));

        mainFrame.addCollection(collection);
        mainFrame.saveWorkspace();

        JOptionPane.showMessageDialog(this, "Successfully imported " + selectedRequests.size()
                + " requests into collection '" + collectionName + "'.", "Success", JOptionPane.INFORMATION_MESSAGE);
        mainFrame.closeTab(this);
    }

    private String generateReadme(OpenAPI api, List<String> serverUrls) {
        StringBuilder sb = new StringBuilder();

        String title = api.getInfo() != null && api.getInfo().getTitle() != null ? api.getInfo().getTitle()
                : "API Documentation";
        String version = api.getInfo() != null && api.getInfo().getVersion() != null ? api.getInfo().getVersion()
                : "1.0.0";
        String description = api.getInfo() != null && api.getInfo().getDescription() != null
                ? api.getInfo().getDescription()
                : "";

        sb.append("# ").append(title).append("\n\n");
        if (!description.isEmpty()) {
            sb.append(description).append("\n\n");
        }
        sb.append("**Version:** ").append(version).append("\n\n");
        sb.append("---\n\n");

        sb.append("## 🌍 Servers\n\n");
        if (serverUrls.isEmpty()) {
            sb.append("- `http://localhost` (Default Fallback)\n");
        } else {
            for (String sUrl : serverUrls) {
                sb.append("- `").append(sUrl).append("`\n");
            }
        }
        sb.append("\n");

        sb.append("## \uD83D\uDD10 Security / Authentication\n\n");
        boolean hasSecurity = false;
        if (api.getComponents() != null && api.getComponents().getSecuritySchemes() != null) {
            for (Map.Entry<String, SecurityScheme> entry : api.getComponents().getSecuritySchemes().entrySet()) {
                hasSecurity = true;
                SecurityScheme scheme = entry.getValue();
                sb.append("- **").append(entry.getKey()).append("** (").append(scheme.getType()).append(")\n");
                if (scheme.getDescription() != null && !scheme.getDescription().isEmpty()) {
                    sb.append("  - *").append(scheme.getDescription().replace("\n", " ")).append("*\n");
                }
                if (scheme.getType() == SecurityScheme.Type.APIKEY) {
                    sb.append("  - Pass in: `").append(scheme.getIn()).append("` as `").append(scheme.getName())
                            .append("`\n");
                } else if (scheme.getType() == SecurityScheme.Type.HTTP) {
                    sb.append("  - Scheme: `").append(scheme.getScheme()).append("`\n");
                }
            }
        }
        if (!hasSecurity) {
            sb.append("- No authentication mechanisms defined.\n");
        }
        sb.append("\n---\n\n");

        sb.append("## \uD83D\uDE80 Endpoints\n\n");

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            boolean isSelected = (Boolean) tableModel.getValueAt(i, 0);
            if (!isSelected)
                continue;

            String method = (String) tableModel.getValueAt(i, 1);
            String path = (String) tableModel.getValueAt(i, 2);

            sb.append("### `").append(method).append("` ").append(path).append("\n\n");

            if (api.getPaths() != null) {
                io.swagger.v3.oas.models.PathItem pathItem = api.getPaths().get(path);
                if (pathItem != null) {
                    io.swagger.v3.oas.models.Operation op = switch (method.toUpperCase()) {
                        case "GET" -> pathItem.getGet();
                        case "POST" -> pathItem.getPost();
                        case "PUT" -> pathItem.getPut();
                        case "DELETE" -> pathItem.getDelete();
                        case "PATCH" -> pathItem.getPatch();
                        default -> null;
                    };

                    if (op != null) {
                        if (op.getSummary() != null && !op.getSummary().isEmpty()) {
                            sb.append("**Summary:** ").append(op.getSummary()).append("\n\n");
                        }
                        if (op.getDescription() != null && !op.getDescription().isEmpty()) {
                            sb.append(op.getDescription()).append("\n\n");
                        }

                        if (op.getParameters() != null && !op.getParameters().isEmpty()) {
                            sb.append("**Parameters:**\n\n");
                            for (io.swagger.v3.oas.models.parameters.Parameter param : op.getParameters()) {
                                String pName = param.getName();
                                String pIn = param.getIn();
                                boolean req = param.getRequired() != null ? param.getRequired() : false;
                                sb.append("- `").append(pName).append("` (").append(pIn).append(")")
                                        .append(req ? " **Required**" : "");
                                if (param.getDescription() != null) {
                                    sb.append(" - ").append(param.getDescription().replace("\n", " "));
                                }
                                sb.append("\n");
                            }
                            sb.append("\n");
                        }

                        if (op.getRequestBody() != null) {
                            sb.append("**Request Body:**\n\n");
                            if (op.getRequestBody().getDescription() != null) {
                                sb.append(op.getRequestBody().getDescription()).append("\n\n");
                            }
                            if (op.getRequestBody().getContent() != null) {
                                for (String mime : op.getRequestBody().getContent().keySet()) {
                                    sb.append("- Content-Type: `").append(mime).append("`\n");
                                }
                            }
                            sb.append("\n");
                        }
                    }
                }
            }
            sb.append("---\n\n");
        }

        return sb.toString();
    }

    private Color getMethodColor(String method) {
        if (method == null)
            return UIManager.getColor("Label.foreground");
        return switch (method.toUpperCase()) {
            case "GET" -> new Color(39, 174, 96);
            case "POST" -> new Color(52, 152, 219);
            case "PUT" -> new Color(243, 156, 18);
            case "PATCH" -> new Color(241, 196, 15);
            case "DELETE" -> new Color(231, 76, 60);
            case "HEAD", "OPTIONS" -> new Color(155, 89, 182);
            default -> UIManager.getColor("Label.foreground");
        };
    }

    public void updateFontSize(int size) {
        FontScaleHelper.scaleFonts(this, size);
        if (table != null && table.getTableHeader() != null) {
            table.getTableHeader().setPreferredSize(new Dimension(-1, size + 16));
        }
        revalidate();
        repaint();
    }
}
