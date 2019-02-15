package world.bentobox.bentobox.api.commands.admin;

import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

public class AdminTrashCommand extends CompositeCommand {

    public AdminTrashCommand(CompositeCommand parent) {
        super(parent, "trash");
    }

    @Override
    public void setup() {
        setPermission("admin.info.trash");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.info.trash.parameters");
        setDescription("commands.admin.info.trash.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() > 1) {
            // Show help
            showHelp(this, user);
            return false;
        }
        // Get target player
        UUID targetUUID = args.isEmpty() ? null : getPlayers().getUUID(args.get(0));
        if (!args.isEmpty() && targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        // Show trash can info for this player
        List<Island> islands = getIslands().getQuarantinedIslandByUser(getWorld(), targetUUID);
        if (islands.isEmpty()) {
            if (args.isEmpty()) {
                user.sendMessage("commands.admin.info.trash.no-unowned-in-trash");
            } else {
                user.sendMessage("general.errors.player-has-no-island");
            }
            return false;
        } else {
            user.sendMessage("commands.admin.info.trash.title");
            int count = 1;
            islands.forEach(i -> {
                user.sendMessage("commands.admin.info.trash.count", TextVariables.NUMBER, String.valueOf(count));
                i.showInfo(user);
            });
            user.sendMessage("commands.admin.info.trash.use-switch", TextVariables.LABEL, getTopLabel());
            user.sendMessage("commands.admin.info.trash.use-emptytrash", TextVariables.LABEL, getTopLabel());
            return true;
        }
    }
}
