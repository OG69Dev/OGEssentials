package dev.og69.ogessentials.managers;

import dev.og69.ogessentials.OGEssentials;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Manages TPA (Teleport Ask) requests between players.
 * 
 * Tracks pending requests with automatic expiry.
 */
public class TpaManager {
    
    private final OGEssentials plugin;
    
    // Tracks: target player UUID -> TPA request
    private final Map<UUID, TpaRequest> pendingRequests = new HashMap<>();
    
    /**
     * Represents a TPA request.
     */
    public record TpaRequest(UUID requesterId, long timestamp) {}
    
    /**
     * Create a new TPA manager.
     * 
     * @param plugin The plugin instance
     */
    public TpaManager(OGEssentials plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Send a TPA request from one player to another.
     * 
     * @param requester The player requesting teleport
     * @param target The target player
     * @return true if request was sent successfully
     */
    public boolean sendRequest(Player requester, Player target) {
        UUID targetId = target.getUniqueId();
        UUID requesterId = requester.getUniqueId();
        
        // Can't TPA to yourself
        if (requesterId.equals(targetId)) {
            return false;
        }
        
        // Check if target already has a pending request
        if (pendingRequests.containsKey(targetId)) {
            return false;
        }
        
        // Create the request
        pendingRequests.put(targetId, new TpaRequest(requesterId, System.currentTimeMillis()));
        return true;
    }
    
    /**
     * Check if a player has a pending TPA request.
     * 
     * @param targetId The target player's UUID
     * @return true if there's a pending request
     */
    public boolean hasRequest(UUID targetId) {
        return pendingRequests.containsKey(targetId);
    }
    
    /**
     * Get the pending TPA request for a player.
     * 
     * @param targetId The target player's UUID
     * @return The TPA request, or null if none
     */
    public TpaRequest getRequest(UUID targetId) {
        return pendingRequests.get(targetId);
    }
    
    /**
     * Accept a TPA request and teleport the requester.
     * 
     * @param target The target player accepting the request
     * @return The requester player, or null if no request or requester offline
     */
    public Player acceptRequest(Player target) {
        TpaRequest request = pendingRequests.remove(target.getUniqueId());
        if (request == null) {
            return null;
        }
        
        Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester != null && requester.isOnline()) {
            requester.teleport(target.getLocation());
            return requester;
        }
        
        return null;
    }
    
    /**
     * Deny a TPA request.
     * 
     * @param target The target player denying the request
     * @return The requester player, or null if no request or requester offline
     */
    public Player denyRequest(Player target) {
        TpaRequest request = pendingRequests.remove(target.getUniqueId());
        if (request == null) {
            return null;
        }
        
        return Bukkit.getPlayer(request.requesterId());
    }
    
    /**
     * Check and expire old TPA requests.
     * Called by scheduled task.
     */
    public void expireOldRequests() {
        long now = System.currentTimeMillis();
        long expiryMillis = getExpiryMillis();
        String prefix = getPrefix();
        
        Iterator<Map.Entry<UUID, TpaRequest>> iterator = pendingRequests.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TpaRequest> entry = iterator.next();
            TpaRequest request = entry.getValue();
            
            if (now - request.timestamp() > expiryMillis) {
                // Request expired
                UUID targetId = entry.getKey();
                UUID requesterId = request.requesterId();
                
                // Notify both players
                Player requester = Bukkit.getPlayer(requesterId);
                Player target = Bukkit.getPlayer(targetId);
                
                if (requester != null && requester.isOnline() && target != null) {
                    requester.sendMessage(prefix + ChatColor.RED + "Your TPA request to " + 
                        target.getName() + " has expired.");
                }
                
                if (target != null && target.isOnline() && requester != null) {
                    target.sendMessage(prefix + ChatColor.RED + "The TPA request from " + 
                        requester.getName() + " has expired.");
                }
                
                iterator.remove();
            }
        }
    }
    
    /**
     * Cancel all requests involving a player (on quit).
     * 
     * @param playerId The player's UUID
     */
    public void cancelPlayerRequests(UUID playerId) {
        // Remove if player is a target
        pendingRequests.remove(playerId);
        
        // Remove if player is a requester
        pendingRequests.entrySet().removeIf(entry -> 
            entry.getValue().requesterId().equals(playerId));
    }
    
    /**
     * Get the TPA request expiry time in milliseconds.
     * 
     * @return Expiry time in milliseconds
     */
    public long getExpiryMillis() {
        int expirySeconds = plugin.getConfig().getInt("tpa.expiry", 60);
        return expirySeconds * 1000L;
    }
    
    /**
     * Clean up all data (called on plugin disable).
     */
    public void cleanup() {
        pendingRequests.clear();
    }
    
    private String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("prefix", "&7[&bOGSMP&7] &r"));
    }
}
