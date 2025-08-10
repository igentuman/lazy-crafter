package igentuman.lazycrafter.crafting;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.resources.ResourceLocation;
import igentuman.lazycrafter.LazyCrafter;
import igentuman.lazycrafter.config.LazyCrafterConfig;

import java.util.*;

/**
 * Plans the optimal crafting sequence for auto-crafting
 */
public class CraftingPlanner {
    private final RecipeManager recipeManager;
    
    public CraftingPlanner(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }
    
    /**
     * Plan the crafting sequence for a target recipe
     */
    public CraftingSequence planCrafting(CraftingRecipe targetRecipe, Inventory playerInventory) {
        CraftingSequence sequence = new CraftingSequence();
        Map<ItemStack, Integer> availableItems = getAvailableItems(playerInventory);
        Set<ResourceLocation> visitedRecipes = new HashSet<>();
        
        if (planRecipeRecursive(targetRecipe, availableItems, sequence, visitedRecipes, 0, 100)) {
            LazyCrafter.logger.info("Successfully planned crafting sequence with {} operations", sequence.size());
            LazyCrafter.logger.debug("Crafting sequence: {}", sequence);
            return sequence;
        } else {
            LazyCrafter.logger.warn("Failed to plan crafting sequence for recipe: {}", targetRecipe.getId());
            return new CraftingSequence(); // Empty sequence
        }
    }
    
    /**
     * Recursively plan crafting for a recipe
     */
    private boolean planRecipeRecursive(CraftingRecipe recipe, Map<ItemStack, Integer> availableItems,
                                      CraftingSequence sequence, Set<ResourceLocation> visitedRecipes,
                                      int depth, int basePriority) {
        if (depth > LazyCrafterConfig.getMaxRecursionDepth()) {
            LazyCrafter.logger.warn("Max recursion depth reached for recipe: {}", recipe.getId());
            return false;
        }
        
        if (visitedRecipes.contains(recipe.getId())) {
            LazyCrafter.logger.warn("Circular dependency: {}", recipe.getId());
            return false; // Circular dependency
        }
        
        visitedRecipes.add(recipe.getId());
        
        try {
            // Check each ingredient
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) continue;
                
                boolean ingredientSatisfied = false;
                
                // Try to use existing items first
                for (ItemStack acceptedItem : ingredient.getItems()) {
                    if (hasEnoughItems(availableItems, acceptedItem, 1)) {
                        consumeItem(availableItems, acceptedItem, 1);
                        ingredientSatisfied = true;
                        break;
                    }
                }
                
                // If we don't have it, try to craft it
                if (!ingredientSatisfied) {
                    LazyCrafter.logger.debug("Looking for recipe to craft ingredient: {}", Arrays.toString(ingredient.getItems()));
                    CraftingRecipe subRecipe = findBestRecipeForIngredient(ingredient, visitedRecipes, availableItems);
                    if (subRecipe != null) {
                        LazyCrafter.logger.debug("Found sub-recipe {} for ingredient {}", subRecipe.getId(), Arrays.toString(ingredient.getItems()));
                        // Recursively plan the sub-recipe with higher priority (lower number = higher priority)
                        if (planRecipeRecursive(subRecipe, availableItems, sequence, 
                                              visitedRecipes, depth + 1, basePriority - 10)) {
                            // Add the crafted item to available items
                            try {
                                ItemStack result = subRecipe.getResultItem(Minecraft.getInstance().level.registryAccess());
                                addItem(availableItems, result, result.getCount());
                                ingredientSatisfied = true;
                                LazyCrafter.logger.debug("Successfully planned sub-recipe {} for ingredient {}", subRecipe.getId(), Arrays.toString(ingredient.getItems()));
                            } catch (Exception e) {
                                LazyCrafter.logger.warn("Error getting result item for recipe {}: {}", subRecipe.getId(), e.getMessage());
                                // Don't mark as satisfied if we can't get the result
                            }
                        } else {
                            LazyCrafter.logger.debug("Failed to plan sub-recipe {} for ingredient {}", subRecipe.getId(), Arrays.toString(ingredient.getItems()));
                        }
                    } else {
                        LazyCrafter.logger.debug("No recipe found for ingredient: {}", Arrays.toString(ingredient.getItems()));
                    }
                }
                
                if (!ingredientSatisfied) {
                    visitedRecipes.remove(recipe.getId());
                    return false;
                }
            }
            
