package dev.og69.ogessentials.commands;

import dev.og69.ogessentials.OGEssentials;
import dev.og69.ogessentials.update.UpdateChecker;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to manually check for plugin updates.
 * 
 * Usage: /ogessentials update
 * Permission: ogessentials.updater.check
 */
public class UpdateCheckCommand implements CommandExecutor, TabCompleter {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        OGEssentials plugin = OGEssentials.getInstance();
        
        if (plugin == null) {
            sender.sendMessage("§cPlugin is not enabled.");
            return true;
        }
        
        UpdateChecker updateChecker = plugin.getUpdateChecker();
        
        if (updateChecker == null) {
            sender.sendMessage("§cUpdate checker is disabled. Enable it in config.yml to use this command.");
            return true;
        }
        
        // Check if we have a cached result
        UpdateChecker.UpdateResult cached = updateChecker.getCachedResult();
        if (cached != null && cached.isSuccess()) {
            displayResult(sender, cached, plugin);
            return true;
        }
        
        // Perform new check
        sender.sendMessage("§7Checking for updates...");
        
        updateChecker.checkForUpdates(result -> {
            if (result == null) {
                sender.sendMessage("§cFailed to check for updates. Please try again later.");
                return;
            }
            
            displayResult(sender, result, plugin);
        });
        
        return true;
    }
    
    /**
     * Display the update check result to the sender.
     * 
     * @param sender The command sender
     * @param result The update result
     * @param plugin The plugin instance
     */
    private void displayResult(CommandSender sender, UpdateChecker.UpdateResult result, OGEssentials plugin) {
        if (!result.isSuccess()) {
            sender.sendMessage("§c[UpdateChecker] " + result.getErrorMessage());
            return;
        }
        
        String currentVersion = plugin.getDescription().getVersion();
        String latestVersion = result.getLatestVersion();
        
        sender.sendMessage("§7=========================================");
        sender.sendMessage("§bOG-Essentials Update Check");
        sender.sendMessage("§7Current version: §f" + currentVersion);
        sender.sendMessage("§7Latest version: §f" + latestVersion);
        
        if (result.isUpdateAvailable()) {
            sender.sendMessage("§eA new version is available!");
            sender.sendMessage("§7Download: §9" + result.getDownloadUrl());
        } else {
            sender.sendMessage("§aYou are running the latest version!");
        }
        
        sender.sendMessage("§7=========================================");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // No tab completion needed for this command
        return new ArrayList<>();
    }
}
