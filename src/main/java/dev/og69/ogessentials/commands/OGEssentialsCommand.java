package dev.og69.ogessentials.commands;

import dev.og69.ogessentials.OGEssentials;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main command handler for /ogessentials with subcommands.
 */
public class OGEssentialsCommand implements CommandExecutor, TabCompleter {
    
    private final UpdateCheckCommand updateCheckCommand;
    
    public OGEssentialsCommand() {
        this.updateCheckCommand = new UpdateCheckCommand();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§7[§bOG-Essentials§7] Use §e/ogessentials update §7to check for updates.");
            sender.sendMessage("§7[§bOG-Essentials§7] Use §e/ogessentials reload §7to reload config.");
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "update":
                // Pass remaining args (should be empty for update command)
                String[] updateArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
                return updateCheckCommand.onCommand(sender, command, label, updateArgs);
            case "reload":
                return handleReload(sender);
            default:
                sender.sendMessage("§cUnknown subcommand: " + subcommand);
                sender.sendMessage("§7Available subcommands: §eupdate§7, §ereload");
                return true;
        }
    }

    /**
     * Handle the reload subcommand.
     */
    private boolean handleReload(CommandSender sender) {
        // Check permission
        if (!sender.hasPermission("ogessentials.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        OGEssentials plugin = OGEssentials.getInstance();
        if (plugin == null) {
            sender.sendMessage(ChatColor.RED + "Plugin not available.");
            return true;
        }

        plugin.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "OG-Essentials configuration reloaded!");
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String partial = args[0].toLowerCase();
            
            if ("update".startsWith(partial)) {
                completions.add("update");
            }
            if ("reload".startsWith(partial) && sender.hasPermission("ogessentials.reload")) {
                completions.add("reload");
            }
            
            return completions;
        }
        
        // Delegate to subcommand tab completer
        if (args.length > 1 && args[0].equalsIgnoreCase("update")) {
            String[] updateArgs = Arrays.copyOfRange(args, 1, args.length);
            return updateCheckCommand.onTabComplete(sender, command, alias, updateArgs);
        }
        
        return new ArrayList<>();
    }
}
