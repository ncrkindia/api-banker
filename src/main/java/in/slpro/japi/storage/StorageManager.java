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

/**
 * StorageManager
 *
 * <p>
 * Core functionality and implementation logic for StorageManager.
 * This singleton class handles the persistence of application data, including
 * {@link AppSettings}, {@link CollectionModel}s, and {@link EnvironmentModel}s.
 * It manages the underlying file system structures, backward compatibility 
 * migrations (e.g. migrating single JSON stores to directory-based stores), 
 * and handles the serialization/deserialization logic using Gson.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.1.0-beta
 * @since 1.0.0
 */
public class StorageManager {
    private static StorageManager instance;
    private final Gson gson;
    private final File bootstrapFile;
    private AppSettings settings;

    /**
     * Private constructor to enforce Singleton pattern.
     * Initializes the Gson serializer, resolves the default user home directory,
     * and guarantees that the primary bootstrap '.japi' configuration folder exists.
     * Finally, it loads or provisions the default AppSettings and updates the logger.
     */
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

    /**
     * Retrieves the synchronized singleton instance of the StorageManager.
     * 
     * @return The active StorageManager instance.
     */
    public static synchronized StorageManager getInstance() {
        if (instance == null) {
            instance = new StorageManager();
        }
        return instance;
    }

    /**
     * Loads settings from the bootstrap config.json file. If the file is missing or 
     * corrupted, provisions a default {@link AppSettings} model and persists it to disk.
     */
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

    /**
     * Persists the current {@link AppSettings} state to the bootstrap config.json file.
     */
    public void saveSettings() {
        try (Writer writer = new FileWriter(bootstrapFile, StandardCharsets.UTF_8)) {
            gson.toJson(settings, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the active application settings.
     * 
     * @return The active {@link AppSettings} object.
     */
    public AppSettings getSettings() {
        return settings;
    }

    /**
     * Updates the primary data directory for the application, persists the configuration,
     * and automatically provisions the new folder structure.
     * 
     * @param newPath The absolute path of the new data directory.
     */
    public void updateDataDirectory(String newPath) {
        settings.setDataDirectory(newPath);
        saveSettings();
        ensureDataDirExists();
    }

    /**
     * Updates the logs directory, persists the setting, and updates the ConsoleLogger hook.
     * 
     * @param newPath The absolute path of the new logs directory.
     */
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

    /**
     * Sanitizes strings (like Collection or Environment names) for safe usage as file system paths.
     * 
     * @param name The raw string to sanitize.
     * @return A sanitized, lowercased version of the string replacing invalid characters with underscores.
     */
    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank())
            return "unnamed";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }

    // --- Collections ---
    
    /**
     * Loads all Collection models from the active data directory.
     * Includes logic to automatically migrate legacy monolithic collections.json files 
     * into the modern split-file directory structure.
     * 
     * @return A list of {@link CollectionModel} instances.
     */
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

    /**
     * Persists a list of collections to the filesystem. 
     * This method dynamically manages orphaned files by cleaning up obsolete JSON files
     * that no longer correspond to an active collection in the workspace.
     * 
     * @param collections The list of collections to persist.
     */
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
