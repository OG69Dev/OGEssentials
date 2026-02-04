package dev.og69.ogessentials.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

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
 * 
 * Tags are shown:
 * - In chat (via displayName)
 * - In tab list (via playerListName)
 * - Above player heads (via scoreboard teams)
 */
public class NameTagManager {
    
    private static final String TEAM_PREFIX = "og_";
    
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
        updateScoreboardTeam(player);
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
        updateScoreboardTeam(player);
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
     * Initialize a player's scoreboard team on join.
     * 
     * @param player The player who joined
     */
    public void initializePlayer(Player player) {
        // Ensure team exists and player is added
        getOrCreateTeam(player);
        updateScoreboardTeam(player);
    }
    
    /**
     * Clean up a player's scoreboard team on quit.
     * 
     * @param player The player who quit
     */
    public void cleanupPlayer(Player player) {
        clearPlayer(player.getUniqueId());
        
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = getTeamName(player);
        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            team.unregister();
        }
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
        
        String newName = ChatColor.translateAlternateColorCodes('&', prefixBuilder.toString() + player.getName() + suffixBuilder.toString());
        
        player.setDisplayName(newName);
        player.setPlayerListName(newName);
    }
    
    /**
     * Update the scoreboard team for a player to show tags above their head.
     * 
     * @param player The player to update
     */
    private void updateScoreboardTeam(Player player) {
        Team team = getOrCreateTeam(player);
        
        UUID playerId = player.getUniqueId();
        Map<String, TagData> tags = playerTags.get(playerId);
        
        if (tags == null || tags.isEmpty()) {
            // No tags, clear prefix/suffix
            team.setPrefix("");
            team.setSuffix("");
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
        
        team.setPrefix(ChatColor.translateAlternateColorCodes('&', prefixBuilder.toString()));
        team.setSuffix(ChatColor.translateAlternateColorCodes('&', suffixBuilder.toString()));
    }
    
    /**
     * Get or create the scoreboard team for a player.
     * 
     * @param player The player
     * @return The team for this player
     */
    private Team getOrCreateTeam(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = getTeamName(player);
        
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        
        // Ensure player is on their team
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
        
        return team;
    }
    
    /**
     * Get the team name for a player.
     * Uses a prefix to avoid conflicts with other plugins.
     * 
     * @param player The player
     * @return The team name
     */
    private String getTeamName(Player player) {
        // Team names are limited to 16 characters in older versions
        // Use first 13 chars of UUID to ensure uniqueness within limit
        String uuidPart = player.getUniqueId().toString().replace("-", "").substring(0, 10);
        return TEAM_PREFIX + uuidPart;
    }
    
    /**
     * Clean up all player data.
     * Called on plugin disable to reset all player names.
     */
    public void cleanup() {
        // Reset all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            resetDisplayName(player);
            
            // Remove team
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            String teamName = getTeamName(player);
            Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                team.unregister();
            }
        }
        
        playerTags.clear();
    }
}
