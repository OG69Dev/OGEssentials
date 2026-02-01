package dev.og69.ogessentials.commands;

import dev.og69.ogessentials.OGEssentials;
import dev.og69.ogessentials.managers.KeepInventoryManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to toggle keep inventory mode.
 * 
 * Usage: /keepinventory
 * Permission: ogessentials.keepinventory
 * Aliases: /keepinv, /ki
 */
public class KeepInventoryCommand implements CommandExecutor {
    
    private final OGEssentials plugin;
    
    /**
     * Create a new keep inventory command handler.
     * 
     * @param plugin The plugin instance
     */
    public KeepInventoryCommand(OGEssentials plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only players can use this command
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        // Get the manager
        KeepInventoryManager manager = plugin.getKeepInventoryManager();
        if (manager == null) {
            player.sendMessage(ChatColor.RED + "Keep Inventory system is not available.");
            return true;
        }
        
        // Toggle the setting
        boolean nowEnabled = manager.toggle(player.getUniqueId());
        
        // Send message
        String prefix = ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("prefix", "&7[&bOGSMP&7] &r"));
        
        if (nowEnabled) {
            player.sendMessage(prefix + ChatColor.GREEN + "Keep Inventory mode enabled.");
        } else {
            player.sendMessage(prefix + ChatColor.RED + "Keep Inventory mode disabled.");
        }
        
        return true;
    }
}
