package dev.og69.ogessentials.listeners;

import dev.og69.ogessentials.managers.BackManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Listens for teleport and death events to save the player's previous location.
 */
public class TeleportListener implements Listener {

    private final BackManager backManager;

    public TeleportListener(BackManager backManager) {
        this.backManager = backManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Save the location the player teleported FROM
        backManager.setLastLocation(event.getPlayer().getUniqueId(), event.getFrom());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Save the location where the player died
        backManager.setLastLocation(event.getEntity().getUniqueId(), event.getEntity().getLocation());
    }
}
