package world.bentobox.bentobox.api.commands.admin.purge;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

public class AdminPurgeUnownedCommand extends ConfirmableCommand {

    public AdminPurgeUnownedCommand(AdminPurgeCommand parent) {
        super(parent, "unowned");
    }

    @Override
    public void setup() {
        setPermission("admin.purge");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.purge.unowned.parameters");
        setDescription("commands.admin.purge.unowned.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (!args.isEmpty()) {
            // Show help
            showHelp(this, user);
            return false;
        }
        AdminPurgeCommand parentCommand = ((AdminPurgeCommand)getParent());
        if (parentCommand.isInPurge()) {
            user.sendMessage("commands.admin.purge.purge-in-progress", TextVariables.LABEL, getTopLabel());
            return false;
        }
        Set<String> unowned = getUnownedIslands();
        user.sendMessage("commands.admin.purge.unowned.unowned-islands", TextVariables.NUMBER, String.valueOf(unowned.size()));
        if (!unowned.isEmpty()) {
            this.askConfirmation(user, () -> {
                parentCommand.setUser(user);
                parentCommand.setIslands(unowned);
                parentCommand.removeIslands();
            });
        }
        return true;
    }

    Set<String> getUnownedIslands() {
        return getPlugin().getIslands().getIslands().stream()
                .filter(i -> !i.getPurgeProtected())
                .filter(i -> i.getWorld().equals(this.getWorld()))
                .filter(i -> i.getOwner() == null)
                .map(Island::getUniqueId)
                .collect(Collectors.toSet());

    }

}