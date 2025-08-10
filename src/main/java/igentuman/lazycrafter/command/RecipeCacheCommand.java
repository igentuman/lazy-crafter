package igentuman.lazycrafter.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import igentuman.lazycrafter.LazyCrafter;
import igentuman.lazycrafter.recipe.AsyncRecipeProcessor;
import igentuman.lazycrafter.recipe.RecipeCache;
import igentuman.lazycrafter.util.PerformanceMonitor;

import java.util.Set;

/**
 * Debug command for monitoring recipe cache status
 */
@Mod.EventBusSubscriber(modid = LazyCrafter.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class RecipeCacheCommand {
    
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("lazycrafter")
            .then(Commands.literal("cache")
                .then(Commands.literal("status")
                    .executes(RecipeCacheCommand::showCacheStatus))
                .then(Commands.literal("invalidate")
                    .executes(RecipeCacheCommand::invalidateCache))
                .then(Commands.literal("rebuild")
                    .executes(RecipeCacheCommand::rebuildCache))
                .then(Commands.literal("stats")
                    .executes(RecipeCacheCommand::showPerformanceStats))
                .then(Commands.literal("circular")
                    .executes(RecipeCacheCommand::showCircularRecipes)))
        );
    }
    
    private static int showCacheStatus(CommandContext<CommandSourceStack> context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return 0;
        
        // Show cache statistics
        RecipeCache.CacheStats cacheStats = RecipeCache.getInstance().getCacheStats();
        AsyncRecipeProcessor.ProcessingStats processingStats = AsyncRecipeProcessor.getInstance().getStats();
        
        minecraft.player.sendSystemMessage(Component.literal("§6=== Lazy Crafter Cache Status ==="));
        minecraft.player.sendSystemMessage(Component.literal(String.format("§7Cache Valid: %s%s", 
            cacheStats.isValid() ? "§aYes" : "§cNo", "§7")));
        minecraft.player.sendSystemMessage(Component.literal(String.format("§7Recipe Chains: §f%d", 
            cacheStats.getRecipeChainCount())));
        minecraft.player.sendSystemMessage(Component.literal(String.format("§7Item Mappings: §f%d", 
            cacheStats.getItemMappingCount())));
        minecraft.player.sendSystemMessage(Component.literal(String.format("§7Complexity Scores: §f%d", 
            cacheStats.getComplexityScoreCount())));
        minecraft.player.sendSystemMessage(Component.literal(String.format("§7Circular Recipes: §c%d", 
            cacheStats.getCircularRecipeCount())));
        
        if (cacheStats.isValid()) {
            long ageSeconds = (System.currentTimeMillis() - cacheStats.getLastCacheTime()) / 1000;
            minecraft.player.sendSystemMessage(Component.literal(String.format("§7Cache Age: §f%d seconds", ageSeconds)));
        }
        
        minecraft.player.sendSystemMessage(Component.literal(String.format("§7Processing: %s%s", 
            processingStats.isProcessing() ? "§eActive" : "§aIdle", "§7")));
        
        if (processingStats.isProcessing()) {
            minecraft.player.sendSystemMessage(Component.literal(String.format("§7Progress: §f%d/%d (%.1f%%)", 
                processingStats.getProcessedCount(), processingStats.getTotalCount(), 
                processingStats.getProgress() * 100)));
        }
        
        return 1;
    }
    
    private static int invalidateCache(CommandContext<CommandSourceStack> context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return 0;
        
        RecipeCache.getInstance().invalidateCache();
        minecraft.player.sendSystemMessage(Component.literal("§e[Lazy Crafter] Recipe cache invalidated"));
        
        return 1;
    }
    
    private static int rebuildCache(CommandContext<CommandSourceStack> context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return 0;
        
        if (AsyncRecipeProcessor.getInstance().isProcessing()) {
            minecraft.player.sendSystemMessage(Component.literal("§c[Lazy Crafter] Recipe processing already in progress"));
            return 0;
        }
        
        if (minecraft.level == null || minecraft.level.getRecipeManager() == null) {
            minecraft.player.sendSystemMessage(Component.literal("§c[Lazy Crafter] No recipe manager available"));
            return 0;
        }
        
        minecraft.player.sendSystemMessage(Component.literal("§e[Lazy Crafter] Starting recipe cache rebuild..."));
        
        AsyncRecipeProcessor.getInstance().startProcessing(minecraft.level.getRecipeManager())
            .thenRun(() -> {
                if (minecraft.player != null) {
                    RecipeCache.CacheStats stats = RecipeCache.getInstance().getCacheStats();
                    minecraft.player.sendSystemMessage(Component.literal(String.format(
                        "§a[Lazy Crafter] Analysis complete! %d chains, %d mappings, %d complexity scores, %d circular recipes found",
                        stats.getRecipeChainCount(), stats.getItemMappingCount(), stats.getComplexityScoreCount(), stats.getCircularRecipeCount())));
                }
            })
            .exceptionally(throwable -> {
                if (minecraft.player != null) {
                    minecraft.player.sendSystemMessage(Component.literal("§c[Lazy Crafter] Cache rebuild failed: " + throwable.getMessage()));
                }
                return null;
            });
        
        return 1;
    }
    
    private static int showPerformanceStats(CommandContext<CommandSourceStack> context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return 0;
        
        minecraft.player.sendSystemMessage(Component.literal("§6=== Performance Statistics ==="));
        
        long cacheHits = PerformanceMonitor.getInstance().getCounter("cache_hits");
        long cacheMisses = PerformanceMonitor.getInstance().getCounter("cache_misses");
        long recursiveSuccess = PerformanceMonitor.getInstance().getCounter("recursive_planning_success");
        long recursiveFailure = PerformanceMonitor.getInstance().getCounter("recursive_planning_failure");
        long circularDetected = PerformanceMonitor.getInstance().getCounter("circular_recipes_detected");
        long recipesAnalyzed = PerformanceMonitor.getInstance().getCounter("recipes_analyzed_clean");
        
        long totalLookups = cacheHits + cacheMisses;
        if (totalLookups > 0) {
            double hitRate = (double) cacheHits / totalLookups * 100;
            minecraft.player.sendSystemMessage(Component.literal(String.format("§7Cache Hit Rate: §f%.1f%% (%d/%d)", 
                hitRate, cacheHits, totalLookups)));
        }
        
        long totalRecursive = recursiveSuccess + recursiveFailure;
        if (totalRecursive > 0) {
            double successRate = (double) recursiveSuccess / totalRecursive * 100;
            minecraft.player.sendSystemMessage(Component.literal(String.format("§7Recursive Success Rate: §f%.1f%% (%d/%d)", 
                successRate, recursiveSuccess, totalRecursive)));
        }
        
        long cacheTime = PerformanceMonitor.getInstance().getTiming("cache_lookup");
        long recursiveTime = PerformanceMonitor.getInstance().getTiming("recursive_planning");
        
        if (cacheHits > 0) {
            minecraft.player.sendSystemMessage(Component.literal(String.format("§7Avg Cache Lookup Time: §f%.2fms", 
                (double) cacheTime / cacheHits)));
        }
        
        if (recursiveSuccess > 0) {
            minecraft.player.sendSystemMessage(Component.literal(String.format("§7Avg Recursive Planning Time: §f%.2fms", 
                (double) recursiveTime / recursiveSuccess)));
        }
        
        if (cacheHits > 0 && recursiveSuccess > 0) {
            double speedup = (double) recursiveTime / recursiveSuccess / ((double) cacheTime / cacheHits);
            minecraft.player.sendSystemMessage(Component.literal(String.format("§7Cache Speedup: §a%.1fx faster", speedup)));
        }
        
        // Show circular recipe detection stats
        long totalAnalyzed = circularDetected + recipesAnalyzed;
        if (totalAnalyzed > 0) {
            double circularRate = (double) circularDetected / totalAnalyzed * 100;
            minecraft.player.sendSystemMessage(Component.literal(String.format("§7Circular Recipe Rate: §c%.1f%% (%d/%d)", 
                circularRate, circularDetected, totalAnalyzed)));
        }
        
        return 1;
    }
    
    private static int showCircularRecipes(CommandContext<CommandSourceStack> context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return 0;
        
        Set<ResourceLocation> circularRecipes = RecipeCache.getInstance().getCircularRecipes();
        
        minecraft.player.sendSystemMessage(Component.literal("§6=== Circular Recipes ==="));
        
        if (circularRecipes.isEmpty()) {
            minecraft.player.sendSystemMessage(Component.literal("§aNo circular recipes detected!"));
        } else {
            minecraft.player.sendSystemMessage(Component.literal(String.format("§cFound %d circular recipes:", circularRecipes.size())));
            
            int count = 0;
            for (ResourceLocation recipeId : circularRecipes) {
                minecraft.player.sendSystemMessage(Component.literal(String.format("§7- %s", recipeId)));
                count++;
                
                // Limit output to prevent spam
                if (count >= 20) {
                    int remaining = circularRecipes.size() - count;
                    if (remaining > 0) {
                        minecraft.player.sendSystemMessage(Component.literal(String.format("§7... and %d more", remaining)));
                    }
                    break;
                }
            }
            
            minecraft.player.sendSystemMessage(Component.literal("§eThese recipes are automatically avoided during auto-crafting"));
        }
        
        return 1;
    }
}