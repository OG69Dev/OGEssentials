package dev.og69.ogessentials.commands;

import dev.og69.ogessentials.OGEssentials;
import dev.og69.ogessentials.managers.HomeManager;
import dev.og69.ogessentials.managers.KeepInventoryManager;
import dev.og69.ogessentials.migration.SkriptMigrationParser;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
            sender.sendMessage("§7[§bOG-Essentials§7] Use §e/ogessentials migrate §7to migrate from Skript.");
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "update":
                // Pass remaining args (should be empty for update command)
                String[] updateArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
                return updateCheckCommand.onCommand(sender, command, label, updateArgs);
            case "migrate":
                return handleMigrate(sender, args);
            default:
                sender.sendMessage("§cUnknown subcommand: " + subcommand);
                sender.sendMessage("§7Available subcommands: §eupdate§7, §emigrate");
                return true;
        }
    }
    
    /**
     * Handle the migrate subcommand.
     */
    private boolean handleMigrate(CommandSender sender, String[] args) {
        // Check permission
        if (!sender.hasPermission("ogessentials.migrate")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        OGEssentials plugin = OGEssentials.getInstance();
        if (plugin == null) {
            sender.sendMessage(ChatColor.RED + "Plugin not available.");
            return true;
        }
        
        SkriptMigrationParser parser = new SkriptMigrationParser(plugin);
        File variablesFile = parser.getDefaultVariablesFile();
        
        // Check if file exists
        if (!variablesFile.exists()) {
            sender.sendMessage(ChatColor.RED + "Skript variables file not found at:");
            sender.sendMessage(ChatColor.GRAY + variablesFile.getPath());
            return true;
        }
        
        boolean confirm = args.length > 1 && args[1].equalsIgnoreCase("confirm");
        
        // Parse data
        Map<UUID, Map<String, Location>> homes = parser.parseHomes(variablesFile);
        Set<String> keepInvPlayers = parser.parseKeepInventoryPlayers(variablesFile);
        
        // Count homes
        int totalHomes = homes.values().stream().mapToInt(Map::size).sum();
        int playerCount = homes.size();
        
        if (!confirm) {
            // Show preview
            sender.sendMessage("");
            sender.sendMessage(ChatColor.AQUA + "=== Skript Migration Preview ===");
            sender.sendMessage(ChatColor.GREEN + "Found " + ChatColor.WHITE + totalHomes + 
                ChatColor.GREEN + " homes for " + ChatColor.WHITE + playerCount + ChatColor.GREEN + " players");
            sender.sendMessage(ChatColor.GREEN + "Found " + ChatColor.WHITE + keepInvPlayers.size() + 
                ChatColor.GREEN + " players with Keep Inventory enabled");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.YELLOW + "Run " + ChatColor.WHITE + "/ogessentials migrate confirm" + 
                ChatColor.YELLOW + " to proceed.");
            sender.sendMessage(ChatColor.RED + "WARNING: This will overwrite existing data!");
            return true;
        }
        
        // Perform migration
        sender.sendMessage(ChatColor.AQUA + "Starting migration...");
        
        int migratedHomes = 0;
        int migratedKeepInv = 0;
        
        // Migrate homes
        HomeManager homeManager = plugin.getHomeManager();
        if (homeManager != null) {
            for (Map.Entry<UUID, Map<String, Location>> playerEntry : homes.entrySet()) {
                UUID playerId = playerEntry.getKey();
                for (Map.Entry<String, Location> homeEntry : playerEntry.getValue().entrySet()) {
                    homeManager.setHome(playerId, homeEntry.getKey(), homeEntry.getValue());
                    migratedHomes++;
                }
            }
            sender.sendMessage(ChatColor.GREEN + "Migrated " + migratedHomes + " homes.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Home system not available, skipping homes.");
        }
        
        // Migrate keep inventory
        KeepInventoryManager keepInvManager = plugin.getKeepInventoryManager();
        if (keepInvManager != null) {
            for (String playerName : keepInvPlayers) {
                UUID playerId = parser.resolvePlayerUUID(playerName);
                if (playerId != null) {
                    keepInvManager.setEnabled(playerId, true);
                    migratedKeepInv++;
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Could not resolve UUID for: " + playerName);
                }
            }
            sender.sendMessage(ChatColor.GREEN + "Migrated " + migratedKeepInv + " keep inventory settings.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Keep Inventory system not available, skipping.");
        }
        
        sender.sendMessage(ChatColor.GREEN + "Migration complete!");
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
            if ("migrate".startsWith(partial) && sender.hasPermission("ogessentials.migrate")) {
                completions.add("migrate");
            }
            
            return completions;
        }
        
        // Delegate to subcommand tab completer
        if (args.length > 1 && args[0].equalsIgnoreCase("update")) {
            String[] updateArgs = Arrays.copyOfRange(args, 1, args.length);
            return updateCheckCommand.onTabComplete(sender, command, alias, updateArgs);
        }
        
        // Migrate subcommand completion
        if (args.length == 2 && args[0].equalsIgnoreCase("migrate")) {
            List<String> completions = new ArrayList<>();
            if ("confirm".startsWith(args[1].toLowerCase())) {
                completions.add("confirm");
            }
            return completions;
        }
        
        return new ArrayList<>();
    }
}
