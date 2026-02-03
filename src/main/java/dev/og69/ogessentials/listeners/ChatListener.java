package dev.og69.ogessentials.listeners;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Listens for player chat events to translate color codes.
 */
public class ChatListener implements Listener {

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.getPlayer().hasPermission("ogessentials.chat.color")) {
            String message = event.getMessage();
            event.setMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }
}
