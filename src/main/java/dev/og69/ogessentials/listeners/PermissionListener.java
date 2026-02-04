package dev.og69.ogessentials.listeners;

import dev.og69.ogessentials.managers.PermissionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener to apply permissions on player join and cleanup on quit.
 */
public class PermissionListener implements Listener {

    private final PermissionManager permissionManager;

    public PermissionListener(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load and apply permissions
        permissionManager.loadPlayer(event.getPlayer().getUniqueId());
        permissionManager.applyPermissions(event.getPlayer());
        permissionManager.updatePlayerNameTag(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        permissionManager.cleanupPlayer(event.getPlayer());
    }
}
