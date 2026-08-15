package in.slpro.apibanker;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import in.slpro.apibanker.ui.MainFrame;

import javax.swing.*;
import java.awt.*;

/**
 * The main entry point for the ApiBanker Desktop Application.
 * 
 * <p>
 * This class is responsible for initializing the application environment,
 * configuring the global Look and Feel (FlatLaf) based on user preferences,
 * setting up theme-specific UI properties (colors, borders, fonts), and
 * launching the primary {@link in.slpro.apibanker.ui.MainFrame} window.
 * </p>
 * 
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.6.0-beta
 * @since 1.0.0
 */
public class App {
    /**
     * The main execution method. Retrieves saved user settings from the
     * {@link in.slpro.apibanker.storage.StorageManager}, applies the appropriate
     * theme,
     * and schedules the creation of the application GUI on the Event Dispatch
     * Thread (EDT).
     *
     * @param args Command line arguments (currently unused)
     */
    public static void main(String[] args) {
        if (System.getProperty("java.home") == null) {

            try {
                java.nio.file.Path tempJre = java.nio.file.Files.createTempDirectory("jre");
                System.setProperty("java.home", tempJre.toAbsolutePath().toString());
            } catch (Exception e) {
                System.setProperty("java.home", ".");
            }
        }

        try {
            System.setProperty("flatlaf.useWindowDecorations", "true");
            System.setProperty("flatlaf.menuBarEmbedded", "true");
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);

            int fontSize = in.slpro.apibanker.storage.StorageManager.getInstance().getSettings().getFontSize();
            if (fontSize < 10)
                fontSize = 16;
            String theme = in.slpro.apibanker.storage.StorageManager.getInstance().getSettings().getTheme();
            setupTheme(theme, fontSize);
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();

            in.slpro.apibanker.model.AppSettings settings = in.slpro.apibanker.storage.StorageManager.getInstance()
                    .getSettings();
            if (settings.getWindowX() != -1 && settings.getWindowY() != -1) {
                frame.setLocation(settings.getWindowX(), settings.getWindowY());
            } else {
                frame.setLocation(0, 0); // Top-left corner
            }

            frame.setVisible(true);
            in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("APP_START", "System",
                    "ApiBanker Version " + getVersion() + " launched.");
        });
    }

    /**
     * Initializes and configures the FlatLaf Look and Feel with custom UI tokens.
     * 
     * <p>
     * This method applies specific color palettes, border radii, and accent colors
     * for either the "dark" or "light" theme. It also globally sets the default
     * font size across all Swing components.
     * </p>
     * 
     * @param theme    The theme identifier ("dark" or "light") to apply.
     * @param fontSize The base font size to set for the default UI font.
     */
    public static void setupTheme(String theme, int fontSize) {
        try {
            boolean isMacOs = System.getProperty("os.name").toLowerCase().contains("mac");
            String uiMode = "Classic";
            try {
                uiMode = in.slpro.apibanker.storage.StorageManager.getInstance().getSettings().getUiMode();
            } catch (Exception ignored) {
            }
            boolean isModern = "Modern".equalsIgnoreCase(uiMode);

            if ("dark".equals(theme)) {
                if (isModern) {
                    com.formdev.flatlaf.themes.FlatMacDarkLaf.setup();
                } else {
                    FlatDarkLaf.setup();
                }

                Color primaryAccentDark = new Color(138, 180, 248); // #8AB4F8

                UIManager.put("AccentColor", primaryAccentDark);

                UIManager.put("Button.arc", isModern ? 999 : 4);
                UIManager.put("Component.arc", isModern ? 12 : 4);
                UIManager.put("TextComponent.arc", isModern ? 12 : 4);
                UIManager.put("ProgressBar.arc", isModern ? 99 : 4);

                // Focus rings and borders
                UIManager.put("Component.focusColor", primaryAccentDark);
                UIManager.put("Component.focusedBorderColor", primaryAccentDark);
                UIManager.put("Button.focusColor", primaryAccentDark);
                UIManager.put("Button.focusedBorderColor", primaryAccentDark);

                UIManager.put("TabbedPane.showTabSeparators", true);
                UIManager.put("TabbedPane.tabType", "underlined");
                UIManager.put("TabbedPane.underlineColor", primaryAccentDark);
                UIManager.put("TabbedPane.inactiveUnderlineColor", primaryAccentDark);
                UIManager.put("TabbedPane.focusColor", new Color(0, 0, 0, 0));
                UIManager.put("TabbedPane.showFocusIndicator", false);
                UIManager.put("TabbedPane.tabHeight", 34);
                UIManager.put("TabbedPane.selectedBackground", new Color(45, 48, 52));
                UIManager.put("TabbedPane.hoverColor", new Color(60, 63, 67));
                UIManager.put("TabbedPane.foreground", new Color(200, 200, 200));
                UIManager.put("TabbedPane.selectedForeground", Color.WHITE);

                Color darkBg = new Color(30, 30, 30);
                Color panelBg = new Color(40, 44, 52);
                Color selectBg = new Color(50, 54, 62);
                Color borderColor = new Color(60, 64, 72);
                Color metricBg = new Color(43, 47, 55);

                UIManager.put("Workspace.background", darkBg);
                UIManager.put("Workspace.panelBackground", panelBg);
                UIManager.put("Workspace.borderColor", borderColor);
                UIManager.put("Workspace.metricCardBackground", metricBg);

                UIManager.put("Sidebar.background", panelBg);
                UIManager.put("Sidebar.treeBackground", panelBg);
                UIManager.put("Sidebar.toolbarBackground", new Color(35, 39, 47));
                UIManager.put("Sidebar.borderColor", borderColor);
                UIManager.put("Sidebar.selectionBackground", selectBg);
            } else {
                if (isModern) {
                    com.formdev.flatlaf.themes.FlatMacLightLaf.setup();
                } else {
                    FlatLightLaf.setup();
                }

                Color primaryAccent = new Color(26, 115, 232); // #1A73E8
                Color lightBg = new Color(255, 255, 255);
                Color panelBg = new Color(248, 249, 250);
                Color darkText = new Color(33, 33, 33);
                Color selectBg = new Color(236, 236, 236);
                Color borderColor = new Color(228, 228, 228);
                Color metricBg = new Color(245, 247, 250);

                UIManager.put("Button.arc", isModern ? 999 : 4);
                UIManager.put("Component.arc", isModern ? 12 : 4);
                UIManager.put("TextComponent.arc", isModern ? 12 : 4);
                UIManager.put("ProgressBar.arc", isModern ? 99 : 4);

                // Accent
                UIManager.put("AccentColor", primaryAccent);

                // Tabs
                UIManager.put("TabbedPane.showTabSeparators", true);
                UIManager.put("TabbedPane.tabType", "underlined");
                UIManager.put("TabbedPane.tabHeight", 34);
                UIManager.put("TabbedPane.selectedBackground", lightBg);
                UIManager.put("TabbedPane.background", panelBg);
                UIManager.put("TabbedPane.foreground", darkText);
                UIManager.put("TabbedPane.selectedForeground", Color.BLACK);
                UIManager.put("TabbedPane.hoverColor", selectBg);
                UIManager.put("TabbedPane.underlineColor", primaryAccent);
                UIManager.put("TabbedPane.inactiveUnderlineColor", primaryAccent);
                UIManager.put("TabbedPane.focusColor", new Color(0, 0, 0, 0));
                UIManager.put("TabbedPane.showFocusIndicator", false);

                // Focus rings and borders
                UIManager.put("Component.focusColor", primaryAccent);
                UIManager.put("Component.focusedBorderColor", primaryAccent);
                UIManager.put("Button.focusColor", primaryAccent);
                UIManager.put("Button.focusedBorderColor", primaryAccent);
                UIManager.put("Button.background", lightBg);
                UIManager.put("Button.hoverBackground", selectBg);

                // Tables
                UIManager.put("Table.selectionBackground", selectBg);
                UIManager.put("Table.selectionForeground", darkText);
                UIManager.put("TableHeader.background", panelBg);
                UIManager.put("TableHeader.bottomSeparatorColor", borderColor);
                UIManager.put("Table.gridColor", borderColor);

                // Lists/Trees Selection
                UIManager.put("Tree.selectionBackground", selectBg);
                UIManager.put("Tree.selectionForeground", darkText);
                UIManager.put("Tree.selectionBorderColor", selectBg);
                UIManager.put("List.selectionBackground", selectBg);
                UIManager.put("List.selectionForeground", darkText);

                // Scrollbars
                UIManager.put("ScrollBar.thumbArc", 999);
                UIManager.put("ScrollBar.thumb", new Color(200, 200, 200));
                UIManager.put("ScrollBar.thumbHover", new Color(150, 150, 150));

                // General window backgrounds to be white as Postman
                UIManager.put("Panel.background", lightBg);
                UIManager.put("control", lightBg);
                UIManager.put("window", lightBg);
                UIManager.put("ScrollPane.background", lightBg);
                UIManager.put("Viewport.background", lightBg);
                UIManager.put("SplitPane.background", lightBg);
                UIManager.put("MenuBar.background", panelBg);
                UIManager.put("ToolBar.background", lightBg);

                // Custom keys for light theme
                UIManager.put("Workspace.background", lightBg);
                UIManager.put("Workspace.panelBackground", panelBg);
                UIManager.put("Workspace.borderColor", borderColor);
                UIManager.put("Workspace.metricCardBackground", metricBg);

                UIManager.put("Sidebar.background", panelBg);
                UIManager.put("Sidebar.treeBackground", panelBg);
                UIManager.put("Sidebar.toolbarBackground", new Color(240, 241, 242));
                UIManager.put("Sidebar.borderColor", new Color(210, 210, 210));
                UIManager.put("Sidebar.selectionBackground", selectBg);
            }

            if (isModern) {
                // Minimize spacing to improve visibility and maximize content area
                UIManager.put("Button.margin", new Insets(2, 10, 2, 10));
                UIManager.put("TextComponent.margin", new Insets(2, 6, 2, 6));
                UIManager.put("ComboBox.padding", new Insets(0, 4, 0, 4));
                UIManager.put("TabbedPane.tabMargins", new Insets(2, 8, 2, 8));

                // Table, List, and Checkbox improvements for Modern Mode
                UIManager.put("Table.cellMargins", new Insets(2, 4, 2, 4));
                UIManager.put("List.cellMargins", new Insets(2, 4, 2, 4));
                UIManager.put("Table.showHorizontalLines", true);
                UIManager.put("Table.showVerticalLines", false);
                UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
                UIManager.put("CheckBox.arc", 999);

                // Apple-like focus rings
                UIManager.put("Component.innerFocusWidth", 0);
                UIManager.put("Component.focusWidth", 2);

                // Rounded scrollbars
                UIManager.put("ScrollBar.thumbArc", 999);
                UIManager.put("ScrollBar.width", 10);

                // Ensure macOS style window decorations are active on non-Mac platforms
                if (!isMacOs) {
                    UIManager.put("TitlePane.macStyleWindowDecorations", true);

                    // macOS window control buttons
                    UIManager.put("TitlePane.closeIcon", new MacWindowIcon(new Color(255, 95, 86), 12)); // Red, with
                                                                                                         // 12px right
                                                                                                         // padding
                    UIManager.put("TitlePane.iconifyIcon", new MacWindowIcon(new Color(255, 189, 46))); // Yellow
                    UIManager.put("TitlePane.maximizeIcon", new MacWindowIcon(new Color(39, 201, 63))); // Green
                    UIManager.put("TitlePane.restoreIcon", new MacWindowIcon(new Color(39, 201, 63))); // Green

                    // Disable the native hover/pressed square backgrounds so only the circle stands
                    // out
                    UIManager.put("TitlePane.closeHoverBackground", new Color(0, 0, 0, 0));
                    UIManager.put("TitlePane.closePressedBackground", new Color(0, 0, 0, 0));
                    UIManager.put("TitlePane.buttonHoverBackground", new Color(0, 0, 0, 0));
                    UIManager.put("TitlePane.buttonPressedBackground", new Color(0, 0, 0, 0));
                }
            } else {
                UIManager.put("Component.innerFocusWidth", 1);
                UIManager.put("Component.focusWidth", 1);
            }

            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, fontSize));
        } catch (Exception e) {
            System.err.println("Failed to initialize theme: " + e.getMessage());
        }
    }

    private static String version = null;

    /**
     * Retrieves the application version from the embedded {@code app.properties}
     * file.
     * 
     * <p>
     * The version property is populated by Maven during the build process. If the
     * properties file is missing or the placeholder is unresolved (e.g., when
     * running
     * directly from an IDE), a fallback beta version string is returned.
     * </p>
     * 
     * @return The current application version string.
     */
    public static String getVersion() {
        if (version != null) {
            return version;
        }
        try (java.io.InputStream is = App.class.getClassLoader().getResourceAsStream("app.properties")) {
            if (is != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(is);
                version = props.getProperty("app.version");
            }
        } catch (Exception e) {
            // fallback
        }
        if (version == null || version.isEmpty() || "${project.version}".equals(version)) {
            version = "1.5.0-beta"; // fallback if running outside jar or un-filtered environment
        }
        return version;
    }

    private static class MacWindowIcon implements javax.swing.Icon {
        private final Color color;
        private final int paddingRight;

        public MacWindowIcon(Color color) {
            this(color, 0);
        }

        public MacWindowIcon(Color color, int paddingRight) {
            this.color = color;
            this.paddingRight = paddingRight;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw the main circle
            g2.setColor(color);
            g2.fillOval(x + 4, y + 4, 20, 20);

            // Draw a subtle border to mimic macOS depth
            g2.setColor(color.darker().darker());
            g2.setStroke(new BasicStroke(0.5f));
            g2.drawOval(x + 4, y + 4, 20, 20);

            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 30 + paddingRight;
        }

        @Override
        public int getIconHeight() {
            return 24;
        }
    }
}
