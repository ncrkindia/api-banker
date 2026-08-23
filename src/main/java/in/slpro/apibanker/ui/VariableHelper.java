package in.slpro.apibanker.ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;

import in.slpro.apibanker.model.CollectionModel;
import in.slpro.apibanker.model.EnvironmentModel;
import in.slpro.apibanker.model.KeyValueItem;
import in.slpro.apibanker.model.RequestModel;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * VariableHelper
 *
 * <p>
 * Core functionality and implementation logic for VariableHelper.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 2.0.0
 * @since 1.0.0
 */
public class VariableHelper {
    public static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    // Dynamic, theme-aware colors
    public static Color getEnvColor() {
        return com.formdev.flatlaf.FlatLaf.isLafDark()
                ? new Color(129, 207, 253) // Lighter blue for dark theme
                : new Color(41, 128, 185); // Darker blue for light theme
    }

    public static Color getCollectionColor() {
        return com.formdev.flatlaf.FlatLaf.isLafDark()
                ? new Color(241, 196, 15) // Bright gold/yellow for dark theme
                : new Color(204, 140, 0); // Amber yellow for light theme
    }

    public static Color getUnresolvedColor() {
        return com.formdev.flatlaf.FlatLaf.isLafDark()
                ? new Color(239, 109, 109) // Lighter red for dark theme
                : new Color(192, 57, 43); // Darker red for light theme
    }

    public static Color getGlobalColor() {
        return com.formdev.flatlaf.FlatLaf.isLafDark()
                ? new Color(155, 89, 182) // Purple for dark theme
                : new Color(142, 68, 173); // Dark purple for light theme
    }

    public static class VariableResolution {
        public final String name;
        public final String source; // Environment name, Collection name, etc.
        public final String value;
        public final boolean resolved;
        public final String type; // "env", "collection", "global"

        public VariableResolution(String name, String source, String value, boolean resolved, String type) {
            this.name = name;
            this.source = source;
            this.value = value;
            this.resolved = resolved;
            this.type = type;
        }

        public Color getColor() {
            if (!resolved) return getUnresolvedColor();
            if ("env".equals(type)) return getEnvColor();
            if ("global".equals(type)) return getGlobalColor();
            return getCollectionColor();
        }
    }

    public static VariableResolution resolveVariable(String varName, CollectionModel collection, MainFrame mainFrame) {
        if (mainFrame == null) {
            mainFrame = MainFrame.getInstance();
        }
        if (mainFrame == null) {
            return new VariableResolution(varName, "Unresolved", null, false, "unresolved");
        }

        // 1. Check Active Environment
        EnvironmentModel activeEnv = mainFrame.getActiveEnvironment();
        if (activeEnv != null && activeEnv.getVariables() != null) {
            for (KeyValueItem kv : activeEnv.getVariables()) {
                if (kv.isEnabled() && varName.equals(kv.getKey())) {
                    String resolvedVal = in.slpro.apibanker.model.VariableHelper.resolveVariables(kv.getValue(), collection, activeEnv);
                    return new VariableResolution(varName, "Environment (" + activeEnv.getName() + ")", resolvedVal,
                            true, "env");
                }
            }
        }

        // 2. Check Collection-level Variables
        if (collection != null && collection.getVariables() != null) {
            for (KeyValueItem kv : collection.getVariables()) {
                if (kv.isEnabled() && varName.equals(kv.getKey())) {
                    String resolvedVal = in.slpro.apibanker.model.VariableHelper.resolveVariables(kv.getValue(), collection, activeEnv);
                    return new VariableResolution(varName, "Collection (" + collection.getName() + ")", resolvedVal,
                            true, "collection");
                }
            }
        }

        // 3. Check Global Variables
        java.util.List<KeyValueItem> globals = in.slpro.apibanker.storage.StorageManager.getInstance().getSettings().getGlobalVariables();
        if (globals != null) {
            for (KeyValueItem kv : globals) {
                if (kv.isEnabled() && varName.equals(kv.getKey())) {
                    String resolvedVal = in.slpro.apibanker.model.VariableHelper.resolveVariables(kv.getValue(), collection, activeEnv);
                    return new VariableResolution(varName, "Global", resolvedVal, true, "global");
                }
            }
        }

        return new VariableResolution(varName, "Unresolved", null, false, "unresolved");
    }

    public static VariableResolution resolveVariable(String varName, RequestModel requestModel, MainFrame mainFrame) {
        CollectionModel collection = MainFrame.findParentCollection(requestModel);
        return resolveVariable(varName, collection, mainFrame);
    }

    public static void attachToTextComponent(JTextComponent comp, RequestModel requestModel, MainFrame mainFrame) {
        attachToTextComponent(comp, MainFrame.findParentCollection(requestModel), mainFrame);
    }

    public static void attachToTextComponent(JTextComponent comp, CollectionModel collection, MainFrame mainFrame) {
        comp.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            private void update() {
                SwingUtilities.invokeLater(() -> comp.repaint());
            }
        });

        // Set the collection context if it is a custom Highlight component
        if (comp instanceof HighlightTextField htf) {
            htf.setCollectionContext(collection, mainFrame);
        } else if (comp instanceof HighlightRSyntaxTextArea hrsa) {
            hrsa.setCollectionContext(collection, mainFrame);
        }

        comp.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            @SuppressWarnings("deprecation")
            public void mouseMoved(MouseEvent e) {
                int pos;
                try {
                    pos = comp.viewToModel2D(e.getPoint());
                } catch (NoSuchMethodError ex) {
                    pos = comp.viewToModel(e.getPoint());
                }
                if (pos >= 0) {
                    String text = comp.getText();
                    Matcher matcher = VAR_PATTERN.matcher(text);
                    while (matcher.find()) {
                        if (pos >= matcher.start() && pos <= matcher.end()) {
                            String varName = matcher.group(1).trim();
                            VariableResolution res = resolveVariable(varName, collection, mainFrame);
                            String tooltip;
                            if (res.resolved) {
                                tooltip = String.format("<html><body style='font-family: sans-serif; padding: 2px;'>" +
                                        "<b>Variable:</b> %s<br/>" +
                                        "<b>Source:</b> %s<br/>" +
                                        "<b>Current Value:</b> <font color='green'>%s</font>" +
                                        "</body></html>",
                                        res.name, res.source, res.value);
                            } else {
                                tooltip = String.format("<html><body style='font-family: sans-serif; padding: 2px;'>" +
                                        "<b>Variable:</b> %s<br/>" +
                                        "<b>Source:</b> <font color='red'>Unresolved</font>" +
                                        "</body></html>",
                                        res.name);
                            }
                            comp.setToolTipText(tooltip);
                            return;
                        }
                    }
                }
                comp.setToolTipText(null);
            }
        });

        // Trigger initial repaint
        SwingUtilities.invokeLater(() -> comp.repaint());
    }
}

