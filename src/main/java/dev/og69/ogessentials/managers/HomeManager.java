package dev.og69.ogessentials.managers;

import dev.og69.ogessentials.OGEssentials;
import dev.og69.ogessentials.storage.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages player homes with SQLite storage.
 * 
 * Provides CRUD operations for homes and enforces max home limits.
 */
public class HomeManager {
    
    private final OGEssentials plugin;
    private final DatabaseManager databaseManager;
    
    /**
     * Create a new home manager.
     * 
     * @param plugin The plugin instance
     * @param databaseManager The database manager
     */
    public HomeManager(OGEssentials plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }
    
    /**
     * Set a home for a player.
     * Updates if home with same name exists, creates new otherwise.
     * 
     * @param playerId The player's UUID
     * @param name The home name
     * @param location The home location
     * @return true if this was an update (home existed), false if new home
     */
    public boolean setHome(UUID playerId, String name, Location location) {
        String sql = """
            INSERT INTO homes (player_uuid, name, world, x, y, z, yaw, pitch)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(player_uuid, name) DO UPDATE SET
                world = excluded.world,
                x = excluded.x,
                y = excluded.y,
                z = excluded.z,
                yaw = excluded.yaw,
                pitch = excluded.pitch
            """;
        
        boolean existed = homeExists(playerId, name);
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerId.toString());
            stmt.setString(2, name.toLowerCase());
            stmt.setString(3, location.getWorld().getName());
            stmt.setDouble(4, location.getX());
            stmt.setDouble(5, location.getY());
            stmt.setDouble(6, location.getZ());
            stmt.setFloat(7, location.getYaw());
            stmt.setFloat(8, location.getPitch());
            
            stmt.executeUpdate();
            return existed;
            
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to set home: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get a home location for a player.
     * 
     * @param playerId The player's UUID
     * @param name The home name
     * @return The home location, or null if not found
     */
    public Location getHome(UUID playerId, String name) {
        String sql = "SELECT world, x, y, z, yaw, pitch FROM homes WHERE player_uuid = ? AND name = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerId.toString());
            stmt.setString(2, name.toLowerCase());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String worldName = rs.getString("world");
                    World world = Bukkit.getWorld(worldName);
                    
                    if (world == null) {
                        plugin.getLogger().warning("World '" + worldName + "' not found for home '" + name + "'");
                        return null;
                    }
                    
                    return new Location(
                        world,
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                    );
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get home: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Delete a home for a player.
     * 
     * @param playerId The player's UUID
     * @param name The home name
     * @return true if home was deleted
     */
    public boolean deleteHome(UUID playerId, String name) {
        String sql = "DELETE FROM homes WHERE player_uuid = ? AND name = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerId.toString());
            stmt.setString(2, name.toLowerCase());
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to delete home: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get all home names for a player.
     * 
     * @param playerId The player's UUID
     * @return List of home names
     */
    public List<String> getHomeNames(UUID playerId) {
        String sql = "SELECT name FROM homes WHERE player_uuid = ? ORDER BY name";
        List<String> names = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerId.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("name"));
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get home names: " + e.getMessage());
        }
        
        return names;
    }
    
    /**
     * Get the number of homes a player has.
     * 
     * @param playerId The player's UUID
     * @return The home count
     */
    public int getHomeCount(UUID playerId) {
        String sql = "SELECT COUNT(*) FROM homes WHERE player_uuid = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerId.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get home count: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Check if a home exists for a player.
     * 
     * @param playerId The player's UUID
     * @param name The home name
     * @return true if home exists
     */
    public boolean homeExists(UUID playerId, String name) {
        String sql = "SELECT 1 FROM homes WHERE player_uuid = ? AND name = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerId.toString());
            stmt.setString(2, name.toLowerCase());
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check home exists: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get the maximum number of homes allowed from config.
     * 
     * @return The max homes limit
     */
    public int getMaxHomes() {
        return plugin.getConfig().getInt("homes.max-homes", 3);
    }
}
