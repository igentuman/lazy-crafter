package igentuman.lazycrafter.recipe;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.resources.ResourceLocation;
import igentuman.lazycrafter.LazyCrafter;
import igentuman.lazycrafter.config.LazyCrafterConfig;
import igentuman.lazycrafter.util.PerformanceMonitor;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Asynchronously hunts for circular recipe dependencies to prevent infinite loops
 */
public class AsyncRecipeProcessor {
    private static final AsyncRecipeProcessor INSTANCE = new AsyncRecipeProcessor();
    
    private final ExecutorService executorService;
    private final AtomicInteger processedRecipes = new AtomicInteger(0);
    private final AtomicInteger totalRecipes = new AtomicInteger(0);
    
    private volatile boolean isProcessing = false;
    private volatile CompletableFuture<Void> currentProcessingTask = null;
    
    public static AsyncRecipeProcessor getInstance() {
        return INSTANCE;
    }
    
    private AsyncRecipeProcessor() {
        // Create a thread pool for recipe processing
        this.executorService = Executors.newFixedThreadPool(
            LazyCrafterConfig.getAsyncThreadCount(),
            r -> {
                Thread t = new Thread(r, "LazyCrafter-RecipeProcessor");
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1); // Lower priority to not interfere with game
                return t;
            }
        );
    }
    
    /**
     * Start asynchronous circular recipe detection
     */
    public CompletableFuture<Void> startProcessing(RecipeManager recipeManager) {
        if (isProcessing) {
            LazyCrafter.logger.warn("Recipe processing already in progress");
            return currentProcessingTask != null ? currentProcessingTask : CompletableFuture.completedFuture(null);
        }
        
        if (!LazyCrafterConfig.isAsyncProcessingEnabled()) {
            LazyCrafter.logger.info("Async processing is disabled, skipping circular recipe detection");
            return CompletableFuture.completedFuture(null);
        }
        
        LazyCrafter.logger.info("Starting async circular recipe detection...");
        
        isProcessing = true;
        processedRecipes.set(0);
        
        // Invalidate existing cache
        RecipeCache.getInstance().invalidateCache();
        
        currentProcessingTask = CompletableFuture.runAsync(() -> {
            try {
                huntCircularRecipes(recipeManager);
            } catch (Exception e) {
                LazyCrafter.logger.error("Error during circular recipe detection", e);
            } finally {
                isProcessing = false;
            }
        }, executorService);
        
        return currentProcessingTask;
    }
    
    /**
     * Hunt for circular recipe dependencies
     */
    private void huntCircularRecipes(RecipeManager recipeManager) {
        long startTime = System.currentTimeMillis();
        
        try {
            Collection<CraftingRecipe> allRecipes = recipeManager.getAllRecipesFor(RecipeType.CRAFTING);
            totalRecipes.set(allRecipes.size());
            
            LazyCrafter.logger.info("Analyzing {} recipes for circular dependencies...", totalRecipes.get());
            
            // Build item-to-recipe mappings for circular detection
            Map<ItemStack, List<CraftingRecipe>> itemToRecipes = buildItemToRecipeMappings(allRecipes);
            
            // Hunt for circular dependencies in parallel
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            Set<ResourceLocation> circularRecipes = ConcurrentHashMap.newKeySet();
            
            for (CraftingRecipe recipe : allRecipes) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        if (detectCircularDependency(recipe, recipeManager, itemToRecipes)) {
                            circularRecipes.add(recipe.getId());
                            LazyCrafter.logger.warn("Circular dependency detected in recipe: {}", recipe.getId());
                            PerformanceMonitor.getInstance().incrementCounter("circular_recipes_detected");
                        } else {
                            PerformanceMonitor.getInstance().incrementCounter("recipes_analyzed_clean");
                        }
                        
                        int processed = processedRecipes.incrementAndGet();
                        
                        // Log progress every 100 recipes and notify player every 500
                        if (processed % 100 == 0) {
                            LazyCrafter.logger.debug("Analyzed {}/{} recipes ({}%)", 
                                processed, totalRecipes.get(), 
                                (processed * 100) / totalRecipes.get());
                        }
                        
                        if (processed % 500 == 0) {
                            // Notify player of progress on main thread
                            Minecraft.getInstance().execute(() -> {
                                if (Minecraft.getInstance().player != null) {
                                    Minecraft.getInstance().player.sendSystemMessage(
                                        net.minecraft.network.chat.Component.literal(
                                            String.format("§7[Lazy Crafter] Analyzing recipes... %d/%d (%.0f%%)", 
                                                processed, totalRecipes.get(), (processed * 100.0) / totalRecipes.get())
                                        )
                                    );
                                }
                            });
                        }
                    } catch (Exception e) {
                        LazyCrafter.logger.debug("Error analyzing recipe {}: {}", recipe.getId(), e.getMessage());
                    }
                }, executorService);
                
                futures.add(future);
            }
            
            // Wait for all recipes to be analyzed
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            // Store circular recipes in cache for avoidance
            RecipeCache.getInstance().storeCircularRecipes(circularRecipes);
            RecipeCache.getInstance().markCacheValid();
            
            long duration = System.currentTimeMillis() - startTime;
            LazyCrafter.logger.info("Completed circular recipe analysis in {}ms. Found {} circular recipes out of {} total", 
                duration, circularRecipes.size(), processedRecipes.get());
            
            if (!circularRecipes.isEmpty()) {
                LazyCrafter.logger.warn("Circular recipes found: {}", circularRecipes);
            }
            
        } catch (Exception e) {
            LazyCrafter.logger.error("Failed to analyze recipes for circular dependencies", e);
        }
    }
    
    /**
     * Build mappings from items to recipes that can produce them
     */
    private Map<ItemStack, List<CraftingRecipe>> buildItemToRecipeMappings(Collection<CraftingRecipe> allRecipes) {
        LazyCrafter.logger.debug("Building item-to-recipe mappings for circular detection...");
        
        Map<ItemStack, List<CraftingRecipe>> itemToRecipes = new ConcurrentHashMap<>();
        
        for (CraftingRecipe recipe : allRecipes) {
            try {
                ItemStack result = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
                if (!result.isEmpty()) {
                    // Find existing entry or create new one
                    ItemStack key = null;
                    for (ItemStack existingKey : itemToRecipes.keySet()) {
                        if (ItemStack.isSameItem(existingKey, result)) {
                            key = existingKey;
                            break;
                        }
                    }
                    
                    if (key == null) {
                        key = result.copy();
                        itemToRecipes.put(key, new ArrayList<>());
                    }
                    
                    itemToRecipes.get(key).add(recipe);
                }
            } catch (Exception e) {
                // Skip recipes that cause errors
                LazyCrafter.logger.debug("Skipping recipe {} for item mapping: {}", recipe.getId(), e.getMessage());
            }
        }
        
        LazyCrafter.logger.debug("Completed item-to-recipe mappings: {} unique items", itemToRecipes.size());
        return itemToRecipes;
    }
    
    /**
     * Detect circular dependency in a recipe
     */
    private boolean detectCircularDependency(CraftingRecipe recipe, RecipeManager recipeManager, 
                                           Map<ItemStack, List<CraftingRecipe>> itemToRecipes) {
        Set<ResourceLocation> visitedRecipes = new HashSet<>();
        return detectCircularDependencyRecursive(recipe, itemToRecipes, visitedRecipes, 0, 
            LazyCrafterConfig.getCircularDetectionDepth());
    }
    
    /**
     * Recursively detect circular dependencies
     */
    private boolean detectCircularDependencyRecursive(CraftingRecipe recipe, 
                                                     Map<ItemStack, List<CraftingRecipe>> itemToRecipes,
                                                     Set<ResourceLocation> visitedRecipes, 
                                                     int depth, int maxDepth) {
        
        // Prevent infinite recursion
        if (depth > maxDepth) {
            return false;
        }
        
        // Check if we've already visited this recipe (circular dependency found!)
        if (visitedRecipes.contains(recipe.getId())) {
            LazyCrafter.logger.debug("Circular dependency detected: {} -> {}", 
                visitedRecipes, recipe.getId());
            return true;
        }
        
        visitedRecipes.add(recipe.getId());
        
        try {
            // Check each ingredient of this recipe
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) continue;
                
                // For each possible item that satisfies this ingredient
                for (ItemStack acceptedItem : ingredient.getItems()) {
                    // Find recipes that can produce this item
                    List<CraftingRecipe> producingRecipes = findRecipesForItem(acceptedItem, itemToRecipes);
                    
                    // Check each producing recipe for circular dependencies
                    for (CraftingRecipe producingRecipe : producingRecipes) {
                        if (detectCircularDependencyRecursive(producingRecipe, itemToRecipes, 
                                new HashSet<>(visitedRecipes), depth + 1, maxDepth)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LazyCrafter.logger.debug("Error checking circular dependency for {}: {}", 
                recipe.getId(), e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Find recipes that can produce the given item
     */
    private List<CraftingRecipe> findRecipesForItem(ItemStack targetItem, Map<ItemStack, List<CraftingRecipe>> itemToRecipes) {
        for (Map.Entry<ItemStack, List<CraftingRecipe>> entry : itemToRecipes.entrySet()) {
            if (ItemStack.isSameItem(entry.getKey(), targetItem)) {
                return entry.getValue();
            }
        }
        return Collections.emptyList();
    }
    

    
    /**
     * Check if processing is currently running
     */
    public boolean isProcessing() {
        return isProcessing;
    }
    
    /**
     * Get processing progress (0.0 to 1.0)
     */
    public double getProgress() {
        int total = totalRecipes.get();
        if (total == 0) return 0.0;
        return (double) processedRecipes.get() / total;
    }
    
    /**
     * Get processing statistics
     */
    public ProcessingStats getStats() {
        return new ProcessingStats(processedRecipes.get(), totalRecipes.get(), isProcessing);
    }
    
    /**
     * Shutdown the processor
     */
    public void shutdown() {
        if (currentProcessingTask != null && !currentProcessingTask.isDone()) {
            currentProcessingTask.cancel(true);
        }
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Processing statistics
     */
    public static class ProcessingStats {
        private final int processedCount;
        private final int totalCount;
        private final boolean isProcessing;
        
        public ProcessingStats(int processedCount, int totalCount, boolean isProcessing) {
            this.processedCount = processedCount;
            this.totalCount = totalCount;
            this.isProcessing = isProcessing;
        }
        
        public int getProcessedCount() { return processedCount; }
        public int getTotalCount() { return totalCount; }
        public boolean isProcessing() { return isProcessing; }
        public double getProgress() { return totalCount > 0 ? (double) processedCount / totalCount : 0.0; }
        
        @Override
        public String toString() {
            return String.format("ProcessingStats{processed=%d, total=%d, progress=%.1f%%, active=%s}", 
                processedCount, totalCount, getProgress() * 100, isProcessing);
        }
    }
}