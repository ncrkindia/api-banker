package in.slpro.apibanker.ui;

import javax.swing.*;

import in.slpro.apibanker.model.EnvironmentModel;
import in.slpro.apibanker.model.KeyValueItem;

import java.awt.*;
import java.util.List;

/**
 * EnvVarUIHelper
 *
 * <p>
 * Core functionality and implementation logic for EnvVarUIHelper.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.0.0-beta
 * @since 1.0.0
 */
public class EnvVarUIHelper {

    public static String resolveVariables(String input, EnvironmentModel environment) {
        if (input == null || environment == null)
            return input;
        List<KeyValueItem> vars = environment.getVariables();
        if (vars == null)
            return input;
        String result = input;
        for (KeyValueItem kv : vars) {
            if (kv.isEnabled() && kv.getKey() != null) {
                result = result.replace("{{" + kv.getKey() + "}}", kv.getValue() != null ? kv.getValue() : "");
            }
        }
        return result;
    }

    public static void highlightVariables(JTextPane textPane, String text, EnvironmentModel environment) {
        // Simple implementation - just sets the text
        textPane.setText(text);
    }

    public static void addVariableAutoComplete(JTextField field, EnvironmentModel environment) {
        // Placeholder for autocomplete support
    }

    public static JPanel createEnvVarBadge(String varName, EnvironmentModel environment) {
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        badge.setOpaque(false);
        JLabel label = new JLabel("{{" + varName + "}}");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        boolean resolved = false;
        if (environment != null && environment.getVariables() != null) {
            resolved = environment.getVariables().stream()
                    .anyMatch(kv -> kv.isEnabled() && varName.equals(kv.getKey()));
        }
        if (!resolved) {
            // Check global variables
            java.util.List<KeyValueItem> globals = in.slpro.apibanker.storage.StorageManager.getInstance().getSettings().getGlobalVariables();
            if (globals != null) {
                resolved = globals.stream()
                        .anyMatch(kv -> kv.isEnabled() && varName.equals(kv.getKey()));
            }
        }

        label.setForeground(resolved ? new Color(39, 174, 96) : new Color(192, 57, 43));
        badge.add(label);
        return badge;
    }
}

