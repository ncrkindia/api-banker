package in.slpro.japi.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import in.slpro.japi.model.AppSettings;
import in.slpro.japi.model.CollectionModel;
import in.slpro.japi.model.EnvironmentModel;
import in.slpro.japi.model.RequestModel;

import in.slpro.japi.logger.ConsoleLogger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class StorageManager {
    private static StorageManager instance;
    private final Gson gson;
    private final File bootstrapFile;
    private AppSettings settings;

    private StorageManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        String userHome = System.getProperty("user.home");
        File bootstrapDir = new File(userHome, ".japi");
        if (!bootstrapDir.exists()) {
            bootstrapDir.mkdirs();
        }
        this.bootstrapFile = new File(bootstrapDir, "config.json");
        loadOrCreateSettings();
        ConsoleLogger.getInstance().setLogsDirectory(new File(settings.getDataDirectory(), "logs"));
    }

    public static synchronized StorageManager getInstance() {
        if (instance == null) {
            instance = new StorageManager();
        }
        return instance;
    }

    private void loadOrCreateSettings() {
        if (bootstrapFile.exists()) {
            try (Reader reader = new FileReader(bootstrapFile, StandardCharsets.UTF_8)) {
                this.settings = gson.fromJson(reader, AppSettings.class);
                if (this.settings == null) {
                    this.settings = new AppSettings();
                }
            } catch (Exception e) {
                e.printStackTrace();
                this.settings = new AppSettings();
            }
        } else {
            this.settings = new AppSettings();
            saveSettings();
        }
        ensureDataDirExists();
        ConsoleLogger.getInstance().setEnableLogging(settings.isEnableLogging());
    }

    public void saveSettings() {
        try (Writer writer = new FileWriter(bootstrapFile, StandardCharsets.UTF_8)) {
            gson.toJson(settings, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AppSettings getSettings() {
        return settings;
    }

    public void updateDataDirectory(String newPath) {
        settings.setDataDirectory(newPath);
        saveSettings();
        ensureDataDirExists();
        ConsoleLogger.getInstance().setLogsDirectory(new File(newPath, "logs"));
    }

    private void ensureDataDirExists() {
        File dataDir = new File(settings.getDataDirectory());
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    // --- Collections ---
    public List<CollectionModel> loadCollections() {
        File file = new File(settings.getDataDirectory(), "collections.json");
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<ArrayList<CollectionModel>>() {}.getType();
            List<CollectionModel> collections = gson.fromJson(reader, listType);
            return collections != null ? collections : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void saveCollections(List<CollectionModel> collections) {
        File file = new File(settings.getDataDirectory(), "collections.json");
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(collections, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Environments ---
    public List<EnvironmentModel> loadEnvironments() {
        File file = new File(settings.getDataDirectory(), "environments.json");
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<ArrayList<EnvironmentModel>>() {}.getType();
            List<EnvironmentModel> envs = gson.fromJson(reader, listType);
            return envs != null ? envs : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void saveEnvironments(List<EnvironmentModel> environments) {
        File file = new File(settings.getDataDirectory(), "environments.json");
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(environments, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- History ---
    public List<RequestModel> loadHistory() {
        File file = new File(settings.getDataDirectory(), "history.json");
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<ArrayList<RequestModel>>() {}.getType();
            List<RequestModel> history = gson.fromJson(reader, listType);
            return history != null ? history : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void saveHistory(List<RequestModel> history) {
        File file = new File(settings.getDataDirectory(), "history.json");
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(history, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
