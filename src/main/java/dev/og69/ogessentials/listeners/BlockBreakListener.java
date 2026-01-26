package dev.og69.ogessentials.listeners;

import dev.og69.ogessentials.OGEssentials;
import dev.og69.ogessentials.commands.Give3x3PickCommand;
import dev.og69.ogessentials.hooks.CoreProtectHook;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for block break events.
 * Handles 3x3 pickaxe mining functionality.
 */
public class BlockBreakListener implements Listener {
    
    private final OGEssentials plugin;
    
    public BlockBreakListener(OGEssentials plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        
        // Check if player is using a 3x3 pickaxe
        if (!Give3x3PickCommand.is3x3Pickaxe(tool)) {
            return;
        }
        
        Block centerBlock = event.getBlock();
        Location centerLocation = centerBlock.getLocation();
        Location playerLocation = player.getLocation();
        float pitch = playerLocation.getPitch();
        float yaw = playerLocation.getYaw();
        
        // Offsets for 3x3 area: -1, 0, 1
        int[] offsets = {-1, 0, 1};
        
        // Determine mining pattern based on pitch
        // If looking up (> 60) or down (< -60), mine horizontal plane
        // Otherwise, mine vertical plane based on yaw (facing direction)
        boolean isHorizontal = pitch > 60 || pitch < -60;
        
        if (isHorizontal) {
            // Mine horizontal plane (X and Z offsets) - looking up or down
            for (int xOffset : offsets) {
                for (int zOffset : offsets) {
                    if (xOffset == 0 && zOffset == 0) {
                        continue; // Skip the center block (already being broken)
                    }
                    
                    Location targetLocation = centerLocation.clone().add(xOffset, 0, zOffset);
                    Block targetBlock = targetLocation.getBlock();
                    
                    if (targetBlock.getType() != Material.AIR) {
                        breakBlockNaturally(player, targetBlock, tool);
                    }
                }
            }
        } else {
            // Mine vertical plane - determine which plane based on yaw
            // Normalize yaw to 0-360 range
            float normalizedYaw = (yaw % 360 + 360) % 360;
            
            // Determine which vertical plane to mine based on facing direction
            // Yaw: 0° = South, 90° = West, 180° = North, 270° = East
            // If facing more north/south (0-45, 135-225, 315-360): use X-Y plane
            // If facing more east/west (45-135, 225-315): use Z-Y plane
            boolean useXZPlane = (normalizedYaw >= 45 && normalizedYaw < 135) || 
                                 (normalizedYaw >= 225 && normalizedYaw < 315);
            
            if (useXZPlane) {
                // Mine vertical plane using Z and Y offsets (facing east/west)
                for (int zOffset : offsets) {
                    for (int yOffset : offsets) {
                        if (zOffset == 0 && yOffset == 0) {
                            continue; // Skip the center block (already being broken)
                        }
                        
                        Location targetLocation = centerLocation.clone().add(0, yOffset, zOffset);
                        Block targetBlock = targetLocation.getBlock();
                        
                        if (targetBlock.getType() != Material.AIR) {
                            breakBlockNaturally(player, targetBlock, tool);
                        }
                    }
                }
            } else {
                // Mine vertical plane using X and Y offsets (facing north/south)
                for (int xOffset : offsets) {
                    for (int yOffset : offsets) {
                        if (xOffset == 0 && yOffset == 0) {
                            continue; // Skip the center block (already being broken)
                        }
                        
                        Location targetLocation = centerLocation.clone().add(xOffset, yOffset, 0);
                        Block targetBlock = targetLocation.getBlock();
                        
                        if (targetBlock.getType() != Material.AIR) {
                            breakBlockNaturally(player, targetBlock, tool);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Break a block naturally using the player's tool.
     * This simulates the player breaking the block with their tool.
     * 
     * @param player The player breaking the block
     * @param block The block to break
     * @param tool The tool being used
     */
    private void breakBlockNaturally(Player player, Block block, ItemStack tool) {
        // Check if block can be broken
        if (block.getType() == Material.AIR || block.getType() == Material.BEDROCK) {
            return;
        }
        
        // Check if player has permission to break this block
        if (!player.hasPermission("ogessentials.3x3pick.break.*")) {
            // Check specific block type permission
            String blockPerm = "ogessentials.3x3pick.break." + block.getType().name().toLowerCase();
            if (!player.hasPermission(blockPerm)) {
                return;
            }
        }
        
        // Save block state for CoreProtect logging (before breaking)
        BlockState oldState = block.getState();
        
        // Break the block naturally (drops items, uses durability)
        // Use the tool to break the block
        block.breakNaturally(tool);
        
        // Log to CoreProtect if available (pass oldState captured before breaking)
        if (CoreProtectHook.isEnabled()) {
            CoreProtectHook.logBlockBreak(player, block, oldState);
        }
    }
}
