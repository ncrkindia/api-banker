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
 * @version 1.0.0-beta
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
                textColor = res.isEnv ? VariableHelper.getEnvColor() : VariableHelper.getCollectionColor();
            }

            try {
                Rectangle r0, r1;
                try {
                    r0 = modelToView2D(start).getBounds();
                    r1 = modelToView2D(end).getBounds();
                } catch (NoSuchMethodError e) {
                    r0 = modelToView(start);
                    r1 = modelToView(end);
                }

                int x = r0.x;
                int y = r0.y;
                int w = r1.x - r0.x;
                int h = r0.height;

                if (w > 0) {
                    // Erase the original text painted by the base component UI
                    Color bg = getBackground();
                    if (selStart != selEnd && start >= selStart && end <= selEnd) {
                        bg = getSelectionColor();
                    }
                    g2.setColor(bg);
                    g2.fillRect(x, y, w, h);

                    // Draw the colored bold variable text
                    g2.setColor(textColor);
                    g2.drawString(text.substring(start, end), x, y + fm.getAscent());
                }
            } catch (BadLocationException ignored) {
            }
        }
        g2.dispose();
    }
}

