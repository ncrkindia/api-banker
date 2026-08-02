package in.slpro.japi.model;

import in.slpro.japi.ui.MainFrame;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * VariableHelper
 *
 * <p>
 * Core functionality and implementation logic for VariableHelper.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.1.0-beta
 * @since 1.0.0
 */
public class VariableHelper {
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    public static String resolveVariablesInString(String input, RequestModel requestModel, MainFrame mainFrame) {
        if (input == null) return null;
        CollectionModel collection = MainFrame.findParentCollection(requestModel);
        EnvironmentModel environment = mainFrame != null ? mainFrame.getActiveEnvironment() : null;
        Matcher matcher = VAR_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            String value = null;
            if (environment != null && environment.getVariables() != null) {
                value = environment.getVariables().stream()
                        .filter(kv -> kv.isEnabled() && varName.equals(kv.getKey()))
                        .map(KeyValueItem::getValue)
                        .findFirst()
                        .orElse(null);
            }
            if (value == null && collection != null && collection.getVariables() != null) {
                value = collection.getVariables().stream()
                        .filter(kv -> kv.isEnabled() && varName.equals(kv.getKey()))
                        .map(KeyValueItem::getValue)
                        .findFirst()
                        .orElse(null);
            }
            if (value == null) {
                value = matcher.group(0);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
