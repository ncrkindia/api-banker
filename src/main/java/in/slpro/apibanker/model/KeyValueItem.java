package in.slpro.apibanker.model;

/**
 * KeyValueItem
 *
 * <p>
 * Core functionality and implementation logic for KeyValueItem.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.0.0-beta
 * @since 1.0.0
 */
public class KeyValueItem {
    private String key;
    private String value;
    private boolean enabled;
    private String description;

    private String type = "text"; // "text" or "file"

    public KeyValueItem() {
        this.enabled = true;
    }

    public KeyValueItem(String key, String value, boolean enabled) {
        this.key = key;
        this.value = value;
        this.enabled = enabled;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

