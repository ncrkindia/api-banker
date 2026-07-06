package in.slpro.japi.model;

public class AppSettings {
    private String dataDirectory;
    private String theme = "light";
    private String activeEnvironmentId;
    private boolean enableLogging = true;
    private int fontSize = 16;
    private int windowWidth = 1300;
    private int windowHeight = 800;
    private boolean windowMaximized = true;

    private java.util.List<OpenTabState> openTabs = new java.util.ArrayList<>();

    private int selectedTabIndex = -1;

    private int consoleWidth = 950;
    private int consoleHeight = 600;
    private int consoleX = -1;
    private int consoleY = -1;

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
    public int getFontSize() { return fontSize; }
    public void setFontSize(int fontSize) { this.fontSize = fontSize; }
    public int getWindowWidth() { return windowWidth; }
    public void setWindowWidth(int windowWidth) { this.windowWidth = windowWidth; }
    public int getWindowHeight() { return windowHeight; }
    public void setWindowHeight(int windowHeight) { this.windowHeight = windowHeight; }
    public boolean isWindowMaximized() { return windowMaximized; }
    public void setWindowMaximized(boolean windowMaximized) { this.windowMaximized = windowMaximized; }
    public int getSelectedTabIndex() { return selectedTabIndex; }
    public void setSelectedTabIndex(int selectedTabIndex) { this.selectedTabIndex = selectedTabIndex; }
    public java.util.List<OpenTabState> getOpenTabs() { return openTabs; }
    public void setOpenTabs(java.util.List<OpenTabState> openTabs) { this.openTabs = openTabs; }

    public int getConsoleWidth() { return consoleWidth; }
    public void setConsoleWidth(int consoleWidth) { this.consoleWidth = consoleWidth; }
    public int getConsoleHeight() { return consoleHeight; }
    public void setConsoleHeight(int consoleHeight) { this.consoleHeight = consoleHeight; }
    public int getConsoleX() { return consoleX; }
    public void setConsoleX(int consoleX) { this.consoleX = consoleX; }
    public int getConsoleY() { return consoleY; }
    public void setConsoleY(int consoleY) { this.consoleY = consoleY; }

    public static class OpenTabState {
        private String type;
        private String requestModelId;

        public OpenTabState() {}
        public OpenTabState(String type, String requestModelId) {
            this.type = type;
            this.requestModelId = requestModelId;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getRequestModelId() { return requestModelId; }
        public void setRequestModelId(String requestModelId) { this.requestModelId = requestModelId; }
    }
}
