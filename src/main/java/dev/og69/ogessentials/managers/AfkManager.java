package dev.og69.ogessentials.managers;

import dev.og69.ogessentials.OGEssentials;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages AFK (Away From Keyboard) status for players.
 * 
 * Tracks player activity and automatically marks players as AFK
 * after a configurable timeout period. Uses NameTagManager to
 * display AFK status in player name tags.
 */
public class AfkManager {
    
    private static final int AFK_TAG_PRIORITY = 100;
    private static final String AFK_TAG_ID = "afk";
    
    private final OGEssentials plugin;
    private final NameTagManager nameTagManager;
    
    // Track last activity time per player
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    
    // Track which players are currently AFK
    private final Set<UUID> afkPlayers = ConcurrentHashMap.newKeySet();
    
    /**
     * Create a new AFK manager.
     * 
     * @param plugin The plugin instance
     * @param nameTagManager The name tag manager to use for display
     */
    public AfkManager(OGEssentials plugin, NameTagManager nameTagManager) {
        this.plugin = plugin;
        this.nameTagManager = nameTagManager;
    }
    
    /**
     * Update a player's last activity time.
     * If the player was AFK, they will be marked as no longer AFK.
     * 
     * @param player The player who performed an action
     */
    public void updateActivity(Player player) {
        UUID playerId = player.getUniqueId();
        lastActivity.put(playerId, System.currentTimeMillis());
        
        // If player was AFK, mark them as no longer AFK
        if (afkPlayers.contains(playerId)) {
            setAfk(player, false);
        }
    }
    
    /**
     * Initialize a player's activity tracking (called on join).
     * 
     * @param player The player who joined
     */
    public void initializePlayer(Player player) {
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
        nameTagManager.initializePlayer(player);
    }
    
    /**
     * Clean up a player's data (called on quit).
     * 
     * @param player The player who quit
     */
    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        lastActivity.remove(playerId);
        afkPlayers.remove(playerId);
        nameTagManager.cleanupPlayer(player);
    }
    
    /**
     * Check if a player is AFK.
     * 
     * @param playerId The player's UUID
     * @return true if the player is AFK
     */
    public boolean isAfk(UUID playerId) {
        return afkPlayers.contains(playerId);
    }
    
    /**
     * Get a player's last activity time.
     * 
     * @param playerId The player's UUID
     * @return The timestamp of last activity, or 0 if not tracked
     */
    public long getLastActivity(UUID playerId) {
        return lastActivity.getOrDefault(playerId, 0L);
    }
    
    /**
     * Set a player's AFK status.
     * 
     * @param player The player
     * @param afk true to mark as AFK, false to mark as active
     */
    public void setAfk(Player player, boolean afk) {
        UUID playerId = player.getUniqueId();
        
        if (afk && !afkPlayers.contains(playerId)) {
            // Mark as AFK
            afkPlayers.add(playerId);
            nameTagManager.setTag(player, AFK_TAG_ID, 
                ChatColor.GRAY + "[AFK] " + ChatColor.RESET, "", AFK_TAG_PRIORITY);
            broadcastAfkMessage(player, true);
        } else if (!afk && afkPlayers.contains(playerId)) {
            // Mark as no longer AFK
            afkPlayers.remove(playerId);
            nameTagManager.removeTag(player, AFK_TAG_ID);
            broadcastAfkMessage(player, false);
        }
    }
    
    /**
     * Toggle a player's AFK status (for manual /afk command).
     * 
     * @param player The player
     */
    public void toggleAfk(Player player) {
        boolean isCurrentlyAfk = afkPlayers.contains(player.getUniqueId());
        setAfk(player, !isCurrentlyAfk);
        
        // Reset activity time if manually going AFK
        if (!isCurrentlyAfk) {
            lastActivity.put(player.getUniqueId(), 0L);
        }
    }
    
    /**
     * Broadcast an AFK status change message to all players.
     * 
     * @param player The player whose status changed
     * @param isAfk true if the player is now AFK
     */
    private void broadcastAfkMessage(Player player, boolean isAfk) {
        String prefix = ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfig().getString("prefix", "&7[&bOGSMP&7] &r"));
        String messageColor = ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("afk.message-color", "&a"));
        
        String message;
        if (isAfk) {
            message = prefix + ChatColor.YELLOW + player.getName() + ChatColor.GRAY + " is now AFK.";
        } else {
            message = prefix + messageColor + player.getName() + ChatColor.GRAY + " is no longer AFK.";
        }
        
        Bukkit.broadcastMessage(message);
    }
    
    /**
     * Get the configured AFK timeout in milliseconds.
     * 
     * @return The timeout in milliseconds
     */
    public long getAfkTimeoutMillis() {
        int timeoutSeconds = plugin.getConfig().getInt("afk.timeout", 300);
        return timeoutSeconds * 1000L;
    }
    
    /**
     * Clean up all data (called on plugin disable).
     */
    public void cleanup() {
        // Reset all player names
        for (UUID playerId : afkPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                nameTagManager.resetDisplayName(player);
            }
        }
        
        lastActivity.clear();
        afkPlayers.clear();
    }
}
