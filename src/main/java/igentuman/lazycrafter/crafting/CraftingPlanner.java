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
                    CraftingRecipe subRecipe = findBestRecipeForIngredient(ingredient);
                    if (subRecipe != null) {
                        // Recursively plan the sub-recipe with higher priority (lower number = higher priority)
                        if (planRecipeRecursive(subRecipe, availableItems, sequence, 
                                              visitedRecipes, depth + 1, basePriority - 10)) {
                            // Add the crafted item to available items
                            ItemStack result = subRecipe.getResultItem(Minecraft.getInstance().level.registryAccess());
                            addItem(availableItems, result, result.getCount());
                            ingredientSatisfied = true;
                        }
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
        Collection<CraftingRecipe> recipes = recipeManager.getAllRecipesFor(RecipeType.CRAFTING);
        
        for (ItemStack targetItem : ingredient.getItems()) {
            // Find recipes that produce this item
            List<CraftingRecipe> candidateRecipes = new ArrayList<>();
            
            for (CraftingRecipe recipe : recipes) {
                ItemStack result = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
                if (ItemStack.isSameItem(result, targetItem)) {
                    candidateRecipes.add(recipe);
                }
            }
            
            // For now, just return the first valid recipe
            // In the future, we could implement more sophisticated selection logic
            if (!candidateRecipes.isEmpty()) {
                return candidateRecipes.get(0);
            }
        }
        
        return null;
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