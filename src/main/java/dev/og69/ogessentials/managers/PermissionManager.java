package dev.og69.ogessentials.managers;

import dev.og69.ogessentials.OGEssentials;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player permissions using SQLite database.
 * Provides PEX-like permission management with prefix/suffix support.
 */
public class PermissionManager {

    private final OGEssentials plugin;
    
    // Cache: UUID -> Set of permissions
    private final Map<UUID, Set<String>> permissionCache = new ConcurrentHashMap<>();
    
    // Cache: UUID -> [prefix, suffix]
    private final Map<UUID, String[]> formatCache = new ConcurrentHashMap<>();
    
    // Active permission attachments
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();

    public PermissionManager(OGEssentials plugin) {
        this.plugin = plugin;
    }

    /**
     * Load all permissions for a player from database.
     */
    public void loadPlayer(UUID uuid) {
        Set<String> permissions = new HashSet<>();
        String prefix = "";
        String suffix = "";
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            // Load permissions
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT permission FROM user_permissions WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    permissions.add(rs.getString("permission"));
                }
            }
            
            // Load prefix/suffix
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT prefix, suffix FROM users WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    prefix = rs.getString("prefix");
                    suffix = rs.getString("suffix");
                    if (prefix == null) prefix = "";
                    if (suffix == null) suffix = "";
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load permissions for " + uuid + ": " + e.getMessage());
        }
        
        permissionCache.put(uuid, permissions);
        formatCache.put(uuid, new String[]{prefix, suffix});
    }

    /**
     * Apply cached permissions to an online player.
     */
    public void applyPermissions(Player player) {
        UUID uuid = player.getUniqueId();
        Set<String> permissions = permissionCache.getOrDefault(uuid, new HashSet<>());
        
        // Remove old attachment if exists
        PermissionAttachment oldAttachment = attachments.remove(uuid);
        if (oldAttachment != null) {
            player.removeAttachment(oldAttachment);
        }
        
        // Create new attachment
        PermissionAttachment attachment = player.addAttachment(plugin);
        attachments.put(uuid, attachment);
        
        // 1. Explicitly negate sensitive permissions for everyone (including Ops) 
        // unless they are specifically granted in our database.
        String[] restrictedPerms = {
            "ogessentials.perm.user.manage",
            "ogessentials.perm.user.view",
            "ogessentials.perm.user.format",
            "ogessentials.perm.reload",
            "ogessentials.perm.*"
        };
        
        for (String restricted : restrictedPerms) {
            if (!permissions.contains(restricted) && !permissions.contains("ogessentials.perm.*")) {
                attachment.setPermission(restricted, false);
            }
        }
        
        // 2. Apply all permissions from database
        for (String perm : permissions) {
            if (perm.startsWith("-")) {
                attachment.setPermission(perm.substring(1), false);
            } else {
                attachment.setPermission(perm, true);
            }
        }
        
        player.recalculatePermissions();
        player.updateCommands();
    }

    /**
     * Cleanup when player quits.
     */
    public void cleanupPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        PermissionAttachment attachment = attachments.remove(uuid);
        if (attachment != null) {
            player.removeAttachment(attachment);
        }
        permissionCache.remove(uuid);
        formatCache.remove(uuid);
        player.updateCommands();
    }

    /**
     * Add a permission to a player.
     */
    public boolean addPermission(UUID uuid, String permission) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            // Ensure user exists
            ensureUserExists(uuid, conn);
            
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT OR IGNORE INTO user_permissions (uuid, permission) VALUES (?, ?)")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, permission);
                stmt.executeUpdate();
            }
            
            // Update cache
            permissionCache.computeIfAbsent(uuid, k -> new HashSet<>()).add(permission);
            
            // Apply to online player
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                applyPermissions(player);
            }
            
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to add permission: " + e.getMessage());
            return false;
        }
    }

    /**
     * Remove a permission from a player.
     */
    public boolean removePermission(UUID uuid, String permission) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM user_permissions WHERE uuid = ? AND permission = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, permission);
                stmt.executeUpdate();
            }
            
            // Update cache
            Set<String> perms = permissionCache.get(uuid);
            if (perms != null) {
                perms.remove(permission);
            }
            
            // Apply to online player
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                applyPermissions(player);
            }
            
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to remove permission: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get all permissions for a player.
     */
    public Set<String> getPermissions(UUID uuid) {
        // Try cache first
        Set<String> cached = permissionCache.get(uuid);
        if (cached != null) {
            return new HashSet<>(cached);
        }
        
        // Load from database
        Set<String> permissions = new HashSet<>();
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT permission FROM user_permissions WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    permissions.add(rs.getString("permission"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get permissions: " + e.getMessage());
        }
        
        return permissions;
    }

    /**
     * Set prefix for a player.
     */
    public boolean setPrefix(UUID uuid, String prefix) {
        return setFormat(uuid, prefix, null);
    }

    /**
     * Set suffix for a player.
     */
    public boolean setSuffix(UUID uuid, String suffix) {
        return setFormat(uuid, null, suffix);
    }

    /**
     * Set prefix and/or suffix for a player.
     */
    private boolean setFormat(UUID uuid, String prefix, String suffix) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            // Ensure user exists
            ensureUserExists(uuid, conn);
            
            if (prefix != null) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE users SET prefix = ? WHERE uuid = ?")) {
                    stmt.setString(1, prefix);
                    stmt.setString(2, uuid.toString());
                    stmt.executeUpdate();
                }
            }
            
            if (suffix != null) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE users SET suffix = ? WHERE uuid = ?")) {
                    stmt.setString(1, suffix);
                    stmt.setString(2, uuid.toString());
                    stmt.executeUpdate();
                }
            }
            
            // Update cache
            String[] format = formatCache.getOrDefault(uuid, new String[]{"", ""});
            if (prefix != null) format[0] = prefix;
            if (suffix != null) format[1] = suffix;
            formatCache.put(uuid, format);
            
            // Update nametag for online player
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && plugin.getNameTagManager() != null) {
                updatePlayerNameTag(player);
            }
            
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to set format: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get prefix for a player.
     */
    public String getPrefix(UUID uuid) {
        String[] format = formatCache.get(uuid);
        if (format != null) {
            return format[0];
        }
        return loadFormatFromDb(uuid)[0];
    }

    /**
     * Get suffix for a player.
     */
    public String getSuffix(UUID uuid) {
        String[] format = formatCache.get(uuid);
        if (format != null) {
            return format[1];
        }
        return loadFormatFromDb(uuid)[1];
    }

    private String[] loadFormatFromDb(UUID uuid) {
        String prefix = "";
        String suffix = "";
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT prefix, suffix FROM users WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    prefix = rs.getString("prefix");
                    suffix = rs.getString("suffix");
                    if (prefix == null) prefix = "";
                    if (suffix == null) suffix = "";
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load format: " + e.getMessage());
        }
        return new String[]{prefix, suffix};
    }

    /**
     * Update player's nametag with prefix/suffix.
     */
    public void updatePlayerNameTag(Player player) {
        UUID uuid = player.getUniqueId();
        String[] format = formatCache.getOrDefault(uuid, new String[]{"", ""});
        
        NameTagManager nameTagManager = plugin.getNameTagManager();
        if (nameTagManager != null) {
            String prefix = format[0];
            String suffix = format[1];
            
            if (!prefix.isEmpty() || !suffix.isEmpty()) {
                nameTagManager.setTag(player, "perm_format", prefix, suffix, 50);
            } else {
                nameTagManager.removeTag(player, "perm_format");
            }
        }
    }

    /**
     * Ensure user entry exists in database.
     */
    private void ensureUserExists(UUID uuid, Connection conn) throws SQLException {
        Player player = Bukkit.getPlayer(uuid);
        String username = player != null ? player.getName() : null;
        
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT OR IGNORE INTO users (uuid, username) VALUES (?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, username);
            stmt.executeUpdate();
        }
        
        // Update username if changed
        if (username != null) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE users SET username = ? WHERE uuid = ?")) {
                stmt.setString(1, username);
                stmt.setString(2, uuid.toString());
                stmt.executeUpdate();
            }
        }
    }

    /**
     * Reload all permissions from database.
     */
    public void reload() {
        permissionCache.clear();
        formatCache.clear();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayer(player.getUniqueId());
            applyPermissions(player);
            updatePlayerNameTag(player);
        }
    }

    /**
     * Cleanup on plugin disable.
     */
    public void cleanup() {
        for (Map.Entry<UUID, PermissionAttachment> entry : attachments.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.removeAttachment(entry.getValue());
            }
        }
        attachments.clear();
        permissionCache.clear();
        formatCache.clear();
    }
}
