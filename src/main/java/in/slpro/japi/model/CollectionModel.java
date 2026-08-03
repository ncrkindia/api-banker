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
    
    // OAuth 2.0 fields
    private String oauth2GrantType = "client_credentials"; // authorization_code, implicit, password, client_credentials
    private String oauth2CallbackUrl = "";
    private String oauth2AuthUrl = "";
    private String oauth2AccessTokenUrl = "";
    private String oauth2ClientId = "";
    private String oauth2ClientSecret = "";
    private String oauth2Scope = "";
    private String oauth2State = "";
    private String oauth2Username = "";
    private String oauth2Password = "";
    private String oauth2ClientAuth = "header"; // header, body
    private String oauth2AccessToken = "";
    
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
    
    // OAuth 2.0 Getters & Setters
    public String getOauth2GrantType() { return oauth2GrantType; }
    public void setOauth2GrantType(String oauth2GrantType) { this.oauth2GrantType = oauth2GrantType; }
    public String getOauth2CallbackUrl() { return oauth2CallbackUrl; }
    public void setOauth2CallbackUrl(String oauth2CallbackUrl) { this.oauth2CallbackUrl = oauth2CallbackUrl; }
    public String getOauth2AuthUrl() { return oauth2AuthUrl; }
    public void setOauth2AuthUrl(String oauth2AuthUrl) { this.oauth2AuthUrl = oauth2AuthUrl; }
    public String getOauth2AccessTokenUrl() { return oauth2AccessTokenUrl; }
    public void setOauth2AccessTokenUrl(String oauth2AccessTokenUrl) { this.oauth2AccessTokenUrl = oauth2AccessTokenUrl; }
    public String getOauth2ClientId() { return oauth2ClientId; }
    public void setOauth2ClientId(String oauth2ClientId) { this.oauth2ClientId = oauth2ClientId; }
    public String getOauth2ClientSecret() { return oauth2ClientSecret; }
    public void setOauth2ClientSecret(String oauth2ClientSecret) { this.oauth2ClientSecret = oauth2ClientSecret; }
    public String getOauth2Scope() { return oauth2Scope; }
    public void setOauth2Scope(String oauth2Scope) { this.oauth2Scope = oauth2Scope; }
    public String getOauth2State() { return oauth2State; }
    public void setOauth2State(String oauth2State) { this.oauth2State = oauth2State; }
    public String getOauth2Username() { return oauth2Username; }
    public void setOauth2Username(String oauth2Username) { this.oauth2Username = oauth2Username; }
    public String getOauth2Password() { return oauth2Password; }
    public void setOauth2Password(String oauth2Password) { this.oauth2Password = oauth2Password; }
    public String getOauth2ClientAuth() { return oauth2ClientAuth; }
    public void setOauth2ClientAuth(String oauth2ClientAuth) { this.oauth2ClientAuth = oauth2ClientAuth; }
    public String getOauth2AccessToken() { return oauth2AccessToken; }
    public void setOauth2AccessToken(String oauth2AccessToken) { this.oauth2AccessToken = oauth2AccessToken; }
    
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
