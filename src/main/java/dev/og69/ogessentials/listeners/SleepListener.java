package dev.og69.ogessentials.listeners;

import dev.og69.ogessentials.OGEssentials;
import dev.og69.ogessentials.managers.AfkManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Implements the half-sleep system.
 * 
 * Night is skipped when at least half of non-AFK players
 * in a world are sleeping.
 */
public class SleepListener implements Listener {
    
    private final OGEssentials plugin;
    private final AfkManager afkManager;
    
    // Track sleeping players per world
    private final Map<String, Set<UUID>> sleepingPlayers = new HashMap<>();
    
    /**
     * Create a new sleep listener.
     * 
     * @param plugin The plugin instance
     * @param afkManager The AFK manager to check AFK status
     */
    public SleepListener(OGEssentials plugin, AfkManager afkManager) {
        this.plugin = plugin;
        this.afkManager = afkManager;
    }
    
    /**
     * Handle player entering a bed.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        // Only process successful bed entries
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }
        
        Player player = event.getPlayer();
        World world = player.getWorld();
        String worldName = world.getName();
        
        // Add player to sleeping set
        sleepingPlayers.computeIfAbsent(worldName, k -> new HashSet<>())
                       .add(player.getUniqueId());
        
        // Calculate counts
        int nonAfkCount = countNonAfkPlayers(world);
        int sleepingCount = sleepingPlayers.get(worldName).size();
        int needed = (int) Math.ceil(nonAfkCount / 2.0);
        
        // Broadcast sleeping status
        String prefix = getPrefix();
        String messageColor = getMessageColor();
        
        String message = prefix + messageColor + sleepingCount + ChatColor.GRAY + "/" + ChatColor.WHITE + 
                         nonAfkCount + ChatColor.GRAY + " sleeping. Need " + ChatColor.YELLOW + needed + 
                         ChatColor.GRAY + " to skip night.";
        
        broadcastToWorld(world, message);
        
        // Check if enough players are sleeping
        if (sleepingCount >= needed && needed > 0) {
            skipNight(world);
        }
    }
    
    /**
     * Handle player leaving a bed.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBedLeave(PlayerBedLeaveEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        String worldName = world.getName();
        
        // Remove player from sleeping set
        Set<UUID> sleeping = sleepingPlayers.get(worldName);
        if (sleeping != null) {
            sleeping.remove(player.getUniqueId());
            
            // Only broadcast if it's still night and there are still players
            if (world.getTime() >= 12541 && world.getTime() <= 23458) {
                int nonAfkCount = countNonAfkPlayers(world);
                int sleepingCount = sleeping.size();
                int needed = (int) Math.ceil(nonAfkCount / 2.0);
                
                if (nonAfkCount > 0) {
                    String prefix = getPrefix();
                    
                    String message = prefix + ChatColor.RED + player.getName() + ChatColor.GRAY + 
                                     " left bed. " + ChatColor.WHITE + sleepingCount + ChatColor.GRAY + 
                                     "/" + ChatColor.WHITE + nonAfkCount + ChatColor.GRAY + 
                                     " sleeping. Need " + ChatColor.YELLOW + needed + ChatColor.GRAY + ".";
                    
                    broadcastToWorld(world, message);
                }
            }
        }
    }
    
    /**
     * Skip the night in a world.
     */
    private void skipNight(World world) {
        String prefix = getPrefix();
        String messageColor = getMessageColor();
        
        String message = prefix + messageColor + "Enough players are sleeping! Skipping night...";
        broadcastToWorld(world, message);
        
        // Set time to day
        world.setTime(0);
        
        // Clear weather
        world.setStorm(false);
        world.setThundering(false);
        
        // Clear sleeping players for this world
        sleepingPlayers.remove(world.getName());
    }
    
    /**
     * Count non-AFK players in a world.
     */
    private int countNonAfkPlayers(World world) {
        int count = 0;
        for (Player player : world.getPlayers()) {
            if (!afkManager.isAfk(player.getUniqueId())) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Broadcast a message to all players in a world.
     */
    private void broadcastToWorld(World world, String message) {
        for (Player player : world.getPlayers()) {
            player.sendMessage(message);
        }
    }
    
    /**
     * Get the configured message prefix.
     */
    private String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("prefix", "&7[&bOGSMP&7] &r"));
    }
    
    /**
     * Get the configured message color.
     */
    private String getMessageColor() {
        return ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("afk.message-color", "&a"));
    }
    
    /**
     * Clean up sleeping data.
     */
    public void cleanup() {
        sleepingPlayers.clear();
    }
}
