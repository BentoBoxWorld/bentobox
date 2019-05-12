package world.bentobox.bentobox.api.commands.admin.schem;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.BlueprintsManager;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AdminSchemListCommand extends CompositeCommand {

    public AdminSchemListCommand(AdminSchemCommand parent) {
        super(parent, "list");
    }


    @Override
    public void setup() {
        setDescription("commands.admin.schem.list.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (!args.isEmpty()) {
            showHelp(this, user);
            return false;
        }
        return true;
    }


    @Override
    public boolean execute(User user, String label, List<String> args) {
        File schems = new File(getAddon().getDataFolder(), BlueprintsManager.FOLDER_NAME);
        if (!schems.exists()) {
            user.sendMessage("commands.admin.schem.list.no-schems");
            return false;
        }
        FilenameFilter schemFilter = (File dir, String name) -> name.toLowerCase(java.util.Locale.ENGLISH).endsWith(BlueprintsManager.BLUEPRINT_SUFFIX);
        List<String> schemList = Arrays.stream(Objects.requireNonNull(schems.list(schemFilter))).map(name -> name.substring(0, name.length() - BlueprintsManager.BLUEPRINT_SUFFIX.length())).collect(Collectors.toList());
        if (schemList.isEmpty()) {
            user.sendMessage("commands.admin.schem.list.no-schems");
            return false;
        }
        user.sendMessage("commands.admin.schem.list.available-schems");
        schemList.forEach(user::sendRawMessage);
        return true;
    }

}
