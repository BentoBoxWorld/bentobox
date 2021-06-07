package world.bentobox.bentobox.listeners.flags.worldsettings;

import java.util.Map;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.projectiles.ProjectileSource;

import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Provide geo limiting to mobs - removed them if they go outside island bounds
 * @author tastybento
 *
 */
public class GeoLimitMobsListener extends FlagListener {

    private Map<Entity, Island> mobSpawnTracker = new WeakHashMap<>();

    /**
     * Start the tracker when the plugin is loaded
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPluginReady(BentoBoxReadyEvent event) {
        // Kick off the task to remove entities that go outside island boundaries
        Bukkit.getScheduler().runTaskTimer(getPlugin(), () -> {
            mobSpawnTracker.entrySet().stream()
            .filter(e -> !e.getValue().onIsland(e.getKey().getLocation()))
            .map(Map.Entry::getKey)
            .forEach(Entity::remove);
            mobSpawnTracker.keySet().removeIf(e -> e == null || e.isDead());
        }, 20L, 20L);
    }

    /**
     * Track where the mob was created. This will determine its allowable movement zone.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMobSpawn(EntitySpawnEvent e) {
        if (getIWM().inWorld(e.getLocation())
                && getIWM().getGeoLimitSettings(e.getLocation().getWorld()).contains(e.getEntityType().name())) {
            getIslands().getIslandAt(e.getLocation()).ifPresent(i -> mobSpawnTracker.put(e.getEntity(), i));
        }
    }

    /**
     * Track where a vehicle was created. This will determine its allowable movement zone.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVehicleSpawn(VehicleCreateEvent e) {
        Entity entity = e.getVehicle();
        if (getIWM().inWorld(entity.getLocation())
                && getIWM().getGeoLimitSettings(entity.getLocation().getWorld()).contains(entity.getType().name())) {
            getIslands().getIslandAt(entity.getLocation()).ifPresent(i -> mobSpawnTracker.put(entity, i));
        }
    }

    /**
     * Clean up the map when entity dies (does not handle entity removal)
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMobDeath(final EntityDeathEvent e) {
        mobSpawnTracker.remove(e.getEntity());
    }

    /**
     * Deal with projectiles fired by entities
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onProjectileExplode(final ExplosionPrimeEvent e) {
        if (e.getEntity() instanceof Projectile && getIWM().inWorld(e.getEntity().getLocation())) {
            ProjectileSource source = ((Projectile)e.getEntity()).getShooter();
            if (source instanceof Entity) {
                Entity shooter = (Entity)source;
                if (mobSpawnTracker.containsKey(shooter)
                        && !mobSpawnTracker.get(shooter).onIsland(e.getEntity().getLocation())) {
                    e.getEntity().remove();
                    e.setCancelled(true);
                }
            }
        }
    }
}
