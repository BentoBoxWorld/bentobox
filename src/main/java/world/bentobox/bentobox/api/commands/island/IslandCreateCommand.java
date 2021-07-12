package world.bentobox.bentobox.api.commands.island;

import java.io.IOException;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.island.NewIsland;
import world.bentobox.bentobox.managers.island.PregenNewIslandLocationStrategy;
import world.bentobox.bentobox.panels.IslandCreationPanel;

/**
 * /island create - Create an island.
 *
 * @author tastybento
 */
public class IslandCreateCommand extends CompositeCommand {

    /**
     * Command to create an island
     * @param islandCommand - parent command
     */
    public IslandCreateCommand(CompositeCommand islandCommand) {
        super(islandCommand, "create", "new");
    }

    @Override
    public void setup() {
        setPermission("island.create");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.create.parameters");
        setDescription("commands.island.create.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // Check if the island is reserved
        @Nullable
        Island island = getIslands().getIsland(getWorld(), user);
        if (island != null) {
            // Reserved islands can be made
            if (island.isReserved()) {
                return true;
            }
            // You cannot make an island
            user.sendMessage("general.errors.already-have-island");
            return false;
        }
        if (getIWM().getMaxIslands(getWorld()) > 0
                && getIslands().getIslandCount(getWorld()) >= getIWM().getMaxIslands(getWorld())) {
            // There is too many islands in the world :(
            user.sendMessage("commands.island.create.too-many-islands");
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Permission check if the name is not the default one
        if (!args.isEmpty()) {
            String name = getPlugin().getBlueprintsManager().validate(getAddon(), args.get(0).toLowerCase(java.util.Locale.ENGLISH));
            if (name == null) {
                // The blueprint name is not valid.
                user.sendMessage("commands.island.create.unknown-blueprint");
                return false;
            }
            if (!getPlugin().getBlueprintsManager().checkPerm(getAddon(), user, args.get(0))) {
                return false;
            }
            // Make island
            return makeIsland(user, name);
        } else {
            // Show panel only if there are multiple bundles available
            if (getPlugin().getBlueprintsManager().getBlueprintBundles(getAddon()).size() > 1) {
                // Show panel
                IslandCreationPanel.openPanel(this, user, label);
                return true;
            }
            return makeIsland(user, BlueprintsManager.DEFAULT_BUNDLE_NAME);
        }
    }

    private boolean makeIsland(User user, String name) {
        user.sendMessage("commands.island.create.creating-island");
        try {
            NewIsland.builder()
            .player(user)
            .addon(getAddon())
            .reason(Reason.CREATE)
            .name(name)
            .locationStrategy(new PregenNewIslandLocationStrategy())
            .build();
        } catch (IOException e) {
            getPlugin().logError("Could not create island for player. " + e.getMessage());
            user.sendMessage(e.getMessage());
            return false;
        }
        if (getSettings().isResetCooldownOnCreate()) {
            getParent().getSubCommand("reset").ifPresent(resetCommand -> resetCommand.setCooldown(user.getUniqueId(), getSettings().getResetCooldown()));
        }
        return true;
    }
}
