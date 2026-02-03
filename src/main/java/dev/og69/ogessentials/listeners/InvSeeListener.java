package dev.og69.ogessentials.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Handles interactions with the InvSee GUI.
 */
public class InvSeeListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith("InvSee: ")) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player viewer)) {
            return;
        }

        // Cancel clicks if viewer cannot modify
        if (!viewer.hasPermission("ogessentials.invsee.modify")) {
            event.setCancelled(true);
            return;
        }

        // Handle clicks in global inventory space (both top and bottom)
        // If clicking outside, ignore
        if (event.getClickedInventory() == null) {
            return;
        }

        // Prevent moving the "filler" items
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem != null && currentItem.getType() == Material.GRAY_STAINED_GLASS_PANE && 
            currentItem.getItemMeta() != null && " ".equals(currentItem.getItemMeta().getDisplayName())) {
            event.setCancelled(true);
            return;
        }
        
        // Prevent moving placeholder items (check name pattern)
        if (currentItem != null && currentItem.getItemMeta() != null && 
            currentItem.getItemMeta().getDisplayName().startsWith("§7No ")) {
            event.setCancelled(true);
            // Allow placing generic item here? Logic gets complex.
            // For now, simpler to cancel. Realistically, an empty slot should be AIR.
            // The placeholder is confusing if we want to put items IN.
            // Let's refine: If it is a placeholder, and we click with an item on cursor, we want to put the item there.
            // But the placeholder is a real item in the GUI.
            // So we'd need to manually handle the swap.
            // For simplicity in this iteration: Cancel clicks on placeholders unless implemented fully.
            // Better approach: In sync, if the GUI slot has our placeholder, we treat it as AIR for logic, but here we must handle the event.
            // Let's rely on the sync task to handle updates and keep it simple: 
            // If modify is enabled, we trust the user not to steal the placeholder glass panes/icons.
            // Or we check if they are trying to take it.
        }

        // Schedule a sync task to update the target player's inventory
        String targetName = title.substring("InvSee: ".length());
        Player target = Bukkit.getPlayer(targetName);
        
        if (target != null && target.isOnline()) {
            Bukkit.getScheduler().runTask(dev.og69.ogessentials.OGEssentials.getInstance(), () -> {
                syncInventory(event.getView().getTopInventory(), target);
            });
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if(!title.startsWith("InvSee: ")) return;
        
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        
        if (!viewer.hasPermission("ogessentials.invsee.modify")) {
            event.setCancelled(true);
            return;
        }
        
        String targetName = title.substring("InvSee: ".length());
        Player target = Bukkit.getPlayer(targetName);
        
        if (target != null && target.isOnline()) {
             Bukkit.getScheduler().runTask(dev.og69.ogessentials.OGEssentials.getInstance(), () -> {
                syncInventory(event.getView().getTopInventory(), target);
            });       
        }
    }

    /**
     * Syncs the GUI state back to the target player's actual inventory.
     */
    private void syncInventory(Inventory gui, Player target) {
        PlayerInventory pInv = target.getInventory();
        
        // Sync Main Inventory (0-35)
        for (int i = 0; i < 36; i++) {
            ItemStack item = gui.getItem(i);
            pInv.setItem(i, item);
        }
        
        // Sync Armor (45-48) -> [Boots, Leggings, Chest, Helmet]
        // 45: Helmet, 46: Chest, 47: Legs, 48: Boots
        
        ItemStack helmet = gui.getItem(45);
        if (isPlaceholder(helmet)) helmet = null;
        pInv.setHelmet(helmet);
        
        ItemStack chest = gui.getItem(46);
        if (isPlaceholder(chest)) chest = null;
        pInv.setChestplate(chest);
        
        ItemStack legs = gui.getItem(47);
        if (isPlaceholder(legs)) legs = null;
        pInv.setLeggings(legs);

        ItemStack boots = gui.getItem(48);
        if (isPlaceholder(boots)) boots = null;
        pInv.setBoots(boots);
        
        // Sync Offhand (50)
        ItemStack offhand = gui.getItem(50);
        if (isPlaceholder(offhand)) offhand = null;
        pInv.setItemInOffHand(offhand);
        
        // Update target's inventory view if open (optional)
        // target.updateInventory(); // Deprecated but sometimes needed
    }
    
    private boolean isPlaceholder(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String name = item.getItemMeta().getDisplayName();
            return name.startsWith("§7No ") || name.equals(" ");
        }
        return false;
    }
}
