package dev.og69.ogessentials.listeners;

import dev.og69.ogessentials.managers.AfkManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for player activity events to track AFK status.
 * 
 * Updates last activity time on movement and handles
 * player join/quit for initialization and cleanup.
 */
public class AfkListener implements Listener {
    
    private final AfkManager afkManager;
    
    /**
     * Create a new AFK listener.
     * 
     * @param afkManager The AFK manager to update
     */
    public AfkListener(AfkManager afkManager) {
        this.afkManager = afkManager;
    }
    
    /**
     * Track player movement to update activity time.
     * Only triggers on actual position change (not just head rotation).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        
        // Only count actual movement, not just head rotation
        if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
            afkManager.updateActivity(event.getPlayer());
        }
    }
    
    /**
     * Initialize player activity tracking on join.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        afkManager.initializePlayer(event.getPlayer());
    }
    
    /**
     * Clean up player data on quit.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        afkManager.cleanupPlayer(event.getPlayer());
    }
}
