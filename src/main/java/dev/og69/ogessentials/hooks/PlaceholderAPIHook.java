package dev.og69.ogessentials.hooks;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Optional hook for PlaceholderAPI integration.
 * Provides placeholders for homes, AFK status, custom enchants, and keep inventory.
 * 
 * This hook is optional - the plugin works fully without PlaceholderAPI.
 * If PlaceholderAPI is not installed, placeholders simply won't be available.
 * 
 * This class uses reflection to load PlaceholderExpansionImpl only when
 * PlaceholderAPI is confirmed to be available, avoiding class loading issues.
 */
public class PlaceholderAPIHook {
    
    private static boolean enabled = false;
    private static Object expansionInstance = null;
    private static Method registerMethod = null;
    private static Method unregisterMethod = null;
    
    /**
     * Initialize the PlaceholderAPI hook.
     * Checks if PlaceholderAPI is installed and registers the expansion using reflection.
     * 
     * @param mainPluginInstance The main OGEssentials plugin instance
     * @return true if PlaceholderAPI is available and hook is registered, false otherwise
     */
    public static boolean initialize(dev.og69.ogessentials.OGEssentials mainPluginInstance) {
        // Double-check PlaceholderAPI is available
        Plugin placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderAPI == null || !placeholderAPI.isEnabled()) {
            enabled = false;
            return false;
        }
        
        try {
            // Load PlaceholderExpansionImpl class using reflection
            // This class extends PlaceholderExpansion, so it will only load if PlaceholderAPI is available
            Class<?> implClass = Class.forName("dev.og69.ogessentials.hooks.PlaceholderExpansionImpl");
            Constructor<?> constructor = implClass.getDeclaredConstructor(Plugin.class);
            expansionInstance = constructor.newInstance(mainPluginInstance);
            
            // Get register method from the expansion instance
            registerMethod = expansionInstance.getClass().getMethod("register");
            
            // Register the expansion
            Boolean result = (Boolean) registerMethod.invoke(expansionInstance);
            if (result != null && result) {
                enabled = true;
                return true;
            }
        } catch (NoClassDefFoundError e) {
            // PlaceholderExpansion class not found - PlaceholderAPI not available
            // This happens when PlaceholderExpansionImpl tries to extend PlaceholderExpansion
            enabled = false;
            return false;
        } catch (ClassNotFoundException e) {
            // PlaceholderAPI classes not found - this is expected if PlaceholderAPI isn't installed
            enabled = false;
            return false;
        } catch (Exception e) {
            // PlaceholderAPI registration failed
            enabled = false;
            return false;
        }
        
        enabled = false;
        return false;
    }
    
    /**
     * Check if the PlaceholderAPI hook is enabled and available.
     * 
     * @return true if PlaceholderAPI is available, false otherwise
     */
    public static boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Disable the hook (called on plugin disable).
     */
    public static void disable() {
        if (expansionInstance != null && enabled) {
            try {
                if (unregisterMethod == null) {
                    unregisterMethod = expansionInstance.getClass().getMethod("unregister");
                }
                unregisterMethod.invoke(expansionInstance);
            } catch (NoClassDefFoundError e) {
                // PlaceholderAPI classes not available - this is expected
            } catch (Exception e) {
                // Ignore errors during unregistration - PlaceholderAPI might not be available
            }
        }
        enabled = false;
        expansionInstance = null;
        registerMethod = null;
        unregisterMethod = null;
    }
}
