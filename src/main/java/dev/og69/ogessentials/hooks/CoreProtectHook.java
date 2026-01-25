package dev.og69.ogessentials.hooks;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Optional hook for CoreProtect integration.
 * Provides block change logging functionality.
 * 
 * This hook is optional - the plugin works fully without CoreProtect.
 * If CoreProtect is not installed, all methods will return false and do nothing.
 */
public class CoreProtectHook {
    
    private static boolean enabled = false;
    private static Object coreProtectAPI = null;
    
    /**
     * Initialize the CoreProtect hook.
     * Checks if CoreProtect is installed and enabled.
     * 
     * @return true if CoreProtect is available and hook is enabled, false otherwise
     */
    public static boolean initialize() {
        Plugin coreProtect = Bukkit.getPluginManager().getPlugin("CoreProtect");
        
        if (coreProtect == null || !coreProtect.isEnabled()) {
            enabled = false;
            return false;
        }
        
        try {
            // Try to get the CoreProtect API
            Class<?> apiClass = Class.forName("net.coreprotect.CoreProtect");
            Class<?> apiGetterClass = Class.forName("net.coreprotect.CoreProtectAPI");
            
            // Get the API instance
            java.lang.reflect.Method getAPIMethod = apiGetterClass.getMethod("getAPI");
            coreProtectAPI = getAPIMethod.invoke(null);
            
            if (coreProtectAPI != null) {
                enabled = true;
                return true;
            }
        } catch (Exception e) {
            // CoreProtect API not available or incompatible version
            enabled = false;
            return false;
        }
        
        enabled = false;
        return false;
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
     * Log a block change to CoreProtect.
     * This method does nothing if CoreProtect is not available.
     * 
     * @param player The player who caused the block change
     * @param block The block that was changed
     * @param oldState The previous state of the block (before change)
     * @return true if the block change was logged successfully, false otherwise
     */
    public static boolean logBlockChange(Player player, Block block, BlockState oldState) {
        if (!enabled || coreProtectAPI == null) {
            return false;
        }
        
        try {
            // Use reflection to call CoreProtect API methods
            // CoreProtect API: logRemoval(player, location, type, data)
            // or logPlacement(player, location, type, data)
            
            Class<?> apiClass = coreProtectAPI.getClass();
            
            // Determine if this is a removal or placement
            if (oldState != null && oldState.getType() != org.bukkit.Material.AIR) {
                // Log removal
                java.lang.reflect.Method logRemoval = apiClass.getMethod(
                    "logRemoval",
                    String.class,
                    org.bukkit.Location.class,
                    org.bukkit.Material.class,
                    org.bukkit.block.data.BlockData.class
                );
                
                logRemoval.invoke(
                    coreProtectAPI,
                    player.getName(),
                    block.getLocation(),
                    oldState.getType(),
                    oldState.getBlockData()
                );
            }
            
            // Log placement if new block is not air
            if (block.getType() != org.bukkit.Material.AIR) {
                java.lang.reflect.Method logPlacement = apiClass.getMethod(
                    "logPlacement",
                    String.class,
                    org.bukkit.Location.class,
                    org.bukkit.Material.class,
                    org.bukkit.block.data.BlockData.class
                );
                
                logPlacement.invoke(
                    coreProtectAPI,
                    player.getName(),
                    block.getLocation(),
                    block.getType(),
                    block.getBlockData()
                );
            }
            
            return true;
        } catch (Exception e) {
            // CoreProtect API call failed
            return false;
        }
    }
    
    /**
     * Log a block break to CoreProtect.
     * Convenience method for logging block breaks.
     * 
     * @param player The player who broke the block
     * @param block The block that was broken
     * @return true if the block break was logged successfully, false otherwise
     */
    public static boolean logBlockBreak(Player player, Block block) {
        if (!enabled || coreProtectAPI == null) {
            return false;
        }
        
        BlockState oldState = block.getState();
        return logBlockChange(player, block, oldState);
    }
    
    /**
     * Disable the hook (called on plugin disable).
     */
    public static void disable() {
        enabled = false;
        coreProtectAPI = null;
    }
}
