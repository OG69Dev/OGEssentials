package dev.og69.ogessentials.migration;

import dev.og69.ogessentials.OGEssentials;
import dev.og69.ogessentials.hooks.SkriptHook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

/**
 * Parses Skript variables using Skript's native API to extract data for migration.
 * 
 * Supports parsing:
 * - Homes: {homes.<UUID>.<name>} = location
 * - Keep Inventory: {keepinv.<player>} = true
 * 
 * Requires Skript to be installed and SkriptHook to be enabled.
 */
public class SkriptMigrationParser {
    
    private final OGEssentials plugin;
    
    /**
     * Create a new migration parser.
     * 
     * @param plugin The plugin instance
     */
    public SkriptMigrationParser(OGEssentials plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Parse homes from Skript variables using Skript's API.
     * 
     * @param variablesFile Ignored - kept for backward compatibility
     * @return Map of player UUID -> (home name -> location)
     */
    public Map<UUID, Map<String, Location>> parseHomes(File variablesFile) {
        Map<UUID, Map<String, Location>> homes = new HashMap<>();
        
        if (!SkriptHook.isEnabled()) {
            plugin.getLogger().warning("Skript hook is not enabled! Cannot migrate Skript homes.");
            plugin.getLogger().warning("Make sure Skript is installed and the hook is enabled in config.");
            return homes;
        }
        
        try {
            // Skript variables with dot notation (homes.uuid.name) aren't list variables
            // so the ::* wildcard syntax doesn't work. Access internal storage directly.
            plugin.getLogger().info("Attempting to retrieve Skript home variables...");
            
            Map<String, Object> variables = SkriptHook.getAllVariablesWithPrefix("homes.");
            
            if (variables.isEmpty()) {
                plugin.getLogger().info("No Skript home variables found.");
                plugin.getLogger().info("Make sure Skript has loaded the variables (server must have started with Skript).");
                return homes;
            }
            
            plugin.getLogger().info("Found " + variables.size() + " Skript variable entries.");
            
            // Process each variable
            // Variable names are in format: "homes.550e8400-e29b-41d4-a716-446655440000.spawn"
            // or with list notation: "homes.550e8400-e29b-41d4-a716-446655440000.list::1"
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String fullVarName = entry.getKey();
                Object value = entry.getValue();
                
                // Remove "homes." prefix
                String varName = fullVarName.substring("homes.".length());
                
                plugin.getLogger().info("Processing variable: " + varName + " = " + (value != null ? value.getClass().getSimpleName() : "null"));
                
                // Skip list metadata entries
                if (varName.contains(".list::") || varName.contains("list::")) {
                    plugin.getLogger().info("Skipping list entry: " + varName);
                    continue;
                }
                
                // Parse the variable name: "<uuid>.<homename>"
                String[] parts = varName.split("\\.", 2);
                if (parts.length != 2) {
                    plugin.getLogger().warning("Unexpected variable name format: " + varName);
                    continue;
                }
                
                try {
                    // Parse UUID
                    UUID playerId = UUID.fromString(parts[0]);
                    String homeName = parts[1].toLowerCase().trim();
                    
                    // Parse location
                    Location location = parseLocation(value);
                    if (location != null) {
                        homes.computeIfAbsent(playerId, k -> new HashMap<>())
                             .put(homeName, location);
                        plugin.getLogger().info("✓ Migrated home '" + homeName + "' for player " + playerId);
                    } else {
                        plugin.getLogger().warning("Failed to parse location for home: " + varName);
                    }
                } catch (IllegalArgumentException e) {
                    // Invalid UUID or location, skip
                    plugin.getLogger().warning("Failed to parse home variable: " + varName + " - " + e.getMessage());
                }
            }
            
            plugin.getLogger().info("Successfully parsed " + homes.size() + " players with homes from Skript.");
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to read Skript home variables: " + e.getMessage());
            e.printStackTrace();
        }
        
        return homes;
    }
    
    /**
     * Parse keep inventory players from Skript variables using Skript's API.
     * Returns player names (not UUIDs) since Skript uses names.
     * 
     * @param variablesFile Ignored - kept for backward compatibility
     * @return Set of player names with keep inventory enabled
     */
    public Set<String> parseKeepInventoryPlayers(File variablesFile) {
        Set<String> players = new HashSet<>();
        
        if (!SkriptHook.isEnabled()) {
            plugin.getLogger().warning("Skript hook is not enabled! Cannot migrate Skript keep inventory data.");
            plugin.getLogger().warning("Make sure Skript is installed and the hook is enabled in config.");
            return players;
        }
        
        try {
            plugin.getLogger().info("Attempting to retrieve Skript keep inventory variables...");
            
            Map<String, Object> variables = SkriptHook.getAllVariablesWithPrefix("keepinv.");
            
            if (variables.isEmpty()) {
                plugin.getLogger().info("No Skript keep inventory variables found.");
                return players;
            }
            
            plugin.getLogger().info("Found " + variables.size() + " Skript keepinv variable entries.");
            
            // Process each variable
            // Variable names are: "keepinv.playername"
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String fullVarName = entry.getKey();
                Object value = entry.getValue();
                
                // Remove "keepinv." prefix
                String playerName = fullVarName.substring("keepinv.".length());
                
                plugin.getLogger().info("Processing keepinv variable: " + playerName + " = " + value);
                
                // Check if value is true
                if (isTrue(value)) {
                    players.add(playerName);
                    plugin.getLogger().info("✓ Migrated keep inventory for player: " + playerName);
                }
            }
            
            plugin.getLogger().info("Successfully parsed " + players.size() + " players with keep inventory from Skript.");
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to read Skript keep inventory variables: " + e.getMessage());
            e.printStackTrace();
        }
        
        return players;
    }
    
    /**
     * Resolve a player name to UUID using Bukkit's offline player lookup.
     * 
     * @param playerName The player name
     * @return The player's UUID, or null if not found
     */
    @SuppressWarnings("deprecation")
    public UUID resolvePlayerUUID(String playerName) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
            return offlinePlayer.getUniqueId();
        }
        return null;
    }
    
    /**
     * Parse a location from Skript's variable value.
     * Skript's API returns Location objects directly for location variables.
     * 
     * @param value The Skript variable value
     * @return The parsed Location, or null if invalid
     */
    private Location parseLocation(Object value) {
        if (value == null) {
            return null;
        }
        
        // Skript's Variables.getVariable() returns org.bukkit.Location objects directly
        // for location variables, already deserialized
        if (value instanceof Location) {
            return (Location) value;
        }
        
        // If it's not a Location object, log a warning with detailed info
        plugin.getLogger().warning("Expected Location object but got: " + value.getClass().getName());
        plugin.getLogger().warning("Value: " + value.toString());
        return null;
    }
    
    /**
     * Check if a value represents true.
     * 
     * @param value The value to check
     * @return true if the value represents true
     */
    private boolean isTrue(Object value) {
        if (value == null) {
            return false;
        }
        
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        
        if (value instanceof String) {
            String str = ((String) value).toLowerCase();
            return str.equals("true") || str.equals("1") || str.equals("yes");
        }
        
        return false;
    }
    
    /**
     * Get the default Skript variables file location.
     * This method is deprecated - it's only kept for backward compatibility.
     * The new parser uses Skript's API directly and doesn't read from files.
     * 
     * @return The default variables.csv file
     * @deprecated Use SkriptHook API instead
     */
    @Deprecated
    public File getDefaultVariablesFile() {
        return new File(plugin.getDataFolder().getParentFile(), "Skript/variables.csv");
    }
}
