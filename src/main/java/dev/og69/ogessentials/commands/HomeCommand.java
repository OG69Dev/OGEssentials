package dev.og69.ogessentials.commands;

import dev.og69.ogessentials.OGEssentials;
import dev.og69.ogessentials.managers.HomeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Command to teleport to a home.
 * 
 * Usage: /home [name]
 *        /home [playername]:[homename] - Teleport to another player's home
 *        /home [playername]: - List another player's homes
 * Permission: ogessentials.home
 *             ogessentials.home.others - Access other players' homes
 */
public class HomeCommand implements CommandExecutor, TabCompleter {
    
    private final OGEssentials plugin;
    
    public HomeCommand(OGEssentials plugin) {
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
        
        // Get home name (default: "home")
        String input = args.length > 0 ? String.join(" ", args) : "home";
        
        // Check if the input contains a colon (other player's home)
        if (input.contains(":")) {
            String[] parts = input.split(":", 2);
            String targetPlayerName = parts[0].trim();
            String homeName = parts.length > 1 ? parts[1].trim() : "";
            
            // Check permission for accessing other players' homes
            if (!player.hasPermission("ogessentials.home.others")) {
                player.sendMessage(prefix + ChatColor.RED + "You don't have permission to access other players homes.");
                return true;
            }
            
            // Get the target player's UUID
            @SuppressWarnings("deprecation")
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName);
            
            if (targetPlayer == null || (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline())) {
                player.sendMessage(prefix + ChatColor.RED + "Player '" + ChatColor.WHITE + targetPlayerName + 
                    ChatColor.RED + "' not found.");
                return true;
            }
            
            UUID targetUUID = targetPlayer.getUniqueId();
            
            // If homeName is empty, list the player's homes
            if (homeName.isEmpty()) {
                List<String> homes = homeManager.getHomeNames(targetUUID);
                
                if (homes.isEmpty()) {
                    player.sendMessage(prefix + ChatColor.RED + targetPlayerName + " has no homes set.");
                    return true;
                }
                
                String homeList = String.join(", ", homes);
                player.sendMessage(prefix + ChatColor.GREEN + targetPlayerName + "'s Homes: " + ChatColor.WHITE + homeList);
                return true;
            }
            
            // Teleport to the target player's home
            Location home = homeManager.getHome(targetUUID, homeName.toLowerCase());
            
            if (home == null) {
                player.sendMessage(prefix + ChatColor.RED + targetPlayerName + " doesn't have a home named '" + 
                    ChatColor.WHITE + homeName + ChatColor.RED + "'.");
                return true;
            }
            
            player.teleport(home);
            player.sendMessage(prefix + ChatColor.GREEN + "Teleported to " + targetPlayerName + "'s home '" + 
                ChatColor.WHITE + homeName + ChatColor.GREEN + "'.");
            
            return true;
        }
        
        // Regular home teleport (own homes)
        String name = input.toLowerCase();
        Location home = homeManager.getHome(player.getUniqueId(), name);
        
        if (home == null) {
            player.sendMessage(prefix + ChatColor.RED + "Home '" + ChatColor.WHITE + name + 
                ChatColor.RED + "' does not exist.");
            return true;
        }
        
        // Teleport player
        player.teleport(home);
        player.sendMessage(prefix + ChatColor.GREEN + "Teleported to home '" + ChatColor.WHITE + name + 
            ChatColor.GREEN + "'.");
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1 && sender instanceof Player player) {
            HomeManager homeManager = plugin.getHomeManager();
            if (homeManager != null) {
                String input = args[0].toLowerCase();
                
                // Tab complete own homes
                for (String homeName : homeManager.getHomeNames(player.getUniqueId())) {
                    if (homeName.toLowerCase().startsWith(input)) {
                        completions.add(homeName);
                    }
                }
                
                // If player has permission, suggest player names with colon
                if (player.hasPermission("ogessentials.home.others")) {
                    // Add online players with colon
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        String suggestion = onlinePlayer.getName() + ":";
                        if (suggestion.toLowerCase().startsWith(input)) {
                            completions.add(suggestion);
                        }
                    }
                }
            }
        }
        
        return completions;
    }
    
    private String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("prefix", "§7[§bOGSMP§7] §r"));
    }
}
