package dev.og69.ogessentials.managers;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the last locations of players for the /back command.
 */
public class BackManager {

    private final Map<UUID, Location> lastLocations = new HashMap<>();

    /**
     * Set the last location for a player.
     *
     * @param uuid     The player's UUID
     * @param location The location to save
     */
    public void setLastLocation(UUID uuid, Location location) {
        lastLocations.put(uuid, location);
    }

    /**
     * Get the last location for a player.
     *
     * @param uuid The player's UUID
     * @return The last location, or null if not set
     */
    public Location getLastLocation(UUID uuid) {
        return lastLocations.get(uuid);
    }

    /**
     * Clear the last location for a player.
     *
     * @param uuid The player's UUID
     */
    public void clearLastLocation(UUID uuid) {
        lastLocations.remove(uuid);
    }

    /**
     * Clear all data.
     */
    public void cleanup() {
        lastLocations.clear();
    }
}
