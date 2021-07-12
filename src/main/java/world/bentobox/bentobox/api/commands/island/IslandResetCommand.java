package world.bentobox.bentobox.api.commands.island;

import java.io.IOException;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.island.NewIsland;
import world.bentobox.bentobox.managers.island.NewIsland.Builder;
import world.bentobox.bentobox.managers.island.PregenNewIslandLocationStrategy;
import world.bentobox.bentobox.panels.IslandCreationPanel;

/**
 * @author tastybento
 */
public class IslandResetCommand extends ConfirmableCommand {

    private boolean noPaste;

    public IslandResetCommand(CompositeCommand islandCommand) {
        super(islandCommand, "reset", "restart");
    }

    /**
     * Creates the island reset command
     * @param islandCommand - parent command
     * @param noPaste - true if resetting should not paste a new island
     */
    public IslandResetCommand(CompositeCommand islandCommand, boolean noPaste) {
        super(islandCommand, "reset", "restart");
        this.noPaste = noPaste;
    }

    @Override
    public void setup() {
        setPermission("island.reset");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.reset.parameters");
        setDescription("commands.island.reset.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // Check cooldown
        if (getSettings().getResetCooldown() > 0 && checkCooldown(user)) {
            return false;
        }
        if (!getIslands().hasIsland(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        int resetsLeft = getPlayers().getResetsLeft(getWorld(), user.getUniqueId());
        if (resetsLeft != -1) {
            // Resets are not unlimited here
            if (resetsLeft == 0) {
                // No resets allowed
                user.sendMessage("commands.island.reset.none-left");
                return false;
            } else {
                // Still some resets left
                // Notify how many resets are left
                user.sendMessage("commands.island.reset.resets-left", TextVariables.NUMBER, String.valueOf(resetsLeft));
            }
        }

        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Permission check if the name is not the default one
        if (!args.isEmpty()) {
            String name = getPlugin().getBlueprintsManager().validate(getAddon(), args.get(0).toLowerCase(java.util.Locale.ENGLISH));
            if (name == null || name.isEmpty()) {
                // The blueprint name is not valid.
                user.sendMessage("commands.island.create.unknown-blueprint");
                return false;
            }
            if (!getPlugin().getBlueprintsManager().checkPerm(getAddon(), user, args.get(0))) {
                return false;
            }
            return resetIsland(user, name);
        } else {
            // Show panel after confirmation
            if (getPlugin().getSettings().isResetConfirmation()) {
                this.askConfirmation(user, user.getTranslation("commands.island.reset.confirmation"), () -> selectBundle(user, label));
            } else {
                selectBundle(user, label);
            }
            return true;
        }
    }

    /**
     * Either selects the bundle to use or asks the user to choose.
     * @since 1.5.1
     */
    private void selectBundle(@NonNull User user, @NonNull String label) {
        // Show panel only if there are multiple bundles available
        if (getPlugin().getBlueprintsManager().getBlueprintBundles(getAddon()).size() > 1) {
            // Show panel - once the player selected a bundle, this will re-run this command
            IslandCreationPanel.openPanel(this, user, label);
        } else {
            resetIsland(user, BlueprintsManager.DEFAULT_BUNDLE_NAME);
        }
    }

    private boolean resetIsland(User user, String name) {
        // Get the player's old island
        Island oldIsland = getIslands().getIsland(getWorld(), user);

        // Fire island preclear event
        IslandEvent.builder()
        .involvedPlayer(user.getUniqueId())
        .reason(Reason.PRECLEAR)
        .island(oldIsland)
        .oldIsland(oldIsland)
        .location(oldIsland.getCenter())
        .build();

        // Reset the island
        user.sendMessage("commands.island.create.creating-island");

        // Kick all island members (including the owner)
        kickMembers(oldIsland);

        // Add a reset
        getPlayers().addReset(getWorld(), user.getUniqueId());

        // Reset the homes of the player
        getPlayers().clearHomeLocations(getWorld(), user.getUniqueId());

        // Create new island and then delete the old one
        try {
            Builder builder = NewIsland.builder()
                    .player(user)
                    .reason(Reason.RESET)
                    .addon(getAddon())
                    .oldIsland(oldIsland)
                    .locationStrategy(new PregenNewIslandLocationStrategy())
                    .name(name);
            if (noPaste) builder.noPaste();
            builder.build();
        } catch (IOException e) {
            getPlugin().logError("Could not create island for player. " + e.getMessage());
            user.sendMessage(e.getMessage());
            return false;
        }
        setCooldown(user.getUniqueId(), getSettings().getResetCooldown());
        return true;
    }

    /**
     * Kicks the members (incl. owner) of the island.
     * @since 1.7.0
     */
    private void kickMembers(Island island) {
        /*
         * We cannot assume the island owner can run /[cmd] team kick (it might be disabled, or there could be permission restrictions...)
         * Therefore, we need to do it manually.
         * Plus, a more specific team event (TeamDeleteEvent) is called by this method.
         */
        island.getMemberSet().forEach(memberUUID -> {

            User member = User.getInstance(memberUUID);
            // Send a "you're kicked" message if the member is not the island owner (send before removing!)
            if (!memberUUID.equals(island.getOwner())) {
                member.sendMessage("commands.island.reset.kicked-from-island", TextVariables.GAMEMODE, getAddon().getDescription().getName());
            }
            // Remove player
            getIslands().removePlayer(getWorld(), memberUUID);

            // Clean player
            getPlayers().cleanLeavingPlayer(getWorld(), member, false);

            // Fire event
            TeamEvent.builder()
            .island(island)
            .reason(TeamEvent.Reason.DELETE)
            .involvedPlayer(memberUUID)
            .build();
        });
    }
}
