package dev.og69.ogessentials.commands;

import dev.og69.ogessentials.OGEssentials;
import dev.og69.ogessentials.managers.BackManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Command to teleport back to the previous location.
 * 
 * Usage: /back
 * Permission: ogessentials.back
 */
public class BackCommand implements CommandExecutor {

    private final OGEssentials plugin;

    public BackCommand(OGEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("ogessentials.back")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        BackManager backManager = plugin.getBackManager();
        if (backManager == null) {
            player.sendMessage(ChatColor.RED + "Back system is not available.");
            return true;
        }

        Location lastLocation = backManager.getLastLocation(player.getUniqueId());
        if (lastLocation == null) {
            player.sendMessage(getPrefix() + ChatColor.RED + "You have no location to go back to.");
            return true;
        }

        // Teleport the player
        player.teleport(lastLocation);
        player.sendMessage(getPrefix() + ChatColor.GREEN + "Returned to your previous location.");

        return true;
    }

    private String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("prefix", "&7[&bOGSMP&7] &r"));
    }
}