            // Add this recipe to the sequence
            sequence.addOperation(recipe, 1, basePriority);
            
            visitedRecipes.remove(recipe.getId());
            return true;
            
        } catch (Exception e) {
            LazyCrafter.logger.error("Error planning recipe: {}", recipe.getId(), e);
            visitedRecipes.remove(recipe.getId());
            return false;
        }
    }
    
    /**
     * Find the best recipe to craft an ingredient
     */
    private CraftingRecipe findBestRecipeForIngredient(Ingredient ingredient) {
        return findBestRecipeForIngredient(ingredient, new HashSet<>());
    }
    
    /**
     * Find the best recipe to craft an ingredient, avoiding circular dependencies
     */
    private CraftingRecipe findBestRecipeForIngredient(Ingredient ingredient, Set<ResourceLocation> currentPath) {
        return findBestRecipeForIngredient(ingredient, currentPath, null);
    }
    
    /**
     * Find the best recipe to craft an ingredient, considering available items
     */
    private CraftingRecipe findBestRecipeForIngredient(Ingredient ingredient, Set<ResourceLocation> currentPath, Map<ItemStack, Integer> availableItems) {
        Collection<CraftingRecipe> recipes = recipeManager.getAllRecipesFor(RecipeType.CRAFTING);
        
        List<CraftingRecipe> validRecipes = new ArrayList<>();
        List<CraftingRecipe> circularRecipes = new ArrayList<>();
        
        for (ItemStack targetItem : ingredient.getItems()) {
            // Find recipes that produce this item
            for (CraftingRecipe recipe : recipes) {
                try {
                    ItemStack result = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
                    if (ItemStack.isSameItem(result, targetItem)) {
                        // Check if this recipe would create a circular dependency
                        if (currentPath.contains(recipe.getId())) {
                            circularRecipes.add(recipe);
                            LazyCrafter.logger.debug("Skipping recipe {} due to circular dependency", recipe.getId());
                        } else {
                            validRecipes.add(recipe);
                        }
                    }
                } catch (Exception e) {
                    // Skip recipes that cause errors (like the ComputerCraft turtle recipe)
                    LazyCrafter.logger.debug("Skipping recipe {} due to error: {}", recipe.getId(), e.getMessage());
                    continue;
                }
            }
        }
        
        // Prefer non-circular recipes
        if (!validRecipes.isEmpty()) {
            // Sort by complexity, considering available items
            validRecipes.sort((r1, r2) -> {
                int complexity1 = getRecipeComplexity(r1, currentPath, availableItems);
                int complexity2 = getRecipeComplexity(r2, currentPath, availableItems);
                return Integer.compare(complexity1, complexity2);
            });
            
            CraftingRecipe selectedRecipe = validRecipes.get(0);
            int selectedComplexity = getRecipeComplexity(selectedRecipe, currentPath, availableItems);
            
            LazyCrafter.logger.debug("Selected recipe {} (complexity: {}) from {} valid options for ingredient {}", 
                selectedRecipe.getId(), selectedComplexity, validRecipes.size(), Arrays.toString(ingredient.getItems()));
            
            // Log all considered recipes for debugging
            if (validRecipes.size() > 1) {
                LazyCrafter.logger.debug("Other recipe options considered:");
                for (int i = 1; i < Math.min(validRecipes.size(), 5); i++) {
                    CraftingRecipe recipe = validRecipes.get(i);
                    int complexity = getRecipeComplexity(recipe, currentPath, availableItems);
                    LazyCrafter.logger.debug("  {} (complexity: {})", recipe.getId(), complexity);
                }
                
                // If there's a significant complexity difference, log it as info
                if (validRecipes.size() > 1) {
                    int secondBestComplexity = getRecipeComplexity(validRecipes.get(1), currentPath, availableItems);
                    if (secondBestComplexity - selectedComplexity > 10) {
                        LazyCrafter.logger.info("Chose recipe {} (complexity: {}) over {} (complexity: {}) for ingredient {}", 
                            selectedRecipe.getId(), selectedComplexity, 
                            validRecipes.get(1).getId(), secondBestComplexity,
                            Arrays.toString(ingredient.getItems()));
                    }
                }
            }
            
            return selectedRecipe;
        }
        
        // If no valid recipes found, log the issue
        if (!circularRecipes.isEmpty()) {
            LazyCrafter.logger.warn("Only circular recipes available for ingredient: {}", 
                Arrays.toString(ingredient.getItems()));
        } else {
            LazyCrafter.logger.warn("No recipes found for ingredient: {}", 
                Arrays.toString(ingredient.getItems()));
        }
        
        return null;
    }
    
    /**
     * Calculate recipe complexity to help choose simpler recipes
     */
    private int getRecipeComplexity(CraftingRecipe recipe, Set<ResourceLocation> currentPath) {
        return getRecipeComplexity(recipe, currentPath, null);
    }
    
    /**
     * Calculate recipe complexity considering available items
     */
    private int getRecipeComplexity(CraftingRecipe recipe, Set<ResourceLocation> currentPath, Map<ItemStack, Integer> availableItems) {
        return getRecipeComplexityRecursive(recipe, currentPath, availableItems, 0, LazyCrafterConfig.getComplexityAnalysisDepth());
    }
    
    /**
     * Recursively calculate recipe complexity with depth limit
     */
    private int getRecipeComplexityRecursive(CraftingRecipe recipe, Set<ResourceLocation> currentPath, 
                                           Map<ItemStack, Integer> availableItems, int depth, int maxDepth) {
        int complexity = 0;
        int ingredientCount = 0;
        int availableIngredients = 0;
        int craftableIngredients = 0;
        int unavailableIngredients = 0;
        
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) continue;
            
            ingredientCount++;
            boolean hasIngredient = false;
            int bestIngredientComplexity = Integer.MAX_VALUE;
            
            // Check if we already have this ingredient
            if (availableItems != null) {
                for (ItemStack acceptedItem : ingredient.getItems()) {
                    if (hasEnoughItems(availableItems, acceptedItem, 1)) {
                        hasIngredient = true;
                        availableIngredients++;
                        bestIngredientComplexity = 1; // Minimal complexity for available items
                        break;
                    }
                }
            }
            
            if (hasIngredient) {
                complexity += bestIngredientComplexity;
                continue;
            }
            
            // We don't have the ingredient, find the best way to craft it
            boolean canBeCrafted = false;
            
            for (ItemStack item : ingredient.getItems()) {
                // Find all recipes that can produce this item
                Collection<CraftingRecipe> itemRecipes = findRecipesForItem(item, currentPath);
                
                for (CraftingRecipe itemRecipe : itemRecipes) {
                    if (depth < maxDepth) {
                        // Recursively calculate complexity for this sub-recipe
                        Set<ResourceLocation> newPath = new HashSet<>(currentPath);
                        newPath.add(recipe.getId());
                        
                        int subComplexity = getRecipeComplexityRecursive(itemRecipe, newPath, availableItems, depth + 1, maxDepth);
                        if (subComplexity < Integer.MAX_VALUE) {
                            canBeCrafted = true;
                            bestIngredientComplexity = Math.min(bestIngredientComplexity, subComplexity + 5); // Add penalty for nested crafting
                        }
                    } else {
                        // At max depth, just check if recipe exists
                        canBeCrafted = true;
                        bestIngredientComplexity = Math.min(bestIngredientComplexity, 10); // Default penalty for deep nesting
                    }
                }
            }
            
            if (canBeCrafted) {
                craftableIngredients++;
                complexity += bestIngredientComplexity;
            } else {
                // Can't craft this ingredient - very high penalty
                unavailableIngredients++;
                complexity += 50; // Higher penalty for truly unavailable ingredients
            }
        }
        
        // Additional complexity factors
        complexity += ingredientCount; // Base complexity for recipe size
        
        // Prefer recipes where we have more ingredients available
        if (ingredientCount > 0) {
            double availabilityRatio = (double) availableIngredients / ingredientCount;
            if (availabilityRatio >= 1.0) {
                complexity -= 5; // Bonus for having all ingredients
            } else if (availabilityRatio >= 0.5) {
                complexity -= 2; // Small bonus for having most ingredients
            }
        }
        
        // Heavy penalty for recipes with unavailable ingredients
        if (unavailableIngredients > 0) {
            complexity += unavailableIngredients * 20;
        }
        
        return Math.max(1, complexity); // Ensure complexity is at least 1
    }
    
    /**
     * Find all recipes that can produce a specific item
     */
    private Collection<CraftingRecipe> findRecipesForItem(ItemStack targetItem, Set<ResourceLocation> currentPath) {
        Collection<CraftingRecipe> allRecipes = recipeManager.getAllRecipesFor(RecipeType.CRAFTING);
        List<CraftingRecipe> validRecipes = new ArrayList<>();
        
        for (CraftingRecipe recipe : allRecipes) {
            // Skip recipes that would create circular dependencies
            if (currentPath.contains(recipe.getId())) {
                continue;
            }
            
            try {
                ItemStack result = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
                if (ItemStack.isSameItem(result, targetItem)) {
                    validRecipes.add(recipe);
                }
            } catch (Exception e) {
                // Skip recipes that cause errors
                LazyCrafter.logger.debug("Skipping recipe {} due to error: {}", recipe.getId(), e.getMessage());
                continue;
            }
        }
        
        return validRecipes;
    }
    
    /**
     * Check if there's a recipe for an item without recursion
     */
    private boolean hasRecipeForItem(ItemStack targetItem, Set<ResourceLocation> currentPath) {
        Collection<CraftingRecipe> recipes = recipeManager.getAllRecipesFor(RecipeType.CRAFTING);
        
        for (CraftingRecipe recipe : recipes) {
            // Skip recipes that would create circular dependencies
            if (currentPath.contains(recipe.getId())) {
                continue;
            }
            
            try {
                ItemStack result = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
                if (ItemStack.isSameItem(result, targetItem)) {
                    return true;
                }
            } catch (Exception e) {
                // Skip recipes that cause errors (like the ComputerCraft turtle recipe)
                LazyCrafter.logger.debug("Skipping recipe {} due to error: {}", recipe.getId(), e.getMessage());
                continue;
            }
        }
        
        return false;
    }
    
    /**
     * Get all available items from player inventory
     */
    private Map<ItemStack, Integer> getAvailableItems(Inventory inventory) {
        Map<ItemStack, Integer> items = new HashMap<>();
        
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                addItem(items, stack, stack.getCount());
            }
        }
        
        return items;
    }
    
    /**
     * Check if we have enough of a specific item
     */
    private boolean hasEnoughItems(Map<ItemStack, Integer> availableItems, ItemStack targetItem, int needed) {
        for (Map.Entry<ItemStack, Integer> entry : availableItems.entrySet()) {
            if (ItemStack.isSameItem(entry.getKey(), targetItem)) {
                return entry.getValue() >= needed;
            }
        }
        return false;
    }
    
    /**
     * Consume items from available items map
     */
    private void consumeItem(Map<ItemStack, Integer> availableItems, ItemStack targetItem, int amount) {
        Iterator<Map.Entry<ItemStack, Integer>> iterator = availableItems.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ItemStack, Integer> entry = iterator.next();
            if (ItemStack.isSameItem(entry.getKey(), targetItem)) {
                int newAmount = entry.getValue() - amount;
                if (newAmount <= 0) {
                    iterator.remove();
                } else {
                    entry.setValue(newAmount);
                }
                break;
            }
        }
    }
    
    /**
     * Add items to available items map
     */
    private void addItem(Map<ItemStack, Integer> availableItems, ItemStack item, int amount) {
        for (Map.Entry<ItemStack, Integer> entry : availableItems.entrySet()) {
            if (ItemStack.isSameItem(entry.getKey(), item)) {
                entry.setValue(entry.getValue() + amount);
                return;
            }
        }
        availableItems.put(item.copy(), amount);
    }
}