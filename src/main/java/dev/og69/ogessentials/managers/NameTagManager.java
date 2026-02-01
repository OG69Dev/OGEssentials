package dev.og69.ogessentials.managers;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player name tags with support for multiple prefixes/suffixes.
 * 
 * This manager allows different systems (AFK, etc.) to add their own
 * prefixes/suffixes to player names without conflicting with each other.
 * Tags are ordered by priority (higher priority = appears first).
 */
public class NameTagManager {
    
    /**
     * Represents a name tag with prefix, suffix, and priority.
     */
    public record TagData(String prefix, String suffix, int priority) {}
    
    // Tag storage: playerId -> (tagId -> TagData)
    private final Map<UUID, Map<String, TagData>> playerTags = new HashMap<>();
    
    /**
     * Set a tag for a player.
     * 
     * @param player The player to set the tag for
     * @param tagId Unique identifier for this tag (e.g., "afk")
     * @param prefix The prefix to add before the player's name
     * @param suffix The suffix to add after the player's name
     * @param priority Higher priority tags appear first
     */
    public void setTag(Player player, String tagId, String prefix, String suffix, int priority) {
        UUID playerId = player.getUniqueId();
        
        playerTags.computeIfAbsent(playerId, k -> new HashMap<>())
                  .put(tagId, new TagData(prefix, suffix, priority));
        
        updateDisplayName(player);
    }
    
    /**
     * Remove a tag from a player.
     * 
     * @param player The player to remove the tag from
     * @param tagId The tag identifier to remove
     */
    public void removeTag(Player player, String tagId) {
        UUID playerId = player.getUniqueId();
        
        Map<String, TagData> tags = playerTags.get(playerId);
        if (tags != null) {
            tags.remove(tagId);
            if (tags.isEmpty()) {
                playerTags.remove(playerId);
            }
        }
        
        updateDisplayName(player);
    }
    
    /**
     * Check if a player has a specific tag.
     * 
     * @param playerId The player's UUID
     * @param tagId The tag identifier to check
     * @return true if the player has the tag
     */
    public boolean hasTag(UUID playerId, String tagId) {
        Map<String, TagData> tags = playerTags.get(playerId);
        return tags != null && tags.containsKey(tagId);
    }
    
    /**
     * Clear all tags for a player (called on quit).
     * 
     * @param playerId The player's UUID
     */
    public void clearPlayer(UUID playerId) {
        playerTags.remove(playerId);
    }
    
    /**
     * Reset a player's display name to their original name.
     * Called when a player has no tags or on cleanup.
     * 
     * @param player The player to reset
     */
    public void resetDisplayName(Player player) {
        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());
    }
    
    /**
     * Rebuild and apply the player's display name based on all active tags.
     * Tags are sorted by priority (higher priority = appears first).
     * 
     * @param player The player to update
     */
    private void updateDisplayName(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, TagData> tags = playerTags.get(playerId);
        
        if (tags == null || tags.isEmpty()) {
            // No tags, reset to original name
            resetDisplayName(player);
            return;
        }
        
        // Sort tags by priority (higher first)
        StringBuilder prefixBuilder = new StringBuilder();
        StringBuilder suffixBuilder = new StringBuilder();
        
        tags.values().stream()
            .sorted(Comparator.comparingInt(TagData::priority).reversed())
            .forEach(tag -> {
                prefixBuilder.append(tag.prefix());
                suffixBuilder.append(tag.suffix());
            });
        
        String newName = prefixBuilder.toString() + player.getName() + suffixBuilder.toString();
        
        player.setDisplayName(newName);
        player.setPlayerListName(newName);
    }
    
    /**
     * Clean up all player data.
     * Called on plugin disable to reset all player names.
     */
    public void cleanup() {
        playerTags.clear();
    }
}
