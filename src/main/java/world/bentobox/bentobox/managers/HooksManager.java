package world.bentobox.bentobox.managers;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.hooks.Hook;

import java.util.*;

/**
 * @author Poslovitch
 */
public class HooksManager {

    private final BentoBox plugin;
    /**
     * List of successfully registered hooks.
     */
    private final Map<Class<? extends Hook>, Hook> types;
    private final Map<String, Hook> names;

    public HooksManager(BentoBox plugin) {
        this.plugin = plugin;
        this.types = new HashMap<>();
        this.names = new HashMap<>();
    }

    private void internalAddHook(Hook hook) {
        types.put(hook.getClass(), hook);
        names.put(hook.getPluginName(), hook);
    }

    private boolean alreadyHooked(Hook hook) {
        return types.containsKey(hook.getClass())
            || names.containsKey(hook.getPluginName());
    }

    public void registerHook(@NonNull Hook hook) {
        if (hook.isPluginAvailable()) {
            plugin.log("Hooking with " + hook.getPluginName() + "...");
            if (alreadyHooked(hook)) {
                plugin.logError("The hook (" + hook.getPluginName() + ") already exist.");
                return;
            }
            if (hook.hook()) {
                internalAddHook(hook);
            } else {
                plugin.logError("Could not hook with " + hook.getPluginName() + ((hook.getFailureCause() != null) ? " because: " + hook.getFailureCause() : "") + ". Skipping...");
            }
        }
        // Do not tell the user if we couldn't hook with a plugin which is not available.
        // We may have in the near future almost ~25 hooks, which would basically spam the console and make users nervous.
    }

    public boolean isHooked(@NonNull String pluginName) {
        return names.containsKey(pluginName);
    }

    public boolean isHooked(@NonNull Class<? extends Hook> hookType) {
        return types.containsKey(hookType);
    }

    /**
     * Returns the list of successfully registered hooks.
     * @return list of successfully registered hooks.
     */
    @NonNull
    public List<Hook> getHooks() {
        return new ArrayList<>(types.values());
    }

    public Optional<Hook> getHook(String pluginName) {
        return Optional.ofNullable(names.get(pluginName));
    }

    @Nullable
    public <T extends Hook> T getHook(Class<T> hookType) {
        return hookType.cast(types.get(hookType));
    }

}
