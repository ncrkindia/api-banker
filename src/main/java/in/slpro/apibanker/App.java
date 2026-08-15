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
 * @version 1.5.0-beta
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
            if ("dark".equals(theme)) {
                FlatDarkLaf.setup();

                Color primaryAccentDark = new Color(138, 180, 248); // #8AB4F8

                UIManager.put("AccentColor", primaryAccentDark);

                UIManager.put("Button.arc", 4);
                UIManager.put("Component.arc", 4);
                UIManager.put("TextComponent.arc", 4);
                UIManager.put("ProgressBar.arc", 4);

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
                FlatLightLaf.setup();

                Color primaryAccent = new Color(26, 115, 232); // #1A73E8
                Color lightBg = new Color(255, 255, 255);
                Color panelBg = new Color(248, 249, 250);
                Color darkText = new Color(33, 33, 33);
                Color selectBg = new Color(236, 236, 236);
                Color borderColor = new Color(228, 228, 228);
                Color metricBg = new Color(245, 247, 250);

                UIManager.put("Button.arc", 4);
                UIManager.put("Component.arc", 4);
                UIManager.put("TextComponent.arc", 4);
                UIManager.put("ProgressBar.arc", 4);

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
}
