package in.slpro.japi;

import in.slpro.japi.ui.MainFrame;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

public class App {
    public static void main(String[] args) {
        try {
            String theme = in.slpro.japi.storage.StorageManager.getInstance().getSettings().getTheme();
            setupTheme(theme, 16);
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
            // Center on screen
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int x = (screenSize.width - frame.getWidth()) / 2;
            int y = (screenSize.height - frame.getHeight()) / 2;
            frame.setLocation(x, y);
        });
    }

    public static void setupTheme(String theme, int fontSize) {
        try {
            if ("dark".equals(theme)) {
                FlatDarkLaf.setup();
                UIManager.put("Button.arc", 8);
                UIManager.put("Component.arc", 8);
                UIManager.put("TextComponent.arc", 8);
                UIManager.put("ProgressBar.arc", 8);
                
                UIManager.put("TabbedPane.showTabSeparators", true);
                UIManager.put("TabbedPane.tabHeight", 32);
                UIManager.put("TabbedPane.selectedBackground", new Color(45, 48, 52));
                UIManager.put("TabbedPane.hoverColor", new Color(60, 63, 67));
            } else {
                FlatLightLaf.setup();
                
                Color postmanOrange = new Color(255, 108, 55); // #FF6C37
                Color lightBg = new Color(255, 255, 255);
                Color panelBg = new Color(248, 249, 250);
                Color darkText = new Color(33, 33, 33);
                Color selectBg = new Color(236, 236, 236);
                Color borderColor = new Color(228, 228, 228);

                UIManager.put("Button.arc", 8);
                UIManager.put("Component.arc", 8);
                UIManager.put("TextComponent.arc", 8);
                UIManager.put("ProgressBar.arc", 8);

                // Accent
                UIManager.put("AccentColor", postmanOrange);

                // Tabs
                UIManager.put("TabbedPane.showTabSeparators", true);
                UIManager.put("TabbedPane.tabHeight", 34);
                UIManager.put("TabbedPane.selectedBackground", lightBg);
                UIManager.put("TabbedPane.background", panelBg);
                UIManager.put("TabbedPane.foreground", darkText);
                UIManager.put("TabbedPane.selectedForeground", Color.BLACK);
                UIManager.put("TabbedPane.hoverColor", selectBg);
                UIManager.put("TabbedPane.underlineColor", postmanOrange);
                UIManager.put("TabbedPane.inactiveUnderlineColor", lightBg);
                UIManager.put("TabbedPane.focusColor", postmanOrange);

                // Focus rings and borders
                UIManager.put("Component.focusColor", postmanOrange);
                UIManager.put("Component.focusedBorderColor", postmanOrange);
                UIManager.put("Button.focusColor", postmanOrange);
                UIManager.put("Button.focusedBorderColor", postmanOrange);
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
            }
            
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, fontSize));
        } catch (Exception e) {
            System.err.println("Failed to initialize theme: " + e.getMessage());
        }
    }
}
