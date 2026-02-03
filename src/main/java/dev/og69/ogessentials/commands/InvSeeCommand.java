package dev.og69.ogessentials.commands;

import dev.og69.ogessentials.OGEssentials;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Command to view and optionally modify another player's inventory.
 * 
 * Usage: /invsee <player>
 * Permissions:
 * - ogessentials.invsee.view (View only)
 * - ogessentials.invsee.modify (View and Modify)
 */
public class InvSeeCommand implements CommandExecutor {

    private final OGEssentials plugin;

    public InvSeeCommand(OGEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("ogessentials.invsee.view") && !player.hasPermission("ogessentials.invsee.modify")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(getPrefix() + ChatColor.RED + "Usage: /invsee <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(getPrefix() + ChatColor.RED + "Player not found or offline.");
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(getPrefix() + ChatColor.RED + "You cannot invsee yourself.");
            return true;
        }

        openInvSee(player, target);
        return true;
    }

    private void openInvSee(Player viewer, Player target) {
        // Create a custom 6-row inventory
        // Title includes target name for identification in the listener
        Inventory inv = Bukkit.createInventory(null, 54, "InvSee: " + target.getName());

        // Fill main inventory (0-35)
        ItemStack[] contents = target.getInventory().getContents();
        for (int i = 0; i < 36; i++) {
            if (i < contents.length && contents[i] != null) {
                inv.setItem(i, contents[i]);
            }
        }

        // Armor slots (36-39)
        ItemStack[] armor = target.getInventory().getArmorContents();
        // Armor contents are typically [boots, leggings, chestplate, helmet]
        // We want to verify order or just place them visually
        // Standard ArmorContents return: 0=Boots, 1=Leggings, 2=Chestplate, 3=Helmet
        
        // Let's visualize bottom-up or top-down. 
        // 36: Helmet, 37: Chest, 38: Legs, 39: Boots (Custom order for GUI)
        
        // Set GUI items
        // Note: Array is [Boots, Leggings, Chest, Helmet]
        if (armor.length >= 4) {
             inv.setItem(36, armor[3]); // Helmet
             inv.setItem(37, armor[2]); // Chestplate
             inv.setItem(38, armor[1]); // Leggings
             inv.setItem(39, armor[0]); // Boots
        }

        // Offhand (40)
        inv.setItem(40, target.getInventory().getItemInOffHand());

        // Fill separators (41-53) to indicate non-editable area or just UI
        ItemStack filler = createFillerItem();
        for (int i = 41; i < 54; i++) {
            inv.setItem(i, filler);
        }
        
        // Add specific indicators
        inv.setItem(45, createIndicatorItem(Material.IRON_HELMET, "§eHelmet"));
        inv.setItem(46, createIndicatorItem(Material.IRON_CHESTPLATE, "§eChestplate"));
        inv.setItem(47, createIndicatorItem(Material.IRON_LEGGINGS, "§eLeggings"));
        inv.setItem(48, createIndicatorItem(Material.IRON_BOOTS, "§eBoots"));
        inv.setItem(49, createIndicatorItem(Material.SHIELD, "§eOffhand"));
        
        // Actually, let's put armor/offhand in a nice row
        // Row 5 (slots 36-44) -> Let's clear and re-do layout to be clearer
        // 0-8: Hotbar
        // 9-35: Storage
        // 
        // Lets use Row 6 (45-53) for Armor/Offhand
        // 45: Helmet, 46: Chest, 47: Legs, 48: Boots, 50: Offhand
        
        // Clear previously set items for 36-40 if we change mind...
        // Actually standard player inventory is:
        // 0-8: Hotbar
        // 9-35: Main Storage (3 rows)
        // That takes up 4 rows (0-35) in Chest GUI.
        
        // Row 5 (36-44): Filler/Separators
        for (int i = 36; i < 45; i++) {
            inv.setItem(i, filler);
        }
        
        // Row 6 (45-53): Armor & Offhand
        // 45: Helmet
        inv.setItem(45, armor.length > 3 ? armor[3] : createPlaceholder(Material.IRON_HELMET, "§7No Helmet"));
        // 46: Chest
        inv.setItem(46, armor.length > 2 ? armor[2] : createPlaceholder(Material.IRON_CHESTPLATE, "§7No Chestplate"));
        // 47: Legs
        inv.setItem(47, armor.length > 1 ? armor[1] : createPlaceholder(Material.IRON_LEGGINGS, "§7No Leggings"));
        // 48: Boots
        inv.setItem(48, armor.length > 0 ? armor[0] : createPlaceholder(Material.IRON_BOOTS, "§7No Boots"));
        
        // 49: Separator
        inv.setItem(49, filler);
        
        // 50: Offhand
        ItemStack offhand = target.getInventory().getItemInOffHand();
        inv.setItem(50, (offhand != null && offhand.getType() != Material.AIR) ? offhand : createPlaceholder(Material.SHIELD, "§7No Offhand"));

        // 51-53: Separator
        for (int i = 51; i < 54; i++) {
            inv.setItem(i, filler);
        }

        viewer.openInventory(inv);
    }

    private ItemStack createFillerItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createIndicatorItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createPlaceholder(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("prefix", "&7[&bOGSMP&7] &r"));
    }
}
