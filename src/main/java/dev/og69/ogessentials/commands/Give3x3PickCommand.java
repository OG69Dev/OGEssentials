package dev.og69.ogessentials.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

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
    
    // NamespacedKey for PersistentDataContainer (initialized via initializeNamespacedKey)
    private static NamespacedKey PICKAXE_ID_KEY = null;
    
    /**
     * Initialize the NamespacedKey for PersistentDataContainer.
     * Must be called during plugin initialization.
     * 
     * @param plugin The plugin instance
     */
    public static void initializeNamespacedKey(Plugin plugin) {
        if (PICKAXE_ID_KEY == null) {
            PICKAXE_ID_KEY = new NamespacedKey(plugin, "3x3_pickaxe");
        }
    }
    
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
            
            // Add PersistentDataContainer tag for secure identification
            // This prevents players from renaming regular pickaxes to make them work
            if (PICKAXE_ID_KEY != null) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                pdc.set(PICKAXE_ID_KEY, PersistentDataType.BOOLEAN, true);
            }
            
            pickaxe.setItemMeta(meta);
        }
        
        return pickaxe;
    }
    
    /**
     * Check if an item is a 3x3 Pickaxe.
     * Supports both Diamond and Netherite pickaxes (for upgrades).
     * Uses PersistentDataContainer for secure identification to prevent renaming abuse.
     * Falls back to display name check for backwards compatibility with old pickaxes.
     * 
     * @param item The item to check
     * @return true if the item is a 3x3 Pickaxe, false otherwise
     */
    public static boolean is3x3Pickaxe(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        // Check if material is a pickaxe (Diamond or Netherite)
        Material type = item.getType();
        if (type != Material.DIAMOND_PICKAXE && type != Material.NETHERITE_PICKAXE) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        // First, check PersistentDataContainer tag (secure method, prevents renaming abuse)
        if (PICKAXE_ID_KEY != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (pdc.has(PICKAXE_ID_KEY, PersistentDataType.BOOLEAN)) {
                Boolean value = pdc.get(PICKAXE_ID_KEY, PersistentDataType.BOOLEAN);
                if (value != null && value) {
                    return true; // Has valid PDC tag
                }
            }
        }
        
        // Fallback to display name check for backwards compatibility with old pickaxes
        // This allows existing pickaxes without PDC tags to still work
        if (meta.hasDisplayName()) {
            String displayName = meta.getDisplayName();
            return displayName.equals(PICKAXE_NAME);
        }
        
        return false;
    }
}
