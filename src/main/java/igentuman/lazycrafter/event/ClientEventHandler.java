package igentuman.lazycrafter.event;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import igentuman.lazycrafter.LazyCrafter;
import igentuman.lazycrafter.recipe.AsyncRecipeProcessor;
import igentuman.lazycrafter.recipe.RecipeCache;
import igentuman.lazycrafter.recipe.RecipeChecker;

/**
 * Client-side event handler for the Lazy Crafter mod
 */
@Mod.EventBusSubscriber(modid = LazyCrafter.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandler {
    
    /**
     * Welcome message when player joins the world and start recipe processing
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                // Send a welcome message to let the player know the mod is loaded
                minecraft.player.sendSystemMessage(
                    Component.translatable("chat.lazycrafter.welcome")
                );
                
                LazyCrafter.logger.info("Lazy Crafter mod is active for player: {}", 
                    minecraft.player.getName().getString());
                
                // Start async recipe processing
                startRecipeProcessing();
            }
        }
    }
    
    /**
     * Handle world load events to trigger recipe processing
     */
    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null && minecraft.player != null) {
                // Start recipe processing when world loads
                startRecipeProcessing();
            }
        }
    }
    
    /**
     * Handle world unload events to clean up
     */
    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            // Invalidate caches when leaving world
            RecipeCache.getInstance().invalidateCache();
            clearRecipeCheckerCaches();
            LazyCrafter.logger.debug("Invalidated recipe caches on world unload");
        }
    }
    
    /**
     * Handle client connecting to world or server
     * This event is triggered when connecting to both single-player worlds and multiplayer servers
     */
    @SubscribeEvent
    public static void onClientConnect(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean isSinglePlayer = minecraft.hasSingleplayerServer();
        
        if (isSinglePlayer) {
            LazyCrafter.logger.info("Client connecting to single-player world, rebuilding circular recipes cache...");
        } else {
            LazyCrafter.logger.info("Client connecting to multiplayer server, rebuilding circular recipes cache...");
        }
        
        // Invalidate existing caches since we're connecting to a potentially different world/server
        RecipeCache.getInstance().invalidateCache();
        // Also clear RecipeChecker cache if it exists
        clearRecipeCheckerCaches();
        
        // Start recipe processing for the new environment
        minecraft.execute(() -> {
            // Delay slightly to ensure the world and recipe manager are fully loaded
            new Thread(() -> {
                try {
                    // Use shorter delay for single-player, longer for multiplayer
                    int delay = isSinglePlayer ? 500 : 1000;
                    Thread.sleep(delay);
                    
                    minecraft.execute(() -> {
                        startRecipeProcessingSilently();
                        String worldType = isSinglePlayer ? "single-player world" : "multiplayer server";
                        LazyCrafter.logger.info("Circular recipes cache rebuild initiated for {}", worldType);
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LazyCrafter.logger.warn("Recipe processing startup interrupted");
                }
            }, "LazyCrafter-Connect").start();
        });
    }
    
    /**
     * Handle client disconnecting from world or server
     */
    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        LazyCrafter.logger.info("Client disconnecting, invalidating circular recipes cache...");
        
        // Invalidate caches when disconnecting since recipes may be different on next connection
        RecipeCache.getInstance().invalidateCache();
        clearRecipeCheckerCaches();
        
        // Cancel any ongoing processing
        if (AsyncRecipeProcessor.getInstance().isProcessing()) {
            LazyCrafter.logger.info("Cancelling ongoing recipe processing due to disconnect");
            // The processor will handle cleanup internally
        }
    }
    
    /**
     * Start async recipe processing with player notification
     */
    private static void startRecipeProcessing() {
        startRecipeProcessingInternal(true);
    }
    
    /**
     * Start async recipe processing silently (without player notification)
     */
    private static void startRecipeProcessingSilently() {
        startRecipeProcessingInternal(false);
    }
    
    /**
     * Internal method to start async recipe processing
     */
    private static void startRecipeProcessingInternal(boolean notifyPlayer) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getRecipeManager() != null) {
            // Check if we already have a valid cache
            if (RecipeCache.getInstance().isCacheValid()) {
                LazyCrafter.logger.debug("Recipe cache is already valid, skipping processing");
                return;
            }
            
            // Start async processing
            AsyncRecipeProcessor.getInstance().startProcessing(minecraft.level.getRecipeManager())
                .thenRun(() -> {
                    LazyCrafter.logger.info("Recipe processing completed successfully");
                    
                    // Log cache statistics
                    RecipeCache.CacheStats stats = RecipeCache.getInstance().getCacheStats();
                    LazyCrafter.logger.info("Recipe analysis stats: {}", stats);
                    
                    if (notifyPlayer) {
                        // Notify player if they're still in game
                        if (minecraft.player != null) {
                            minecraft.player.sendSystemMessage(
                                Component.literal(String.format("§a[Lazy Crafter] Recipe analysis complete! Found %d circular recipes to avoid", 
                                    stats.getCircularRecipeCount()))
                            );
                        }
                    } else {
                        // Silent mode - just log
                        LazyCrafter.logger.debug("Recipe analysis complete! Found {} circular recipes to avoid", 
                            stats.getCircularRecipeCount());
                    }
                })
                .exceptionally(throwable -> {
                    LazyCrafter.logger.error("Recipe processing failed", throwable);
                    if (notifyPlayer && minecraft.player != null) {
                        minecraft.player.sendSystemMessage(
                            Component.literal("§c[Lazy Crafter] Recipe analysis failed - circular recipes may not be detected")
                        );
                    }
                    return null;
                });
        }
    }
    
    /**
     * Clear RecipeChecker caches
     * Since RecipeChecker instances are created per usage, this method serves as a utility
     * to ensure any cached data is cleared when connecting to new worlds/servers
     */
    private static void clearRecipeCheckerCaches() {
        // RecipeChecker instances are created per usage and maintain their own cache
        // The cache clearing will happen naturally when new instances are created
        // This method is here for consistency and future extensibility
        LazyCrafter.logger.debug("RecipeChecker caches will be cleared on next usage");
    }
}