package world.bentobox.bentobox.managers;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.BlueprintPaster;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.database.json.BentoboxTypeAdapterFactory;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

/**
 * Handles Blueprints
 *
 * @author Poslovitch, tastybento
 * @since 1.5.0
 */
public class BlueprintsManager {

    private static final String BLUEPRINT_BUNDLE_SUFFIX = ".json";
    public static final String BLUEPRINT_SUFFIX = ".blu";
    public static final String DEFAULT_BUNDLE_NAME = "default";

    public static final @NonNull String FOLDER_NAME = "blueprints";
    private static final String FOR = "' for ";
    public static final String UNCOMPRESSED_BLUEPRINT_SUFFIX = ".blj";

    /**
     * Map of blueprint bundles to game mode addon.
     * Inner map's key is the uniqueId of the blueprint bundle so it's
     * easy to get from a UI
     */
    private @NonNull Map<GameModeAddon, List<BlueprintBundle>> blueprintBundles;

    /**
     * Map of blueprints. There can be many blueprints per game mode addon
     * Inner map's key is the blueprint's name so it's easy to get from a UI
     */
    private @NonNull Map<GameModeAddon, List<Blueprint>> blueprints;

    /**
     * Gson used for serializing/deserializing the bundle class
     */
    private final Gson gson;

    private final @NonNull BentoBox plugin;

    private @NonNull Set<GameModeAddon> blueprintsLoaded;


    public BlueprintsManager(@NonNull BentoBox plugin) {
        this.plugin = plugin;
        // Must use ConcurrentHashMap because the maps are loaded async and they need to be thread safe
        this.blueprintBundles = new ConcurrentHashMap<>();
        this.blueprints = new ConcurrentHashMap<>();
        @SuppressWarnings({"rawtypes", "unchecked"})
        GsonBuilder builder = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .enableComplexMapKeySerialization()
        .setPrettyPrinting()
        // This enables gson to deserialize enum maps
        .registerTypeAdapter(EnumMap.class, (InstanceCreator<EnumMap>) type -> {
            Type[] types = (((ParameterizedType) type).getActualTypeArguments());
            return new EnumMap((Class<?>) types[0]);
        });
        // Disable <>'s escaping etc.
        builder.disableHtmlEscaping();
        // Register adapter factory
        builder.registerTypeAdapterFactory(new BentoboxTypeAdapterFactory(plugin));
        gson = builder.create();
        // Loaded tracker
        blueprintsLoaded = new HashSet<>();
    }

    /**
     * Extracts the blueprints and bundles provided by this {@link GameModeAddon} in its .jar file.
     * This will do nothing if the blueprints folder already exists for this GameModeAddon.
     *
     * @param addon the {@link GameModeAddon} to extract the blueprints from.
     */
    public void extractDefaultBlueprints(@NonNull GameModeAddon addon) {
        File folder = getBlueprintsFolder(addon);
        if (folder.exists()) {
            // If the folder exists, do not copy anything from the jar
            return;
        }

        if (!folder.exists() && !folder.mkdirs()) {
            plugin.logError("Could not create the '" + FOLDER_NAME + "' folder!");
            plugin.logError("This might be due to incorrectly set-up write permissions on the operating system.");
            return;
        }

        // Get any blueprints or bundles from the jar and save them.
        try (JarFile jar = new JarFile(addon.getFile())) {
            Util.listJarFiles(jar, FOLDER_NAME, BLUEPRINT_BUNDLE_SUFFIX).forEach(name -> addon.saveResource(name, false));
            Util.listJarFiles(jar, FOLDER_NAME, BLUEPRINT_SUFFIX).forEach(name -> addon.saveResource(name, false));
        } catch (IOException e) {
            plugin.logError("Could not load blueprint files from addon jar " + e.getMessage());
        }
    }

    /**
     * Get the blueprint bundles of this addon.
     *
     * @param addon the {@link GameModeAddon} to get the blueprint bundles.
     */
    @NonNull
    public Map<String, BlueprintBundle> getBlueprintBundles(@NonNull GameModeAddon addon) {
        if (!blueprintBundles.containsKey(addon)) {
            return new HashMap<>();
        }
        return blueprintBundles.get(addon).stream().collect(Collectors.toMap(BlueprintBundle::getUniqueId, b -> b));
    }

