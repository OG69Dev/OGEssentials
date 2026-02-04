package dev.og69.ogessentials;

import dev.og69.ogessentials.hooks.CoreProtectHook;
import dev.og69.ogessentials.hooks.SkriptHook;
import dev.og69.ogessentials.listeners.AfkListener;
import dev.og69.ogessentials.listeners.KeepInventoryListener;
import dev.og69.ogessentials.listeners.SleepListener;
import dev.og69.ogessentials.managers.AfkManager;
import dev.og69.ogessentials.managers.HomeManager;
import dev.og69.ogessentials.managers.KeepInventoryManager;
import dev.og69.ogessentials.managers.NameTagManager;
import dev.og69.ogessentials.managers.TpaManager;
import dev.og69.ogessentials.managers.BackManager;
import dev.og69.ogessentials.managers.PermissionManager;
import dev.og69.ogessentials.storage.DatabaseManager;
import dev.og69.ogessentials.tasks.AfkCheckTask;
import dev.og69.ogessentials.tasks.TpaExpiryTask;
import dev.og69.ogessentials.update.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
    
    private UpdateChecker updateChecker;
    private int updateCheckTaskId = -1;
    private boolean isInitialStartup = true;
    
    // AFK and Half-Sleep system
    private NameTagManager nameTagManager;
    private AfkManager afkManager;
    private SleepListener sleepListener;
    private int afkCheckTaskId = -1;
    
    // Keep Inventory system
    private KeepInventoryManager keepInventoryManager;
    private KeepInventoryListener keepInventoryListener;
    
    // Database and Homes system
    private DatabaseManager databaseManager;
    private HomeManager homeManager;
    
    // TPA system
    private TpaManager tpaManager;
    private int tpaExpiryTaskId = -1;

    // Back system
    private BackManager backManager;

    // Permission system
    private PermissionManager permissionManager;
    
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
        
        // Initialize NamespacedKey for 3x3 pickaxe PersistentDataContainer
        dev.og69.ogessentials.commands.Give3x3PickCommand.initializeNamespacedKey(this);
        
        // Initialize AFK and Half-Sleep managers
        initializeAfkSystem();
        
        // Initialize Keep Inventory system
        initializeKeepInventorySystem();
        
        // Initialize Database and Homes system
        initializeHomesSystem();
        
        // Initialize TPA system
        initializeTpaSystem();

        // Initialize Back system
        backManager = new BackManager();

        // Initialize Permission system
        permissionManager = new PermissionManager(this);
        // Register commands
        registerCommands();
        
        // Register event listeners
        registerListeners();
        
        // Initialize update checker
        initializeUpdateChecker();
        
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
        
        // Cancel update check task if running
        if (updateCheckTaskId != -1) {
            Bukkit.getScheduler().cancelTask(updateCheckTaskId);
            updateCheckTaskId = -1;
        }
        
        // Clean up AFK and Half-Sleep system
        cleanupAfkSystem();
        
        // Clean up Keep Inventory system
        cleanupKeepInventorySystem();
        
        // Clean up TPA system
        cleanupTpaSystem();

        if (backManager != null) {
            backManager.cleanup();
        }

        if (permissionManager != null) {
            permissionManager.cleanup();
        }
        
        // Clean up Homes system and database
        cleanupHomesSystem();
        
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
            if (CoreProtectHook.isEnabled()) {
                // Already initialized, skip logging
            } else if (CoreProtectHook.initialize()) {
                getLogger().info("CoreProtect hook enabled!");
            } else {
                getLogger().info("CoreProtect not found - block logging will not be available.");
            }
        } else {
            getLogger().info("CoreProtect hook disabled in config.");
        }
        
        // Initialize Skript hook
        if (getConfig().getBoolean("hooks.skript.enabled", true)) {
            if (SkriptHook.isEnabled()) {
                // Already initialized, skip logging
            } else if (SkriptHook.initialize()) {
                getLogger().info("Skript hook enabled!");
            } else {
                getLogger().info("Skript not found.");
            }
        } else {
            getLogger().info("Skript hook disabled in config.");
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
        SkriptHook.disable();
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
        
        // Register ogessentials command (with update subcommand)
        org.bukkit.command.PluginCommand ogessentialsCommand = getCommand("ogessentials");
        if (ogessentialsCommand != null) {
            dev.og69.ogessentials.commands.OGEssentialsCommand executor = 
                new dev.og69.ogessentials.commands.OGEssentialsCommand();
            ogessentialsCommand.setExecutor(executor);
            ogessentialsCommand.setTabCompleter(executor);
        }
        
        // Register AFK command
        org.bukkit.command.PluginCommand afkCommand = getCommand("afk");
        if (afkCommand != null) {
            afkCommand.setExecutor(new dev.og69.ogessentials.commands.AfkCommand(this));
        }
        
        // Register Keep Inventory command
        org.bukkit.command.PluginCommand keepInvCommand = getCommand("keepinventory");
        if (keepInvCommand != null) {
            keepInvCommand.setExecutor(new dev.og69.ogessentials.commands.KeepInventoryCommand(this));
        }
        
        // Register Home commands
        org.bukkit.command.PluginCommand setHomeCmd = getCommand("sethome");
        if (setHomeCmd != null) {
            dev.og69.ogessentials.commands.SetHomeCommand executor = new dev.og69.ogessentials.commands.SetHomeCommand(this);
            setHomeCmd.setExecutor(executor);
            setHomeCmd.setTabCompleter(executor);
        }
        
        org.bukkit.command.PluginCommand homeCmd = getCommand("home");
        if (homeCmd != null) {
            dev.og69.ogessentials.commands.HomeCommand executor = new dev.og69.ogessentials.commands.HomeCommand(this);
            homeCmd.setExecutor(executor);
            homeCmd.setTabCompleter(executor);
        }
        
        org.bukkit.command.PluginCommand delHomeCmd = getCommand("delhome");
        if (delHomeCmd != null) {
            dev.og69.ogessentials.commands.DelHomeCommand executor = new dev.og69.ogessentials.commands.DelHomeCommand(this);
            delHomeCmd.setExecutor(executor);
            delHomeCmd.setTabCompleter(executor);
        }
        
        org.bukkit.command.PluginCommand homesCmd = getCommand("homes");
        if (homesCmd != null) {
            homesCmd.setExecutor(new dev.og69.ogessentials.commands.HomesCommand(this));
        }
        
        // Register TPA commands
        org.bukkit.command.PluginCommand tpaCmd = getCommand("tpa");
        if (tpaCmd != null) {
            dev.og69.ogessentials.commands.TpaCommand executor = new dev.og69.ogessentials.commands.TpaCommand(this);
            tpaCmd.setExecutor(executor);
            tpaCmd.setTabCompleter(executor);
        }
        
        org.bukkit.command.PluginCommand tpAcceptCmd = getCommand("tpaccept");
        if (tpAcceptCmd != null) {
            tpAcceptCmd.setExecutor(new dev.og69.ogessentials.commands.TpAcceptCommand(this));
        }
        
        org.bukkit.command.PluginCommand tpDenyCmd = getCommand("tpdeny");
        if (tpDenyCmd != null) {
            tpDenyCmd.setExecutor(new dev.og69.ogessentials.commands.TpDenyCommand(this));
        }

        // Register InvSee command
        org.bukkit.command.PluginCommand invSeeCmd = getCommand("invsee");
        if (invSeeCmd != null) {
            invSeeCmd.setExecutor(new dev.og69.ogessentials.commands.InvSeeCommand(this));
        }

        // Register Fly command
        org.bukkit.command.PluginCommand flyCmd = getCommand("fly");
        if (flyCmd != null) {
            flyCmd.setExecutor(new dev.og69.ogessentials.commands.FlyCommand(this));
        }

        // Register Back command

        // Register Back command
        org.bukkit.command.PluginCommand backCmd = getCommand("back");
        if (backCmd != null) {
            backCmd.setExecutor(new dev.og69.ogessentials.commands.BackCommand(this));
        }

        // Register Permission command
        org.bukkit.command.PluginCommand permCmd = getCommand("perm");
        if (permCmd != null) {
            dev.og69.ogessentials.commands.PermissionCommand permissionCommand = 
                new dev.og69.ogessentials.commands.PermissionCommand(this, permissionManager);
            permCmd.setExecutor(permissionCommand);
            permCmd.setTabCompleter(permissionCommand);
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
        
        // Register AFK listener
        getServer().getPluginManager().registerEvents(
            new AfkListener(afkManager),
            this
        );
        
        // Register Sleep listener for half-sleep system
        sleepListener = new SleepListener(this, afkManager);
        getServer().getPluginManager().registerEvents(sleepListener, this);

        // Register Teleport listener for /back
        getServer().getPluginManager().registerEvents(
            new dev.og69.ogessentials.listeners.TeleportListener(backManager),
            this
        );
        
        // Register Chat listener
        getServer().getPluginManager().registerEvents(
            new dev.og69.ogessentials.listeners.ChatListener(),
            this
        );

        // Register InvSee listener
        getServer().getPluginManager().registerEvents(
            new dev.og69.ogessentials.listeners.InvSeeListener(),
            this
        );

        // Register Disabled Commands listener
        getServer().getPluginManager().registerEvents(
            new dev.og69.ogessentials.listeners.DisabledCommandsListener(this),
            this
        );

        // Register Permission listener
        getServer().getPluginManager().registerEvents(
            new dev.og69.ogessentials.listeners.PermissionListener(permissionManager),
            this
        );
    }
    
    /**
     * Initialize the AFK and Half-Sleep system.
     */
    private void initializeAfkSystem() {
        // Initialize managers
        nameTagManager = new NameTagManager();
        afkManager = new AfkManager(this, nameTagManager);
        
        // Start AFK check task (runs every 60 seconds = 1200 ticks)
        afkCheckTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
            this,
            new AfkCheckTask(afkManager, this),
            1200L, // Initial delay: 60 seconds
            1200L  // Repeat interval: 60 seconds
        );
        
        getLogger().info("AFK and Half-Sleep system initialized!");
    }
    
    /**
     * Clean up the AFK and Half-Sleep system.
     */
    private void cleanupAfkSystem() {
        // Cancel AFK check task
        if (afkCheckTaskId != -1) {
            Bukkit.getScheduler().cancelTask(afkCheckTaskId);
            afkCheckTaskId = -1;
        }
        
        // Clean up managers
        if (afkManager != null) {
            afkManager.cleanup();
        }
        if (nameTagManager != null) {
            nameTagManager.cleanup();
        }
        if (sleepListener != null) {
            sleepListener.cleanup();
        }
    }
    
    /**
     * Initialize the Keep Inventory system.
     */
    private void initializeKeepInventorySystem() {
        // Check if feature is enabled in config
        if (!getConfig().getBoolean("keep-inventory.enabled", true)) {
            getLogger().info("Keep Inventory system disabled in config.");
            return;
        }
        
        // Initialize manager (loads data from file)
        keepInventoryManager = new KeepInventoryManager(this);
        
        // Initialize and register listener
        keepInventoryListener = new KeepInventoryListener(this, keepInventoryManager);
        getServer().getPluginManager().registerEvents(keepInventoryListener, this);
        
        getLogger().info("Keep Inventory system initialized!");
    }
    
    /**
     * Clean up the Keep Inventory system.
     */
    private void cleanupKeepInventorySystem() {
        if (keepInventoryListener != null) {
            keepInventoryListener.cleanup();
        }
        if (keepInventoryManager != null) {
            keepInventoryManager.cleanup();
        }
    }
    
    /**
     * Initialize the Database and Homes system.
     */
    private void initializeHomesSystem() {
        // Initialize database
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Failed to initialize database! Homes system will not be available.");
            return;
        }
        
        // Initialize home manager
        homeManager = new HomeManager(this, databaseManager);
        
        getLogger().info("Homes system initialized!");
    }
    
    /**
     * Clean up the Homes system and database.
     */
    private void cleanupHomesSystem() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }
    
    /**
     * Initialize the TPA system.
     */
    private void initializeTpaSystem() {
        // Initialize TPA manager
        tpaManager = new TpaManager(this);
        
        // Start TPA expiry check task (runs every 5 seconds = 100 ticks)
        tpaExpiryTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
            this,
            new TpaExpiryTask(tpaManager),
            100L, // Initial delay: 5 seconds
            100L  // Repeat interval: 5 seconds
        );
        
        getLogger().info("TPA system initialized!");
    }
    
    /**
     * Clean up the TPA system.
     */
    private void cleanupTpaSystem() {
        // Cancel TPA expiry task
        if (tpaExpiryTaskId != -1) {
            Bukkit.getScheduler().cancelTask(tpaExpiryTaskId);
            tpaExpiryTaskId = -1;
        }
        
        if (tpaManager != null) {
            tpaManager.cleanup();
        }
    }
    
    /**
     * Initialize the update checker if enabled in config.
     */
    private void initializeUpdateChecker() {
        if (!getConfig().getBoolean("updater.enabled", true)) {
            return;
        }
        
        updateChecker = new UpdateChecker(this, getDescription().getVersion(), "OG69Dev", "OGEssentials");
        
        // Check for updates on startup
        updateChecker.checkForUpdates(this::handleUpdateResult);
        
        // Schedule periodic checks
        int intervalHours = getConfig().getInt("updater.check-interval", 24);
        if (intervalHours > 0) {
            long intervalTicks = intervalHours * 3600 * 20L; // Convert hours to ticks (1 hour = 3600 seconds = 72000 ticks)
            updateCheckTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                this,
                () -> updateChecker.checkForUpdates(this::handleUpdateResult),
                intervalTicks,
                intervalTicks
            );
        }
    }
    
    /**
     * Handle update check results.
     * 
     * @param result The update result
     */
    private void handleUpdateResult(UpdateChecker.UpdateResult result) {
        if (result == null) {
            return;
        }
        
        if (!result.isSuccess()) {
            // Only log errors to console, don't spam admins
            getLogger().warning("[UpdateChecker] " + result.getErrorMessage());
            return;
        }
        
        if (result.isUpdateAvailable()) {
            // Log to console
            getLogger().info("=========================================");
            getLogger().info("A new version of OG-Essentials is available!");
            getLogger().info("Current version: " + getDescription().getVersion());
            getLogger().info("Latest version: " + result.getLatestVersion());
            getLogger().info("Download: " + result.getDownloadUrl());
            getLogger().info("=========================================");
            
            // Notify admins if enabled
            if (getConfig().getBoolean("updater.notify-admins", true)) {
                String message = ChatColor.translateAlternateColorCodes('&',
                    "&7[&bOG-Essentials&7] &eA new version is available: &a" + result.getLatestVersion() +
                    " &7(current: &c" + getDescription().getVersion() + "&7)");
                
                Bukkit.getOnlinePlayers().stream()
                    .filter(player -> player.hasPermission("ogessentials.updater.notify"))
                    .forEach(player -> player.sendMessage(message));
            }
        } else {
            // Already on latest version - only log on initial startup, not on reload
            // (isInitialStartup flag is handled in initializeUpdateChecker now)
            if (isInitialStartup) {
                getLogger().info("[UpdateChecker] You are running the latest version (" + getDescription().getVersion() + ")");
                isInitialStartup = false;
            }
        }
    }
    
    /**
     * Get the update checker instance.
     * 
     * @return The update checker, or null if disabled
     */
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
    
    /**
     * Get the name tag manager instance.
     * 
     * @return The name tag manager
     */
    public NameTagManager getNameTagManager() {
        return nameTagManager;
    }
    
    /**
     * Get the AFK manager instance.
     * 
     * @return The AFK manager
     */
    public AfkManager getAfkManager() {
        return afkManager;
    }
    
    /**
     * Get the Keep Inventory manager instance.
     * 
     * @return The Keep Inventory manager, or null if disabled
     */
    public KeepInventoryManager getKeepInventoryManager() {
        return keepInventoryManager;
    }
    
    /**
     * Get the database manager instance.
     * 
     * @return The database manager, or null if not initialized
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    /**
     * Get the home manager instance.
     * 
     * @return The home manager, or null if not initialized
     */
    public HomeManager getHomeManager() {
        return homeManager;
    }
    
    /**
     * Get the TPA manager instance.
     * 
     * @return The TPA manager
     */
    public TpaManager getTpaManager() {
        return tpaManager;
    }

    /**
     * Get the Back manager instance.
     *
     * @return The Back manager
     */
    public BackManager getBackManager() {
        return backManager;
    }

    /**
     * Get the Permission manager instance.
     *
     * @return The Permission manager
     */
    public PermissionManager getPermissionManager() {
        return permissionManager;
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
        
        // Reinitialize update checker if config changed
        // Note: isInitialStartup will be false on reload, so "latest version" message won't be logged
        if (updateCheckTaskId != -1) {
            Bukkit.getScheduler().cancelTask(updateCheckTaskId);
            updateCheckTaskId = -1;
        }
        initializeUpdateChecker();
    }
}
