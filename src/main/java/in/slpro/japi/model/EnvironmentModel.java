package in.slpro.japi.model;

import java.util.ArrayList;
import java.util.List;

public class EnvironmentModel {
    private String id;
    private String name;
    private List<KeyValueItem> variables = new ArrayList<>();

    public EnvironmentModel() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<KeyValueItem> getVariables() { return variables; }
    public void setVariables(List<KeyValueItem> variables) { this.variables = variables; }
}
