package in.slpro.japi.ui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * FontScaleHelper
 *
 * <p>
 * Core functionality and implementation logic for FontScaleHelper.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.1.0-beta
 * @since 1.0.0
 */
public class FontScaleHelper {
    public static void scaleFonts(Component comp, int size) {
        if (comp == null) return;

        Font current = comp.getFont();
        if (current != null) {
            int style = current.getStyle();
            String family = current.getFamily();
            if (comp instanceof org.fife.ui.rsyntaxtextarea.RSyntaxTextArea || 
                (current.getName() != null && current.getName().toLowerCase().contains("mono"))) {
                family = "JetBrains Mono";
            }
            comp.setFont(new Font(family, style, size));
        } else {
            comp.setFont(new Font("Segoe UI", Font.PLAIN, size));
        }

        if (comp instanceof JTable table) {
            if (table.getTableHeader() != null && table.getTableHeader().getFont() != null) {
                Font thFont = table.getTableHeader().getFont();
                table.getTableHeader().setFont(new Font(thFont.getFamily(), thFont.getStyle(), size));
            }
            table.setRowHeight(size + 12);
        }

        if (comp instanceof AbstractButton btn) {
            if (!"×".equals(btn.getText())) {
                int padY = Math.max(2, size / 5);
                int padX = Math.max(6, size / 2);
                btn.setMargin(new Insets(padY, padX, padY, padX));
            }
        }

        if (comp instanceof JComponent jcomp) {
            if (jcomp.getBorder() instanceof TitledBorder titledBorder) {
                Font borderFont = titledBorder.getTitleFont();
                if (borderFont != null) {
                    titledBorder.setTitleFont(new Font(borderFont.getFamily(), borderFont.getStyle(), size));
                } else {
                    titledBorder.setTitleFont(new Font("Segoe UI", Font.BOLD, size));
                }
            }
        }

        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                scaleFonts(child, size);
            }
        }
    }
}