    /**
     * Get the default blueprint bundle for game mode
     * @param addon - game mode addon
     * @return the default blueprint bundle or null if none
     * @since 1.8.0
     */
    @Nullable
    public BlueprintBundle getDefaultBlueprintBundle(@NonNull GameModeAddon addon) {
        if (blueprintBundles.containsKey(addon)) {
            return blueprintBundles.get(addon).stream().filter(bb -> bb.getUniqueId().equals(DEFAULT_BUNDLE_NAME)).findFirst().orElse(null);
        }
        return null;
    }

    /**
     * Returns a {@link File} instance of the blueprints folder of this {@link GameModeAddon}.
     *
     * @param addon the {@link GameModeAddon}
     * @return a {@link File} instance of the blueprints folder of this GameModeAddon.
     */
    @NonNull
    private File getBlueprintsFolder(@NonNull GameModeAddon addon) {
        return new File(addon.getDataFolder(), FOLDER_NAME);
    }

    /**
     * Loads the blueprint bundles of this addon from its blueprints folder.
     *
     * @param addon the {@link GameModeAddon} to load the blueprints of.
     */
    public void loadBlueprintBundles(@NonNull GameModeAddon addon) {
        // Set loading flag
        blueprintsLoaded.add(addon);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Load bundles
            blueprintBundles.put(addon, new ArrayList<>());
            if (!loadBundles(addon)) {
                makeDefaults(addon);
                loadBundles(addon);
            }
            // Load blueprints
            loadBlueprints(addon);

            // Clear loading flag
            blueprintsLoaded.remove(addon);
        });
    }

    /**
     * Check if all blueprints are loaded. Only query after all GameModes have been loaded.
     * @return true if all blueprints are loaded
     */
    public boolean isBlueprintsLoaded() {
        return blueprintsLoaded.isEmpty();
    }

    private boolean loadBundles(@NonNull GameModeAddon addon) {
        File bpf = getBlueprintsFolder(addon);
        if (!bpf.exists()) {
            plugin.logError("There is no blueprint folder for addon " + addon.getDescription().getName());
            bpf.mkdirs();
        }
        boolean loaded = false;
        File[] bundles = bpf.listFiles((dir, name) -> name.toLowerCase(Locale.ENGLISH).endsWith(BLUEPRINT_BUNDLE_SUFFIX));
        if (bundles == null || bundles.length == 0) {
            return false;
        }
        for (File file : bundles) {
            try {
                BlueprintBundle bb = gson.fromJson(new FileReader(file), BlueprintBundle.class);
                if (bb != null) {
                    // Make sure there is no existing bundle with the same uniqueId
                    if (blueprintBundles.get(addon).stream().noneMatch(bundle ->  bundle.getUniqueId().equals(bb.getUniqueId()))) {
                        blueprintBundles.get(addon).add(bb);
                        plugin.log("Loaded Blueprint Bundle '" + bb.getUniqueId() + FOR + addon.getDescription().getName() + ".");
                        loaded = true;
                    } else {
                        // There is a bundle that already uses this uniqueId.
                        // In that case, we log that and do not load the new bundle.
                        plugin.logWarning("Could not load blueprint bundle '" + file.getName() + FOR + addon.getDescription().getName() + ".");
                        plugin.logWarning("The uniqueId '" + bb.getUniqueId() + "' is already used by another Blueprint Bundle.");
                        plugin.logWarning("This can occur if the Blueprint Bundles' files were manually edited.");
                        plugin.logWarning("Please review your Blueprint Bundles' files and make sure their uniqueIds are not in duplicate.");
                    }
                }
            } catch (Exception e) {
                plugin.logError("Could not load blueprint bundle '" + file.getName() + "'. Cause: " + e.getMessage() + ".");
                plugin.logStacktrace(e);
            }
        }
        return loaded;
    }

    private BlueprintBundle getDefaultBlueprintBundle() {
        BlueprintBundle bb = new BlueprintBundle();
        bb.setIcon(Material.PAPER);
        bb.setUniqueId(DEFAULT_BUNDLE_NAME);
        bb.setDisplayName("Default bundle");
        bb.setDescription(Collections.singletonList(ChatColor.AQUA + "Default bundle of blueprints"));
        return bb;
    }

    private Blueprint getDefaultBlueprint() {
        Blueprint defaultBp = new Blueprint();
        defaultBp.setName("bedrock");
        defaultBp.setDescription(Collections.singletonList(ChatColor.AQUA + "A bedrock block"));
        defaultBp.setBedrock(new Vector(0, 0, 0));
        Map<Vector, BlueprintBlock> map = new HashMap<>();
        map.put(new Vector(0, 0, 0), new BlueprintBlock("minecraft:bedrock"));
        defaultBp.setBlocks(map);
        return defaultBp;
    }

    /**
     * This should never be needed and is just a boot strap
     *
     * @param addon addon
     */
    private void makeDefaults(@NonNull GameModeAddon addon) {
        plugin.logError("No blueprint bundles found! Creating a default one.");
        BlueprintBundle bb = getDefaultBlueprintBundle();
        // Default blueprints
        Blueprint defaultBp = getDefaultBlueprint();
        // Save a default "bedrock" blueprint
        new BlueprintClipboardManager(plugin, getBlueprintsFolder(addon)).saveBlueprint(defaultBp);
        // This blueprint is used for all environments
        bb.setBlueprint(World.Environment.NORMAL, defaultBp);
        bb.setBlueprint(World.Environment.NETHER, defaultBp);
        bb.setBlueprint(World.Environment.THE_END, defaultBp);
        bb.setUniqueId(DEFAULT_BUNDLE_NAME);
        blueprintBundles.get(addon).add(bb);
        this.saveBlueprintBundles();
    }

    /**
     * Loads all the blueprints of this addon from its blueprints folder.
     *
     * @param addon the {@link GameModeAddon} to load the blueprints of.
     */
    public void loadBlueprints(@NonNull GameModeAddon addon) {
        blueprints.put(addon, new ArrayList<>());
        File bpf = getBlueprintsFolder(addon);
        if (!bpf.exists()) {
            plugin.logError("There is no blueprint folder for addon " + addon.getDescription().getName());
            bpf.mkdirs();
        }
        File[] bps = bpf.listFiles((dir, name) -> name.toLowerCase(Locale.ENGLISH).endsWith(BLUEPRINT_SUFFIX) 
                || name.toLowerCase(Locale.ENGLISH).endsWith(UNCOMPRESSED_BLUEPRINT_SUFFIX));
        if (bps == null || bps.length == 0) {
            plugin.logError("No blueprints found for " + addon.getDescription().getName());
            return;
        }
        for (File file : bps) {
            String fileName = file.getName().substring(0, file.getName().length() - BLUEPRINT_SUFFIX.length());
            try {
                Blueprint bp = new BlueprintClipboardManager(plugin, bpf).loadBlueprint(fileName);
                bp.setName(fileName);
                blueprints.get(addon).add(bp);
                plugin.log("Loaded blueprint '" + bp.getName() + FOR + addon.getDescription().getName());
            } catch (Exception e) {
                plugin.logError("Could not load blueprint " + fileName + " " + e.getMessage());
                plugin.logStacktrace(e);
            }
        }
    }

    /**
     * Adds a blueprint to addon's list of blueprints. If the list already contains a blueprint with the same name
     * it is replaced.
     *
     * @param addon - the {@link GameModeAddon}
     * @param bp    - blueprint
     */
    public void addBlueprint(@NonNull GameModeAddon addon, @NonNull Blueprint bp) {
        blueprints.putIfAbsent(addon, new ArrayList<>());
        blueprints.get(addon).removeIf(b -> b.getName().equals(bp.getName()));
        blueprints.get(addon).add(bp);
        plugin.log("Added blueprint '" + bp.getName() + FOR + addon.getDescription().getName());
    }

    /**
     * Saves a blueprint into addon's blueprint folder
     *
     * @param addon - the {@link GameModeAddon}
     * @param bp    - blueprint to save
     */
    public boolean saveBlueprint(@NonNull GameModeAddon addon, @NonNull Blueprint bp) {
        return new BlueprintClipboardManager(plugin, getBlueprintsFolder(addon)).saveBlueprint(bp);
    }

    /**
     * Save blueprint bundle for game mode
     *
     * @param addon - gamemode addon
     * @param bb    blueprint bundle to save
     */
    public void saveBlueprintBundle(GameModeAddon addon, BlueprintBundle bb) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File bpf = getBlueprintsFolder(addon);
            if (!bpf.exists()) {
                bpf.mkdirs();
            }
            File fileName = new File(bpf, sanitizeFileName(bb.getUniqueId()) + BLUEPRINT_BUNDLE_SUFFIX);
            String toStore = gson.toJson(bb, BlueprintBundle.class);
            try (FileWriter fileWriter = new FileWriter(fileName)) {
                fileWriter.write(toStore);
            } catch (IOException e) {
                plugin.logError("Could not save blueprint bundle file: " + e.getMessage());
            }
        });
    }

    /**
     * Sanitizes a filename as much as possible retaining the original name
     * @param name - filename to sanitize
     * @return sanitized name
     */
    public static String sanitizeFileName(String name) {
        return name
                .chars()
                .mapToObj(i -> (char) i)
                .map(c -> Character.isWhitespace(c) ? '_' : c)
                .filter(c -> Character.isLetterOrDigit(c) || c == '-' || c == '_')
                .map(String::valueOf)
                .collect(Collectors.joining());
    }

    /**
     * Saves all the blueprint bundles
     */
    public void saveBlueprintBundles() {
        blueprintBundles.forEach((k, v) -> v.forEach(m -> saveBlueprintBundle(k, m)));
    }

    /**
     * Get blueprints for this game mode
     *
     * @param addon - game mode addon
     * @return Map of name and blueprint or empty map
     */
    public Map<String, Blueprint> getBlueprints(GameModeAddon addon) {
        if (!blueprints.containsKey(addon)) {
            return new HashMap<>();
        }
        return blueprints.get(addon).stream().collect(Collectors.toMap(Blueprint::getName, b -> b));
    }

    /**
     * Unregisters the Blueprint from the manager and deletes the file.
     * @param addon game mode addon
     * @param name name of the Blueprint to delete
     * @since 1.9.0
     */
    public void deleteBlueprint(GameModeAddon addon, String name) {
        List<Blueprint> addonBlueprints = blueprints.get(addon);
        Iterator<Blueprint> it = addonBlueprints.iterator();
        while (it.hasNext()) {
            Blueprint b = it.next();
            if (b.getName().equalsIgnoreCase(name)) {
                it.remove();
                blueprints.put(addon, addonBlueprints);

                File file = new File(getBlueprintsFolder(addon), b.getName() + BLUEPRINT_SUFFIX);
                // Delete the file
                try {
                    Files.deleteIfExists(file.toPath());
                } catch (IOException e) {
                    plugin.logError("Could not delete Blueprint " + e.getLocalizedMessage());
                }
            }
        }
    }

    /**
     * Paste the islands to world
     *
     * @param addon  - GameModeAddon
     * @param island - island
     * @param name   - bundle name
     */
    public void paste(GameModeAddon addon, Island island, String name) {
        paste(addon, island, name, null);
    }

    /**
     * Paste islands to the world and run task afterwards
     *
     * @param addon  - the game mode addon
     * @param island - the island
     * @param name   - name of bundle to paste
     * @param task   - task to run after pasting is completed
     * @return true if okay, false is there is a problem
     */
    public boolean paste(GameModeAddon addon, Island island, String name, Runnable task) {
        if (validate(addon, name) == null) {
            plugin.logError("Tried to paste '" + name + "' but the bundle is not loaded!");
            return false;
        }
        BlueprintBundle bb = getBlueprintBundles(addon).get(name.toLowerCase(Locale.ENGLISH));
        if (!blueprints.containsKey(addon) || blueprints.get(addon).isEmpty()) {
            plugin.logError("No blueprints loaded for bundle '" + name + "'!");
            return false;
        }
        Blueprint bp = getBlueprints(addon).get(bb.getBlueprint(World.Environment.NORMAL));
        if (bp == null) {
            // Oops, no overworld
            bp = getBlueprints(addon).get("island");
            plugin.logError("Blueprint bundle has no normal world blueprint, using default");
            if (bp == null) {
                plugin.logError("NO DEFAULT BLUEPRINT FOUND! Make sure 'island.blu' exists!");
            }
        }
        // Paste
        if (bp != null) {
            new BlueprintPaster(plugin, bp, addon.getOverWorld(), island).paste().thenAccept(b -> pasteNether(addon, bb, island).thenAccept(b2 ->
            pasteEnd(addon, bb, island).thenAccept(b3 -> Bukkit.getScheduler().runTask(plugin, task))));
        }
        return true;

    }

    private CompletableFuture<Boolean> pasteNether(GameModeAddon addon, BlueprintBundle bb, Island island) {
        if (bb.getBlueprint(World.Environment.NETHER) != null
                && addon.getWorldSettings().isNetherGenerate()
                && addon.getWorldSettings().isNetherIslands()
                && addon.getNetherWorld() != null) {
            Blueprint bp = getBlueprints(addon).get(bb.getBlueprint(World.Environment.NETHER));
            if (bp != null) {
                return new BlueprintPaster(plugin, bp, addon.getNetherWorld(), island).paste();
            }
        }
        return CompletableFuture.completedFuture(false);
    }

    private CompletableFuture<Boolean> pasteEnd(GameModeAddon addon, BlueprintBundle bb, Island island) {
        // Make end island
        if (bb.getBlueprint(World.Environment.THE_END) != null
                && addon.getWorldSettings().isEndGenerate()
                && addon.getWorldSettings().isEndIslands()
                && addon.getEndWorld() != null) {
            Blueprint bp = getBlueprints(addon).get(bb.getBlueprint(World.Environment.THE_END));
            if (bp != null) {
                return new BlueprintPaster(plugin, bp, addon.getEndWorld(), island).paste();
            }
        }
        return CompletableFuture.completedFuture(false);
    }

    /**
     * Validate if the bundle name is valid or not
     *
     * @param addon - game mode addon
     * @param name  - bundle name
     * @return bundle name or null if it's invalid
     */
    public @Nullable String validate(GameModeAddon addon, String name) {
        if (name == null) {
            return null;
        }
        if (blueprintBundles.containsKey(addon) && getBlueprintBundles(addon).containsKey(name.toLowerCase(Locale.ENGLISH))) {
            return name;
        }
        return null;
    }

    /**
     * Adds a blueprint bundle. If a bundle with the same uniqueId exists, it will be replaced
     *
     * @param addon - the game mode addon
     * @param bb    - the blueprint bundle
     */
    public void addBlueprintBundle(GameModeAddon addon, BlueprintBundle bb) {
        if (blueprintBundles.containsKey(addon)) {
            // Remove any bundles with the same name
            blueprintBundles.get(addon).removeIf(b -> b.getUniqueId().equals(bb.getUniqueId()));
        }
        blueprintBundles.computeIfAbsent(addon, k -> new ArrayList<>()).add(bb);
    }

    /**
     * Checks if a player has permission to see or use this blueprint bundle.
     *
     * @param addon - addon making the request
     * @param user  - user making the request
     * @param name  - name of the blueprint bundle
     * @return <tt>true</tt> if allowed, <tt>false</tt> if not or bundle does not exist
     */
    public boolean checkPerm(@NonNull Addon addon, @NonNull User user, @NonNull String name) {
        // Permission
        String permission = addon.getPermissionPrefix() + "island.create." + name;
        // Get Blueprint bundle
        BlueprintBundle bb = getBlueprintBundles((GameModeAddon) addon).get(name.toLowerCase(Locale.ENGLISH));
        if (bb == null || (bb.isRequirePermission() && !name.equals(DEFAULT_BUNDLE_NAME) && !user.hasPermission(permission))) {
            user.sendMessage("general.errors.no-permission", TextVariables.PERMISSION, permission);
            return false;
        }
        return true;
    }

    /**
     * Removes a blueprint bundle
     *
     * @param addon - Game Mode Addon
     * @param bb    - Blueprint Bundle to delete
     */
    public void deleteBlueprintBundle(@NonNull GameModeAddon addon, BlueprintBundle bb) {
        if (blueprintBundles.containsKey(addon)) {
            blueprintBundles.get(addon).removeIf(k -> k.getUniqueId().equals(bb.getUniqueId()));
        }
        File bpf = getBlueprintsFolder(addon);
        File fileName = new File(bpf, sanitizeFileName(bb.getUniqueId()) + BLUEPRINT_BUNDLE_SUFFIX);
        try {
            Files.deleteIfExists(fileName.toPath());
        } catch (IOException e) {
            plugin.logError("Could not delete Blueprint Bundle " + e.getLocalizedMessage());
        }
    }

    /**
     * Rename a blueprint
     *
     * @param addon - Game Mode Addon
     * @param bp    - blueprint
     * @param name  - new name
     */
    public void renameBlueprint(GameModeAddon addon, Blueprint bp, String name) {
        if (bp.getName().equalsIgnoreCase(name)) {
            // If the name is the same, do not do anything
            return;
        }
        File bpf = getBlueprintsFolder(addon);
        // Get the filename
        File fileName = new File(bpf, sanitizeFileName(bp.getName()) + BLUEPRINT_SUFFIX);
        // Delete the old file
        try {
            Files.deleteIfExists(fileName.toPath());
        } catch (IOException e) {
            plugin.logError("Could not delete old Blueprint " + e.getLocalizedMessage());
        }
        // Set new name
        bp.setName(name.toLowerCase(Locale.ENGLISH));
        // Save it
        saveBlueprint(addon, bp);
    }

}
