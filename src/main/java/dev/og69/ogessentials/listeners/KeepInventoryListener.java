package dev.og69.ogessentials.listeners;

import dev.og69.ogessentials.OGEssentials;
import dev.og69.ogessentials.managers.KeepInventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.GameRule;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listens for damage and death events to implement keep inventory functionality.
 * 
 * Tracks the last player who damaged each player, and on death checks if
 * either the victim or killer has keep inventory enabled.
 */
public class KeepInventoryListener implements Listener {
    
    private final OGEssentials plugin;
    private final KeepInventoryManager manager;
    
    // Track last attacker for each player (victim UUID -> attacker UUID)
    private final Map<UUID, UUID> lastAttacker = new HashMap<>();
    
    /**
     * Create a new keep inventory listener.
     * 
     * @param plugin The plugin instance
     * @param manager The keep inventory manager
     */
    public KeepInventoryListener(OGEssentials plugin, KeepInventoryManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }
    
    /**
     * Track when a player damages another player.
     * This is used to determine the "killer" for the killer protection feature.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Only track player-on-player damage
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        
        // Get the actual damager (handle projectiles, etc.)
        Player attacker = getPlayerAttacker(event);
        if (attacker == null) {
            return;
        }
        
        // Don't track self-damage
        if (victim.getUniqueId().equals(attacker.getUniqueId())) {
            return;
        }
        
        // Store the attacker
        lastAttacker.put(victim.getUniqueId(), attacker.getUniqueId());
    }
    
    /**
     * Handle player death to apply keep inventory if enabled.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        UUID victimId = victim.getUniqueId();
        
        // Check if server keepInventory gamerule is enabled
        // If it is, let vanilla handle it and don't interfere
        Boolean keepInventoryRule = victim.getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY);
        if (keepInventoryRule != null && keepInventoryRule) {
            return;
        }
        
        // Check if victim has keep inventory enabled
        boolean victimHasKeepInv = manager.isEnabled(victimId);
        
        // Check if killer has keep inventory enabled
        boolean killerHasKeepInv = false;
        UUID killerId = lastAttacker.get(victimId);
        Player killer = null;
        
        if (killerId != null) {
            killer = Bukkit.getPlayer(killerId);
            if (killer != null && killer.isOnline()) {
                killerHasKeepInv = manager.isEnabled(killerId);
            }
        }
        
        // Apply keep inventory if either has it enabled
        if (victimHasKeepInv || killerHasKeepInv) {
            // Keep inventory and experience
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            
            // Clear drops since we're keeping them
            event.getDrops().clear();
            event.setDroppedExp(0);
            
            // Send appropriate messages
            String prefix = getPrefix();
            
            if (victimHasKeepInv) {
                victim.sendMessage(prefix + ChatColor.GREEN + "You kept your inventory because Keep Inventory mode is enabled.");
            } else {
                // Killer's keep inventory protected the victim
                victim.sendMessage(prefix + ChatColor.GREEN + "Because your killer has Keep Inventory enabled, you kept your items.");
                if (killer != null) {
                    killer.sendMessage(prefix + ChatColor.GREEN + "Your Keep Inventory mode protected your victim's items.");
                }
            }
        }
        
        // Clear the last attacker entry
        lastAttacker.remove(victimId);
    }
    
    /**
     * Get the player attacker from a damage event, handling projectiles and other indirect damage.
     */
    private Player getPlayerAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            return (Player) event.getDamager();
        }
        
        // Handle projectiles (arrows, tridents, etc.)
        if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }
        
        // Handle tamed animals
        if (event.getDamager() instanceof org.bukkit.entity.Tameable tameable) {
            if (tameable.getOwner() instanceof Player) {
                return (Player) tameable.getOwner();
            }
        }
        
        return null;
    }
    
    /**
     * Get the configured message prefix.
     */
    private String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("prefix", "&7[&bOGSMP&7] &r"));
    }
    
    /**
     * Clean up data.
     */
    public void cleanup() {
        lastAttacker.clear();
    }
}
