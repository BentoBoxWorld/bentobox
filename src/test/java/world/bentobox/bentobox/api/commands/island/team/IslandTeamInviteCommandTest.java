/**
 *
 */
package world.bentobox.bentobox.api.commands.island.team;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class IslandTeamInviteCommandTest {

    private CompositeCommand ic;
    private UUID uuid;
    private User user;
    private IslandsManager im;
    private PlayersManager pm;
    private UUID notUUID;
    @Mock
    private Settings s;
    private Island island;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while(notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        User.setPlugin(plugin);

        // Parent command has no aliases
        ic = mock(CompositeCommand.class);
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Player has island to begin with
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.inTeam(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.any())).thenReturn(true);
        when(im.getOwner(Mockito.any(), Mockito.any())).thenReturn(uuid);
        island = mock(Island.class);
        when(island.getRank(Mockito.any())).thenReturn(RanksManager.OWNER_RANK);
        when(im.getIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(island);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);

        // Player Manager
        pm = mock(PlayersManager.class);

        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        // IWM friendly name
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getFriendlyName(Mockito.any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testExecuteNoIsland() {
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(false);
        when(im.inTeam(Mockito.any(), Mockito.any(UUID.class))).thenReturn(false);
        IslandTeamInviteCommand itl = new IslandTeamInviteCommand(ic);
        assertFalse(itl.canExecute(user, itl.getLabel(), new ArrayList<>()));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.no-island"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testExecuteLowRank() {
        when(island.getRank(Mockito.any())).thenReturn(RanksManager.MEMBER_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.OWNER_RANK);
        IslandTeamInviteCommand itl = new IslandTeamInviteCommand(ic);
        assertFalse(itl.canExecute(user, itl.getLabel(), new ArrayList<>()));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.no-permission"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testExecuteNoTarget() {
        IslandTeamInviteCommand itl = new IslandTeamInviteCommand(ic);
        assertFalse(itl.canExecute(user, itl.getLabel(), new ArrayList<>()));
        // Show help
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#execute(User, String, java.util.List)}.
     */
    @Test
    public void testExecuteUnknownPlayer() {
        IslandTeamInviteCommand itl = new IslandTeamInviteCommand(ic);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(null);
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage("general.errors.unknown-player", "[name]", name[0]);
    }


    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#execute(User, String, java.util.List)}.
     */
    @Test
    public void testExecuteOfflinePlayer() {
        PowerMockito.mockStatic(User.class);
        when(User.getInstance(Mockito.any(UUID.class))).thenReturn(user);
        when(user.isOnline()).thenReturn(false);
        IslandTeamInviteCommand itl = new IslandTeamInviteCommand(ic);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(uuid);
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.offline-player"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#execute(User, String, java.util.List)}.
     */
    @Test
    public void testExecuteSamePlayer() {
        PowerMockito.mockStatic(User.class);
        when(User.getInstance(Mockito.any(UUID.class))).thenReturn(user);
        when(user.isOnline()).thenReturn(true);
        IslandTeamInviteCommand itl = new IslandTeamInviteCommand(ic);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(uuid);
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage(Mockito.eq("commands.island.team.invite.errors.cannot-invite-self"));
    }


    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#execute(User, String, java.util.List)}.
     */
    @Test
    public void testExecuteDifferentPlayerInTeam() {
        PowerMockito.mockStatic(User.class);
        when(User.getInstance(Mockito.any(UUID.class))).thenReturn(user);
        when(user.isOnline()).thenReturn(true);
        IslandTeamInviteCommand itl = new IslandTeamInviteCommand(ic);
        String[] name = {"tastybento"};
        when(pm.getUUID(Mockito.any())).thenReturn(notUUID);
        when(im.inTeam(Mockito.any(), Mockito.any())).thenReturn(true);
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        Mockito.verify(user).sendMessage(Mockito.eq("commands.island.team.invite.errors.already-on-team"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#execute(User, String, java.util.List)}.
     */
    @Test
    public void testExecuteCoolDownActive() {
        // 10 minutes = 600 seconds
        when(s.getInviteCooldown()).thenReturn(10);
        IslandTeamInviteCommand itl = new IslandTeamInviteCommand(ic);
        String[] name = {"tastybento"};
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));

    }

}
