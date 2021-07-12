package world.bentobox.bentobox.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.BlueprintClipboard;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {Bukkit.class, BentoBox.class} )
public class BlueprintClipboardManagerTest {

    private static final String BLUEPRINT = "blueprint";

    @Mock
    private BentoBox plugin;
    @Mock
    private BlueprintClipboard clipboard;

    private File blueprintFolder;

    private String json = "{\n" +
            "    \"name\": \"blueprint\",\n" +
            "    \"attached\": {},\n" +
            "    \"entities\": {},\n" +
            "    \"blocks\": [\n" +
            "        [\n" +
            "            [3.0, -5.0, 8.0], {\n" +
            "                \"blockData\": \"minecraft:stone\"\n" +
            "            }\n" +
            "        ],\n" +
            "        [\n" +
            "            [6.0, -13.0, -20.0], {\n" +
            "                \"blockData\": \"minecraft:diorite\"\n" +
            "            }\n" +
            "        ]\n" +
            "    ],\n" +
            "    \"xSize\": 10,\n" +
            "    \"ySize\": 10,\n" +
            "    \"zSize\": 10,\n" +
            "    \"bedrock\": [-2.0, -16.0, -1.0]\n" +
            "}";

    private String jsonNoBedrock = "{\n" +
            "    \"name\": \"blueprint\",\n" +
            "    \"attached\": {},\n" +
            "    \"entities\": {},\n" +
            "    \"blocks\": [\n" +
            "        [\n" +
            "            [3.0, -5.0, 8.0], {\n" +
            "                \"blockData\": \"minecraft:stone\"\n" +
            "            }\n" +
            "        ],\n" +
            "        [\n" +
            "            [6.0, -13.0, -20.0], {\n" +
            "                \"blockData\": \"minecraft:diorite\"\n" +
            "            }\n" +
            "        ]\n" +
            "    ],\n" +
            "    \"xSize\": 10,\n" +
            "    \"ySize\": 10,\n" +
            "    \"zSize\": 10\n" +
            "}";

