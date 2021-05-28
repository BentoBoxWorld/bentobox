package world.bentobox.bentobox.listeners.flags.clicklisteners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.listeners.flags.clicklisteners.GeoMobLimitTab.EntityLimitTabType;
import world.bentobox.bentobox.managers.HooksManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, Util.class})
public class GeoMobLimitTabTest {

    @Mock
    private User user;
    @Mock
    private World world;
    @Mock
    private TabbedPanel panel;
    @Mock
    private BentoBox plugin;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private @NonNull Inventory inv;
    private List<String> list;
    @Mock
    private GameModeAddon gma;
    @Mock
    private HooksManager hm;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Bukkit
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getAddon(any())).thenReturn(Optional.of(gma));
        list = new ArrayList<>();
        list.add("BAT");
        list.add("COW");
        when(iwm.getMobLimitSettings(any())).thenReturn(list);
        when(iwm.getGeoLimitSettings(any())).thenReturn(list);
        // Panel
        when(panel.getInventory()).thenReturn(inv);
        // User
        when(user.getTranslation(anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        // Util
        PowerMockito.mockStatic(Util.class, Mockito.CALLS_REAL_METHODS);
        when(Util.getWorld(any())).thenReturn(world);
        // HooksManager
        when(plugin.getHooks()).thenReturn(hm);
    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.GeoMobLimitTab#onClick(world.bentobox.bentobox.api.panels.Panel, world.bentobox.bentobox.api.user.User, org.bukkit.event.inventory.ClickType, int)}.
     */
    @Test
    public void testOnClick() {
        GeoMobLimitTab tab = new GeoMobLimitTab(user, EntityLimitTabType.GEO_LIMIT, world);
        // BAT and COW in list
        assertEquals(2, list.size());
        assertEquals("COW", list.get(1));
        assertEquals("BAT", list.get(0));
        // Click on BAT
        tab.onClick(panel, user, ClickType.LEFT, 9);
        assertEquals(1, list.size());
        assertEquals("COW", list.get(0));
        // Click on BAT again to have it added
        tab.onClick(panel, user, ClickType.LEFT, 9);
        assertEquals(2, list.size());
        assertEquals("COW", list.get(0));
        assertEquals("BAT", list.get(1));
        verify(gma, times(2)).saveWorldSettings();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.GeoMobLimitTab#onClick(world.bentobox.bentobox.api.panels.Panel, world.bentobox.bentobox.api.user.User, org.bukkit.event.inventory.ClickType, int)}.
     */
    @Test
    public void testOnClickMobLimit() {
        GeoMobLimitTab tab = new GeoMobLimitTab(user, EntityLimitTabType.MOB_LIMIT, world);
        // BAT and COW in list
        assertEquals(2, list.size());
        assertEquals("COW", list.get(1));
        assertEquals("BAT", list.get(0));
        // Click on BAT
        tab.onClick(panel, user, ClickType.LEFT, 9);
        assertEquals(1, list.size());
        assertEquals("COW", list.get(0));
        // Click on BAT again to have it added
        tab.onClick(panel, user, ClickType.LEFT, 9);
        assertEquals(2, list.size());
        assertEquals("COW", list.get(0));
        assertEquals("BAT", list.get(1));
        verify(gma, times(2)).saveWorldSettings();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.GeoMobLimitTab#getIcon()}.
     */
    @Test
    public void testGetIcon() {
        GeoMobLimitTab tab = new GeoMobLimitTab(user, EntityLimitTabType.MOB_LIMIT, world);
        PanelItem icon = tab.getIcon();
        assertEquals("protection.flags.LIMIT_MOBS.name", icon.getName());
        assertEquals(Material.IRON_BOOTS, icon.getItem().getType());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.GeoMobLimitTab#getIcon()}.
     */
    @Test
    public void testGetIconGeoLimit() {
        GeoMobLimitTab tab = new GeoMobLimitTab(user, EntityLimitTabType.GEO_LIMIT, world);
        PanelItem icon = tab.getIcon();
        assertEquals("protection.flags.GEO_LIMIT_MOBS.name", icon.getName());
        assertEquals(Material.CHAINMAIL_CHESTPLATE, icon.getItem().getType());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.GeoMobLimitTab#getName()}.
     */
    @Test
    public void testGetName() {
        GeoMobLimitTab tab = new GeoMobLimitTab(user, EntityLimitTabType.MOB_LIMIT, world);
        assertEquals("protection.flags.LIMIT_MOBS.name", tab.getName());
        tab = new GeoMobLimitTab(user, EntityLimitTabType.GEO_LIMIT, world);
        assertEquals("protection.flags.GEO_LIMIT_MOBS.name", tab.getName());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.GeoMobLimitTab#getPanelItems()}.
     */
    @Test
    public void testGetPanelItemsMobLimit() {
        GeoMobLimitTab tab = new GeoMobLimitTab(user, EntityLimitTabType.MOB_LIMIT, world);
        List<@Nullable PanelItem> items = tab.getPanelItems();
        assertFalse(items.isEmpty());
        items.forEach(i -> {
            if (i.getName().equals("Cow") || i.getName().equals("Bat")) {
                assertEquals("Name : " + i.getName(), Material.RED_SHULKER_BOX, i.getItem().getType());
            } else {
                assertEquals("Name : " + i.getName(), Material.GREEN_SHULKER_BOX, i.getItem().getType());
            }
        });
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.GeoMobLimitTab#getPanelItems()}.
     */
    @Test
    public void testGetPanelItemsGeoLimit() {
        GeoMobLimitTab tab = new GeoMobLimitTab(user, EntityLimitTabType.GEO_LIMIT, world);
        List<@Nullable PanelItem> items = tab.getPanelItems();
        assertFalse(items.isEmpty());
        items.forEach(i -> {
            if (i.getName().equals("Cow") || i.getName().equals("Bat")) {
                assertEquals("Name : " + i.getName(), Material.GREEN_SHULKER_BOX, i.getItem().getType());
            } else {
                assertEquals("Name : " + i.getName(), Material.RED_SHULKER_BOX, i.getItem().getType());
            }
        });
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.GeoMobLimitTab#getPermission()}.
     */
    @Test
    public void testGetPermission() {
        GeoMobLimitTab tab = new GeoMobLimitTab(user, EntityLimitTabType.GEO_LIMIT, world);
        assertTrue(tab.getPermission().isEmpty());
    }

}
