package in.slpro.japi.ui;

import in.slpro.japi.model.CollectionModel;
import in.slpro.japi.model.RequestModel;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.List;

public class SidebarPanel extends JPanel {
    private final MainFrame mainFrame;

    // Collections
    private DefaultTreeModel collectionsTreeModel;
    private DefaultMutableTreeNode collectionsRoot;
    private JTree collectionsTree;

    // History
    private DefaultTreeModel historyTreeModel;
    private DefaultMutableTreeNode historyRoot;
    private JTree historyTree;

    public SidebarPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(240, 0));
        setBackground(UIManager.getColor("Sidebar.background"));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        tabs.addTab("Collections", buildCollectionsPanel());
        tabs.addTab("History", buildHistoryPanel());

        add(tabs, BorderLayout.CENTER);
    }

    // ─── Collections ────────────────────────────────────────────────────────

    private JPanel buildCollectionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIManager.getColor("Sidebar.background"));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        toolbar.setBackground(UIManager.getColor("Sidebar.toolbarBackground"));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Sidebar.borderColor")));

        JButton newCollBtn = new JButton("+ Collection");
        newCollBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        newCollBtn.addActionListener(e -> createCollection());

        JButton newReqBtn = new JButton("+ Request");
        newReqBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        newReqBtn.addActionListener(e -> createRequest());

        toolbar.add(newCollBtn);
        toolbar.add(newReqBtn);
        panel.add(toolbar, BorderLayout.NORTH);

        // Tree
        collectionsRoot = new DefaultMutableTreeNode("Collections");
        collectionsTreeModel = new DefaultTreeModel(collectionsRoot) {
            @Override
            public void valueForPathChanged(TreePath path, Object newValue) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                String newName = (newValue != null) ? newValue.toString().trim() : "";
                if (!newName.isEmpty()) {
                    if (node.getUserObject() instanceof CollectionModel col) {
                        col.setName(newName);
                        nodeChanged(node);
                        mainFrame.saveCollections();
                    } else if (node.getUserObject() instanceof RequestModel req) {
                        req.setName(newName);
                        nodeChanged(node);
                        mainFrame.saveCollections();
                        mainFrame.updateTabTitle(req);
                    }
                }
            }
        };
        collectionsTree = new JTree(collectionsTreeModel);
        collectionsTree.setRootVisible(false);
        collectionsTree.setShowsRootHandles(true);
        collectionsTree.setBackground(UIManager.getColor("Sidebar.treeBackground"));
        collectionsTree.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        collectionsTree.setRowHeight(28);
        collectionsTree.setCellRenderer(new CollectionTreeRenderer());
        collectionsTree.setCellEditor(new javax.swing.tree.DefaultTreeCellEditor(collectionsTree, (javax.swing.tree.DefaultTreeCellRenderer) collectionsTree.getCellRenderer()) {
            @Override
            public boolean isCellEditable(EventObject event) {
                if (event instanceof MouseEvent) {
                    return false;
                }
                return super.isCellEditable(event);
            }
        });
        collectionsTree.setEditable(true);

        collectionsTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path = collectionsTree.getPathForLocation(e.getX(), e.getY());
                if (path == null) return;
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

                if (e.getClickCount() == 2) {
                    if (node.getUserObject() instanceof RequestModel req) {
                        mainFrame.openRequest(req);
                    }
                }
                if (SwingUtilities.isRightMouseButton(e)) {
                    showCollectionContextMenu(e.getX(), e.getY(), node);
                }
            }
        });

        panel.add(new JScrollPane(collectionsTree), BorderLayout.CENTER);
        return panel;
    }

    public void refreshCollections(List<CollectionModel> collections) {
        collectionsRoot.removeAllChildren();
        for (CollectionModel collection : collections) {
            DefaultMutableTreeNode collNode = new DefaultMutableTreeNode(collection);
            for (RequestModel req : collection.getRequests()) {
                collNode.add(new DefaultMutableTreeNode(req));
            }
            collectionsRoot.add(collNode);
        }
        collectionsTreeModel.reload();
        expandAllNodes(collectionsTree);
    }

    private void expandAllNodes(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private void createCollection() {
        String name = JOptionPane.showInputDialog(this, "Collection name:", "New Collection", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return;
        mainFrame.createCollection(name);
    }

    private void createRequest() {
        TreePath path = collectionsTree.getSelectionPath();
        CollectionModel targetCollection = null;
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node.getUserObject() instanceof CollectionModel col) {
                targetCollection = col;
            } else if (node.getUserObject() instanceof RequestModel && node.getParent() instanceof DefaultMutableTreeNode parent) {
                if (parent.getUserObject() instanceof CollectionModel col) targetCollection = col;
            }
        }
        if (targetCollection == null) {
            List<CollectionModel> cols = mainFrame.getCollections();
            if (cols.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please create a collection first.", "No Collection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            targetCollection = (CollectionModel) JOptionPane.showInputDialog(this,
                    "Select collection:", "Add Request", JOptionPane.PLAIN_MESSAGE,
                    null, cols.toArray(), cols.get(0));
        }
        if (targetCollection == null) return;
        String name = JOptionPane.showInputDialog(this, "Request name:", "New Request", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return;
        mainFrame.addRequestToCollection(targetCollection, name);
    }

    private void showCollectionContextMenu(int x, int y, DefaultMutableTreeNode node) {
        JPopupMenu menu = new JPopupMenu();
        if (node.getUserObject() instanceof CollectionModel col) {
            JMenuItem rename = new JMenuItem("Rename Collection");
            rename.addActionListener(e -> {
                collectionsTree.startEditingAtPath(new TreePath(node.getPath()));
            });
            
            JMenu addMenu = new JMenu("Add");
            JMenuItem addReqItem = new JMenuItem("Add Request");
            addReqItem.addActionListener(e -> createRequest());
            JMenuItem addRunnerItem = new JMenuItem("Add Runner");
            addRunnerItem.addActionListener(e -> mainFrame.addRunnerToCollection(col));
            JMenuItem addJwtItem = new JMenuItem("Add JWT");
            addJwtItem.addActionListener(e -> mainFrame.openJwtDecoder());
            JMenuItem addCompItem = new JMenuItem("Add Data Comparator");
            addCompItem.addActionListener(e -> mainFrame.addComparatorToCollection(col));
            JMenuItem addMockItem = new JMenuItem("Add Mock Server");
            addMockItem.addActionListener(e -> mainFrame.addMockServerToCollection(col));
            JMenuItem addJsonItem = new JMenuItem("Add JSON Formatter");
            addJsonItem.addActionListener(e -> mainFrame.openJsonTool());
            
            addMenu.add(addReqItem);
            addMenu.add(addRunnerItem);
            addMenu.add(addJwtItem);
            addMenu.add(addCompItem);
            addMenu.add(addMockItem);
            addMenu.add(addJsonItem);

            JMenuItem delete = new JMenuItem("Delete Collection");
            delete.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this, "Delete collection \"" + col.getName() + "\"?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) mainFrame.deleteCollection(col);
            });
            JMenuItem importCol = new JMenuItem("Import from Postman...");
            importCol.addActionListener(e -> mainFrame.importPostmanCollection());
            JMenuItem exportCol = new JMenuItem("Export Collection...");
            exportCol.addActionListener(e -> mainFrame.exportCollection(col));
            
            boolean isOthers = MainFrame.OTHERS_COLLECTION_ID.equals(col.getId());

            menu.add(rename);
            if (isOthers) rename.setEnabled(false);
            menu.add(addMenu);
            menu.addSeparator();
            menu.add(importCol);
            menu.add(exportCol);
            menu.addSeparator();
            menu.add(delete);
            if (isOthers) delete.setEnabled(false);
        } else if (node.getUserObject() instanceof RequestModel req) {
            JMenuItem open = new JMenuItem("Open");
            open.addActionListener(e -> mainFrame.openRequest(req));
            JMenuItem rename = new JMenuItem("Rename");
            rename.addActionListener(e -> {
                collectionsTree.startEditingAtPath(new TreePath(node.getPath()));
            });
            JMenuItem duplicate = new JMenuItem("Duplicate");
            duplicate.addActionListener(e -> mainFrame.duplicateRequest(req));
            JMenuItem saveAs = new JMenuItem("Save As...");
            saveAs.addActionListener(e -> mainFrame.saveRequestAs(req));
            JMenuItem moveTo = new JMenuItem("Move to Collection...");
            moveTo.addActionListener(e -> mainFrame.moveRequestToCollection(req));
            JMenuItem delete = new JMenuItem("Delete");
            delete.addActionListener(e -> mainFrame.deleteRequest(req));
            menu.add(open);
            menu.add(rename);
            menu.add(duplicate);
            menu.add(saveAs);
            menu.add(moveTo);
            menu.addSeparator();
            menu.add(delete);
        }
        menu.show(collectionsTree, x, y);
    }

    // ─── History ─────────────────────────────────────────────────────────────

    private JPanel buildHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIManager.getColor("Sidebar.background"));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        toolbar.setBackground(UIManager.getColor("Sidebar.toolbarBackground"));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Sidebar.borderColor")));
        JButton clearBtn = new JButton("Clear All");
        clearBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        clearBtn.addActionListener(e -> {
            mainFrame.clearHistory();
            refreshHistory(new ArrayList<>());
        });
        toolbar.add(clearBtn);
        panel.add(toolbar, BorderLayout.NORTH);

        historyRoot = new DefaultMutableTreeNode("History");
        historyTreeModel = new DefaultTreeModel(historyRoot);
        historyTree = new JTree(historyTreeModel);
        historyTree.setRootVisible(false);
        historyTree.setShowsRootHandles(true);
        historyTree.setBackground(UIManager.getColor("Sidebar.treeBackground"));
        historyTree.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        historyTree.setRowHeight(26);
        historyTree.setCellRenderer(new HistoryTreeRenderer());
        ToolTipManager.sharedInstance().registerComponent(historyTree);

        historyTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) return;
                TreePath path = historyTree.getPathForLocation(e.getX(), e.getY());
                if (path == null) return;
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.getUserObject() instanceof RequestModel req) {
                    mainFrame.openHistoryRequest(req);
                }
            }
        });

        panel.add(new JScrollPane(historyTree), BorderLayout.CENTER);
        return panel;
    }

    public void refreshHistory(List<RequestModel> history) {
        historyRoot.removeAllChildren();

        // Group by Year desc -> Month desc -> Date desc
        Map<Integer, Map<java.time.Month, Map<Integer, List<RequestModel>>>> nestedGroup = new TreeMap<>(Collections.reverseOrder());

        for (RequestModel req : history) {
            LocalDate date = req.getTimestamp() != null
                    ? Instant.ofEpochMilli(req.getTimestamp()).atZone(ZoneId.systemDefault()).toLocalDate()
                    : LocalDate.now();

            nestedGroup.computeIfAbsent(date.getYear(), y -> new TreeMap<>((m1, m2) -> Integer.compare(m2.getValue(), m1.getValue())))
                       .computeIfAbsent(date.getMonth(), m -> new TreeMap<>(Collections.reverseOrder()))
                       .computeIfAbsent(date.getDayOfMonth(), d -> new ArrayList<>())
                       .add(req);
        }

        for (Map.Entry<Integer, Map<java.time.Month, Map<Integer, List<RequestModel>>>> yEntry : nestedGroup.entrySet()) {
            DefaultMutableTreeNode yearNode = new DefaultMutableTreeNode(yEntry.getKey().toString());
            historyRoot.add(yearNode);

            for (Map.Entry<java.time.Month, Map<Integer, List<RequestModel>>> mEntry : yEntry.getValue().entrySet()) {
                String mName = mEntry.getKey().name();
                mName = mName.substring(0, 1).toUpperCase() + mName.substring(1).toLowerCase();
                DefaultMutableTreeNode monthNode = new DefaultMutableTreeNode(mName);
                yearNode.add(monthNode);

                for (Map.Entry<Integer, List<RequestModel>> dEntry : mEntry.getValue().entrySet()) {
                    DefaultMutableTreeNode dateNode = new DefaultMutableTreeNode(String.format("%02d", dEntry.getKey()));
                    monthNode.add(dateNode);

                    // Sort within day desc by timestamp
                    List<RequestModel> dayReqs = new ArrayList<>(dEntry.getValue());
                    dayReqs.sort((a, b) -> {
                        long ta = a.getTimestamp() != null ? a.getTimestamp() : 0;
                        long tb = b.getTimestamp() != null ? b.getTimestamp() : 0;
                        return Long.compare(tb, ta);
                    });

                    for (RequestModel req : dayReqs) {
                        dateNode.add(new DefaultMutableTreeNode(req));
                    }
                }
            }
        }

        historyTreeModel.reload();
        expandAllNodes(historyTree);
    }

    // ─── Renderers ────────────────────────────────────────────────────────────

    private static class CollectionTreeRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                       boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode node) {
                if (node.getUserObject() instanceof CollectionModel col) {
                    setText("📁 " + col.getName());
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if (node.getUserObject() instanceof RequestModel req) {
                    if ("runner".equals(req.getType())) {
                        Color runnerColor = new Color(255, 108, 55); // Postman Orange
                        setText("<html><span style='color:" + toHex(runnerColor) + ";font-weight:bold;'>RUNNER</span> " + req.getName() + "</html>");
                    } else if ("comparator".equals(req.getType())) {
                        Color compColor = new Color(142, 68, 173); // Purple
                        setText("<html><span style='color:" + toHex(compColor) + ";font-weight:bold;'>COMPARE</span> " + req.getName() + "</html>");
                    } else if ("mockserver".equals(req.getType())) {
                        Color mockColor = new Color(41, 128, 185); // Blue
                        setText("<html><span style='color:" + toHex(mockColor) + ";font-weight:bold;'>MOCK</span> " + req.getName() + "</html>");
                    } else {
                        String method = req.getMethod() != null ? req.getMethod() : "GET";
                        Color methodColor = getMethodColor(method);
                        setText("<html><span style='color:" + toHex(methodColor) + ";font-weight:bold;'>" +
                                method + "</span> " + req.getName() + "</html>");
                    }
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
            }
            setBackground(selected ? UIManager.getColor("Sidebar.selectionBackground") : UIManager.getColor("Sidebar.treeBackground"));
            setOpaque(true);
            return this;
        }
    }

    private static class HistoryTreeRenderer extends DefaultTreeCellRenderer {
        private static String getStatusText(int code) {
            return switch (code) {
                case 0 -> "Error";
                case 200 -> "OK"; case 201 -> "Created"; case 204 -> "No Content";
                case 301 -> "Moved Permanently"; case 302 -> "Found"; case 304 -> "Not Modified";
                case 400 -> "Bad Request"; case 401 -> "Unauthorized"; case 403 -> "Forbidden";
                case 404 -> "Not Found"; case 405 -> "Method Not Allowed"; case 409 -> "Conflict";
                case 422 -> "Unprocessable Entity"; case 429 -> "Too Many Requests";
                case 500 -> "Internal Server Error"; case 502 -> "Bad Gateway";
                case 503 -> "Service Unavailable"; case 504 -> "Gateway Timeout";
                default -> "Unknown";
            };
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                       boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode node) {
                if (node.getUserObject() instanceof RequestModel req) {
                    String method = req.getMethod() != null ? req.getMethod() : "GET";
                    String url = req.getUrl() != null ? req.getUrl() : "";
                    String displayUrl = url.isEmpty() ? "(No URL)" : (url.length() > 40 ? url.substring(0, 40) + "…" : url);
                    Color methodColor = getMethodColor(method);
                    setText("<html><span style='color:" + toHex(methodColor) + ";font-weight:bold;'>" +
                            method + "</span> " + displayUrl + "</html>");

                    // Tooltip showing full URL + status
                    StringBuilder tip = new StringBuilder("<html>");
                    if (req.getResponseStatus() != null) {
                        int code = req.getResponseStatus();
                        tip.append("<b>Status:</b> ").append(code).append(" ").append(getStatusText(code)).append("<br>");
                    }
                    String fullUrl = req.getActualUrl() != null ? req.getActualUrl() : url;
                    if (fullUrl.isEmpty()) {
                        fullUrl = "(No URL)";
                    }
                    tip.append("<b>URL:</b> ").append(fullUrl).append("</html>");
                    setToolTipText(tip.toString());
                } else {
                    // Date header node (Year, Month, or Date)
                    setText("<html><b>" + node.getUserObject() + "</b></html>");
                    setFont(getFont().deriveFont(Font.BOLD, 11f));
                    setToolTipText(null);
                }
            }
            setBackground(selected ? UIManager.getColor("Sidebar.selectionBackground") : UIManager.getColor("Sidebar.treeBackground"));
            setOpaque(true);
            return this;
        }
    }

    public void updateFontSize(int size) {
        FontScaleHelper.scaleFonts(this, size);
        collectionsTree.setRowHeight(size + 16);
        historyTree.setRowHeight(size + 14);
        collectionsTree.repaint();
        historyTree.repaint();
    }

    private static Color getMethodColor(String method) {
        return switch (method.toUpperCase()) {
            case "GET" -> new Color(39, 174, 96);
            case "POST" -> new Color(52, 152, 219);
            case "PUT" -> new Color(230, 126, 34);
            case "PATCH" -> new Color(155, 89, 182);
            case "DELETE" -> new Color(192, 57, 43);
            default -> new Color(100, 100, 100);
        };
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
