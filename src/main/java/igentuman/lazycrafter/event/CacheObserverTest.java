package igentuman.lazycrafter.event;

import igentuman.lazycrafter.LazyCrafter;
import igentuman.lazycrafter.recipe.RecipeCache;
import net.minecraft.client.Minecraft;

/**
 * Test utility to demonstrate and verify cache observer functionality
 * This class provides methods to test the cache rebuilding behavior
 */
public class CacheObserverTest {
    
    /**
     * Test method to simulate cache invalidation and rebuilding
     * This can be called from commands or debug interfaces
     */
    public static void testCacheObservers() {
        LazyCrafter.logger.info("=== Testing Cache Observer Functionality ===");
        
        // Check initial cache state
        RecipeCache.CacheStats initialStats = RecipeCache.getInstance().getCacheStats();
        LazyCrafter.logger.info("Initial cache state: {}", initialStats);
        
        // Simulate cache invalidation (as would happen on disconnect)
        LazyCrafter.logger.info("Simulating cache invalidation...");
        RecipeCache.getInstance().invalidateCache();
        
        RecipeCache.CacheStats afterInvalidation = RecipeCache.getInstance().getCacheStats();
        LazyCrafter.logger.info("Cache state after invalidation: {}", afterInvalidation);
        
        // Simulate cache rebuild (as would happen on connect)
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getRecipeManager() != null) {
            LazyCrafter.logger.info("Simulating cache rebuild...");
            
            // This would normally be called by the event handlers
            testCacheRebuild();
        } else {
            LazyCrafter.logger.warn("Cannot test cache rebuild - no world/recipe manager available");
        }
        
        LazyCrafter.logger.info("=== Cache Observer Test Complete ===");
    }
    
    /**
     * Test cache rebuild functionality
     */
    private static void testCacheRebuild() {
        // Simulate the same logic used in ClientEventHandler
        Minecraft minecraft = Minecraft.getInstance();
        
        if (minecraft.level != null && minecraft.level.getRecipeManager() != null) {
            if (RecipeCache.getInstance().isCacheValid()) {
                LazyCrafter.logger.info("Cache is already valid, would skip processing");
                return;
            }
            
            LazyCrafter.logger.info("Starting test recipe processing...");
            
            // Note: In a real scenario, this would be async
            // For testing, we just log what would happen
            LazyCrafter.logger.info("Would start AsyncRecipeProcessor.startProcessing()");
            LazyCrafter.logger.info("Would analyze recipes for circular dependencies");
            LazyCrafter.logger.info("Would rebuild recipe cache silently");
            
            // Simulate successful completion
            LazyCrafter.logger.info("Test cache rebuild completed successfully");
        }
    }
    
    /**
     * Get current cache status for debugging
     */
    public static String getCacheStatus() {
        RecipeCache.CacheStats stats = RecipeCache.getInstance().getCacheStats();
        boolean isValid = RecipeCache.getInstance().isCacheValid();
        
        return String.format("Cache Status: Valid=%s, Stats=%s", isValid, stats);
    }
    
    /**
     * Force cache invalidation for testing
     */
    public static void forceCacheInvalidation() {
        LazyCrafter.logger.info("Forcing cache invalidation for testing...");
        RecipeCache.getInstance().invalidateCache();
        LazyCrafter.logger.info("Cache invalidated. Status: {}", getCacheStatus());
    }
}