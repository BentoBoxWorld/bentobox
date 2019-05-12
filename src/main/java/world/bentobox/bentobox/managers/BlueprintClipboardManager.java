package world.bentobox.bentobox.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.blueprints.Blueprint;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.BlueprintClipboard;
import world.bentobox.bentobox.blueprints.BlueprintPaster;
import world.bentobox.bentobox.database.json.BentoboxTypeAdapterFactory;
import world.bentobox.bentobox.database.objects.Island;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author tastybento
 * @since 1.5.0
 */
public class BlueprintClipboardManager {

    private static final String LOAD_ERROR = "Could not load blueprint file - does not exist : ";

    private File blueprintFolder;

    private BlueprintClipboard clipboard;

    private Gson gson;

    private BentoBox plugin;

    public BlueprintClipboardManager(BentoBox plugin, File blueprintFolder) {
        this(plugin, blueprintFolder, null);
    }

    public BlueprintClipboardManager(BentoBox plugin, File blueprintFolder, BlueprintClipboard clipboard) {
        super();
        this.plugin = plugin;
        if (!blueprintFolder.exists()) {
            blueprintFolder.mkdirs();
        }
        this.blueprintFolder = blueprintFolder;
        this.clipboard = clipboard;
        getGson();
    }

    /**
     * @return the clipboard
     */
    public BlueprintClipboard getClipboard() {
        return clipboard;
    }

    private void getGson() {
        GsonBuilder builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().enableComplexMapKeySerialization();
        // Disable <>'s escaping etc.
        builder.disableHtmlEscaping();
        // Register adapter factory
        builder.registerTypeAdapterFactory(new BentoboxTypeAdapterFactory(plugin));
        gson = builder.create();
    }

    /**
     * Load a file to clipboard
     * @param fileName - filename in schems folder
     * @throws IOException - if there's a load error with unziping or name
     * @throws InvalidConfigurationException - the YAML of the schem is at fault
     */
    public void load(String fileName) throws IOException {
        clipboard = new BlueprintClipboard(loadBlueprint(fileName));

    }

    /**
     * Loads a blueprint
     * @param fileName - the filename without the suffix
     * @return the blueprint
     * @throws IOException
     */
    public Blueprint loadBlueprint(String fileName) throws IOException {
        File zipFile = new File(blueprintFolder, fileName + BlueprintsManager.BLUEPRINT_SUFFIX);
        if (!zipFile.exists()) {
            plugin.logError(LOAD_ERROR + zipFile.getName());
            throw new IOException(LOAD_ERROR + zipFile.getName());
        }
        unzip(zipFile.getAbsolutePath());
        File file = new File(blueprintFolder, fileName);
        if (!file.exists()) {
            plugin.logError(LOAD_ERROR + file.getName());
            throw new IOException(LOAD_ERROR + file.getName() + " temp file");
        }
        Blueprint bp = gson.fromJson(new FileReader(file), Blueprint.class);
        Files.delete(file.toPath());
        return bp;
    }

    /**
     * Load a blueprint to the clipboard for a user
     * @param user - user trying to load
     * @param fileName - filename
     * @return - <tt>true</tt> if load is successful, <tt>false</tt> if not
     */
    public boolean load(User user, String fileName) {
        try {
            load(fileName);
        } catch (IOException e1) {
            user.sendMessage("commands.admin.schem.could-not-load");
            plugin.logError("Could not load schems file: " + fileName + " " + e1.getMessage());
            return false;
        }
        user.sendMessage("general.success");
        return true;
    }

    /**
     * Paste clipboard to this location
     * @param location - location
     */
    public void pasteClipboard(Location location) {
        if (clipboard != null) {
            new BlueprintPaster(plugin, clipboard, location);
        } else {
            plugin.logError("Clipboard has no block data in it to paste!");
        }
    }

    /**
     * Pastes the clipboard to island location.
     * If pos1 and pos2 are not set already, they are automatically set to the pasted coordinates
     * @param world - world in which to paste
     * @param island - location to paste
     * @param task - task to run after pasting
     */
    public void pasteIsland(World world, Island island, Runnable task) {
        new BlueprintPaster(plugin, clipboard, world, island, task);
    }

    /**
     * Save the clipboard to a file
     * @param user - user who is copying
     * @param newName - new name of this blueprint
     * @return - true if successful, false if error
     */
    public boolean save(User user, String newName) {
        clipboard.getBp().setName(newName);
        File file = new File(blueprintFolder, newName);
        if (saveBlueprint(file, clipboard.getBp())) {
            user.sendMessage("general.success");
            return true;
        }
        user.sendMessage("commands.admin.schem.could-not-save", "[message]", "Could not save temp blueprint file.");
        return false;
    }

    /**
     * Save a blueprint
     * @param file - file to save to
     * @param blueprint - blueprint
     * @return true if successful, false if not
     */
    public boolean saveBlueprint(File file, Blueprint blueprint) {
        String toStore = gson.toJson(blueprint, Blueprint.class);
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(toStore);
        } catch (IOException e) {
            plugin.logError("Could not save temporary blueprint file: " + file.getName());
            return false;
        }
        try {
            zip(file);
        } catch (IOException e) {
            plugin.logError("Could not zip temporary blueprint file: " + file.getName());
            return false;
        }
        return true;
    }

    private void unzip(final String zipFilePath) throws IOException {
        Path path = Paths.get(zipFilePath);
        if (!(path.toFile().exists())) {
            throw new IOException("No file exists!");
        }
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                Path filePath = Paths.get(path.getParent().toString(), entry.getName());
                if (!entry.isDirectory()) {
                    unzipFiles(zipInputStream, filePath);
                } else {
                    Files.createDirectories(filePath);
                }

                zipInputStream.closeEntry();
                entry = zipInputStream.getNextEntry();
            }
        }
    }

    private void unzipFiles(final ZipInputStream zipInputStream, final Path unzipFilePath) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(unzipFilePath.toAbsolutePath().toString()))) {
            byte[] bytesIn = new byte[1024];
            int read;
            while ((read = zipInputStream.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }

    private void zip(File targetFile) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(targetFile.getAbsolutePath() + BlueprintsManager.BLUEPRINT_SUFFIX))) {
            zipOutputStream.putNextEntry(new ZipEntry(targetFile.getName()));
            try (FileInputStream inputStream = new FileInputStream(targetFile)) {
                final byte[] buffer = new byte[1024];
                int length;
                while((length = inputStream.read(buffer)) >= 0) {
                    zipOutputStream.write(buffer, 0, length);
                }
                try {
                    Files.delete(targetFile.toPath());
                } catch (Exception e) {
                    plugin.logError(e.getMessage());
                }
            }
        }
    }

}