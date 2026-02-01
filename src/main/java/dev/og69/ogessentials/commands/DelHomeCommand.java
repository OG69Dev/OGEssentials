package dev.og69.ogessentials.commands;

import dev.og69.ogessentials.OGEssentials;
import dev.og69.ogessentials.managers.HomeManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to delete a home.
 * 
 * Usage: /delhome <name>
 * Permission: ogessentials.home
 */
public class DelHomeCommand implements CommandExecutor, TabCompleter {
    
    private final OGEssentials plugin;
    
    public DelHomeCommand(OGEssentials plugin) {
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
        
        // Require home name
        if (args.length == 0) {
            player.sendMessage(prefix + ChatColor.RED + "Usage: " + ChatColor.WHITE + "/delhome <name>");
            return true;
        }
        
        String name = String.join(" ", args).toLowerCase();
        
        // Check if home exists
        if (!homeManager.homeExists(player.getUniqueId(), name)) {
            player.sendMessage(prefix + ChatColor.RED + "Home '" + ChatColor.WHITE + name + 
                ChatColor.RED + "' does not exist.");
            return true;
        }
        
        // Delete the home
        if (homeManager.deleteHome(player.getUniqueId(), name)) {
            player.sendMessage(prefix + ChatColor.GREEN + "Deleted home '" + ChatColor.WHITE + name + 
                ChatColor.GREEN + "'.");
        } else {
            player.sendMessage(prefix + ChatColor.RED + "Failed to delete home.");
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1 && sender instanceof Player player) {
            HomeManager homeManager = plugin.getHomeManager();
            if (homeManager != null) {
                String input = args[0].toLowerCase();
                for (String homeName : homeManager.getHomeNames(player.getUniqueId())) {
                    if (homeName.toLowerCase().startsWith(input)) {
                        completions.add(homeName);
                    }
                }
            }
        }
        
        return completions;
    }
    
    private String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("prefix", "&7[&bOGSMP&7] &r"));
    }
}
