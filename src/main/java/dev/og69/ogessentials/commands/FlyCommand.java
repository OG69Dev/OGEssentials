package dev.og69.ogessentials.commands;

import dev.og69.ogessentials.OGEssentials;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to toggle flight mode for players.
 * 
 * Usage: /fly [player]
 * Permission: ogessentials.fly
 *             ogessentials.fly.others
 */
public class FlyCommand implements CommandExecutor {

    private final OGEssentials plugin;

    public FlyCommand(OGEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Toggle self
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Usage: /fly <player>");
                return true;
            }

            if (!player.hasPermission("ogessentials.fly")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            toggleFlight(player, sender);
            return true;
        }

        // Toggle others
        if (!sender.hasPermission("ogessentials.fly.others")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to toggle flight for others.");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(getPrefix() + ChatColor.RED + "Player not found.");
            return true;
        }

        toggleFlight(target, sender);
        return true;
    }

    private void toggleFlight(Player target, CommandSender sender) {
        boolean isFlying = target.getAllowFlight();
        target.setAllowFlight(!isFlying);
        
        // If disabling flight, also stop them from currently flying to prevent fall damage?
        // Usually safe to just unset allow flight, they will fall. 
        // Some plugins handle fall damage protection, but basic toggle is requested.
        
        String status = !isFlying ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled";
        String msg = getPrefix() + ChatColor.GREEN + "Flight mode " + status + ChatColor.GREEN + ".";

        if (target.equals(sender)) {
            sender.sendMessage(msg);
        } else {
            sender.sendMessage(msg);
            target.sendMessage(msg);
        }
    }

    private String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("prefix", "&7[&bOGSMP&7] &r"));
    }
}