    private void zip(File targetFile) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(targetFile.getAbsolutePath() + BlueprintsManager.BLUEPRINT_SUFFIX))) {
            zipOutputStream.putNextEntry(new ZipEntry(targetFile.getName()));
            try (FileInputStream inputStream = new FileInputStream(targetFile)) {
                final byte[] buffer = new byte[1024];
                int length;
                while((length = inputStream.read(buffer)) >= 0) {
                    zipOutputStream.write(buffer, 0, length);
                }
            }
            try {
                Files.delete(targetFile.toPath());
            } catch (Exception e) {
                plugin.logError(e.getMessage());
            }
        }
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        blueprintFolder = new File("blueprints");
        // Clear any residual files
        tearDown();
        PowerMockito.mockStatic(Bukkit.class);
        BlockData blockData = mock(BlockData.class);
        when(Bukkit.createBlockData(any(Material.class))).thenReturn(blockData);
        when(blockData.getAsString()).thenReturn("test123");
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {

        if (blueprintFolder.exists()) {
            // Clean up file system
            Files.walk(blueprintFolder.toPath())
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
        }
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintClipboardManager#BlueprintClipboardManager(world.bentobox.bentobox.BentoBox, java.io.File)}.
     */
    @Test
    public void testBlueprintClipboardManagerBentoBoxFile() {
        new BlueprintClipboardManager(plugin, blueprintFolder);
        assertTrue(blueprintFolder.exists());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintClipboardManager#BlueprintClipboardManager(world.bentobox.bentobox.BentoBox, java.io.File, world.bentobox.bentobox.blueprints.BlueprintClipboard)}.
     */
    @Test
    public void testBlueprintClipboardManagerBentoBoxFileBlueprintClipboard() {
        new BlueprintClipboardManager(plugin, blueprintFolder, clipboard);
        assertTrue(blueprintFolder.exists());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintClipboardManager#getClipboard()}.
     */
    @Test
    public void testGetClipboard() {
        BlueprintClipboardManager bcm = new BlueprintClipboardManager(plugin, blueprintFolder, clipboard);
        assertEquals(clipboard, bcm.getClipboard());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintClipboardManager#loadBlueprint(java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testLoadBlueprintNoSuchFile() {
        BlueprintClipboardManager bcm = new BlueprintClipboardManager(plugin, blueprintFolder);
        try {
            bcm.loadBlueprint("test");
        } catch (Exception e) {
            assertTrue(e instanceof IOException);
        } finally {
            verify(plugin).logError("Could not load blueprint file - does not exist : test.blu");
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintClipboardManager#loadBlueprint(java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testLoadBlueprintNoFileInZip() throws IOException {
        blueprintFolder.mkdirs();
        // Make a blueprint file
        YamlConfiguration config = new YamlConfiguration();
        config.set("hello", "this is a test");
        File configFile = new File(blueprintFolder, "blueprint.blu");
        config.save(configFile);
        assertTrue(configFile.exists());
        BlueprintClipboardManager bcm = new BlueprintClipboardManager(plugin, blueprintFolder);
        try {
            bcm.loadBlueprint(BLUEPRINT);
        } catch (Exception e) {
            assertTrue(e instanceof IOException);
        } finally {
            verify(plugin).logError("Could not load blueprint file - does not exist : blueprint");
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintClipboardManager#loadBlueprint(java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testLoadBlueprintFileInZipJSONError() throws IOException {
        blueprintFolder.mkdirs();
        // Make a blueprint file
        YamlConfiguration config = new YamlConfiguration();
        config.set("hello", "this is a test");
        File configFile = new File(blueprintFolder, BLUEPRINT);
        config.save(configFile);
        // Zip it
        zip(configFile);
        BlueprintClipboardManager bcm = new BlueprintClipboardManager(plugin, blueprintFolder);
        try {
            bcm.loadBlueprint(BLUEPRINT);
        } catch (Exception e) {
            assertTrue(e instanceof IOException);
        } finally {
            verify(plugin).logError("Blueprint has JSON error: blueprint");
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintClipboardManager#loadBlueprint(java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testLoadBlueprintFileInZipNoBedrock() throws IOException {
        blueprintFolder.mkdirs();
        // Make a blueprint file
        File configFile = new File(blueprintFolder, BLUEPRINT);
        byte[] bytes = jsonNoBedrock.getBytes(StandardCharsets.UTF_8);
        Files.write(configFile.toPath(), bytes, StandardOpenOption.CREATE);
        // Zip it
        zip(configFile);
        BlueprintClipboardManager bcm = new BlueprintClipboardManager(plugin, blueprintFolder);
        Blueprint bp = bcm.loadBlueprint(BLUEPRINT);
        verify(plugin).logWarning("Blueprint blueprint.blu had no bedrock block in it so one was added automatically in the center. You should check it.");
        // Verify bedrock was placed in the center of the blueprint
        assertEquals(5, bp.getBedrock().getBlockX());
        assertEquals(5, bp.getBedrock().getBlockY());
        assertEquals(5, bp.getBedrock().getBlockZ());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintClipboardManager#loadBlueprint(java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testLoadBlueprintFileInZip() throws IOException {
        blueprintFolder.mkdirs();
        // Make a blueprint file
        File configFile = new File(blueprintFolder, BLUEPRINT);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Files.write(configFile.toPath(), bytes, StandardOpenOption.CREATE);
        // Zip it
        zip(configFile);
        BlueprintClipboardManager bcm = new BlueprintClipboardManager(plugin, blueprintFolder);
        Blueprint bp = bcm.loadBlueprint(BLUEPRINT);
        assertEquals(-2, bp.getBedrock().getBlockX());
        assertEquals(-16, bp.getBedrock().getBlockY());
        assertEquals(-1, bp.getBedrock().getBlockZ());
        assertTrue(bp.getAttached().isEmpty());
        assertTrue(bp.getEntities().isEmpty());
        assertEquals(2, bp.getBlocks().size());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintClipboardManager#loadBlueprint(java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testLoadUncompressedBlueprint() throws IOException {
        blueprintFolder.mkdirs();
        // Make a blueprint file
        File configFile = new File(blueprintFolder, BLUEPRINT + ".blj");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Files.write(configFile.toPath(), bytes, StandardOpenOption.CREATE);
        BlueprintClipboardManager bcm = new BlueprintClipboardManager(plugin, blueprintFolder);
        Blueprint bp = bcm.loadBlueprint(BLUEPRINT);
        assertEquals(-2, bp.getBedrock().getBlockX());
        assertEquals(-16, bp.getBedrock().getBlockY());
        assertEquals(-1, bp.getBedrock().getBlockZ());
        assertTrue(bp.getAttached().isEmpty());
        assertTrue(bp.getEntities().isEmpty());
        assertEquals(2, bp.getBlocks().size());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintClipboardManager#load(java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testLoadString() throws IOException {
        blueprintFolder.mkdirs();
        // Make a blueprint file
        File configFile = new File(blueprintFolder, BLUEPRINT);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Files.write(configFile.toPath(), bytes, StandardOpenOption.CREATE);
        // Zip it
        zip(configFile);
        BlueprintClipboardManager bcm = new BlueprintClipboardManager(plugin, blueprintFolder);
        bcm.load(BLUEPRINT);
        Blueprint bp = bcm.getClipboard().getBlueprint();
        assertEquals(-2, bp.getBedrock().getBlockX());
        assertEquals(-16, bp.getBedrock().getBlockY());
        assertEquals(-1, bp.getBedrock().getBlockZ());
        assertTrue(bp.getAttached().isEmpty());
        assertTrue(bp.getEntities().isEmpty());
        assertEquals(2, bp.getBlocks().size());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintClipboardManager#load(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testLoadUserString() throws IOException {
        blueprintFolder.mkdirs();
        // Make a blueprint file
        File configFile = new File(blueprintFolder, BLUEPRINT);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Files.write(configFile.toPath(), bytes, StandardOpenOption.CREATE);
        // Zip it
        zip(configFile);
        BlueprintClipboardManager bcm = new BlueprintClipboardManager(plugin, blueprintFolder);
        User user = mock(User.class);
        assertTrue(bcm.load(user, BLUEPRINT));
        verify(user).sendMessage("general.success");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintClipboardManager#load(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testLoadUncompressedUserString() throws IOException {
        blueprintFolder.mkdirs();
        // Make a blueprint file
        File configFile = new File(blueprintFolder, BLUEPRINT + ".blj");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Files.write(configFile.toPath(), bytes, StandardOpenOption.CREATE);
        BlueprintClipboardManager bcm = new BlueprintClipboardManager(plugin, blueprintFolder);
        User user = mock(User.class);
        assertTrue(bcm.load(user, BLUEPRINT));
        verify(user).sendMessage("general.success");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintClipboardManager#load(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testLoadUserStringFail() throws IOException {
        BlueprintClipboardManager bcm = new BlueprintClipboardManager(plugin, blueprintFolder);
        User user = mock(User.class);
        assertFalse(bcm.load(user, BLUEPRINT));
        verify(user).sendMessage("commands.admin.blueprint.could-not-load");
        verify(plugin).logError("Could not load blueprint file - does not exist : blueprint.blu");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintClipboardManager#save(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testSave() throws IOException {
        // Load a blueprint, then save it
        blueprintFolder.mkdirs();
        // Make a blueprint file
        File configFile = new File(blueprintFolder, BLUEPRINT);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Files.write(configFile.toPath(), bytes, StandardOpenOption.CREATE);
        // Zip it
        zip(configFile);
        BlueprintClipboardManager bcm = new BlueprintClipboardManager(plugin, blueprintFolder);
        bcm.load(BLUEPRINT);
        User user = mock(User.class);
        assertTrue(bcm.save(user, "test1234"));
        File bp = new File(blueprintFolder, "test1234.blu");
        assertTrue(bp.exists());
        verify(user).sendMessage("general.success");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintClipboardManager#save(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testSaveBadChars() throws IOException {
        // Load a blueprint, then save it
        blueprintFolder.mkdirs();
        // Make a blueprint file
        File configFile = new File(blueprintFolder, BLUEPRINT);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Files.write(configFile.toPath(), bytes, StandardOpenOption.CREATE);
        // Zip it
        zip(configFile);
        BlueprintClipboardManager bcm = new BlueprintClipboardManager(plugin, blueprintFolder);
        bcm.load(BLUEPRINT);
        User user = mock(User.class);
        assertTrue(bcm.save(user, "test.1234/../../film"));
        File bp = new File(blueprintFolder, "test1234film.blu");
        assertTrue(bp.exists());
        verify(user).sendMessage("general.success");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintClipboardManager#save(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testSaveForeignChars() throws IOException {
        // Load a blueprint, then save it
        blueprintFolder.mkdirs();
        // Make a blueprint file
        File configFile = new File(blueprintFolder, BLUEPRINT);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Files.write(configFile.toPath(), bytes, StandardOpenOption.CREATE);
        // Zip it
        zip(configFile);
        BlueprintClipboardManager bcm = new BlueprintClipboardManager(plugin, blueprintFolder);
        bcm.load(BLUEPRINT);
        User user = mock(User.class);
        assertTrue(bcm.save(user, "日本語の言葉"));
        File bp = new File(blueprintFolder, "日本語の言葉.blu");
        assertTrue(bp.exists());
        verify(user).sendMessage("general.success");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintClipboardManager#save(world.bentobox.bentobox.api.user.User, java.lang.String)}.
     * @throws IOException
     */
    @Test
    public void testSaveForeignBadChars() throws IOException {
        // Load a blueprint, then save it
        blueprintFolder.mkdirs();
        // Make a blueprint file
        File configFile = new File(blueprintFolder, BLUEPRINT);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Files.write(configFile.toPath(), bytes, StandardOpenOption.CREATE);
        // Zip it
        zip(configFile);
        BlueprintClipboardManager bcm = new BlueprintClipboardManager(plugin, blueprintFolder);
        bcm.load(BLUEPRINT);
        User user = mock(User.class);
        assertTrue(bcm.save(user, "日本語の言葉/../../../config"));
        File bp = new File(blueprintFolder, "日本語の言葉config.blu");
        assertTrue(bp.exists());
        verify(user).sendMessage("general.success");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintClipboardManager#saveBlueprint(world.bentobox.bentobox.blueprints.Blueprint)}.
     */
    @Test
    public void testSaveBlueprintNoName() {
        BlueprintClipboardManager bcm = new BlueprintClipboardManager(plugin, blueprintFolder);
        Blueprint blueprint = mock(Blueprint.class);
        when(blueprint.getName()).thenReturn("");
        assertFalse(bcm.saveBlueprint(blueprint));
        verify(plugin).logError("Blueprint name was empty - could not save it");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.BlueprintClipboardManager#saveBlueprint(world.bentobox.bentobox.blueprints.Blueprint)}.
     */
    @Test
    public void testSaveBlueprintSuccess() {
        BlueprintClipboardManager bcm = new BlueprintClipboardManager(plugin, blueprintFolder);
        Blueprint blueprint = new Blueprint();
        blueprint.setName("test123");
        assertTrue(bcm.saveBlueprint(blueprint));
        File bp = new File(blueprintFolder, "test123.blu");
        assertTrue(bp.exists());
    }

}
