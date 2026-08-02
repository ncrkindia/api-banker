package in.slpro.japi.model;

import java.util.ArrayList;
import java.util.List;

public class CollectionModel {
    private String id;
    private String name;
    private List<RequestModel> requests = new ArrayList<>();
    private List<CollectionModel> folders = new ArrayList<>();
    private String sslSetting = "INHERIT"; // INHERIT, VERIFY, NO_VERIFY
    private String redirectSetting = "INHERIT"; // INHERIT, YES, NO

    // Collection level configurations
    private String readme = "";
    private String authType = "none"; // none, bearer, basic, apiKey
    private String authToken = "";
    private String authUsername = "";
    private String authPassword = "";
    private String authApiKeyName = "";
    private String authApiKeyValue = "";
    private String authApiKeyIn = "header"; // header or query
    private List<KeyValueItem> variables = new ArrayList<>();
    private String preRequestScript = "";
    private String postRequestScript = "";

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

    public String getReadme() { return readme; }
    public void setReadme(String readme) { this.readme = readme; }
    public String getRedirectSetting() {
        if (redirectSetting == null) return "INHERIT";
        return redirectSetting;
    }
    public void setRedirectSetting(String redirectSetting) { this.redirectSetting = redirectSetting; }
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    public String getAuthToken() { return authToken; }
    public void setAuthToken(String authToken) { this.authToken = authToken; }
    public String getAuthUsername() { return authUsername; }
    public void setAuthUsername(String authUsername) { this.authUsername = authUsername; }
    public String getAuthPassword() { return authPassword; }
    public void setAuthPassword(String authPassword) { this.authPassword = authPassword; }
    public String getAuthApiKeyName() { return authApiKeyName; }
    public void setAuthApiKeyName(String authApiKeyName) { this.authApiKeyName = authApiKeyName; }
    public String getAuthApiKeyValue() { return authApiKeyValue; }
    public void setAuthApiKeyValue(String authApiKeyValue) { this.authApiKeyValue = authApiKeyValue; }
    public String getAuthApiKeyIn() { return authApiKeyIn; }
    public void setAuthApiKeyIn(String authApiKeyIn) { this.authApiKeyIn = authApiKeyIn; }
    public List<KeyValueItem> getVariables() { return variables; }
    public void setVariables(List<KeyValueItem> variables) { this.variables = variables; }
    public String getPreRequestScript() { return preRequestScript; }
    public void setPreRequestScript(String preRequestScript) { this.preRequestScript = preRequestScript; }
    public String getPostRequestScript() { return postRequestScript; }
    public void setPostRequestScript(String postRequestScript) { this.postRequestScript = postRequestScript; }
    public List<CollectionModel> getFolders() {
        if (folders == null) {
            folders = new ArrayList<>();
        }
        return folders;
    }
    public void setFolders(List<CollectionModel> folders) { this.folders = folders; }
    public String getSslSetting() {
        if (sslSetting == null || sslSetting.isBlank()) {
            sslSetting = "INHERIT";
        }
        return sslSetting;
    }
    public void setSslSetting(String sslSetting) { this.sslSetting = sslSetting; }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
