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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches recipe chains and dependencies for fast lookup during gameplay
 */
public class RecipeCache {
    private static final RecipeCache INSTANCE = new RecipeCache();
    
    // Cache for recipe chains - maps recipe ID to its complete dependency chain
    private final Map<ResourceLocation, RecipeChain> recipeChains = new ConcurrentHashMap<>();
    
    // Cache for item-to-recipe mappings - maps item to recipes that can produce it
    private final Map<ItemStack, List<CraftingRecipe>> itemToRecipes = new ConcurrentHashMap<>();
    
    // Cache for recipe complexity scores
    private final Map<ResourceLocation, Integer> recipeComplexity = new ConcurrentHashMap<>();
    
    // Cache for circular recipes (to avoid them)
    private final Set<ResourceLocation> circularRecipes = ConcurrentHashMap.newKeySet();
    
    // Cache validity tracking
    private volatile boolean cacheValid = false;
    private volatile long lastCacheTime = 0;
    
    public static RecipeCache getInstance() {
        return INSTANCE;
    }
    
    private RecipeCache() {}
    
    /**
     * Check if the cache is valid and up-to-date
     */
    public boolean isCacheValid() {
        return cacheValid && LazyCrafterConfig.isCachingEnabled();
    }
    
    /**
     * Get the cached recipe chain for a recipe
     */
    public RecipeChain getRecipeChain(ResourceLocation recipeId) {
        if (!isCacheValid()) {
            return null;
        }
        return recipeChains.get(recipeId);
    }
    
    /**
     * Get cached recipes that can produce the given item
     */
    public List<CraftingRecipe> getRecipesForItem(ItemStack item) {
        if (!isCacheValid()) {
            return Collections.emptyList();
        }
        
        for (Map.Entry<ItemStack, List<CraftingRecipe>> entry : itemToRecipes.entrySet()) {
            if (ItemStack.isSameItem(entry.getKey(), item)) {
                return new ArrayList<>(entry.getValue());
            }
        }
        return Collections.emptyList();
    }
    
    /**
     * Get cached complexity score for a recipe
     */
    public Integer getRecipeComplexity(ResourceLocation recipeId) {
        if (!isCacheValid()) {
            return null;
        }
        return recipeComplexity.get(recipeId);
    }
    
    /**
     * Store a recipe chain in the cache
     */
    public void cacheRecipeChain(ResourceLocation recipeId, RecipeChain chain) {
        recipeChains.put(recipeId, chain);
    }
    
    /**
     * Store item-to-recipe mapping in the cache
     */
    public void cacheItemToRecipe(ItemStack item, CraftingRecipe recipe) {
        itemToRecipes.computeIfAbsent(item.copy(), k -> new ArrayList<>()).add(recipe);
    }
    
    /**
     * Store recipe complexity in the cache
     */
    public void cacheRecipeComplexity(ResourceLocation recipeId, int complexity) {
        recipeComplexity.put(recipeId, complexity);
    }
    
    /**
     * Store circular recipes to avoid them
     */
    public void storeCircularRecipes(Set<ResourceLocation> circular) {
        circularRecipes.clear();
        circularRecipes.addAll(circular);
    }
    
    /**
     * Check if a recipe is circular (should be avoided)
     */
    public boolean isCircularRecipe(ResourceLocation recipeId) {
        return circularRecipes.contains(recipeId);
    }
    
    /**
     * Get all circular recipes
     */
    public Set<ResourceLocation> getCircularRecipes() {
        return new HashSet<>(circularRecipes);
    }
    
    /**
     * Mark the cache as valid
     */
    public void markCacheValid() {
        this.cacheValid = true;
        this.lastCacheTime = System.currentTimeMillis();
        LazyCrafter.logger.info("Recipe cache marked as valid with {} recipe chains, {} item mappings, {} complexity scores, {} circular recipes", 
            recipeChains.size(), itemToRecipes.size(), recipeComplexity.size(), circularRecipes.size());
    }
    
    /**
     * Invalidate the cache
     */
    public void invalidateCache() {
        this.cacheValid = false;
        recipeChains.clear();
        itemToRecipes.clear();
        recipeComplexity.clear();
        circularRecipes.clear();
        LazyCrafter.logger.info("Recipe cache invalidated");
    }
    
    /**
     * Get cache statistics
     */
    public CacheStats getCacheStats() {
        return new CacheStats(
            recipeChains.size(),
            itemToRecipes.size(),
            recipeComplexity.size(),
            circularRecipes.size(),
            cacheValid,
            lastCacheTime
        );
    }
    
    /**
     * Represents a complete recipe chain with all dependencies
     */
    public static class RecipeChain {
        private final CraftingRecipe mainRecipe;
        private final List<CraftingRecipe> dependencies;
        private final Map<ResourceLocation, Integer> ingredientCounts;
        private final int totalComplexity;
        private final boolean canBeCrafted;
        
        public RecipeChain(CraftingRecipe mainRecipe, List<CraftingRecipe> dependencies, 
                          Map<ResourceLocation, Integer> ingredientCounts, int totalComplexity, boolean canBeCrafted) {
            this.mainRecipe = mainRecipe;
            this.dependencies = new ArrayList<>(dependencies);
            this.ingredientCounts = new HashMap<>(ingredientCounts);
            this.totalComplexity = totalComplexity;
            this.canBeCrafted = canBeCrafted;
        }
        
        public CraftingRecipe getMainRecipe() {
            return mainRecipe;
        }
        
        public List<CraftingRecipe> getDependencies() {
            return new ArrayList<>(dependencies);
        }
        
        public Map<ResourceLocation, Integer> getIngredientCounts() {
            return new HashMap<>(ingredientCounts);
        }
        
        public int getTotalComplexity() {
            return totalComplexity;
        }
        
        public boolean canBeCrafted() {
            return canBeCrafted;
        }
        
        /**
         * Get the complete crafting order (dependencies first)
         */
        public List<CraftingRecipe> getCraftingOrder() {
            List<CraftingRecipe> order = new ArrayList<>(dependencies);
            order.add(mainRecipe);
            return order;
        }
    }
    
    /**
     * Cache statistics for monitoring
     */
    public static class CacheStats {
        private final int recipeChainCount;
        private final int itemMappingCount;
        private final int complexityScoreCount;
        private final int circularRecipeCount;
        private final boolean isValid;
        private final long lastCacheTime;
        
        public CacheStats(int recipeChainCount, int itemMappingCount, int complexityScoreCount, 
                         int circularRecipeCount, boolean isValid, long lastCacheTime) {
            this.recipeChainCount = recipeChainCount;
            this.itemMappingCount = itemMappingCount;
            this.complexityScoreCount = complexityScoreCount;
            this.circularRecipeCount = circularRecipeCount;
            this.isValid = isValid;
            this.lastCacheTime = lastCacheTime;
        }
        
        public int getRecipeChainCount() { return recipeChainCount; }
        public int getItemMappingCount() { return itemMappingCount; }
        public int getComplexityScoreCount() { return complexityScoreCount; }
        public int getCircularRecipeCount() { return circularRecipeCount; }
        public boolean isValid() { return isValid; }
        public long getLastCacheTime() { return lastCacheTime; }
        
        @Override
        public String toString() {
            return String.format("CacheStats{chains=%d, mappings=%d, complexity=%d, circular=%d, valid=%s, age=%dms}", 
                recipeChainCount, itemMappingCount, complexityScoreCount, circularRecipeCount, isValid, 
                System.currentTimeMillis() - lastCacheTime);
        }
    }
}