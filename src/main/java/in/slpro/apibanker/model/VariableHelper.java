package in.slpro.apibanker.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import in.slpro.apibanker.ui.MainFrame;

/**
 * VariableHelper
 *
 * <p>
 * Core functionality and implementation logic for VariableHelper.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 2.0.0
 * @since 1.0.0
 */
public class VariableHelper {
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    public static String resolveVariablesInString(String input, RequestModel requestModel, MainFrame mainFrame) {
        if (input == null) return null;
        EnvironmentModel environment = mainFrame != null ? mainFrame.getActiveEnvironment() : null;
        return resolveVariables(input, requestModel, environment);
    }

    public static String resolveVariables(String input, RequestModel requestModel, EnvironmentModel environment) {
        CollectionModel collection = MainFrame.findParentCollection(requestModel);
        return resolveVariables(input, collection, environment, true, true, true, 0);
    }

    public static String resolveVariables(String input, CollectionModel collection, EnvironmentModel environment) {
        return resolveVariables(input, collection, environment, true, true, true, 0);
    }

    private static String resolveVariables(String input, CollectionModel collection, EnvironmentModel environment, 
            boolean allowEnv, boolean allowColl, boolean allowGlobal, int depth) {
        if (input == null || depth > 10)
            return input;
        
        Matcher matcher = VAR_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            String value = null;
            boolean fromEnv = false;
            boolean fromColl = false;
            boolean fromGlobal = false;

            if (allowEnv && environment != null && environment.getVariables() != null) {
                value = environment.getVariables().stream()
                        .filter(kv -> kv.isEnabled() && varName.equals(kv.getKey()))
                        .map(KeyValueItem::getValue)
                        .findFirst()
                        .orElse(null);
                if (value != null) fromEnv = true;
            }
            if (value == null && allowColl && collection != null && collection.getVariables() != null) {
                value = collection.getVariables().stream()
                        .filter(kv -> kv.isEnabled() && varName.equals(kv.getKey()))
                        .map(KeyValueItem::getValue)
                        .findFirst()
                        .orElse(null);
                if (value != null) fromColl = true;
            }
            if (value == null && allowGlobal) {
                java.util.List<KeyValueItem> globals = in.slpro.apibanker.storage.StorageManager.getInstance().getSettings().getGlobalVariables();
                if (globals != null) {
                    value = globals.stream()
                            .filter(kv -> kv.isEnabled() && varName.equals(kv.getKey()))
                            .map(KeyValueItem::getValue)
                            .findFirst()
                            .orElse(null);
                    if (value != null) fromGlobal = true;
                }
            }

            if (value == null) {
                value = matcher.group(0);
            } else {
                if (fromEnv) {
                    value = resolveVariables(value, collection, environment, true, false, false, depth + 1);
                } else if (fromColl) {
                    value = resolveVariables(value, collection, environment, true, false, false, depth + 1);
                } else if (fromGlobal) {
                    value = resolveVariables(value, collection, environment, true, true, true, depth + 1);
                }
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value != null ? value : ""));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}

