package world.bentobox.bentobox.api.addons;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.request.AddonRequestHandler;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;

/**
 * Add-on class for BentoBox. Extend this to create an add-on. The operation
 * and methods are very similar to Bukkit's JavaPlugin.
 *
 * @author tastybento, ComminQ_Q
 */
public abstract class Addon {

    private static final String ADDON_CONFIG_FILENAME = "config.yml";
    private State state;
    private AddonDescription description;
    private FileConfiguration config;
    private File dataFolder;
    private File file;
    private Map<String, AddonRequestHandler> requestHandlers = new HashMap<>();

    protected Addon() {
        state = State.DISABLED;
    }

    /**
     * Executes code when enabling the addon.
     * This is called after {@link #onLoad()}.
     * <br/>
     * Note that commands and worlds registration <b>must</b> be done in {@link #onLoad()}, if need be.
     * Failure to do so <b>will</b> result in issues such as tab-completion not working for commands.
     */
    public abstract void onEnable();

    /**
     * Executes code when disabling the addon.
     */
    public abstract void onDisable();

    /**
     * Executes code when loading the addon.
     * This is called before {@link #onEnable()}.
     * This <b>must</b> be used to setup configuration, worlds and commands.
     */
    public void onLoad() {}

    /**
     * Executes code when reloading the addon.
     */
    public void onReload() {}

    public BentoBox getPlugin() {
        return BentoBox.getInstance();
    }

    /**
     * Represents the current run-time state of a {@link Addon}.
     *
     * @author Poslovitch
     */
    public enum State {
        /**
         * The addon has been correctly loaded.
         * @since 1.1
         */
        LOADED,

        /**
         * The addon has been correctly enabled and is now fully working.
         */
        ENABLED,

        /**
         * The addon is fully disabled.
         */
        DISABLED,

        /**
         * The addon has not been loaded because it requires a different version of BentoBox or of the server software.
         */
        INCOMPATIBLE,

        /**
         * The addon has not been enabled because a dependency is missing.
         */
        MISSING_DEPENDENCY,

        /**
         * The addon loading or enabling process has been interrupted by an unhandled error.
         */
        ERROR
    }

    /**
     * @return the addon's default config file
     */
    public FileConfiguration getConfig() {
        if (config == null) {
            config = loadYamlFile();
        }
        return config;
    }

    /**
     * @return Addon's data folder
     */
    public File getDataFolder() {
        return dataFolder;
    }

    /**
     * @return Addon's description
     */
    public AddonDescription getDescription() {
        return description;
    }

    /**
     * @return the file
     */
    public File getFile() {
        return file;
    }

    /**
     * @return Logger
     */
    public Logger getLogger() {
        return getPlugin().getLogger();
    }

    /**
     * Convenience method to obtain the server
     *
     * @return the server object
     */
    public Server getServer() {
        return Bukkit.getServer();
    }

    public boolean isEnabled() {
        return state == State.ENABLED;
    }

    /**
     * Gets the current {@link State} of this Addon.
     * @return the current State of this Addon.
     * @since 1.1
     */
    public State getState() {
        return state;
    }

    /**
     * Load YAML config file
     *
     * @return Yaml File configuration
     */
    private FileConfiguration loadYamlFile() {
        File yamlFile = new File(dataFolder, ADDON_CONFIG_FILENAME);

        YamlConfiguration yamlConfig = null;
        if (yamlFile.exists()) {
            try {
                yamlConfig = new YamlConfiguration();
                yamlConfig.load(yamlFile);
            } catch (Exception e) {
                Bukkit.getLogger().severe(() -> "Could not load config.yml: " + e.getMessage());
            }
        }
        return yamlConfig;
    }

    /**
     * Register a listener for this addon. This MUST be used in order for the addon to be reloadable
     *
     * @param listener - listener
     */
    public void registerListener(Listener listener) {
        BentoBox.getInstance().getAddonsManager().registerListener(this, listener);
    }

    /**
     * Saves the FileConfiguration retrievable by getConfig().
     */
    public void saveConfig() {
        try {
            getConfig().save(new File(dataFolder, ADDON_CONFIG_FILENAME));
        } catch (IOException e) {
            Bukkit.getLogger().severe("Could not save config! " + this.getDescription().getName() + " " + e.getMessage());
        }
    }

    /**
     * Discards any data in getConfig() and reloads from disk.
     * @since 1.13.0
     */
    public void reloadConfig() {
        config = loadYamlFile();
    }

    /**
     * Saves the addon's config.yml file to the addon's data folder and loads it. If
     * the file exists already, it will not be replaced.
     */
    public void saveDefaultConfig() {
        saveResource(ADDON_CONFIG_FILENAME, false);
        config = loadYamlFile();
    }

    /**
     * Saves a resource contained in this add-on's jar file to the addon's data
     * folder.
     *
     * @param resourcePath
     *            in jar file
     * @param replace
     *            - if true, will overwrite previous file
     */
    public void saveResource(String resourcePath, boolean replace) {
        saveResource(resourcePath, dataFolder, replace, false);
    }

