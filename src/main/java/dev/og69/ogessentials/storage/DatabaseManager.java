package dev.og69.ogessentials.storage;

import dev.og69.ogessentials.OGEssentials;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite database connections and table initialization.
 * 
 * Uses a single data.db file for all plugin data storage.
 */
public class DatabaseManager {
    
    private final OGEssentials plugin;
    private final File databaseFile;
    private Connection connection;
    
    /**
     * Create a new database manager.
     * 
     * @param plugin The plugin instance
     */
    public DatabaseManager(OGEssentials plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "data.db");
    }
    
    /**
     * Initialize the database connection and create tables.
     * 
     * @return true if initialization was successful
     */
    public boolean initialize() {
        try {
            // Ensure data folder exists
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            // Load SQLite driver
            Class.forName("org.sqlite.JDBC");
            
            // Create connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            
            // Create tables
            createTables();
            
            plugin.getLogger().info("Database initialized successfully!");
            return true;
            
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite JDBC driver not found: " + e.getMessage());
            return false;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Create all database tables if they don't exist.
     */
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Homes table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS homes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    name TEXT NOT NULL,
                    world TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL,
                    yaw REAL DEFAULT 0,
                    pitch REAL DEFAULT 0,
                    created_at INTEGER DEFAULT (strftime('%s', 'now')),
                    UNIQUE(player_uuid, name)
                )
                """);
            
            // Create index for faster lookups
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_homes_player ON homes(player_uuid)");
        }
    }
    
    /**
     * Get the database connection.
     * Reconnects if the connection is closed.
     * 
     * @return The database connection
     * @throws SQLException if connection fails
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        }
        return connection;
    }
    
    /**
     * Close the database connection.
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to close database connection: " + e.getMessage());
            }
        }
    }
    
    /**
     * Check if the database is connected.
     * 
     * @return true if connected
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
