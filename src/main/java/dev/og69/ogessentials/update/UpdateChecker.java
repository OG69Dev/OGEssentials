package dev.og69.ogessentials.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Update checker that uses GitHub Releases API to check for newer plugin versions.
 */
public class UpdateChecker {
    
    private final JavaPlugin plugin;
    private final String currentVersion;
    private final String repoOwner;
    private final String repoName;
    
    private final AtomicReference<UpdateResult> cachedResult = new AtomicReference<>();
    private volatile long lastCheckTime = 0;
    private static final long CACHE_DURATION = 3600000; // 1 hour in milliseconds
    
    /**
     * Create a new UpdateChecker instance.
     * 
     * @param plugin The plugin instance
     * @param currentVersion The current plugin version
     * @param repoOwner The GitHub repository owner
     * @param repoName The GitHub repository name
     */
    public UpdateChecker(JavaPlugin plugin, String currentVersion, String repoOwner, String repoName) {
        this.plugin = plugin;
        this.currentVersion = currentVersion;
        this.repoOwner = repoOwner;
        this.repoName = repoName;
    }
    
    /**
     * Check for updates asynchronously and call the callback with the result.
     * 
     * @param callback The callback to invoke with the update result
     */
    public void checkForUpdates(UpdateResultCallback callback) {
        // Check cache first
        long now = System.currentTimeMillis();
        if (cachedResult.get() != null && (now - lastCheckTime) < CACHE_DURATION) {
            UpdateResult cached = cachedResult.get();
            if (callback != null) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.onResult(cached));
            }
            return;
        }
        
        // Perform async check
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UpdateResult result = performCheck();
            
            // Update cache
            if (result != null) {
                cachedResult.set(result);
                lastCheckTime = System.currentTimeMillis();
            }
            
            // Call callback on main thread
            if (callback != null) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.onResult(result));
            }
        });
    }
    
    /**
     * Get the cached update result, if available.
     * 
     * @return The cached result, or null if no check has been performed yet
     */
    public UpdateResult getCachedResult() {
        return cachedResult.get();
    }
    
    /**
     * Perform the actual update check by querying GitHub API.
     * 
     * @return The update result, or null if check failed
     */
    private UpdateResult performCheck() {
        try {
            String apiUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/releases/latest";
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set timeouts
            connection.setConnectTimeout(10000); // 10 seconds
            connection.setReadTimeout(10000); // 10 seconds
            connection.setRequestProperty("User-Agent", "OGEssentials");
            connection.setRequestMethod("GET");
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                
                String latestVersion = json.get("tag_name").getAsString();
                String downloadUrl = json.get("html_url").getAsString();
                
                // Normalize version (remove "v" prefix if present)
                latestVersion = normalizeVersion(latestVersion);
                String normalizedCurrent = normalizeVersion(currentVersion);
                
                boolean updateAvailable = isNewerVersion(latestVersion, normalizedCurrent);
                
                return new UpdateResult(true, latestVersion, downloadUrl, updateAvailable, null);
                
            } else if (responseCode == 429) {
                // Rate limited
                return new UpdateResult(false, null, null, false, "Rate limited by GitHub API. Please try again later.");
            } else {
                // Other HTTP error
                return new UpdateResult(false, null, null, false, "HTTP " + responseCode + " error when checking for updates.");
            }
            
        } catch (java.net.SocketTimeoutException e) {
            return new UpdateResult(false, null, null, false, "Connection timeout while checking for updates.");
        } catch (java.net.UnknownHostException e) {
            return new UpdateResult(false, null, null, false, "Could not connect to GitHub. Check your internet connection.");
        } catch (java.io.IOException e) {
            return new UpdateResult(false, null, null, false, "Network error: " + e.getMessage());
        } catch (Exception e) {
            return new UpdateResult(false, null, null, false, "Failed to check for updates: " + e.getMessage());
        }
    }
    
    /**
     * Normalize a version string by removing "v" prefix and trimming.
     * 
     * @param version The version string to normalize
     * @return The normalized version
     */
    private String normalizeVersion(String version) {
        if (version == null) {
            return "";
        }
        version = version.trim();
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }
        return version;
    }
    
    /**
     * Check if the latest version is newer than the current version.
     * Uses semantic version comparison.
     * 
     * @param latest The latest version string
     * @param current The current version string
     * @return True if latest is newer than current
     */
    private boolean isNewerVersion(String latest, String current) {
        if (latest == null || current == null) {
            return false;
        }
        
        // If versions are equal, no update
        if (latest.equalsIgnoreCase(current)) {
            return false;
        }
        
        // Handle pre-release versions (e.g., "1.0.0-beta" is older than "1.0.0")
        String latestBase = latest.split("-")[0].split("\\+")[0];
        String currentBase = current.split("-")[0].split("\\+")[0];
        
        // Split into components
        String[] latestParts = latestBase.split("\\.");
        String[] currentParts = currentBase.split("\\.");
        
        // Compare each component
        int maxLength = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < maxLength; i++) {
            int latestNum = 0;
            int currentNum = 0;
            
            try {
                if (i < latestParts.length) {
                    latestNum = Integer.parseInt(latestParts[i]);
                }
            } catch (NumberFormatException e) {
                // If we can't parse, treat as 0
            }
            
            try {
                if (i < currentParts.length) {
                    currentNum = Integer.parseInt(currentParts[i]);
                }
            } catch (NumberFormatException e) {
                // If we can't parse, treat as 0
            }
            
            if (latestNum > currentNum) {
                return true;
            } else if (latestNum < currentNum) {
                return false;
            }
        }
        
        // If all components are equal, check if one has pre-release suffix
        boolean latestHasSuffix = latest.contains("-") || latest.contains("+");
        boolean currentHasSuffix = current.contains("-") || current.contains("+");
        
        // If current has suffix but latest doesn't, latest is newer
        if (currentHasSuffix && !latestHasSuffix) {
            return true;
        }
        
        // Otherwise, versions are equal or we can't determine
        return false;
    }
    
    /**
     * Result of an update check.
     */
    public static class UpdateResult {
        private final boolean success;
        private final String latestVersion;
        private final String downloadUrl;
        private final boolean updateAvailable;
        private final String errorMessage;
        
        public UpdateResult(boolean success, String latestVersion, String downloadUrl, boolean updateAvailable, String errorMessage) {
            this.success = success;
            this.latestVersion = latestVersion;
            this.downloadUrl = downloadUrl;
            this.updateAvailable = updateAvailable;
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getLatestVersion() {
            return latestVersion;
        }
        
        public String getDownloadUrl() {
            return downloadUrl;
        }
        
        public boolean isUpdateAvailable() {
            return updateAvailable;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
    /**
     * Callback interface for update check results.
     */
    @FunctionalInterface
    public interface UpdateResultCallback {
        /**
         * Called when an update check completes.
         * This is called on the main server thread.
         * 
         * @param result The update result, or null if check failed
         */
        void onResult(UpdateResult result);
    }
}
