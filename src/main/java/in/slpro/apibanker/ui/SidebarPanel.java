package in.slpro.apibanker.ui;

import javax.swing.*;
import javax.swing.tree.*;

import in.slpro.apibanker.model.CollectionModel;
import in.slpro.apibanker.model.RequestModel;

import java.awt.*;
import java.awt.event.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.List;

/**
 * SidebarPanel
 *
 * <p>
 * This panel represents the primary navigation structure of the ApiBanker IDE.
 * It provides a dual-tabbed JTree interface for navigating saved
 * {@link CollectionModel}s
 * and reviewing the chronological execution {@link RequestModel} History. It
 * acts as the
 * drag-and-drop controller for organizing folders and routing node selection
 * events
 * to the {@link MainFrame} workspace.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.0.0-beta
 * @since 1.0.0
 */
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

    private boolean isRefreshingTree = false;

    /**
     * Constructs the primary Sidebar navigation panel.
     * <p>
     * Initializes the split "Collections" and "History" tabs containing standard
     * Swing JTrees. Configures custom cell renderers for displaying HTTP
     * method-colored
     * badges, and binds tree selection events to open the corresponding tabs in the
     * workspace.
     * </p>
     * 
     * @param mainFrame The root application window (acting as the event router for
     *                  tree clicks).
     */
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
        collectionsTree.setCellEditor(new javax.swing.tree.DefaultTreeCellEditor(collectionsTree,
                (javax.swing.tree.DefaultTreeCellRenderer) collectionsTree.getCellRenderer()) {
            @Override
            public boolean isCellEditable(EventObject event) {
                if (event instanceof MouseEvent) {
                    return false;
                }
                return super.isCellEditable(event);
            }
        });
        collectionsTree.setEditable(true);
        collectionsTree.setToggleClickCount(0);

        collectionsTree.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                javax.swing.tree.TreePath path = collectionsTree.getSelectionPath();
                if (path == null)
                    return;
                javax.swing.tree.DefaultMutableTreeNode node = (javax.swing.tree.DefaultMutableTreeNode) path
                        .getLastPathComponent();
                Object uo = node.getUserObject();

                boolean ctrl = e.isControlDown();
                int code = e.getKeyCode();

                if (code == java.awt.event.KeyEvent.VK_F2) {
                    collectionsTree.startEditingAtPath(path);
                } else if (code == java.awt.event.KeyEvent.VK_DELETE) {
                    if (uo instanceof CollectionModel c)
                        mainFrame.deleteCollection(c);
                    else if (uo instanceof RequestModel r)
                        mainFrame.deleteRequest(r);
                } else if (ctrl && code == java.awt.event.KeyEvent.VK_O) {
                    if (uo instanceof RequestModel r)
                        mainFrame.openRequest(r);
                    else if (uo instanceof CollectionModel c)
                        mainFrame.openCollection(c);
                } else if (ctrl && code == java.awt.event.KeyEvent.VK_C) {
                    clipboardNode = uo;
                    MainFrame.showToast(SidebarPanel.this, "Copied");
                } else if (ctrl && code == java.awt.event.KeyEvent.VK_V) {
                    handlePaste(node);
                } else if (ctrl && code == java.awt.event.KeyEvent.VK_D) {
                    clipboardNode = uo;
                    handlePaste(node);
                }
            }
        });

        collectionsTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = collectionsTree.getClosestRowForLocation(e.getX(), e.getY());
                if (row == -1)
                    return;
                Rectangle bounds = collectionsTree.getRowBounds(row);
                if (bounds == null || e.getY() < bounds.y || e.getY() >= bounds.y + bounds.height)
                    return;
                TreePath path = collectionsTree.getPathForRow(row);
                if (path == null)
                    return;
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

                if (SwingUtilities.isRightMouseButton(e)) {
                    collectionsTree.setSelectionPath(path);
                    showCollectionContextMenu(e.getX(), e.getY(), node);
                } else if (e.getClickCount() == 2) {
                    if (node.getUserObject() instanceof RequestModel req) {
                        mainFrame.openRequest(req);
                    } else if (node.getUserObject() instanceof CollectionModel col) {
                        mainFrame.openCollection(col);
                    }
                }
            }
        });

        collectionsTree.addTreeExpansionListener(new javax.swing.event.TreeExpansionListener() {
            @Override
            public void treeExpanded(javax.swing.event.TreeExpansionEvent event) {
                saveExpandedState();
            }

            @Override
            public void treeCollapsed(javax.swing.event.TreeExpansionEvent event) {
                saveExpandedState();
            }
        });

        panel.add(new JScrollPane(collectionsTree), BorderLayout.CENTER);
        return panel;
    }

    private void saveExpandedState() {
        if (!isRefreshingTree) {
            java.util.List<String> expandedIds = getExpandedNodeIds(collectionsTree);
            in.slpro.apibanker.storage.StorageManager.getInstance().getSettings().setExpandedTreeNodes(expandedIds);
            in.slpro.apibanker.storage.StorageManager.getInstance().saveSettings();
        }
    }

    public void refreshCollections(List<CollectionModel> collections) {
        isRefreshingTree = true;
        java.util.List<String> expandedIds = in.slpro.apibanker.storage.StorageManager.getInstance().getSettings()
                .getExpandedTreeNodes();
        if (expandedIds == null) {
            expandedIds = new ArrayList<>();
        }
        collectionsRoot.removeAllChildren();
        for (CollectionModel collection : collections) {
            DefaultMutableTreeNode collNode = new DefaultMutableTreeNode(collection);
            populateCollectionNode(collNode, collection);
            collectionsRoot.add(collNode);
        }
        collectionsTreeModel.reload();
        restoreExpandedNodes(collectionsTree, collectionsRoot, expandedIds);
        isRefreshingTree = false;
    }

    private List<String> getExpandedNodeIds(JTree tree) {
        List<String> expandedIds = new ArrayList<>();
        for (int i = 0; i < tree.getRowCount(); i++) {
            if (tree.isExpanded(i)) {
                TreePath path = tree.getPathForRow(i);
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObject = node.getUserObject();
                if (userObject instanceof CollectionModel col) {
                    expandedIds.add("COL_" + col.getId());
                } else if (userObject instanceof RequestModel req) {
                    expandedIds.add("REQ_" + req.getId());
                } else if (userObject instanceof String str) {
                    expandedIds.add("STR_" + str);
                }
            }
        }
        return expandedIds;
    }

    private void restoreExpandedNodes(JTree tree, DefaultMutableTreeNode node, List<String> expandedIds) {
        Object userObject = node.getUserObject();
        String id = null;
        if (userObject instanceof CollectionModel col) {
            id = "COL_" + col.getId();
        } else if (userObject instanceof RequestModel req) {
            id = "REQ_" + req.getId();
        } else if (userObject instanceof String str) {
            id = "STR_" + str;
        }

        if (id != null && expandedIds.contains(id)) {
            tree.expandPath(new TreePath(node.getPath()));
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            restoreExpandedNodes(tree, (DefaultMutableTreeNode) node.getChildAt(i), expandedIds);
        }
    }

    private void populateCollectionNode(DefaultMutableTreeNode node, CollectionModel collection) {
        if (collection.getFolders() != null) {
            for (CollectionModel subFolder : collection.getFolders()) {
                DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(subFolder);
                populateCollectionNode(folderNode, subFolder);
                node.add(folderNode);
            }
        }
        if (collection.getRequests() != null) {
            for (RequestModel req : collection.getRequests()) {
                node.add(new DefaultMutableTreeNode(req));
            }
        }
    }

    private void createCollection() {
        String name = JOptionPane.showInputDialog(this, "Collection name:", "New Collection",
                JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank())
            return;
        mainFrame.createCollection(name);
    }

    private void createFolder(DefaultMutableTreeNode node) {
        if (node.getUserObject() instanceof CollectionModel parentCol) {
            String name = JOptionPane.showInputDialog(this, "Folder name:", "New Folder", JOptionPane.PLAIN_MESSAGE);
            if (name == null || name.isBlank())
                return;
            CollectionModel newFolder = new CollectionModel();
            newFolder.setId(java.util.UUID.randomUUID().toString());
            newFolder.setName(name);
            parentCol.getFolders().add(newFolder);
            mainFrame.saveCollections();
            refreshCollections(mainFrame.getCollections());
        }
    }

    private void createRequest() {
        TreePath path = collectionsTree.getSelectionPath();
        CollectionModel targetCollection = null;
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node.getUserObject() instanceof CollectionModel col) {
                targetCollection = col;
            } else if (node.getUserObject() instanceof RequestModel
                    && node.getParent() instanceof DefaultMutableTreeNode parent) {
                if (parent.getUserObject() instanceof CollectionModel col)
                    targetCollection = col;
            }
        }
        if (targetCollection == null) {
            List<MainFrame.CollectionPathWrapper> wrappers = mainFrame.getAllCollectionsAndFoldersWithPaths();
            if (wrappers.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please create a collection first.", "No Collection",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            MainFrame.CollectionPathWrapper selected = (MainFrame.CollectionPathWrapper) JOptionPane.showInputDialog(
                    this,
                    "Select collection/folder:", "Add Request", JOptionPane.PLAIN_MESSAGE,
                    null, wrappers.toArray(), wrappers.get(0));
            if (selected == null)
                return;
            targetCollection = selected.model;
        }
        if (targetCollection == null)
            return;
        String name = JOptionPane.showInputDialog(this, "Request name:", "New Request", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank())
            return;
        mainFrame.addRequestToCollection(targetCollection, name);
    }

    private void showCollectionContextMenu(int x, int y, DefaultMutableTreeNode node) {
        JPopupMenu menu = new JPopupMenu();
        if (node.getUserObject() instanceof CollectionModel col) {
            JMenuItem rename = new JMenuItem("Rename Collection");
            rename.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0));
            rename.addActionListener(e -> {
                collectionsTree.startEditingAtPath(new TreePath(node.getPath()));
            });

            JMenuItem open = new JMenuItem("Open");
            open.setAccelerator(
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_DOWN_MASK));
            open.addActionListener(e -> mainFrame.openCollection(col));

            JMenu addMenu = new JMenu("Add");
            JMenuItem addReqItem = new JMenuItem("Add Request");
            addReqItem.addActionListener(e -> createRequest());
            JMenuItem addFolderItem = new JMenuItem("Add Folder");
            addFolderItem.addActionListener(e -> createFolder(node));
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
            JMenuItem addWsItem = new JMenuItem("Add WebSocket Client");
            addWsItem.addActionListener(e -> mainFrame.addWebSocketToCollection(col));

            addMenu.add(addReqItem);
            addMenu.add(addFolderItem);
            addMenu.add(addRunnerItem);
            addMenu.add(addJwtItem);
            addMenu.add(addCompItem);
            addMenu.add(addMockItem);
            addMenu.add(addJsonItem);
            addMenu.add(addWsItem);

            JMenuItem delete = new JMenuItem("Delete Collection");
            delete.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
            delete.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this, "Delete collection \"" + col.getName() + "\"?",
                        "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION)
                    mainFrame.deleteCollection(col);
            });

            JMenuItem copy = new JMenuItem("Copy");
            copy.setAccelerator(
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_DOWN_MASK));
            copy.addActionListener(e -> {
                clipboardNode = node.getUserObject();
                MainFrame.showToast(SidebarPanel.this, "Copied");
            });
            JMenuItem paste = new JMenuItem("Paste");
            paste.setAccelerator(
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_DOWN_MASK));
            paste.addActionListener(e -> handlePaste(node));
            JMenuItem duplicateCol = new JMenuItem("Duplicate");
            duplicateCol.setAccelerator(
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_DOWN_MASK));
            duplicateCol.addActionListener(e -> {
                clipboardNode = node.getUserObject();
                handlePaste(node);
            });
            JMenu importMenu = new JMenu("Import");
            JMenuItem importApiBankerItem = new JMenuItem("Import ApiBanker Files");
            importApiBankerItem.addActionListener(e -> mainFrame.importApiBankerFiles());
            JMenuItem importPostmanItem = new JMenuItem("Import Postman Files");
            importPostmanItem.addActionListener(e -> mainFrame.importPostmanFiles());
            importMenu.add(importApiBankerItem);
            importMenu.add(importPostmanItem);

            JMenu exportMenu = new JMenu("Export Collection");
            JMenuItem exportApiBankerItem = new JMenuItem("Export as ApiBanker Collection");
            exportApiBankerItem.addActionListener(e -> mainFrame.exportApiBankerCollection(col));
            JMenuItem exportPostmanItem = new JMenuItem("Export as Postman Collection");
            exportPostmanItem.addActionListener(e -> mainFrame.exportCollection(col));
            exportMenu.add(exportApiBankerItem);
            exportMenu.add(exportPostmanItem);

            boolean isOthers = MainFrame.OTHERS_COLLECTION_ID.equals(col.getId());

            menu.add(open);
            menu.add(rename);
            if (isOthers)
                rename.setEnabled(false);
            menu.add(copy);
            menu.add(paste);
            menu.add(duplicateCol);
            menu.addSeparator();
            menu.add(addMenu);
            menu.addSeparator();
            menu.add(importMenu);
            menu.add(exportMenu);
            menu.addSeparator();
            menu.add(delete);
            if (isOthers)
                delete.setEnabled(false);
        } else if (node.getUserObject() instanceof RequestModel req) {
            JMenuItem open = new JMenuItem("Open");
            open.setAccelerator(
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_DOWN_MASK));
            open.addActionListener(e -> mainFrame.openRequest(req));
            JMenuItem rename = new JMenuItem("Rename");
            rename.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0));
            rename.addActionListener(e -> {
                collectionsTree.startEditingAtPath(new TreePath(node.getPath()));
            });
            JMenuItem duplicate = new JMenuItem("Duplicate");
            duplicate.setAccelerator(
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_DOWN_MASK));
            duplicate.addActionListener(e -> {
                clipboardNode = node.getUserObject();
                handlePaste(node);
            });
            JMenuItem copy = new JMenuItem("Copy");
            copy.setAccelerator(
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_DOWN_MASK));
            copy.addActionListener(e -> {
                clipboardNode = node.getUserObject();
                MainFrame.showToast(SidebarPanel.this, "Copied");
            });
            JMenuItem paste = new JMenuItem("Paste");
            paste.setAccelerator(
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_DOWN_MASK));
            paste.addActionListener(e -> handlePaste(node));

            JMenuItem saveAs = new JMenuItem("Save As...");
            saveAs.addActionListener(e -> mainFrame.saveRequestAs(req));
            JMenuItem moveTo = new JMenuItem("Move to Collection...");
            moveTo.addActionListener(e -> mainFrame.moveRequestToCollection(req));
            JMenuItem delete = new JMenuItem("Delete");
            delete.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
            delete.addActionListener(e -> mainFrame.deleteRequest(req));
            menu.add(open);
            menu.add(rename);
            menu.add(copy);
            menu.add(paste);
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
                if (e.getClickCount() != 2)
                    return;
                int row = historyTree.getClosestRowForLocation(e.getX(), e.getY());
                if (row == -1)
                    return;
                Rectangle bounds = historyTree.getRowBounds(row);
                if (bounds == null || e.getY() < bounds.y || e.getY() >= bounds.y + bounds.height)
                    return;
                TreePath path = historyTree.getPathForRow(row);
                if (path == null)
                    return;
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
        Map<Integer, Map<java.time.Month, Map<Integer, List<RequestModel>>>> nestedGroup = new TreeMap<>(
                Collections.reverseOrder());

        for (RequestModel req : history) {
            LocalDate date = req.getTimestamp() != null
                    ? Instant.ofEpochMilli(req.getTimestamp()).atZone(ZoneId.systemDefault()).toLocalDate()
                    : LocalDate.now();

            nestedGroup
                    .computeIfAbsent(date.getYear(),
                            y -> new TreeMap<>((m1, m2) -> Integer.compare(m2.getValue(), m1.getValue())))
                    .computeIfAbsent(date.getMonth(), m -> new TreeMap<>(Collections.reverseOrder()))
                    .computeIfAbsent(date.getDayOfMonth(), d -> new ArrayList<>())
                    .add(req);
        }

        for (Map.Entry<Integer, Map<java.time.Month, Map<Integer, List<RequestModel>>>> yEntry : nestedGroup
                .entrySet()) {
            DefaultMutableTreeNode yearNode = new DefaultMutableTreeNode(yEntry.getKey().toString());
            historyRoot.add(yearNode);

            for (Map.Entry<java.time.Month, Map<Integer, List<RequestModel>>> mEntry : yEntry.getValue().entrySet()) {
                String mName = mEntry.getKey().name();
                mName = mName.substring(0, 1).toUpperCase() + mName.substring(1).toLowerCase();
                DefaultMutableTreeNode monthNode = new DefaultMutableTreeNode(mName);
                yearNode.add(monthNode);

                for (Map.Entry<Integer, List<RequestModel>> dEntry : mEntry.getValue().entrySet()) {
                    DefaultMutableTreeNode dateNode = new DefaultMutableTreeNode(
                            String.format("%02d", dEntry.getKey()));
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
    }

    // ─── Renderers ────────────────────────────────────────────────────────────

    private static class CollectionTreeRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode node) {
                if (node.getUserObject() instanceof CollectionModel col) {
                    setText(col.getName());
                    setFont(getFont().deriveFont(Font.BOLD));
                    // Keep default FlatLaf folder icon
                } else if (node.getUserObject() instanceof RequestModel req) {
                    Color fgColor = selected ? UIManager.getColor("Tree.selectionForeground")
                            : UIManager.getColor("Tree.foreground");
                    if (fgColor == null)
                        fgColor = UIManager.getColor("Label.foreground");
                    if (fgColor == null)
                        fgColor = selected ? Color.WHITE : Color.BLACK;
                    String fgHex = toHex(fgColor);
                    if ("runner".equals(req.getType())) {
                        Color runnerColor = UIManager.getColor("AccentColor");
                        if (runnerColor == null)
                            runnerColor = new Color(26, 115, 232);
                        setText("<html><span style='color:" + toHex(runnerColor)
                                + ";font-weight:bold;'>RUNNER</span> <span style='color:" + fgHex + "'>"
                                + req.getName() + "</span></html>");
                    } else if ("comparator".equals(req.getType())) {
                        Color compColor = new Color(142, 68, 173); // Purple
                        setText("<html><span style='color:" + toHex(compColor)
                                + ";font-weight:bold;'>COMPARE</span> <span style='color:" + fgHex + "'>"
                                + req.getName() + "</span></html>");
                    } else if ("mockserver".equals(req.getType())) {
                        Color mockColor = new Color(41, 128, 185); // Blue
                        setText("<html><span style='color:" + toHex(mockColor)
                                + ";font-weight:bold;'>MOCK</span> <span style='color:" + fgHex + "'>"
                                + req.getName() + "</span></html>");
                    } else if ("websocket".equals(req.getType())) {
                        Color wsColor = new Color(46, 204, 113); // Green
                        setText("<html><span style='color:" + toHex(wsColor)
                                + ";font-weight:bold;'>WS</span> <span style='color:" + fgHex + "'>"
                                + req.getName() + "</span></html>");
                    } else {
                        String method = req.getMethod() != null ? req.getMethod() : "GET";
                        Color methodColor = getMethodColor(method);
                        setText("<html><span style='color:" + toHex(methodColor) + ";font-weight:bold;'>" +
                                method + "</span> <span style='color:" + fgHex + "'>" + req.getName()
                                + "</span></html>");
                    }
                    setFont(getFont().deriveFont(Font.PLAIN));
                    setIcon(null); // Clear generic file icon next to request methods
                }
            }
            setBackground(selected ? UIManager.getColor("Sidebar.selectionBackground")
                    : UIManager.getColor("Sidebar.treeBackground"));
            setOpaque(true);
            return this;
        }
    }

    private static class HistoryTreeRenderer extends DefaultTreeCellRenderer {
        private static String getStatusText(int code) {
            return switch (code) {
                case 0 -> "Error";
                case 200 -> "OK";
                case 201 -> "Created";
                case 204 -> "No Content";
                case 301 -> "Moved Permanently";
                case 302 -> "Found";
                case 304 -> "Not Modified";
                case 400 -> "Bad Request";
                case 401 -> "Unauthorized";
                case 403 -> "Forbidden";
                case 404 -> "Not Found";
                case 405 -> "Method Not Allowed";
                case 409 -> "Conflict";
                case 422 -> "Unprocessable Entity";
                case 429 -> "Too Many Requests";
                case 500 -> "Internal Server Error";
                case 502 -> "Bad Gateway";
                case 503 -> "Service Unavailable";
                case 504 -> "Gateway Timeout";
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
                    String displayUrl = url.isEmpty() ? "(No URL)"
                            : (url.length() > 40 ? url.substring(0, 40) + "…" : url);
                    Color methodColor = getMethodColor(method);
                    Color fgColor = selected ? UIManager.getColor("Tree.selectionForeground")
                            : UIManager.getColor("Tree.foreground");
                    if (fgColor == null)
                        fgColor = UIManager.getColor("Label.foreground");
                    if (fgColor == null)
                        fgColor = selected ? Color.WHITE : Color.BLACK;
                    String fgHex = toHex(fgColor);
                    setText("<html><span style='color:" + toHex(methodColor) + ";font-weight:bold;'>" +
                            method + "</span> <span style='color:" + fgHex + "'>" + displayUrl + "</span></html>");

                    // Tooltip showing full URL + status
                    StringBuilder tip = new StringBuilder("<html>");
                    if (req.getResponseStatus() != null) {
                        int code = req.getResponseStatus();
                        tip.append("<b>Status:</b> ").append(code).append(" ").append(getStatusText(code))
                                .append("<br>");
                    }
                    String fullUrl = req.getActualUrl() != null ? req.getActualUrl() : url;
                    if (fullUrl.isEmpty()) {
                        fullUrl = "(No URL)";
                    }
                    tip.append("<b>URL:</b> ").append(fullUrl).append("</html>");
                    setToolTipText(tip.toString());
                    setFont(tree.getFont().deriveFont(Font.PLAIN));
                    setIcon(null);
                } else {
                    // Date header node (Year, Month, or Date)
                    Color fgColor = selected ? UIManager.getColor("Tree.selectionForeground")
                            : UIManager.getColor("Tree.foreground");
                    if (fgColor == null)
                        fgColor = UIManager.getColor("Label.foreground");
                    if (fgColor == null)
                        fgColor = selected ? Color.WHITE : Color.BLACK;
                    String fgHex = toHex(fgColor);
                    setText("<html><b style='color:" + fgHex + "'>" + node.getUserObject() + "</b></html>");
                    setFont(tree.getFont().deriveFont(Font.BOLD, tree.getFont().getSize() - 1f));
                    setToolTipText(null);
                    setIcon(null);
                }
            }
            setBackground(selected ? UIManager.getColor("Sidebar.selectionBackground")
                    : UIManager.getColor("Sidebar.treeBackground"));
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

    private static Object clipboardNode = null;

    private void handlePaste(DefaultMutableTreeNode targetNode) {
        if (clipboardNode == null)
            return;
        Object copied = deepCopyModel(clipboardNode);
        if (copied == null)
            return;

        Object targetUserObj = targetNode.getUserObject();
        CollectionModel parentCol = null;
        if (targetUserObj instanceof CollectionModel) {
            parentCol = (CollectionModel) targetUserObj;
        } else if (targetUserObj instanceof RequestModel) {
            parentCol = findParentCollectionOrFolder(targetNode);
        }

        if (parentCol != null) {
            if (copied instanceof RequestModel req) {
                if (parentCol.getRequests() == null)
                    parentCol.setRequests(new ArrayList<>());
                parentCol.getRequests().add(req);
            } else if (copied instanceof CollectionModel folder) {
                if (parentCol.getFolders() == null)
                    parentCol.setFolders(new ArrayList<>());
                parentCol.getFolders().add(folder);
            }
            mainFrame.saveCollections();
            refreshCollections(mainFrame.getCollections());
            MainFrame.showToast(this, "Pasted successfully.");
        }
    }

    private CollectionModel findParentCollectionOrFolder(DefaultMutableTreeNode node) {
        javax.swing.tree.TreeNode parent = node.getParent();
        while (parent != null) {
            if (parent instanceof DefaultMutableTreeNode dNode) {
                Object uo = dNode.getUserObject();
                if (uo instanceof CollectionModel col)
                    return col;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private Object deepCopyModel(Object obj) {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        if (obj instanceof RequestModel) {
            RequestModel cp = gson.fromJson(gson.toJson(obj), RequestModel.class);
            cp.setId(java.util.UUID.randomUUID().toString());
            cp.setName(cp.getName() + " Copy");
            return cp;
        } else if (obj instanceof CollectionModel) {
            CollectionModel cp = gson.fromJson(gson.toJson(obj), CollectionModel.class);
            reassignIdsRecursive(cp);
            cp.setName(cp.getName() + " Copy");
            return cp;
        }
        return null;
    }

    private void reassignIdsRecursive(CollectionModel col) {
        col.setId(java.util.UUID.randomUUID().toString());
        if (col.getRequests() != null) {
            for (RequestModel req : col.getRequests())
                req.setId(java.util.UUID.randomUUID().toString());
        }
        if (col.getFolders() != null) {
            for (CollectionModel folder : col.getFolders())
                reassignIdsRecursive(folder);
        }
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}


