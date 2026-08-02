package in.slpro.japi.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SyntaxHighlighter
 *
 * <p>
 * Core functionality and implementation logic for SyntaxHighlighter.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.1.0-beta
 * @since 1.0.0
 */
public class SyntaxHighlighter {
    public static String highlightJson(String json) {
        if (json == null) return "";
        // Basic escaping
        String html = json.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        
        StringBuffer sb = new StringBuffer("<html><body style='font-family:\"JetBrains Mono\",\"Courier New\",monospace; font-size:11px; margin:5px;'>");
        
        Pattern pattern = Pattern.compile(
            "(\"(\\\\u[a-zA-Z0-9]{4}|\\\\[^u]|[^\\\\\"])*\")(\\s*:)?|([-+]?\\b\\d+(?:\\.\\d*)?(?:[eE][-+]?\\d+)?\\b|true\\b|false\\b|null\\b)|([\\{\\}\\[\\]\\,])"
        );
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            String strLiteral = matcher.group(1);
            String colon = matcher.group(3);
            String primitive = matcher.group(4);
            String structural = matcher.group(5);
            
            if (strLiteral != null) {
                if (colon != null) {
                    matcher.appendReplacement(sb, "<span style='color:#2980b9; font-weight:bold;'>" + strLiteral + "</span>" + colon);
                } else {
                    matcher.appendReplacement(sb, "<span style='color:#27ae60;'>" + strLiteral + "</span>");
                }
            } else if (primitive != null) {
                String color = "null".equals(primitive) ? "#7f8c8d" : (primitive.equals("true") || primitive.equals("false") ? "#8e44ad" : "#d35400");
                matcher.appendReplacement(sb, "<span style='color:" + color + "; font-weight:bold;'>" + primitive + "</span>");
            } else if (structural != null) {
                matcher.appendReplacement(sb, "<span style='color:#2c3e50; font-weight:bold;'>" + structural + "</span>");
            }
        }
        matcher.appendTail(sb);
        sb.append("</body></html>");
        
        return sb.toString().replace("\n", "<br/>").replace(" ", "&nbsp;");
    }

    public static String highlightXml(String xml) {
        if (xml == null) return "";
        String html = xml.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        
        StringBuffer sb = new StringBuffer("<html><body style='font-family:\"JetBrains Mono\",\"Courier New\",monospace; font-size:11px; margin:5px;'>");
        
        Pattern pattern = Pattern.compile(
            "(&lt;\\/?[a-zA-Z0-9:-]+)|(\\/?&gt;)|(\\s+[a-zA-Z0-9:-]+)\\s*=|(\"[^\"]*\")"
        );
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            String startTag = matcher.group(1);
            String endTag = matcher.group(2);
            String attrName = matcher.group(3);
            String attrVal = matcher.group(4);
            
            if (startTag != null) {
                matcher.appendReplacement(sb, "<span style='color:#2980b9; font-weight:bold;'>" + startTag + "</span>");
            } else if (endTag != null) {
                matcher.appendReplacement(sb, "<span style='color:#2980b9; font-weight:bold;'>" + endTag + "</span>");
            } else if (attrName != null) {
                matcher.appendReplacement(sb, "<span style='color:#d35400;'>" + attrName + "</span>=");
            } else if (attrVal != null) {
                matcher.appendReplacement(sb, "<span style='color:#27ae60;'>" + attrVal + "</span>");
            }
        }
        matcher.appendTail(sb);
        sb.append("</body></html>");
        
        return sb.toString().replace("\n", "<br/>").replace(" ", "&nbsp;");
    }
}
