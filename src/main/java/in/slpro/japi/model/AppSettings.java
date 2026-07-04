package in.slpro.japi.model;

public class AppSettings {
    private String dataDirectory;
    private String theme = "light";
    private String activeEnvironmentId;
    private boolean enableLogging = true;

    public AppSettings() {
        String userHome = System.getProperty("user.home");
        this.dataDirectory = userHome + "/.japi/data";
    }

    public String getDataDirectory() { return dataDirectory; }
    public void setDataDirectory(String dataDirectory) { this.dataDirectory = dataDirectory; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public String getActiveEnvironmentId() { return activeEnvironmentId; }
    public void setActiveEnvironmentId(String id) { this.activeEnvironmentId = id; }
    public boolean isEnableLogging() { return enableLogging; }
    public void setEnableLogging(boolean enableLogging) { this.enableLogging = enableLogging; }
}
