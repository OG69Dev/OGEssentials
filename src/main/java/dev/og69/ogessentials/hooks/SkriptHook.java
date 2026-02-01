package dev.og69.ogessentials.hooks;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Optional hook for Skript integration.
 * Provides access to Skript variables using Skript's native API.
 * 
 * This hook is optional - the plugin works fully without Skript.
 * If Skript is not installed, all methods will return null/false.
 * 
 * Uses reflection to avoid hard dependency on Skript classes.
 */
public class SkriptHook {
    
    private static boolean enabled = false;
    private static Method getVariableMethod = null;
    private static Object variablesStorage = null;
    
    /**
     * Initialize the Skript hook.
     * Checks if Skript is installed and loads the Variables API.
     * 
     * @return true if Skript is available and hook is enabled, false otherwise
     */
    public static boolean initialize() {
        Plugin skriptPlugin = Bukkit.getPluginManager().getPlugin("Skript");
        
        if (skriptPlugin == null || !skriptPlugin.isEnabled()) {
            enabled = false;
            getVariableMethod = null;
            variablesStorage = null;
            return false;
        }
        
        try {
            // Load the Variables class from Skript
            Class<?> variablesClass = Class.forName("ch.njol.skript.variables.Variables");
            
            // Get the getVariable method
            // public static @Nullable Object getVariable(String name, @Nullable Event event, boolean local)
            getVariableMethod = variablesClass.getMethod(
                "getVariable",
                String.class,
                org.bukkit.event.Event.class,
                boolean.class
            );
            
            // Try to access the internal variables map for getAllVariables functionality
            // Skript stores variables in a static field, usually called "variables" or "variablesMap"
            try {
                Field variablesField = variablesClass.getDeclaredField("variables");
                variablesField.setAccessible(true);
                variablesStorage = variablesField.get(null);
                Bukkit.getLogger().info("[SkriptHook] Successfully accessed internal 'variables' field");
                if (variablesStorage != null) {
                    Bukkit.getLogger().info("[SkriptHook] Storage type: " + variablesStorage.getClass().getName());
                }
            } catch (NoSuchFieldException e) {
                // Try alternative field names
                Bukkit.getLogger().info("[SkriptHook] Field 'variables' not found, trying 'variablesMap'...");
                try {
                    Field variablesField = variablesClass.getDeclaredField("variablesMap");
                    variablesField.setAccessible(true);
                    variablesStorage = variablesField.get(null);
                    Bukkit. getLogger().info("[SkriptHook] Successfully accessed internal 'variablesMap' field");
                    if (variablesStorage != null) {
                        Bukkit.getLogger().info("[SkriptHook] Storage type: " + variablesStorage.getClass().getName());
                    }
                } catch (NoSuchFieldException e2) {
                    // List all fields to see what's available
                    Bukkit.getLogger().warning("[SkriptHook] Could not find variables storage field. Available fields:");
                    for (Field field : variablesClass.getDeclaredFields()) {
                        Bukkit.getLogger().warning("[SkriptHook]   - " + field.getName() + " (" + field.getType().getName() + ")");
                    }
                    variablesStorage = null;
                }
            }
            
            enabled = true;
            return true;
            
        } catch (ClassNotFoundException e) {
            // Skript classes not available
            enabled = false;
            getVariableMethod = null;
            variablesStorage = null;
            return false;
        } catch (NoSuchMethodException e) {
            // Variables.getVariable method not found - incompatible Skript version
            enabled = false;
            getVariableMethod = null;
            variablesStorage = null;
            return false;
        } catch (Exception e) {
            // Skript API not available or incompatible version
            enabled = false;
            getVariableMethod = null;
            variablesStorage = null;
            return false;
        }
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
     * Get a Skript variable value.
     * 
     * @param name The variable name (e.g., "homes.550e8400-e29b-41d4-a716-446655440000.spawn")
     * @return The variable value, or null if not found or hook is disabled
     */
    public static Object getVariable(String name) {
        if (!enabled || getVariableMethod == null) {
            return null;
        }
        
        try {
            // Call Variables.getVariable(name, null, false)
            // null event = global variable, false = not local
            return getVariableMethod.invoke(null, name, null, false);
        } catch (Exception e) {
            // Variable not found or API call failed
            return null;
        }
    }
    
    /**
     * Get all Skript variables matching a pattern using wildcard syntax.
     * 
     * For example:
     * - "homes::*" returns all variables starting with "homes"
     * - "keepinv::*" returns all variables starting with "keepinv"
     * 
     * The returned map has variable names as keys (without the wildcard prefix)
     * and variable values as values.
     * 
     * @param pattern The variable pattern with wildcard (e.g., "homes::*")
     * @return Map of variable name suffixes to values, or empty map if hook is disabled
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getVariables(String pattern) {
        if (!enabled || getVariableMethod == null) {
            return new HashMap<>();
        }
        
        try {
            // Call Variables.getVariable with wildcard pattern
            // This returns a Map<String, Object> of all matching variables
            Object result = getVariableMethod.invoke(null, pattern, null, false);
            
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            }
            
            return new HashMap<>();
        } catch (Exception e) {
            // API call failed
            return new HashMap<>();
        }
    }
    
    /**
     * Get all Skript variables with a specific prefix by directly accessing internal storage.
     * This is a fallback method that doesn't rely on wildcard patterns.
     * 
     * @param prefix The variable name prefix (e.g., "homes.")
     * @return Map of full variable names to values
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getAllVariablesWithPrefix(String prefix) {
        Map<String, Object> result = new HashMap<>();
        
        if (!enabled) {
            Bukkit.getLogger().warning("[SkriptHook] Hook not enabled!");
            return result;
        }
        
        if (variablesStorage == null) {
            Bukkit.getLogger().warning("[SkriptHook] Internal variables storage not available!");
            return result;
        }
        
        try {
            Bukkit.getLogger().info("[SkriptHook] Searching for variables with prefix: " + prefix);
            Bukkit.getLogger().info("[SkriptHook] Storage type: " + variablesStorage.getClass().getName());
            
            // VariablesMap doesn't have entrySet(), so we need to access its internal Map field
            Class<?> storageClass = variablesStorage.getClass();
            
            // Inspect all fields to find the underlying Map
            Bukkit.getLogger().info("[SkriptHook] Inspecting VariablesMap fields...");
            Field[] allFields = storageClass.getDeclaredFields();
            
            for (Field field : allFields) {
                Bukkit.getLogger().info("[SkriptHook]   Field: " + field.getName() + " (" + field.getType().getSimpleName() + ")");
            }
            
            // Try to find a Map field
            Field mapField = null;
            for (Field field : allFields) {
                if (Map.class.isAssignableFrom(field.getType())) {
                    mapField = field;
                    Bukkit.getLogger().info("[SkriptHook] Found Map field: " + field.getName());
                    break;
                }
            }
            
            if (mapField != null) {
                mapField.setAccessible(true);
                Object actualMap = mapField.get(variablesStorage);
                
                if (actualMap instanceof Map) {
                    Map<?, ?> varsMap = (Map<?, ?>) actualMap;
                    Bukkit.getLogger().info("[SkriptHook] Accessed underlying Map with " + varsMap.size() + " entries");
                    
                    int totalCount = 0;
                    int matchCount = 0;
                    
                    for (Map.Entry<?, ?> entry : varsMap.entrySet()) {
                        totalCount++;
                        String varName = entry.getKey().toString();
                        
                        if (totalCount <= 5) {
                            Bukkit.getLogger().info("[SkriptHook] Sample key: " + varName);
                        }
                        
                        // Check if variable name starts with prefix
                        if (varName.startsWith(prefix)) {
                            matchCount++;
                            Bukkit.getLogger().info("[SkriptHook] Found matching variable: " + varName);
                            
                            // Get the variable value using the API to ensure proper deserialization
                            Object value = getVariable(varName);
                            if (value != null) {
                                result.put(varName, value);
                                Bukkit.getLogger().info("[SkriptHook]   → Value type: " + value.getClass().getName());
                            } else {
                                Bukkit.getLogger().warning("[SkriptHook]   → getVariable returned null for: " + varName);
                            }
                        }
                    }
                    
                    Bukkit.getLogger().info("[SkriptHook] Processed " + totalCount + " total variables");
                    Bukkit.getLogger().info("[SkriptHook] Found " + matchCount + " variables matching prefix '" + prefix + "'");
                } else {
                    Bukkit.getLogger().warning("[SkriptHook] Map field didn't contain a Map: " + actualMap.getClass().getName());
                }
            } else {
                Bukkit.getLogger().warning("[SkriptHook] Could not find a Map field in VariablesMap");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[SkriptHook] Failed to access internal storage: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * Disable the hook (called on plugin disable).
     */
    public static void disable() {
        enabled = false;
        getVariableMethod = null;
        variablesStorage = null;
    }
}
