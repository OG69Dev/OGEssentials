package dev.og69.ogessentials;

import dev.og69.ogessentials.hooks.CoreProtectHook;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

/**
 * Main plugin class for OG-Essentials.
 * 
 * This plugin provides essential server features including:
 * - TPA system
 * - Homes system
 * - AFK detection and half-sleep
 * - 3x3 Pickaxe
 * - Command blocking
 * - Keep Inventory
 * - Custom enchants
 * 
 * Optional hooks are available for:
 * - PlaceholderAPI (for placeholders)
 * - CoreProtect (for block logging)
 * 
 * The plugin works fully without any hooks installed.
 */
public class OGEssentials extends JavaPlugin implements Listener {
    
    private static OGEssentials instance;
    private static boolean placeholderAPIHookLoaded = false;
    private int placeholderAPICheckTaskId = -1;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config if it doesn't exist
        saveDefaultConfig();
        
        // Register plugin enable listener to catch when PlaceholderAPI loads
        getServer().getPluginManager().registerEvents(this, this);
        
        // Initialize hooks (optional - plugin works without them)
        // Check immediately, and also schedule a delayed check for PlaceholderAPI
        initializeHooks();
        
        // Schedule a delayed check for PlaceholderAPI in case it loads after us
        schedulePlaceholderAPICheck();
        
        // Register commands
        registerCommands();
        
        // Register event listeners
        registerListeners();
        
        getLogger().info("OG-Essentials has been enabled!");
        getLogger().info("Version: " + getDescription().getVersion());
    }
    
    @Override
    public void onDisable() {
        // Cancel delayed PlaceholderAPI check task if running
        if (placeholderAPICheckTaskId != -1) {
            Bukkit.getScheduler().cancelTask(placeholderAPICheckTaskId);
            placeholderAPICheckTaskId = -1;
        }
        
        // Disable hooks
        disableHooks();
        
        getLogger().info("OG-Essentials has been disabled!");
        
        instance = null;
    }
    
    /**
     * Listen for plugin enable events to catch when PlaceholderAPI loads.
     */
    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin().getName().equals("PlaceholderAPI")) {
            // PlaceholderAPI just enabled, try to initialize the hook
            if (!placeholderAPIHookLoaded && getConfig().getBoolean("hooks.placeholderapi.enabled", true)) {
                getLogger().info("PlaceholderAPI detected! Initializing hook...");
                initializePlaceholderAPIHook();
            }
        }
    }
    
    /**
     * Schedule a delayed check for PlaceholderAPI in case it loads after us.
     */
    private void schedulePlaceholderAPICheck() {
        if (placeholderAPIHookLoaded || !getConfig().getBoolean("hooks.placeholderapi.enabled", true)) {
            return; // Already loaded or disabled
        }
        
        // Check every 5 ticks (0.25 seconds) for up to 2 seconds (40 ticks)
        final int maxAttempts = 8;
        final int[] attempts = {0};
        
        placeholderAPICheckTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            attempts[0]++;
            
            if (placeholderAPIHookLoaded) {
                // Hook loaded, cancel task
                Bukkit.getScheduler().cancelTask(placeholderAPICheckTaskId);
                placeholderAPICheckTaskId = -1;
                return;
            }
            
            if (attempts[0] >= maxAttempts) {
                // Max attempts reached, cancel task
                Bukkit.getScheduler().cancelTask(placeholderAPICheckTaskId);
                placeholderAPICheckTaskId = -1;
                return;
            }
            
            // Check if PlaceholderAPI is now available
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                initializePlaceholderAPIHook();
                if (placeholderAPIHookLoaded) {
                    Bukkit.getScheduler().cancelTask(placeholderAPICheckTaskId);
                    placeholderAPICheckTaskId = -1;
                }
            }
        }, 5L, 5L); // Start after 5 ticks, repeat every 5 ticks
    }
    
    /**
     * Initialize the PlaceholderAPI hook.
     * Extracted to a separate method so it can be called from multiple places.
     */
    private void initializePlaceholderAPIHook() {
        if (placeholderAPIHookLoaded) {
            return; // Already loaded
        }
        
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return; // Not available
        }
        
        try {
            // Use reflection to load and initialize PlaceholderAPIHook
            Class<?> hookClass = Class.forName("dev.og69.ogessentials.hooks.PlaceholderAPIHook");
            Method initMethod = hookClass.getMethod("initialize", OGEssentials.class);
            Boolean result = (Boolean) initMethod.invoke(null, this);
            if (result != null && result) {
                getLogger().info("PlaceholderAPI hook enabled!");
                placeholderAPIHookLoaded = true;
            }
        } catch (NoClassDefFoundError e) {
            // PlaceholderAPI classes not available - this is expected if PlaceholderAPI isn't installed
        } catch (Exception e) {
            // Ignore - will try again later
        }
    }
    
    /**
     * Initialize optional hooks.
     * Hooks are optional enhancements - the plugin works fully without them.
     */
    private void initializeHooks() {
        // Initialize PlaceholderAPI hook (more commonly used)
        // Try to initialize immediately if available
        if (getConfig().getBoolean("hooks.placeholderapi.enabled", true)) {
            initializePlaceholderAPIHook();
            if (!placeholderAPIHookLoaded) {
                getLogger().info("PlaceholderAPI not found yet - will check again when it loads.");
            }
        } else {
            getLogger().info("PlaceholderAPI hook disabled in config.");
        }
        
        // Initialize CoreProtect hook (rarely used)
        if (getConfig().getBoolean("hooks.coreprotect.enabled", true)) {
            if (CoreProtectHook.initialize()) {
                getLogger().info("CoreProtect hook enabled!");
            } else {
                getLogger().info("CoreProtect not found - block logging will not be available.");
            }
        } else {
            getLogger().info("CoreProtect hook disabled in config.");
        }
    }
    
    /**
     * Disable all hooks.
     */
    private void disableHooks() {
        // Disable PlaceholderAPI hook using reflection
        if (placeholderAPIHookLoaded) {
            try {
                Class<?> hookClass = Class.forName("dev.og69.ogessentials.hooks.PlaceholderAPIHook");
                Method disableMethod = hookClass.getMethod("disable");
                disableMethod.invoke(null);
            } catch (NoClassDefFoundError e) {
                // PlaceholderAPI classes not available - this is expected
            } catch (Exception e) {
                // Ignore - PlaceholderAPI might not be available
            }
        }
        
        CoreProtectHook.disable();
    }
    
    /**
     * Get the plugin instance.
     * 
     * @return The plugin instance, or null if disabled
     */
    public static OGEssentials getInstance() {
        return instance;
    }
    
    /**
     * Register all commands.
     */
    private void registerCommands() {
        // Register 3x3 pickaxe command
        org.bukkit.command.PluginCommand give3x3PickCommand = getCommand("give3x3pick");
        if (give3x3PickCommand != null) {
            dev.og69.ogessentials.commands.Give3x3PickCommand executor = new dev.og69.ogessentials.commands.Give3x3PickCommand();
            give3x3PickCommand.setExecutor(executor);
            give3x3PickCommand.setTabCompleter(executor);
        }
    }
    
    /**
     * Register all event listeners.
     */
    private void registerListeners() {
        // Register block break listener for 3x3 pickaxe
        getServer().getPluginManager().registerEvents(
            new dev.og69.ogessentials.listeners.BlockBreakListener(this),
            this
        );
    }
    
    /**
     * Reload the plugin configuration.
     */
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        
        // Reinitialize hooks if config changed
        disableHooks();
        placeholderAPIHookLoaded = false;
        initializeHooks();
    }
}
