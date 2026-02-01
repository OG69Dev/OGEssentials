package dev.og69.ogessentials.commands;

import dev.og69.ogessentials.OGEssentials;
import dev.og69.ogessentials.managers.TpaManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to request teleport to another player.
 * 
 * Usage: /tpa <player>
 * Permission: ogessentials.tpa
 */
public class TpaCommand implements CommandExecutor, TabCompleter {
    
    private final OGEssentials plugin;
    
    public TpaCommand(OGEssentials plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        TpaManager tpaManager = plugin.getTpaManager();
        if (tpaManager == null) {
            player.sendMessage(ChatColor.RED + "TPA system is not available.");
            return true;
        }
        
        String prefix = getPrefix();
        
        // Require target player
        if (args.length == 0) {
            player.sendMessage(prefix + ChatColor.RED + "Usage: " + ChatColor.WHITE + "/tpa <player>");
            return true;
        }
        
        // Find target player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(prefix + ChatColor.RED + "Player not found.");
            return true;
        }
        
        // Can't TPA to yourself
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(prefix + ChatColor.RED + "You can't send a TPA request to yourself!");
            return true;
        }
        
        // Check if target already has a pending request
        if (tpaManager.hasRequest(target.getUniqueId())) {
            player.sendMessage(prefix + ChatColor.RED + "That player already has a pending TPA request!");
            return true;
        }
        
        // Send the request
        if (tpaManager.sendRequest(player, target)) {
            // Get expiry time for message
            int expirySeconds = plugin.getConfig().getInt("tpa.expiry", 60);
            
            player.sendMessage(prefix + ChatColor.GREEN + "TPA request sent to " + ChatColor.WHITE + 
                target.getName() + ChatColor.GREEN + "! They have " + expirySeconds + " seconds to accept.");
            
            target.sendMessage(prefix + ChatColor.YELLOW + player.getName() + ChatColor.GREEN + 
                " wants to teleport to you. Type " + ChatColor.YELLOW + "/tpaccept" + 
                ChatColor.GREEN + " or " + ChatColor.RED + "/tpdeny");
            target.sendMessage(prefix + ChatColor.GRAY + "This request will expire in " + 
                expirySeconds + " seconds.");
        } else {
            player.sendMessage(prefix + ChatColor.RED + "Failed to send TPA request.");
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1 && sender instanceof Player player) {
            String input = args[0].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                // Don't suggest self
                if (!online.getUniqueId().equals(player.getUniqueId())) {
                    if (online.getName().toLowerCase().startsWith(input)) {
                        completions.add(online.getName());
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
