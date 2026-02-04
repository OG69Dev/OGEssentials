package dev.og69.ogessentials.listeners;

import dev.og69.ogessentials.OGEssentials;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

/**
 * Listens for player commands and blocks disabled commands.
 * Uses the command-blocker.blocked-commands config section.
 * Handles namespaced commands (e.g., minecraft:gamemode).
 */
public class DisabledCommandsListener implements Listener {

    private final OGEssentials plugin;

    public DisabledCommandsListener(OGEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) {
            return;
        }

        String message = event.getMessage();
        
        // Extract the command (without leading /)
        if (!message.startsWith("/")) {
            return;
        }
        
        String fullCommand = message.substring(1).split(" ")[0].toLowerCase();
        
        // Strip namespace prefix (minecraft:, bukkit:, etc.)
        String baseCommand = fullCommand;
        if (fullCommand.contains(":")) {
            baseCommand = fullCommand.substring(fullCommand.indexOf(":") + 1);
        }
        
        // Get blocked commands from config
        List<String> blockedCommands = plugin.getConfig().getStringList("command-blocker.blocked-commands");
        
        // Check if command is blocked (case-insensitive)
        boolean isBlocked = false;
        for (String blocked : blockedCommands) {
            if (blocked.equalsIgnoreCase(baseCommand) || blocked.equalsIgnoreCase(fullCommand)) {
                isBlocked = true;
                break;
            }
        }
        
        if (isBlocked) {
            event.setCancelled(true);
            String prefix = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("prefix", "&7[&bOGSMP&7] &r"));
            event.getPlayer().sendMessage(prefix + ChatColor.RED + "❌ This command (" + 
                ChatColor.DARK_RED + "/" + baseCommand + ChatColor.RED + ") is disabled on this server.");
        }
    }
}
