package dev.og69.ogessentials.commands;

import dev.og69.ogessentials.OGEssentials;
import dev.og69.ogessentials.managers.AfkManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to manually toggle AFK status.
 * 
 * Usage: /afk
 * Permission: ogessentials.afk
 */
public class AfkCommand implements CommandExecutor {
    
    private final OGEssentials plugin;
    
    /**
     * Create a new AFK command handler.
     * 
     * @param plugin The plugin instance
     */
    public AfkCommand(OGEssentials plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only players can use this command
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        // Toggle AFK status
        AfkManager afkManager = plugin.getAfkManager();
        if (afkManager != null) {
            afkManager.toggleAfk(player);
        } else {
            player.sendMessage(ChatColor.RED + "AFK system is not available.");
        }
        
        return true;
    }
}
