package dev.og69.ogessentials.commands;

import dev.og69.ogessentials.OGEssentials;
import dev.og69.ogessentials.managers.TpaManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to accept a pending TPA request.
 * 
 * Usage: /tpaccept
 * Permission: ogessentials.tpa
 */
public class TpAcceptCommand implements CommandExecutor {
    
    private final OGEssentials plugin;
    
    public TpAcceptCommand(OGEssentials plugin) {
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
        
        // Check if player has a pending request
        if (!tpaManager.hasRequest(player.getUniqueId())) {
            player.sendMessage(prefix + ChatColor.RED + "You have no pending TPA requests.");
            return true;
        }
        
        // Accept the request (teleports requester to player)
        Player requester = tpaManager.acceptRequest(player);
        
        if (requester != null) {
            player.sendMessage(prefix + ChatColor.GREEN + "You accepted the teleport request from " + 
                ChatColor.WHITE + requester.getName() + ChatColor.GREEN + ".");
            requester.sendMessage(prefix + ChatColor.GREEN + player.getName() + 
                " accepted your TPA request!");
        } else {
            player.sendMessage(prefix + ChatColor.RED + "The requester is no longer online.");
        }
        
        return true;
    }
    
    private String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("prefix", "&7[&bOGSMP&7] &r"));
    }
}
