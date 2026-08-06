package in.slpro.apibanker.model;

import java.util.ArrayList;
import java.util.List;

/**
 * EnvironmentModel
 *
 * <p>
 * Represents an isolated environment context (e.g. 'Development', 'Production')
 * containing a scoped collection of key-value variables. These variables are
 * dynamically injected into URLs, headers, and bodies at runtime using the
 * `{{variableName}}` syntax.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.0.0-beta
 * @since 1.0.0
 */
public class EnvironmentModel {
    private String id;
    private String name;
    private List<KeyValueItem> variables = new ArrayList<>();

    /**
     * Default constructor required for JSON serialization.
     */
    public EnvironmentModel() {
    }

    /** @return The unique identifier of this environment (typically a UUID). */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /** @return The human-readable name of the environment (e.g., 'Production'). */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** @return The list of key-value variables scoped to this environment. */
    public List<KeyValueItem> getVariables() {
        return variables;
    }

    public void setVariables(List<KeyValueItem> variables) {
        this.variables = variables;
    }
}

