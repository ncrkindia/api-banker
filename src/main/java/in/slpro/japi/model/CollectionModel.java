package in.slpro.japi.model;

import java.util.ArrayList;
import java.util.List;

public class CollectionModel {
    private String id;
    private String name;
    private List<RequestModel> requests = new ArrayList<>();

    public CollectionModel() {}

    public CollectionModel(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<RequestModel> getRequests() { return requests; }
    public void setRequests(List<RequestModel> requests) { this.requests = requests; }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
