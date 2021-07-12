package world.bentobox.bentobox.managers.island;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.util.Util;

/**
 * Provides island locations and pregenerates future chunks to speed up island teleportation
 * @author tastybento
 * @since 1.17.0
 *
 */
public class PregenNewIslandLocationStrategy implements NewIslandLocationStrategy {

    private static final int VD = 16 * Bukkit.getServer().getViewDistance();
    protected BentoBox plugin = BentoBox.getInstance();
    protected Queue<Location> queue = new LinkedList<>();
    private int i;

    @Override
    public Location getNextLocation(World world) {
        Location last = plugin.getIslands().getLast(world);
        if (last == null) {
            last = new Location(world,
                    (double) plugin.getIWM().getIslandXOffset(world) + plugin.getIWM().getIslandStartX(world),
                    plugin.getIWM().getIslandHeight(world),
                    (double) plugin.getIWM().getIslandZOffset(world) + plugin.getIWM().getIslandStartZ(world));
        }
        // Find a free spot
        try {
            while (isIsland(last)) {
                nextGridLocation(last);
            }
        } catch (IOException e) {
            // We could not find a free spot within the limit required. It's likely this
            // world is not empty
            plugin.logError("Could not find a free spot for islands! Is this world empty?");
            return null;
        }
        // Save the last spot
        plugin.getIslands().setLast(last);
        // Load chunks if Paper
        if (Util.isPaper()) {
            preloadChunks(last.clone());
        }
        return last;
    }

    private void preloadChunks(Location loc) {
        // Make a queue to load
        for (int i = 0; i < 5; i++) {
            nextGridLocation(loc);
            if (!Util.isChunkGenerated(loc)) {
                for (int x = loc.getBlockX() - VD; x <= loc.getBlockX() + VD; x += 16) {
                    for (int z = loc.getBlockZ() - VD; z <= loc.getBlockZ() + VD; z += 16) {
                        queue.add(new Location(loc.getWorld(), x, loc.getBlockY(), z));
                    }  
                }
                queue.add(loc.clone());
            }
        }
        // Run it 10 seconds later
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.log("Chunk preloading started. " + queue.size() + " chunks to preload.");
            asyncLoadChunks();
            }, 200L);
        
    }



    private void asyncLoadChunks() {
        if (!queue.isEmpty()) {
            Location l = queue.poll();
            if (!Util.isChunkGenerated(l)) {
                Util.getChunkAtAsync(l).thenRun(() -> asyncLoadChunks());
            } else {
                asyncLoadChunks();
            }
        } else {
            plugin.log("Chunk preloading complete.");
        }
    }

    /**
     * Checks if there is an island or blocks at this location
     *
     * @param location - the location
     * @return true if island present or if in deletion
     * @throws IOException 
     */
    private boolean isIsland(Location location) throws IOException {
        if (plugin.getIslands().getIslandAt(location).isPresent() || plugin.getIslandDeletionManager().inDeletion(location)) {
            return true;
        }
        // Block check
        World world = Util.getWorld(location.getWorld());
        if (plugin.getIWM().isCheckForBlocks(world) 
                && !plugin.getIWM().isUseOwnGenerator(world) 
                && Arrays.asList(BlockFace.values()).stream().anyMatch(bf -> !location.getBlock().getRelative(bf).isEmpty() 
                        && !location.getBlock().getRelative(bf).getType().equals(Material.WATER))) {
            // Block found
            i++;
            if (i > 1000) {
                throw new IOException("Could not find a free spot of island. Is the generator broken?");
            }
            plugin.getIslands().createIsland(location);
            return true;
        }
        return false;
    }

    /**
     * Finds the next free island spot based off the last known island.
     * Uses island_distance setting from the config file. Builds up in a grid fashion.
     *
     * @param lastIsland - last island location
     */
    private void nextGridLocation(final Location lastIsland) {
        int x = lastIsland.getBlockX();
        int z = lastIsland.getBlockZ();
        int d = plugin.getIWM().getIslandDistance(lastIsland.getWorld()) * 2;
        if (x < z) {
            if (-1 * x < z) {
                lastIsland.setX(lastIsland.getX() + d);
                return;
            }
            lastIsland.setZ(lastIsland.getZ() + d);
            return;
        }
        if (x > z) {
            if (-1 * x >= z) {
                lastIsland.setX(lastIsland.getX() - d);
                return;
            }
            lastIsland.setZ(lastIsland.getZ() - d);
            return;
        }
        if (x <= 0) {
            lastIsland.setZ(lastIsland.getZ() + d);
            return;
        }
        lastIsland.setZ(lastIsland.getZ() - d);
        return;
    }
}
