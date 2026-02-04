package dev.og69.ogessentials.commands;

import dev.og69.ogessentials.OGEssentials;
import dev.og69.ogessentials.managers.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Command handler for /perm (PEX-like permission management).
 */
public class PermissionCommand implements CommandExecutor, TabCompleter {

    private final OGEssentials plugin;
    private final PermissionManager permissionManager;

    public PermissionCommand(OGEssentials plugin, PermissionManager permissionManager) {
        this.plugin = plugin;
        this.permissionManager = permissionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender has ANY permission to use this command or is console
        // Since we specify permission in plugin.yml, Bukkit blocks it for those without view perm.
        // We handle the rest in subcommands.
        if (!(sender instanceof org.bukkit.command.ConsoleCommandSender) && 
            !sender.hasPermission("ogessentials.perm.user.view")) {
            // This message will rarely be seen if plugin.yml blocks it first.
            sender.sendMessage(ChatColor.RED + "I'm sorry, but you do not have permission to perform this command. Please contact the server administrators if you believe that this is in error.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "reload":
                return handleReload(sender);
            case "user":
                return handleUser(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand: " + subcommand);
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleReload(CommandSender sender) {
        if (!(sender instanceof org.bukkit.command.ConsoleCommandSender) && !sender.hasPermission("ogessentials.perm.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        permissionManager.reload();
        sender.sendMessage(ChatColor.GREEN + "Permissions reloaded!");
        return true;
    }

    private boolean handleUser(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /perm user <player> <add|remove|list|info|prefix|suffix> [value]");
            return true;
        }

        String playerName = args[1];
        String action = args[2].toLowerCase();

        // Resolve player UUID
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = target.getUniqueId();

        // Check if player has ever played
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player '" + playerName + "' not found.");
            return true;
        }

        switch (action) {
            case "add":
                return handleAddPermission(sender, uuid, playerName, args);
            case "remove":
                return handleRemovePermission(sender, uuid, playerName, args);
            case "list":
                return handleListPermissions(sender, uuid, playerName);
            case "info":
                return handleInfo(sender, uuid, playerName);
            case "prefix":
                return handlePrefix(sender, uuid, playerName, args);
            case "suffix":
                return handleSuffix(sender, uuid, playerName, args);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown action: " + action);
                sender.sendMessage(ChatColor.GRAY + "Available: add, remove, list, info, prefix, suffix");
                return true;
        }
    }

    private boolean handleAddPermission(CommandSender sender, UUID uuid, String playerName, String[] args) {
        if (!(sender instanceof org.bukkit.command.ConsoleCommandSender) && !sender.hasPermission("ogessentials.perm.user.manage")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /perm user <player> add <permission>");
            return true;
        }

        String permission = args[3];
        if (permissionManager.addPermission(uuid, permission)) {
            sender.sendMessage(ChatColor.GREEN + "Added permission '" + ChatColor.WHITE + permission + 
                ChatColor.GREEN + "' to " + ChatColor.WHITE + playerName);
            
            // Notify player if online
            Player targetPlayer = Bukkit.getPlayer(uuid);
            if (targetPlayer != null) {
                String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix", "&7[&bOG-Essentials&7] &r"));
                String senderName = (sender instanceof Player) ? sender.getName() : "Console";
                targetPlayer.sendMessage(prefix + ChatColor.AQUA + "You have been granted permission: " + ChatColor.YELLOW + permission + ChatColor.AQUA + " by " + ChatColor.YELLOW + senderName);
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to add permission.");
        }
        return true;
    }

    private boolean handleRemovePermission(CommandSender sender, UUID uuid, String playerName, String[] args) {
        if (!(sender instanceof org.bukkit.command.ConsoleCommandSender) && !sender.hasPermission("ogessentials.perm.user.manage")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /perm user <player> remove <permission>");
            return true;
        }

        String permission = args[3];
        if (permissionManager.removePermission(uuid, permission)) {
            sender.sendMessage(ChatColor.GREEN + "Removed permission '" + ChatColor.WHITE + permission + 
                ChatColor.GREEN + "' from " + ChatColor.WHITE + playerName);
            
            // Notify player if online
            Player targetPlayer = Bukkit.getPlayer(uuid);
            if (targetPlayer != null) {
                String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix", "&7[&bOG-Essentials&7] &r"));
                String senderName = (sender instanceof Player) ? sender.getName() : "Console";
                targetPlayer.sendMessage(prefix + ChatColor.RED + "The permission: " + ChatColor.YELLOW + permission + ChatColor.RED + " has been removed from you by " + ChatColor.YELLOW + senderName);
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to remove permission.");
        }
        return true;
    }

    private boolean handleListPermissions(CommandSender sender, UUID uuid, String playerName) {
        if (!(sender instanceof org.bukkit.command.ConsoleCommandSender) && !sender.hasPermission("ogessentials.perm.user.view")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        Set<String> permissions = permissionManager.getPermissions(uuid);
        
        sender.sendMessage(ChatColor.AQUA + "=== Permissions for " + playerName + " ===");
        if (permissions.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No permissions set.");
        } else {
            for (String perm : permissions) {
                if (perm.startsWith("-")) {
                    sender.sendMessage(ChatColor.RED + "  - " + perm);
                } else {
                    sender.sendMessage(ChatColor.GREEN + "  + " + perm);
                }
            }
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender, UUID uuid, String playerName) {
        if (!(sender instanceof org.bukkit.command.ConsoleCommandSender) && !sender.hasPermission("ogessentials.perm.user.view")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        String prefix = permissionManager.getPrefix(uuid);
        String suffix = permissionManager.getSuffix(uuid);
        Set<String> permissions = permissionManager.getPermissions(uuid);

        sender.sendMessage(ChatColor.AQUA + "=== User Info: " + playerName + " ===");
        sender.sendMessage(ChatColor.GRAY + "UUID: " + ChatColor.WHITE + uuid);
        sender.sendMessage(ChatColor.GRAY + "Prefix: " + ChatColor.WHITE + "'" + 
            ChatColor.translateAlternateColorCodes('&', prefix) + ChatColor.WHITE + "'");
        sender.sendMessage(ChatColor.GRAY + "Suffix: " + ChatColor.WHITE + "'" + 
            ChatColor.translateAlternateColorCodes('&', suffix) + ChatColor.WHITE + "'");
        sender.sendMessage(ChatColor.GRAY + "Permissions: " + ChatColor.WHITE + permissions.size());
        
        return true;
    }

    private boolean handlePrefix(CommandSender sender, UUID uuid, String playerName, String[] args) {
        if (!(sender instanceof org.bukkit.command.ConsoleCommandSender) && !sender.hasPermission("ogessentials.perm.user.format")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /perm user <player> prefix \"<prefix>\"");
            return true;
        }

        // Join remaining args and strip quotes
        String prefix = joinArgs(args, 3);
        prefix = stripQuotes(prefix);

        if (permissionManager.setPrefix(uuid, prefix)) {
            sender.sendMessage(ChatColor.GREEN + "Set prefix for " + ChatColor.WHITE + playerName + 
                ChatColor.GREEN + " to: " + ChatColor.translateAlternateColorCodes('&', prefix));
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to set prefix.");
        }
        return true;
    }

    private boolean handleSuffix(CommandSender sender, UUID uuid, String playerName, String[] args) {
        if (!(sender instanceof org.bukkit.command.ConsoleCommandSender) && !sender.hasPermission("ogessentials.perm.user.format")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /perm user <player> suffix \"<suffix>\"");
            return true;
        }

        // Join remaining args and strip quotes
        String suffix = joinArgs(args, 3);
        suffix = stripQuotes(suffix);

        if (permissionManager.setSuffix(uuid, suffix)) {
            sender.sendMessage(ChatColor.GREEN + "Set suffix for " + ChatColor.WHITE + playerName + 
                ChatColor.GREEN + " to: " + ChatColor.translateAlternateColorCodes('&', suffix));
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to set suffix.");
        }
        return true;
    }

    private String joinArgs(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private String stripQuotes(String s) {
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() > 1) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "=== OGEssentials Permissions ===");
        sender.sendMessage(ChatColor.GRAY + "/perm user <player> add <permission>");
        sender.sendMessage(ChatColor.GRAY + "/perm user <player> remove <permission>");
        sender.sendMessage(ChatColor.GRAY + "/perm user <player> list");
        sender.sendMessage(ChatColor.GRAY + "/perm user <player> info");
        sender.sendMessage(ChatColor.GRAY + "/perm user <player> prefix \"<prefix>\"");
        sender.sendMessage(ChatColor.GRAY + "/perm user <player> suffix \"<suffix>\"");
        sender.sendMessage(ChatColor.GRAY + "/perm reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            if ("user".startsWith(partial)) completions.add("user");
            if ("reload".startsWith(partial) && sender.hasPermission("ogessentials.perm.reload")) {
                completions.add("reload");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("user")) {
            // Player names
            String partial = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("user")) {
            String partial = args[2].toLowerCase();
            List<String> actions = Arrays.asList("add", "remove", "list", "info", "prefix", "suffix");
            for (String action : actions) {
                if (action.startsWith(partial)) {
                    completions.add(action);
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("user")) {
            String action = args[2].toLowerCase();
            String partial = args[3].toLowerCase();
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            Set<String> currentPerms = permissionManager.getPermissions(target.getUniqueId());

            if (action.equals("remove")) {
                // Suggest existing permissions
                for (String perm : currentPerms) {
                    if (perm.toLowerCase().startsWith(partial)) {
                        completions.add(perm);
                    }
                }
            } else if (action.equals("add")) {
                // Suggest permissions registered in Bukkit that the player doesn't have
                for (org.bukkit.permissions.Permission perm : Bukkit.getPluginManager().getPermissions()) {
                    String name = perm.getName();
                    if (name.toLowerCase().startsWith(partial) && !currentPerms.contains(name)) {
                        completions.add(name);
                    }
                }
            }
        }

        return completions;
    }
}
