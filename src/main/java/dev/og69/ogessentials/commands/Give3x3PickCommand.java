package dev.og69.ogessentials.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Command to give a 3x3 Pickaxe to a player.
 * 
 * Usage: /give3x3pick [player]
 * Permission: ogessentials.3x3pick.give
 */
public class Give3x3PickCommand implements CommandExecutor, TabCompleter {
    
    private static final String PICKAXE_NAME = "§b3x3 Pickaxe";
    private static final String PICKAXE_LORE = "§7Mines in a 3x3 area";
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && args.length == 0) {
            sender.sendMessage("§cYou must specify a player when using this command from console.");
            return true;
        }
        
        Player target;
        
        if (args.length > 0) {
            // Give to specified player
            target = sender.getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer '" + args[0] + "' is not online.");
                return true;
            }
            
            // Give the pickaxe
            ItemStack pickaxe = create3x3Pickaxe();
            target.getInventory().addItem(pickaxe);
            
            // Send messages
            sender.sendMessage("§aYou have given a §b3x3 Pickaxe §ato " + target.getName() + ".");
            target.sendMessage("§aYou have received a §b3x3 Pickaxe§a!");
        } else {
            // Give to command sender
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cYou must be a player to use this command without arguments.");
                return true;
            }
            
            target = (Player) sender;
            ItemStack pickaxe = create3x3Pickaxe();
            target.getInventory().addItem(pickaxe);
            target.sendMessage("§aYou have been given the §b3x3 Pickaxe§a!");
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String partial = args[0].toLowerCase();
            
            for (Player player : sender.getServer().getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
            
            return completions;
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Create a 3x3 Pickaxe item.
     * 
     * @return The 3x3 Pickaxe ItemStack
     */
    public static ItemStack create3x3Pickaxe() {
        ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = pickaxe.getItemMeta();
        
        if (meta != null) {
            // Set display name using legacy color codes
            meta.setDisplayName(PICKAXE_NAME);
            
            // Set lore
            meta.setLore(Arrays.asList(PICKAXE_LORE));
            
            pickaxe.setItemMeta(meta);
        }
        
        return pickaxe;
    }
    
    /**
     * Check if an item is a 3x3 Pickaxe.
     * 
     * @param item The item to check
     * @return true if the item is a 3x3 Pickaxe, false otherwise
     */
    public static boolean is3x3Pickaxe(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_PICKAXE) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        
        String displayName = meta.getDisplayName();
        return displayName.equals(PICKAXE_NAME);
    }
}
