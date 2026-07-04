package in.slpro.japi.ui;

import in.slpro.japi.model.RequestModel;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class ComparatorPanel extends JPanel {
    private final RequestModel requestModel;
    private final JTextArea leftArea;
    private final JTextArea rightArea;
    private final JTextPane diffPane;
    private int currentFontSize = 12;

    public ComparatorPanel(MainFrame mainFrame, RequestModel requestModel) {
        this.requestModel = requestModel;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(Color.WHITE);

        leftArea = new JTextArea();
        leftArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        leftArea.setLineWrap(true);

        rightArea = new JTextArea();
        rightArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        rightArea.setLineWrap(true);

        diffPane = new JTextPane();
        diffPane.setContentType("text/html");
        diffPane.setEditable(false);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topBar.setBackground(Color.WHITE);
        JButton compareBtn = new JButton("Compare");
        compareBtn.setBackground(new Color(52, 152, 219));
        compareBtn.setForeground(Color.WHITE);
        compareBtn.addActionListener(e -> compare());
        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> {
            leftArea.setText("");
            rightArea.setText("");
            diffPane.setText("");
        });
        topBar.add(compareBtn);
        topBar.add(clearBtn);
        add(topBar, BorderLayout.NORTH);

        JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        topSplit.setResizeWeight(0.5);

        JScrollPane leftScroll = new JScrollPane(leftArea);
        leftScroll.setBorder(BorderFactory.createTitledBorder("Text A (Original)"));

        JScrollPane rightScroll = new JScrollPane(rightArea);
        rightScroll.setBorder(BorderFactory.createTitledBorder("Text B (Revised)"));

        topSplit.setLeftComponent(leftScroll);
        topSplit.setRightComponent(rightScroll);

        JScrollPane diffScroll = new JScrollPane(diffPane);
        diffScroll.setBorder(BorderFactory.createTitledBorder("Diff Result"));

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplit.setResizeWeight(0.4);
        mainSplit.setTopComponent(topSplit);
        mainSplit.setBottomComponent(diffScroll);

        add(mainSplit, BorderLayout.CENTER);
    }

    private void compare() {
        List<String> leftLines = Arrays.asList(leftArea.getText().split("\n", -1));
        List<String> rightLines = Arrays.asList(rightArea.getText().split("\n", -1));
        Patch<String> patch = DiffUtils.diff(leftLines, rightLines);

        StringBuilder html = new StringBuilder("<html><body style='font-family:JetBrains Mono,monospace;font-size:" + currentFontSize + "px;'>");
        int leftIdx = 0;
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            while (leftIdx < delta.getSource().getPosition()) {
                html.append("<div style='background:#fff;padding:1px 4px;'>").append(escapeHtml(leftLines.get(leftIdx))).append("</div>");
                leftIdx++;
            }
            for (String line : delta.getSource().getLines()) {
                html.append("<div style='background:#ffeef0;padding:1px 4px;color:#c0392b;'>- ").append(escapeHtml(line)).append("</div>");
                leftIdx++;
            }
            for (String line : delta.getTarget().getLines()) {
                html.append("<div style='background:#e6ffed;padding:1px 4px;color:#27ae60;'>+ ").append(escapeHtml(line)).append("</div>");
            }
        }
        while (leftIdx < leftLines.size()) {
            html.append("<div style='background:#fff;padding:1px 4px;'>").append(escapeHtml(leftLines.get(leftIdx))).append("</div>");
            leftIdx++;
        }
        html.append("</body></html>");
        diffPane.setText(html.toString());
        diffPane.setCaretPosition(0);
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace(" ", "&nbsp;");
    }

    public void updateFontSize(int size) {
        this.currentFontSize = size;
        FontScaleHelper.scaleFonts(this, size);
        if (diffPane.getText() != null && !diffPane.getText().isEmpty()) {
            compare();
        }
    }

    public RequestModel getRequestModel() {
        return requestModel;
    }
}