    /**
     * Saves a resource contained in this add-on's jar file to the destination
     * folder.
     *
     * @param jarResource
     *            in jar file
     * @param destinationFolder
     *            on file system
     * @param replace
     *            - if true, will overwrite previous file
     * @param noPath
     *            - if true, the resource's path will be ignored when saving
     * @return file written, or null if none
     */
    public File saveResource(String jarResource, File destinationFolder, boolean replace, boolean noPath) {
        if (jarResource == null || jarResource.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        jarResource = jarResource.replace('\\', '/');
        try (JarFile jar = new JarFile(file)) {
            JarEntry jarConfig = jar.getJarEntry(jarResource);
            if (jarConfig != null) {
                try (InputStream in = jar.getInputStream(jarConfig)) {
                    if (in == null) {
                        throw new IllegalArgumentException(
                                "The embedded resource '" + jarResource + "' cannot be found in " + jar.getName());
                    }
                    // There are two options, use the path of the resource or not
                    File outFile = new File(destinationFolder, jarResource);
                    if (noPath) {
                        outFile = new File(destinationFolder, outFile.getName());
                    }
                    // Make any dirs that need to be made
                    outFile.getParentFile().mkdirs();
                    if (!outFile.exists() || replace) {
                        java.nio.file.Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    return outFile;
                }
            } else {
                // No file in the jar
                throw new IllegalArgumentException(
                        "The embedded resource '" + jarResource + "' cannot be found in " + jar.getName());
            }
        } catch (IOException e) {
            BentoBox.getInstance().logError(
                    "Could not save from jar file. From " + jarResource + " to " + destinationFolder.getAbsolutePath());
        }
        return null;
    }

    /**
     * Tries to load a YAML file from the Jar
     * @param jarResource - YAML file in jar
     * @return YamlConfiguration - may be empty
     * @throws IOException - if the file cannot be found or loaded from the Jar
     * @throws InvalidConfigurationException - if the yaml is malformed
     */
    public YamlConfiguration getYamlFromJar(String jarResource) throws IOException, InvalidConfigurationException {
        if (jarResource == null || jarResource.equals("")) {
            throw new IllegalArgumentException("jarResource cannot be null or empty");
        }
        YamlConfiguration result = new YamlConfiguration();
        jarResource = jarResource.replace('\\', '/');
        try (JarFile jar = new JarFile(file)) {
            JarEntry jarConfig = jar.getJarEntry(jarResource);
            if (jarConfig != null) {
                try (InputStreamReader in = new InputStreamReader(jar.getInputStream(jarConfig))) {
                    result.load(in);
                }
            }
        }
        return result;
    }

    /**
     * Get the resource from Jar file
     * @param jarResource - jar resource filename
     * @return resource or null if there is a problem
     */
    public InputStream getResource(String jarResource) {
        if (jarResource == null || jarResource.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        jarResource = jarResource.replace('\\', '/');
        try (JarFile jar = new JarFile(file)) {
            JarEntry jarConfig = jar.getJarEntry(jarResource);
            if (jarConfig != null) {
                try (InputStream in = jar.getInputStream(jarConfig)) {
                    return in;
                }
            }
        } catch (IOException e) {
            Bukkit.getLogger().severe("Could not open from jar file. " + jarResource);
        }
        return null;
    }
    /**
     * Set the file that contains this addon
     *
     * @param f the file to set
     */
    public void setFile(File f) {
        file = f;
    }

    /**
     * Set this addon's data folder
     *
     * @param file - data folder
     */
    public void setDataFolder(File file) {
        dataFolder = file;
    }

    /**
     * Set this addons description
     *
     * @param description - description
     */
    public void setDescription(AddonDescription description) {
        this.description = description;
    }

    /**
     * Sets the addon's state.
     * @param state the state to set
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * Get Players Manager
     * @return Players manager
     */
    public PlayersManager getPlayers() {
        return getPlugin().getPlayers();
    }

    /**
     * Get Islands Manager
     * @return Islands manager
     */
    public IslandsManager getIslands() {
        return getPlugin().getIslands();
    }
    
    /**
     * Get Islands Manager
     * @return Islands manager
     * @see #getIslands()
     * @since 1.17.1
     */
    public IslandsManager getIslandsManager() {
        return getPlugin().getIslandsManager();
    }

    /**
     * Get the Addon By Name
     * @return Optional Addon
     */
    public Optional<Addon> getAddonByName(String name) {
        return getPlugin().getAddonsManager().getAddonByName(name);
    }

    public void log(String string) {
        getPlugin().log(getDescription() != null ? "[" + getDescription().getName() + "] " + string : string);
    }

    public void logWarning(String string) {
        getPlugin().logWarning(getDescription() != null ? "[" + getDescription().getName() + "] " + string : string);
    }

    public void logError(String string) {
        getPlugin().logError(getDescription() != null ? "[" + getDescription().getName() + "] " + string : string);
    }

    /**
     * Returns the permission prefix corresponding to this addon.
     * It contains the addon's name plus a trailing dot.
     * @return Permission prefix string
     */
    public String getPermissionPrefix() {
        return this.getDescription().getName().toLowerCase(Locale.ENGLISH) + ".";
    }

    /**
     * Register request handler to answer requests from plugins.
     * @param handler request handler
     */
    public void registerRequestHandler(AddonRequestHandler handler) {
        requestHandlers.put(handler.getLabel(), handler);
    }

    /**
     * Send request to addon.
     * @param label label
     * @param metaData meta data
     * @return request response, null if no response.
     */
    public Object request(String label, Map<String, Object> metaData) {
        label = label.toLowerCase(Locale.ENGLISH);
        AddonRequestHandler handler = requestHandlers.get(label);
        if(handler != null) {
            return handler.handle(metaData);
        } else {
            return null;
        }
    }


    /**
     * Register a flag for this addon.
     * @param flag the flag to register.
     * @return {@code true} if the flag was registered successfully, {@code false} otherwise.
     * @since 1.5.0
     */
    public boolean registerFlag(Flag flag) {
        return getPlugin().getFlagsManager().registerFlag(this, flag);
    }

    /**
     * Called when all addons have been loaded by BentoBox
     * @since 1.8.0
     */
    public void allLoaded() {}
}
