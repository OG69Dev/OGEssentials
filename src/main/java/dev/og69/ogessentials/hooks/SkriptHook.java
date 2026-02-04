package dev.og69.ogessentials.hooks;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Optional hook for Skript integration.
 * 
 * This hook is optional - the plugin works fully without Skript.
 * Currently only detects if Skript is installed for future use.
 */
public class SkriptHook {
    
    private static boolean enabled = false;
    
    /**
     * Initialize the Skript hook.
     * Checks if Skript is installed.
     * 
     * @return true if Skript is available, false otherwise
     */
    public static boolean initialize() {
        Plugin skriptPlugin = Bukkit.getPluginManager().getPlugin("Skript");
        
        if (skriptPlugin == null || !skriptPlugin.isEnabled()) {
            enabled = false;
            return false;
        }
        
        enabled = true;
        return true;
    }
    
    /**
     * Check if the Skript hook is enabled and available.
     * 
     * @return true if Skript is available, false otherwise
     */
    public static boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Disable the hook (called on plugin disable).
     */
    public static void disable() {
        enabled = false;
    }
}
