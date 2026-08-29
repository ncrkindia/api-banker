package in.slpro.apibanker.ui;

import javax.swing.*;
import javax.swing.text.BadLocationException;

import in.slpro.apibanker.model.CollectionModel;

import java.awt.*;
import java.util.regex.Matcher;

/**
 * HighlightTextField
 *
 * <p>
 * Core functionality and implementation logic for HighlightTextField.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 2.0.1
 * @since 1.0.0
 */
public class HighlightTextField extends JTextField {
    private CollectionModel collectionModel;
    private MainFrame mainFrame;

    public HighlightTextField() {
        super();
    }

    public void setCollectionContext(CollectionModel collectionModel, MainFrame mainFrame) {
        this.collectionModel = collectionModel;
        this.mainFrame = mainFrame;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        String text = getText();
        if (text == null || text.isEmpty())
            return;

        Matcher matcher = VariableHelper.VAR_PATTERN.matcher(text);
        Font font = getFont();
        FontMetrics fm = getFontMetrics(font);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(font);

        int selStart = getSelectionStart();
        int selEnd = getSelectionEnd();

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String varName = matcher.group(1).trim();
            VariableHelper.VariableResolution res = VariableHelper.resolveVariable(varName, collectionModel, mainFrame);

            Color textColor = VariableHelper.getUnresolvedColor();
            if (res.resolved) {
                textColor = res.getColor();
            }

            try {
                int selS = Math.max(start, selStart);
                int selE = Math.min(end, selEnd);

                if (selS < selE) { // there is an overlap
                    drawSegment(g2, text, start, selS, getBackground(), textColor, fm);
                    drawSegment(g2, text, selS, selE, getSelectionColor(), getSelectedTextColor(), fm);
                    drawSegment(g2, text, selE, end, getBackground(), textColor, fm);
                } else {
                    drawSegment(g2, text, start, end, getBackground(), textColor, fm);
                }
            } catch (Exception ignored) {
            }
        }
        g2.dispose();
    }

    private void drawSegment(Graphics2D g2, String text, int s, int e, Color bg, Color fg, FontMetrics fm) {
        if (s >= e)
            return;
        try {
            Rectangle r0, r1;
            try {
                r0 = modelToView2D(s).getBounds();
                r1 = modelToView2D(e).getBounds();
            } catch (NoSuchMethodError ex) {
                r0 = modelToView(s);
                r1 = modelToView(e);
            }
            int x = r0.x;
            int y = r0.y;
            int w = r1.x - r0.x;
            int h = r0.height;

            if (w > 0) {
                g2.setColor(bg);
                g2.fillRect(x, y, w, h);
                g2.setColor(fg);
                g2.drawString(text.substring(s, e), x, y + fm.getAscent());
            }
        } catch (BadLocationException ignored) {
        }
    }
}

