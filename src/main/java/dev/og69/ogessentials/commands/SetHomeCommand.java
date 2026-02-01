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
 * Command to set a home at the player's current location.
 * 
 * Usage: /sethome [name]
 * Permission: ogessentials.home
 */
public class SetHomeCommand implements CommandExecutor, TabCompleter {
    
    private final OGEssentials plugin;
    
    public SetHomeCommand(OGEssentials plugin) {
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
        
        // Get home name (default: "home")
        String name = args.length > 0 ? String.join(" ", args).toLowerCase() : "home";
        
        String prefix = getPrefix();
        
        // Check if at max homes (only if creating new home)
        if (!homeManager.homeExists(player.getUniqueId(), name)) {
            int currentCount = homeManager.getHomeCount(player.getUniqueId());
            int maxHomes = homeManager.getMaxHomes();
            
            if (currentCount >= maxHomes) {
                player.sendMessage(prefix + ChatColor.RED + "You have reached the max number of homes (" + 
                    ChatColor.WHITE + maxHomes + ChatColor.RED + ").");
                return true;
            }
        }
        
        // Set the home
        boolean wasUpdate = homeManager.setHome(player.getUniqueId(), name, player.getLocation());
        
        if (wasUpdate) {
            player.sendMessage(prefix + ChatColor.YELLOW + "Home '" + ChatColor.WHITE + name + 
                ChatColor.YELLOW + "' has been updated to your new location.");
        } else {
            player.sendMessage(prefix + ChatColor.GREEN + "Home '" + ChatColor.WHITE + name + 
                ChatColor.GREEN + "' has been set at your current location.");
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
                // Suggest existing home names for updating
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
