package world.bentobox.bentobox.panels.settings;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.inventory.ClickType;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;
import world.bentobox.bentobox.api.panels.Tab;
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;

/**
 * Implements a {@link Tab} that shows settings for
 * {@link world.bentobox.bentobox.api.flags.Flag.Type#PROTECTION}, {@link world.bentobox.bentobox.api.flags.Flag.Type#SETTING}, {@link world.bentobox.bentobox.api.flags.Flag.Type#WORLD_SETTING}
 * @author tastybento
 * @since 1.6.0
 *
 */
public class SettingsTab implements Tab, ClickHandler {

    protected static final String PROTECTION_PANEL = "protection.panel.";
    protected BentoBox plugin = BentoBox.getInstance();
    protected Flag.Type type;
    protected User user;
    protected World world;
    protected Island island;
    protected Flag.Mode mode = Flag.Mode.BASIC;

    /**
     * Show a tab of settings
     * @param world - world
     * @param user - user who is viewing the tab
     * @param island - the island
     * @param type - flag type
     */
    public SettingsTab(World world, User user, Island island, Type type) {
        this.world = world;
        this.user = user;
        this.island = island;
        this.type = type;
    }

    /**
     * Show a tab of settings
     * @param world - world
     * @param user - user who is viewing the tab
     * @param type - flag type
     */
    public SettingsTab(World world, User user, Type type) {
        this.world = world;
        this.user = user;
        this.type = type;
    }

    /**
     * @return list of flags that will be shown in this panel
     */
    protected List<Flag> getFlags() {
        // Get a list of flags of the correct type and sort by the translated names
        List<Flag> flags = plugin.getFlagsManager().getFlags().stream().filter(f -> f.getType().equals(type))
                .sorted(Comparator.comparing(flag -> user.getTranslation(flag.getNameReference())))
                .collect(Collectors.toList());
        // Remove any that are not for this game mode
        plugin.getIWM().getAddon(world).ifPresent(gm -> flags.removeIf(f -> !f.getGameModes().isEmpty() && !f.getGameModes().contains(gm)));
        // Remove any that are the wrong rank or that will be on the top row
        plugin.getIWM().getAddon(world).ifPresent(gm -> flags.removeIf(f -> f.getMode().isGreaterThan(mode) ||
                f.getMode().equals(Flag.Mode.TOP_ROW)));
        return flags;
    }

    /**
     * Get the icon for this tab
     * @return panel item
     */
    @Override
    public PanelItem getIcon() {
        PanelItemBuilder pib = new PanelItemBuilder();
        // Set the icon
        pib.icon(type.getIcon());
        pib.name(getName());
        pib.description(user.getTranslation(PROTECTION_PANEL + type.toString() + ".description"));
        return pib.build();
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.panels.Tab#getName()
     */
    @Override
    public String getName() {
        return user.getTranslation(PROTECTION_PANEL + type.toString() + ".title", "[world_name]", plugin.getIWM().getFriendlyName(world));
    }

    /**
     * Get all the flags as panel items
     * @return list of all the panel items for this flag type
     */
    @Override
    public List<PanelItem> getPanelItems() {
        List<Flag> flags = getFlags();
        int i = 0;
        // Jump past empty tabs
        while (flags.isEmpty() && i++ < Flag.Mode.values().length) {
            mode = mode.getNextFlag();
            flags = getFlags();
        }
        return flags.stream().map((f -> f.toPanelItem(plugin, user, island, plugin.getIWM().getHiddenFlags(world).contains(f.getID())))).collect(Collectors.toList());
    }

    @Override
    public Map<Integer, PanelItem> getTabIcons() {
        Map<Integer, PanelItem> icons = new HashMap<>();
        // Add the lock icon - we want it to be displayed no matter the tab
        if (island != null) {
            icons.put(5, Flags.LOCK.toPanelItem(plugin, user, island, false));
        }
        // Add the mode icon
        switch(mode) {
        case ADVANCED:
            icons.put(7, new PanelItemBuilder().icon(Material.GOLD_INGOT)
                    .name(user.getTranslation(PROTECTION_PANEL + "advanced"))
                    .clickHandler(this)
                    .build());
            break;
        case EXPERT:
            icons.put(7, new PanelItemBuilder().icon(Material.NETHER_BRICK)
                    .name(user.getTranslation(PROTECTION_PANEL + "expert"))
                    .clickHandler(this)
                    .build());
            break;
        default:
            icons.put(7, new PanelItemBuilder().icon(Material.IRON_INGOT)
                    .name(user.getTranslation(PROTECTION_PANEL + "basic"))
                    .clickHandler(this)
                    .build());
        }
        return icons;
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.panels.Tab#getPermission()
     */
    @Override
    public String getPermission() {
        // All of these tabs can be seen by anyone
        return "";
    }

    /**
     * @return the type
     */
    public Flag.Type getType() {
        return type;
    }

    /**
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * @return the world
     */
    public World getWorld() {
        return world;
    }

    /**
     * @return the island
     */
    public Island getIsland() {
        return island;
    }

    @Override
    public boolean onClick(Panel panel, User user, ClickType clickType, int slot) {
        // Cycle the mode
        mode = mode.getNextFlag();
        if (panel instanceof TabbedPanel) {
            TabbedPanel tp = ((TabbedPanel)panel);
            tp.setActivePage(0);
            tp.refreshPanel();
        }
        return true;
    }

}
