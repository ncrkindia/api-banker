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
        ConsoleLogger.getInstance().setLogsDirectory(new File(settings.getLogsDirectory()));
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
    }

    public void updateLogsDirectory(String newPath) {
        settings.setLogsDirectory(newPath);
        saveSettings();
        File logsDir = new File(newPath);
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        ConsoleLogger.getInstance().setLogsDirectory(logsDir);
    }

    private void ensureDataDirExists() {
        File dataDir = new File(settings.getDataDirectory());
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank())
            return "unnamed";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }

    // --- Collections ---
    public List<CollectionModel> loadCollections() {
        File folder = new File(settings.getDataDirectory(), "collections");
        if (!folder.exists()) {
            folder.mkdirs();
            // Backward compatibility
            File oldFile = new File(settings.getDataDirectory(), "collections.json");
            if (oldFile.exists()) {
                try (Reader reader = new FileReader(oldFile, StandardCharsets.UTF_8)) {
                    Type listType = new TypeToken<ArrayList<CollectionModel>>() {}.getType();
                    List<CollectionModel> collections = gson.fromJson(reader, listType);
                    if (collections != null) {
                        saveCollections(collections);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                oldFile.delete();
            }
        }

        List<CollectionModel> collections = new ArrayList<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                    CollectionModel col = gson.fromJson(reader, CollectionModel.class);
                    if (col != null) {
                        collections.add(col);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return collections;
    }

    public void saveCollections(List<CollectionModel> collections) {
        File folder = new File(settings.getDataDirectory(), "collections");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        List<String> activeFilenames = new ArrayList<>();
        for (CollectionModel col : collections) {
            if (col.getId() == null || col.getId().isBlank()) {
                col.setId(java.util.UUID.randomUUID().toString());
            }
            String baseName = sanitizeFilename(col.getName());
            String filename = baseName + ".json";
            int count = 1;
            while (activeFilenames.contains(filename)) {
                filename = baseName + "_" + count + ".json";
                count++;
            }
            activeFilenames.add(filename);

            File file = new File(folder, filename);
            try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                gson.toJson(col, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                if (!activeFilenames.contains(file.getName().toLowerCase())) {
                    file.delete();
                }
            }
        }
    }

    // --- Environments ---
    public List<EnvironmentModel> loadEnvironments() {
        File folder = new File(settings.getDataDirectory(), "environments");
        if (!folder.exists()) {
            folder.mkdirs();
            // Backward compatibility
            File oldFile = new File(settings.getDataDirectory(), "environments.json");
            if (oldFile.exists()) {
                try (Reader reader = new FileReader(oldFile, StandardCharsets.UTF_8)) {
                    Type listType = new TypeToken<ArrayList<EnvironmentModel>>() {}.getType();
                    List<EnvironmentModel> envs = gson.fromJson(reader, listType);
                    if (envs != null) {
                        saveEnvironments(envs);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                oldFile.delete();
            }
        }

        List<EnvironmentModel> envs = new ArrayList<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                    EnvironmentModel env = gson.fromJson(reader, EnvironmentModel.class);
                    if (env != null) {
                        envs.add(env);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return envs;
    }

    public void saveEnvironments(List<EnvironmentModel> environments) {
        File folder = new File(settings.getDataDirectory(), "environments");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        List<String> activeFilenames = new ArrayList<>();
        for (EnvironmentModel env : environments) {
            if (env.getId() == null || env.getId().isBlank()) {
                env.setId(java.util.UUID.randomUUID().toString());
            }
            String baseName = sanitizeFilename(env.getName());
            String filename = baseName + ".json";
            int count = 1;
            while (activeFilenames.contains(filename)) {
                filename = baseName + "_" + count + ".json";
                count++;
            }
            activeFilenames.add(filename);

            File file = new File(folder, filename);
            try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                gson.toJson(env, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                if (!activeFilenames.contains(file.getName().toLowerCase())) {
                    file.delete();
                }
            }
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
