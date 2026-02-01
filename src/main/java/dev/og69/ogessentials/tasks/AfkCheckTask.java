package dev.og69.ogessentials.tasks;

import dev.og69.ogessentials.OGEssentials;
import dev.og69.ogessentials.managers.AfkManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Scheduled task that checks for inactive players and marks them as AFK.
 * 
 * Runs every 60 seconds (1200 ticks) and checks if any player has been
 * inactive longer than the configured timeout.
 */
public class AfkCheckTask implements Runnable {
    
    private final AfkManager afkManager;
    private final OGEssentials plugin;
    
    /**
     * Create a new AFK check task.
     * 
     * @param afkManager The AFK manager to use
     * @param plugin The plugin instance
     */
    public AfkCheckTask(AfkManager afkManager, OGEssentials plugin) {
        this.afkManager = afkManager;
        this.plugin = plugin;
    }
    
    @Override
    public void run() {
        long now = System.currentTimeMillis();
        long timeoutMillis = afkManager.getAfkTimeoutMillis();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Skip players already AFK
            if (afkManager.isAfk(player.getUniqueId())) {
                continue;
            }
            
            long lastActivity = afkManager.getLastActivity(player.getUniqueId());
            
            // Check if player has been inactive too long
            if (lastActivity > 0 && (now - lastActivity) > timeoutMillis) {
                afkManager.setAfk(player, true);
            }
        }
    }
}
