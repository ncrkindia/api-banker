package in.slpro.apibanker.ui;

import javax.swing.*;
import java.awt.*;

/**
 * VectorIcon
 *
 * <p>
 * Core functionality and implementation logic for VectorIcon.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 2.0.0
 * @since 1.0.0
 */
public class VectorIcon implements Icon {
    public interface Painter {
        void paint(Graphics2D g, int w, int h, Color color);
    }

    private final Painter painter;
    private final int width;
    private final int height;

    public VectorIcon(int width, int height, Painter painter) {
        this.width = width;
        this.height = height;
        this.painter = painter;
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.translate(x, y);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        Color color = (c != null) ? c.getForeground() : Color.BLACK;
        painter.paint(g2d, width, height, color);

        g2d.dispose();
    }

    // Static creator methods for standard icons
    public static Icon getReadIcon(int size) {
        return new VectorIcon(size, size, (g, w, h, color) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(1.5f));
            // Draw eye shape
            g.drawArc(1, h / 4, w - 2, h / 2, 0, 180);
            g.drawArc(1, h / 4, w - 2, h / 2, 180, 180);
            // Draw pupil
            g.fillOval(w / 2 - 2, h / 2 - 2, 4, 4);
        });
    }

    public static Icon getEditIcon(int size) {
        return new VectorIcon(size, size, (g, w, h, color) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Draw pencil line
            g.drawLine(2, h - 3, w - 3, 2);
            g.drawLine(3, h - 2, w - 2, 3);
            // Tip
            int[] xp = { 2, 5, 2 };
            int[] yp = { h - 2, h - 2, h - 5 };
            g.fillPolygon(xp, yp, 3);
        });
    }

    public static Icon getLinkIcon(int size) {
        return new VectorIcon(size, size, (g, w, h, color) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // First loop (top-right)
            g.drawRoundRect(w / 2 - 1, 2, w / 2 - 1, h / 2 - 1, 3, 3);
            // Second loop (bottom-left)
            g.drawRoundRect(1, h / 2 - 1, w / 2 - 1, h / 2 - 1, 3, 3);
            // Connect line
            g.drawLine(w / 4 + 1, 3 * h / 4 - 1, 3 * w / 4 - 1, h / 4 + 1);
        });
    }

    public static Icon getBulletIcon(int size) {
        return new VectorIcon(size, size, (g, w, h, color) -> {
            g.setColor(color);
            // Dots
            g.fillOval(1, h / 4 - 2, 3, 3);
            g.fillOval(1, h / 2 - 2, 3, 3);
            g.fillOval(1, 3 * h / 4 - 2, 3, 3);
            // Lines
            g.setStroke(new BasicStroke(1.5f));
            g.drawLine(7, h / 4 - 1, w - 1, h / 4 - 1);
            g.drawLine(7, h / 2 - 1, w - 1, h / 2 - 1);
            g.drawLine(7, 3 * h / 4 - 1, w - 1, 3 * h / 4 - 1);
        });
    }

    public static Icon getNumberIcon(int size) {
        return new VectorIcon(size, size, (g, w, h, color) -> {
            g.setColor(color);
            g.setFont(new Font("Segoe UI", Font.BOLD, 9));
            g.drawString("1", 0, h / 2 - 1);
            g.drawString("2", 0, h - 1);
            // Lines
            g.setStroke(new BasicStroke(1.5f));
            g.drawLine(7, h / 4, w - 1, h / 4);
            g.drawLine(7, 3 * h / 4, w - 1, 3 * h / 4);
        });
    }

    public static Icon getQuoteIcon(int size) {
        return new VectorIcon(size, size, (g, w, h, color) -> {
            g.setColor(color);
            g.setFont(new Font("Segoe UI", Font.BOLD, size + 2));
            g.drawString("“", 1, h - 1);
        });
    }

    public static Icon getTableIcon(int size) {
        return new VectorIcon(size, size, (g, w, h, color) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(1.2f));
            g.drawRect(1, 1, w - 3, h - 3);
            g.drawLine(w / 2 - 1, 1, w / 2 - 1, h - 2);
            g.drawLine(1, h / 2 - 1, w - 2, h / 2 - 1);
        });
    }

    public static Icon getLineIcon(int size) {
        return new VectorIcon(size, size, (g, w, h, color) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(1.5f));
            g.drawLine(1, h / 2, w - 2, h / 2);
        });
    }
}

