package dev.og69.ogessentials.tasks;

import dev.og69.ogessentials.managers.TpaManager;

/**
 * Scheduled task that checks for expired TPA requests.
 * 
 * Runs every 5 seconds (100 ticks) to match script behavior.
 */
public class TpaExpiryTask implements Runnable {
    
    private final TpaManager tpaManager;
    
    /**
     * Create a new TPA expiry task.
     * 
     * @param tpaManager The TPA manager to check
     */
    public TpaExpiryTask(TpaManager tpaManager) {
        this.tpaManager = tpaManager;
    }
    
    @Override
    public void run() {
        tpaManager.expireOldRequests();
    }
}
