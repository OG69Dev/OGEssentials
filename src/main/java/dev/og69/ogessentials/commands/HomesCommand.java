package dev.og69.ogessentials.commands;

import dev.og69.ogessentials.OGEssentials;
import dev.og69.ogessentials.managers.HomeManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Command to list all homes.
 * 
 * Usage: /homes
 * Permission: ogessentials.home
 */
public class HomesCommand implements CommandExecutor {
    
    private final OGEssentials plugin;
    
    public HomesCommand(OGEssentials plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        HomeManager homeManager = plugin.getHomeManager();
        if (homeManager == null) {
            player.sendMessage(ChatColor.RED + "Home system is not available.");
            return true;
        }
        
        String prefix = getPrefix();
        
        // Get all home names
        List<String> homes = homeManager.getHomeNames(player.getUniqueId());
        
        if (homes.isEmpty()) {
            player.sendMessage(prefix + ChatColor.RED + "You have no homes set.");
            return true;
        }
        
        // Format as comma-separated list (matching script behavior)
        String homeList = String.join(", ", homes);
        player.sendMessage(prefix + ChatColor.GREEN + "Your Homes: " + ChatColor.WHITE + homeList);
        
        return true;
    }
    
    private String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("prefix", "&7[&bOGSMP&7] &r"));
    }
}
