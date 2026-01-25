package dev.og69.ogessentials.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Implementation of PlaceholderExpansion for OG-Essentials.
 * 
 * This class extends PlaceholderExpansion and will only be loaded
 * if PlaceholderAPI is available (since it's only referenced via reflection).
 * 
 * Note: This class WILL cause a NoClassDefFoundError if PlaceholderAPI
 * is not installed, which is why it's only loaded via reflection after
 * checking that PlaceholderAPI is available.
 */
public class PlaceholderExpansionImpl extends PlaceholderExpansion {
    
    private final Plugin plugin;
    private static dev.og69.ogessentials.OGEssentials mainPlugin = null;
    
    public PlaceholderExpansionImpl(Plugin plugin) {
        this.plugin = plugin;
        mainPlugin = (dev.og69.ogessentials.OGEssentials) plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "ogessentials";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().isEmpty() 
            ? "OG69" 
            : plugin.getDescription().getAuthors().get(0);
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean canRegister() {
        return org.bukkit.Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }
    
    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (mainPlugin == null) {
            return null;
        }
        
        if (player == null) {
            return null;
        }
        
        UUID playerUUID = player.getUniqueId();
        
        // Parse placeholder parameters
        switch (params.toLowerCase()) {
            case "homes_count":
            case "homes":
                // Get number of homes for player
                // TODO: Implement when HomeStorage is available
                return "0";
            
            case "afk_status":
            case "afk":
                // Get AFK status for player
                // TODO: Implement when AfkStorage is available
                return "false";
            
            case "3x3_level":
            case "3x3pick_level":
                // Get 3x3 enchant level on held item
                // TODO: Implement when EnchantManager is available
                if (player.getInventory().getItemInMainHand() != null) {
                    return "0";
                }
                return "0";
            
            case "keepinv_enabled":
            case "keepinventory":
                // Get keep inventory status for player
                // TODO: Implement when KeepInventory storage is available
                return "false";
            
            default:
                return null;
        }
    }
    
    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) {
            return null;
        }
        
        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer != null) {
            return onPlaceholderRequest(onlinePlayer, params);
        }
        
        return null;
    }
}
