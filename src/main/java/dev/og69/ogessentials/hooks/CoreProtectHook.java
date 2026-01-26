package dev.og69.ogessentials.hooks;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Optional hook for CoreProtect integration.
 * Provides block change logging functionality using CoreProtect API v10+.
 * 
 * This hook is optional - the plugin works fully without CoreProtect.
 * If CoreProtect is not installed, all methods will return false and do nothing.
 * 
 * Based on CoreProtect API v10 documentation:
 * https://docs.coreprotect.net/api/version/v10/
 */
public class CoreProtectHook {
    
    private static boolean enabled = false;
    private static Object coreProtectAPI = null;
    private static boolean hasBlockStateMethods = false; // API v10+ has thread-safe BlockState methods
    
    /**
     * Initialize the CoreProtect hook.
     * Checks if CoreProtect is installed, enabled, and API version is >= 10.
     * 
     * @return true if CoreProtect is available and hook is enabled, false otherwise
     */
    public static boolean initialize() {
        Plugin coreProtectPlugin = Bukkit.getPluginManager().getPlugin("CoreProtect");
        
        if (coreProtectPlugin == null || !coreProtectPlugin.isEnabled()) {
            enabled = false;
            coreProtectAPI = null;
            return false;
        }
        
        try {
            // Check if plugin is CoreProtect instance
            Class<?> coreProtectClass = Class.forName("net.coreprotect.CoreProtect");
            if (!coreProtectClass.isInstance(coreProtectPlugin)) {
                enabled = false;
                coreProtectAPI = null;
                return false;
            }
            
            // Get API instance via getAPI() method on plugin instance
            java.lang.reflect.Method getAPIMethod = coreProtectClass.getMethod("getAPI");
            Object api = getAPIMethod.invoke(coreProtectPlugin);
            
            if (api == null) {
                enabled = false;
                coreProtectAPI = null;
                return false;
            }
            
            // Check if API is enabled
            java.lang.reflect.Method isEnabledMethod = api.getClass().getMethod("isEnabled");
            Boolean apiEnabled = (Boolean) isEnabledMethod.invoke(api);
            if (apiEnabled == null || !apiEnabled) {
                enabled = false;
                coreProtectAPI = null;
                return false;
            }
            
            // Check API version (must be >= 10)
            java.lang.reflect.Method apiVersionMethod = api.getClass().getMethod("APIVersion");
            Integer apiVersion = (Integer) apiVersionMethod.invoke(api);
            if (apiVersion == null || apiVersion < 10) {
                enabled = false;
                coreProtectAPI = null;
                return false;
            }
            
            // Check if BlockState methods are available (API v10+)
            try {
                api.getClass().getMethod("logRemoval", String.class, BlockState.class);
                api.getClass().getMethod("logPlacement", String.class, BlockState.class);
                hasBlockStateMethods = true;
            } catch (NoSuchMethodException e) {
                hasBlockStateMethods = false;
            }
            
            coreProtectAPI = api;
            enabled = true;
            return true;
            
        } catch (ClassNotFoundException e) {
            // CoreProtect classes not available
            enabled = false;
            coreProtectAPI = null;
            return false;
        } catch (Exception e) {
            // CoreProtect API not available or incompatible version
            enabled = false;
            coreProtectAPI = null;
            return false;
        }
    }
    
    /**
     * Check if the CoreProtect hook is enabled and available.
     * 
     * @return true if CoreProtect is available, false otherwise
     */
    public static boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Log a block removal to CoreProtect.
     * Uses thread-safe BlockState method if available (API v10+), otherwise falls back to location-based method.
     * 
     * @param user The username to log as having removed the block
     * @param blockState The BlockState of the block before removal (thread-safe method)
     * @return true if the block removal was logged successfully, false otherwise
     */
    public static boolean logRemoval(String user, BlockState blockState) {
        if (!enabled || coreProtectAPI == null || blockState == null) {
            return false;
        }
        
        try {
            Class<?> apiClass = coreProtectAPI.getClass();
            
            if (hasBlockStateMethods) {
                // Use thread-safe BlockState method (API v10+)
                java.lang.reflect.Method logRemoval = apiClass.getMethod(
                    "logRemoval",
                    String.class,
                    BlockState.class
                );
                
                Boolean result = (Boolean) logRemoval.invoke(coreProtectAPI, user, blockState);
                return result != null && result;
            } else {
                // Fallback to location-based method
                java.lang.reflect.Method logRemoval = apiClass.getMethod(
                    "logRemoval",
                    String.class,
                    org.bukkit.Location.class,
                    org.bukkit.Material.class,
                    org.bukkit.block.data.BlockData.class
                );
                
                Boolean result = (Boolean) logRemoval.invoke(
                    coreProtectAPI,
                    user,
                    blockState.getLocation(),
                    blockState.getType(),
                    blockState.getBlockData()
                );
                return result != null && result;
            }
        } catch (Exception e) {
            // CoreProtect API call failed
            return false;
        }
    }
    
    /**
     * Log a block placement to CoreProtect.
     * Uses thread-safe BlockState method if available (API v10+), otherwise falls back to location-based method.
     * 
     * @param user The username to log as having placed the block
     * @param blockState The BlockState of the block after placement (thread-safe method)
     * @return true if the block placement was logged successfully, false otherwise
     */
    public static boolean logPlacement(String user, BlockState blockState) {
        if (!enabled || coreProtectAPI == null || blockState == null) {
            return false;
        }
        
        try {
            Class<?> apiClass = coreProtectAPI.getClass();
            
            if (hasBlockStateMethods) {
                // Use thread-safe BlockState method (API v10+)
                java.lang.reflect.Method logPlacement = apiClass.getMethod(
                    "logPlacement",
                    String.class,
                    BlockState.class
                );
                
                Boolean result = (Boolean) logPlacement.invoke(coreProtectAPI, user, blockState);
                return result != null && result;
            } else {
                // Fallback to location-based method
                java.lang.reflect.Method logPlacement = apiClass.getMethod(
                    "logPlacement",
                    String.class,
                    org.bukkit.Location.class,
                    org.bukkit.Material.class,
                    org.bukkit.block.data.BlockData.class
                );
                
                Boolean result = (Boolean) logPlacement.invoke(
                    coreProtectAPI,
                    user,
                    blockState.getLocation(),
                    blockState.getType(),
                    blockState.getBlockData()
                );
                return result != null && result;
            }
        } catch (Exception e) {
            // CoreProtect API call failed
            return false;
        }
    }
    
    /**
     * Log a block break to CoreProtect.
     * This method logs the removal of a block using its state before it was broken.
     * 
     * @param player The player who broke the block
     * @param block The block that was broken (after breaking, will be AIR)
     * @param oldState The BlockState of the block before it was broken (must be captured before breaking)
     * @return true if the block break was logged successfully, false otherwise
     */
    public static boolean logBlockBreak(Player player, Block block, BlockState oldState) {
        if (!enabled || coreProtectAPI == null || oldState == null) {
            return false;
        }
        
        // Use the oldState to log the removal
        return logRemoval(player.getName(), oldState);
    }
    
    /**
     * Disable the hook (called on plugin disable).
     */
    public static void disable() {
        enabled = false;
        coreProtectAPI = null;
        hasBlockStateMethods = false;
    }
}
