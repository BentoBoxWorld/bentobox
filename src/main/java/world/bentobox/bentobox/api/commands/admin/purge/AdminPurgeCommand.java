package world.bentobox.bentobox.api.commands.admin.purge;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent.IslandDeletedEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

public class AdminPurgeCommand extends CompositeCommand implements Listener {

    private int count;
    private boolean inPurge;
    private boolean toBeConfirmed;
    private Iterator<String> it;
    private User user;
    private Set<String> islands = new HashSet<>();

    public AdminPurgeCommand(CompositeCommand parent) {
        super(parent, "purge");
        getAddon().registerListener(this);
    }

    @Override
    public void setup() {
        setPermission("admin.purge");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.purge.parameters");
        setDescription("commands.admin.purge.description");
        new AdminPurgeStopCommand(this);
        new AdminPurgeUnownedCommand(this);
        new AdminPurgeProtectCommand(this);
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (inPurge) {
            user.sendMessage("commands.admin.purge.purge-in-progress", TextVariables.LABEL, getTopLabel());
            return false;
        }
        if (args.isEmpty()) {
            // Show help
            showHelp(this, user);
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.get(0).equalsIgnoreCase("confirm") && toBeConfirmed && this.user.equals(user)) {
            removeIslands();
            return true;
        }
        // Clear tbc
        toBeConfirmed = false;
        islands.clear();
        this.user = user;
        try {
            Integer days = Integer.parseInt(args.get(0));
            if (days < 1) {
                user.sendMessage("commands.admin.purge.days-one-or-more");
                return false;
            }
            islands = getOldIslands(days);
            user.sendMessage("commands.admin.purge.purgable-islands", TextVariables.NUMBER, String.valueOf(islands.size()));
            if (!islands.isEmpty()) {
                toBeConfirmed = true;
                user.sendMessage("commands.admin.purge.confirm", TextVariables.LABEL, this.getTopLabel());
                return false;
            }
        } catch(Exception e) {
            user.sendMessage("commands.admin.purge.number-error");
            return false;
        }
        return true;
    }

    void removeIslands() {
        inPurge = true;
        user.sendMessage("commands.admin.purge.see-console-for-status");
        it = islands.iterator();
        count = 0;
        // Delete first island
        deleteIsland();
    }

    private void deleteIsland() {
        if (it.hasNext()) {
            getIslands().getIslandById(it.next()).ifPresent(i -> {
                getIslands().deleteIsland(i, true, null);
                count++;
                getPlugin().log(count + " islands purged");
            });
        } else {
            user.sendMessage("commands.admin.purge.completed");
            inPurge = false;
        }

    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    void onIslandDeleted(IslandDeletedEvent e) {
        if (inPurge) {
            deleteIsland();
        }
    }

    Set<String> getOldIslands(int days) {
        return getPlugin().getIslands().getIslands().stream()
                .filter(i -> !i.getPurgeProtected())
                .filter(i -> i.getWorld().equals(this.getWorld()))
                .filter(i -> i.getOwner() != null)
                .filter(i -> i.getMembers().size() == 1)
                .filter(i -> (System.currentTimeMillis() - Bukkit.getOfflinePlayer(i.getOwner()).getLastPlayed()) > days * 1000 * 24 * 3600)
                .map(Island::getUniqueId)
                .collect(Collectors.toSet());
    }

    /**
     * @return the inPurge
     */
    boolean isInPurge() {
        return inPurge;
    }

    /**
     * Stop the purge
     */
    void stop() {
        inPurge = false;
    }

    /**
     * @param user the user to set
     */
    void setUser(User user) {
        this.user = user;
    }

    /**
     * @param islands the islands to set
     */
    void setIslands(Set<String> islands) {
        this.islands = islands;
    }
}
