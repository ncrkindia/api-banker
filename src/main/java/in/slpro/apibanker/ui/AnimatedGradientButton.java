package in.slpro.apibanker.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class AnimatedGradientButton extends JButton {
    private float angle = 0;
    private Timer animTimer;
    private boolean isHovered = false;
    
    public AnimatedGradientButton(String text) {
        super(text);
        
        animTimer = new Timer(30, e -> {
            if (isHovered && isGradientTheme()) {
                angle += 0.05f;
                repaint();
            }
        });
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                if (isGradientTheme()) {
                    setCursor(new Cursor(Cursor.HAND_CURSOR));
                    animTimer.start();
                    repaint();
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                if (isGradientTheme()) {
                    setCursor(Cursor.getDefaultCursor());
                    animTimer.stop();
                    repaint();
                }
            }
        });
    }
    
    private boolean isGradientTheme() {
        try {
            return "gradient".equals(in.slpro.apibanker.storage.StorageManager.getInstance().getSettings().getTheme());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!isGradientTheme() || !isEnabled()) {
            super.paintComponent(g);
            return;
        }
        
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int w = getWidth();
        int h = getHeight();
        int arc = 16; 
        
        g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));
        
        if (isHovered) {
            AffineTransform oldT = g2.getTransform();
            g2.translate(w / 2.0, h / 2.0);
            g2.rotate(angle);
            
            float[] fractions = {0.0f, 0.33f, 0.66f, 1.0f};
            Color[] colors = {new Color(99, 102, 241), new Color(236, 72, 153), new Color(139, 92, 246), new Color(99, 102, 241)};
            LinearGradientPaint lgp = new LinearGradientPaint(-w, -h, w, h, fractions, colors);
            g2.setPaint(lgp);
            g2.fillRect(-w * 2, -h * 2, w * 4, h * 4);
            
            g2.setTransform(oldT);
        } else {
            // static gradient
            float[] fractions = {0.0f, 1.0f};
            Color[] colors = {new Color(99, 102, 241), new Color(139, 92, 246)};
            LinearGradientPaint lgp = new LinearGradientPaint(0, 0, w, h, fractions, colors);
            g2.setPaint(lgp);
            g2.fillRoundRect(0, 0, w, h, arc, arc);
        }
        
        g2.setClip(null);
        
        // draw text
        FontMetrics fm = g2.getFontMetrics(getFont());
        int x = (w - fm.stringWidth(getText())) / 2;
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.setColor(Color.WHITE);
        g2.drawString(getText(), x, y);
        
        g2.dispose();
    }
    
    @Override
    protected void paintBorder(Graphics g) {
        if (!isGradientTheme() || !isEnabled()) {
            super.paintBorder(g);
        }
    }
}
