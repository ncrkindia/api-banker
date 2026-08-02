package in.slpro.japi.model;

import java.util.ArrayList;
import java.util.List;

/**
 * CollectionModel
 *
 * <p>
 * Represents the primary hierarchical entity in the JAPI workspace.
 * A Collection acts as a folder/container that groups multiple {@link RequestModel}s,
 * sub-folders (other CollectionModels), and scoped variables. It also defines 
 * inheritable configurations like SSL verification rules, HTTP redirect settings, 
 * authentication protocols, and pre/post-request scripts.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.1.0-beta
 * @since 1.0.0
 */
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

    /**
     * Default constructor required for JSON serialization/deserialization by Gson.
     */
    public CollectionModel() {}

    /**
     * Constructs a new Collection with a unique ID and a display name.
     * 
     * @param id A universally unique identifier (e.g. UUID).
     * @param name The human-readable name of the collection.
     */
    public CollectionModel(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /** @return The unique identifier of this collection. */
    public String getId() { return id; }
    
    /** @param id The unique identifier to set. */
    public void setId(String id) { this.id = id; }
    
    /** @return The display name of the collection shown in the UI sidebar. */
    public String getName() { return name; }
    
    /** @param name The display name to set. */
    public void setName(String name) { this.name = name; }
    
    /** @return The list of direct child {@link RequestModel}s belonging to this collection/folder. */
    public List<RequestModel> getRequests() { return requests; }
    
    /** @param requests The list of requests to bind to this collection. */
    public void setRequests(List<RequestModel> requests) { this.requests = requests; }

    /** @return The raw markdown content of the collection's README documentation. */
    public String getReadme() { return readme; }
    public void setReadme(String readme) { this.readme = readme; }
    
    /** @return The HTTP Redirect strategy (INHERIT, YES, NO). Defaults to INHERIT. */
    public String getRedirectSetting() {
        if (redirectSetting == null) return "INHERIT";
        return redirectSetting;
    }
    public void setRedirectSetting(String redirectSetting) { this.redirectSetting = redirectSetting; }
    
    /** @return The Authorization strategy (none, bearer, basic, apiKey) applied to child requests. */
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
    
    /** @return The list of collection-scoped variables accessible via {{varName}}. */
    public List<KeyValueItem> getVariables() { return variables; }
    public void setVariables(List<KeyValueItem> variables) { this.variables = variables; }
    
    /** @return The raw JavaScript block to execute before any child request is fired. */
    public String getPreRequestScript() { return preRequestScript; }
    public void setPreRequestScript(String preRequestScript) { this.preRequestScript = preRequestScript; }
    
    /** @return The raw JavaScript block to execute after any child request completes. */
    public String getPostRequestScript() { return postRequestScript; }
    public void setPostRequestScript(String postRequestScript) { this.postRequestScript = postRequestScript; }
    
    /** @return The list of sub-folders (which are also CollectionModels). */
    public List<CollectionModel> getFolders() {
        if (folders == null) {
            folders = new ArrayList<>();
        }
        return folders;
    }
    public void setFolders(List<CollectionModel> folders) { this.folders = folders; }
    
    /** @return The SSL Verification strategy (INHERIT, VERIFY, NO_VERIFY). Defaults to INHERIT. */
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
