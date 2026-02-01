package dev.og69.ogessentials.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.og69.ogessentials.OGEssentials;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages Keep Inventory settings for players.
 * 
 * Tracks which players have keep inventory enabled and persists
 * the data to a JSON file so settings survive server restarts.
 */
public class KeepInventoryManager {
    
    private final OGEssentials plugin;
    private final Set<UUID> enabledPlayers = new HashSet<>();
    private final File dataFile;
    private final Gson gson;
    
    /**
     * Create a new Keep Inventory manager.
     * 
     * @param plugin The plugin instance
     */
    public KeepInventoryManager(OGEssentials plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "keepinventory.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Load data from file on startup
        load();
    }
    
    /**
     * Check if a player has keep inventory enabled.
     * 
     * @param playerId The player's UUID
     * @return true if keep inventory is enabled for this player
     */
    public boolean isEnabled(UUID playerId) {
        return enabledPlayers.contains(playerId);
    }
    
    /**
     * Set a player's keep inventory status.
     * 
     * @param playerId The player's UUID
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(UUID playerId, boolean enabled) {
        if (enabled) {
            enabledPlayers.add(playerId);
        } else {
            enabledPlayers.remove(playerId);
        }
        save();
    }
    
    /**
     * Toggle a player's keep inventory status.
     * 
     * @param playerId The player's UUID
     * @return The new status (true if now enabled)
     */
    public boolean toggle(UUID playerId) {
        boolean newState;
        if (enabledPlayers.contains(playerId)) {
            enabledPlayers.remove(playerId);
            newState = false;
        } else {
            enabledPlayers.add(playerId);
            newState = true;
        }
        save();
        return newState;
    }
    
    /**
     * Load data from the JSON file.
     */
    private void load() {
        if (!dataFile.exists()) {
            return;
        }
        
        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<KeepInventoryData>() {}.getType();
            KeepInventoryData data = gson.fromJson(reader, type);
            
            if (data != null && data.enabled_players != null) {
                enabledPlayers.clear();
                for (String uuidString : data.enabled_players) {
                    try {
                        enabledPlayers.add(UUID.fromString(uuidString));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in keepinventory.json: " + uuidString);
                    }
                }
                plugin.getLogger().info("Loaded " + enabledPlayers.size() + " players with Keep Inventory enabled.");
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load keepinventory.json: " + e.getMessage());
        }
    }
    
    /**
     * Save data to the JSON file.
     */
    private void save() {
        // Ensure data folder exists
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        try (FileWriter writer = new FileWriter(dataFile)) {
            KeepInventoryData data = new KeepInventoryData();
            data.enabled_players = enabledPlayers.stream()
                .map(UUID::toString)
                .collect(Collectors.toList());
            
            gson.toJson(data, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save keepinventory.json: " + e.getMessage());
        }
    }
    
    /**
     * Clean up and save data (called on plugin disable).
     */
    public void cleanup() {
        save();
        enabledPlayers.clear();
    }
    
    /**
     * Data class for JSON serialization.
     */
    private static class KeepInventoryData {
        List<String> enabled_players;
    }
}
