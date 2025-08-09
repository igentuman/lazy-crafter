package igentuman.lazycrafter.recipe;

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
import igentuman.lazycrafter.crafting.CraftingPlanner;
import igentuman.lazycrafter.crafting.CraftingSequence;

import java.util.*;

/**
 * Handles recursive checking of recipe availability based on player inventory
 */
public class RecipeChecker {
    private final RecipeManager recipeManager;
    private final Map<ResourceLocation, Boolean> recipeCache = new HashMap<>();
    
    public RecipeChecker(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }
    
    /**
     * Checks if a recipe can be crafted with current inventory, including nested crafting
     */
    public boolean canCraftRecipe(CraftingRecipe recipe, Inventory playerInventory) {
        if (!LazyCrafterConfig.isAutoCraftingEnabled()) {
            return false;
        }
        
        // Check cache first if caching is enabled
        if (LazyCrafterConfig.isCachingEnabled() && recipeCache.containsKey(recipe.getId())) {
            return recipeCache.get(recipe.getId());
        }
        
        try {
            // Use the crafting planner to check if the recipe can be crafted
            CraftingPlanner planner = new CraftingPlanner(recipeManager);
            CraftingSequence sequence = planner.planCrafting(recipe, playerInventory);
            
            boolean canCraft = !sequence.isEmpty();
            
            // Cache the result if caching is enabled
            if (LazyCrafterConfig.isCachingEnabled()) {
                recipeCache.put(recipe.getId(), canCraft);
            }
            
            return canCraft;
            
        } catch (Exception e) {
            LazyCrafter.logger.error("Error checking recipe: {}", recipe.getId(), e);
            return false;
        }
    }
    
    /**
     * Recursive method to check if recipe can be crafted
     */
    private boolean canCraftRecipeRecursive(CraftingRecipe recipe, Inventory playerInventory, 
                                          Set<ResourceLocation> visitedRecipes, int depth) {
        if (depth > LazyCrafterConfig.getMaxRecursionDepth()) {
            LazyCrafter.logger.warn("Max recursion depth reached for recipe: " + recipe.getId());
            return false;
        }
        
        // Prevent infinite recursion
        if (visitedRecipes.contains(recipe.getId())) {
            return false;
        }
        
        // Check cache first
        if (recipeCache.containsKey(recipe.getId())) {
            return recipeCache.get(recipe.getId());
        }
        
        visitedRecipes.add(recipe.getId());
        
        // Create a copy of inventory to simulate crafting
        Map<ItemStack, Integer> availableItems = getAvailableItems(playerInventory);
        
        // Check each ingredient
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) continue;
            
            boolean ingredientSatisfied = false;
            
            // Check if we have the ingredient directly
            for (ItemStack acceptedItem : ingredient.getItems()) {
                if (hasEnoughItems(availableItems, acceptedItem, 1)) {
                    consumeItem(availableItems, acceptedItem, 1);
                    ingredientSatisfied = true;
                    break;
                }
            }
            
            // If we don't have it directly, try to craft it
            if (!ingredientSatisfied) {
                ingredientSatisfied = tryToCraftIngredient(ingredient, availableItems, visitedRecipes, depth + 1);
            }
            
            if (!ingredientSatisfied) {
                visitedRecipes.remove(recipe.getId());
                recipeCache.put(recipe.getId(), false);
                return false;
            }
        }
        
        visitedRecipes.remove(recipe.getId());
        recipeCache.put(recipe.getId(), true);
        return true;
    }
    
    /**
     * Try to craft an ingredient from available items
     */
    private boolean tryToCraftIngredient(Ingredient ingredient, Map<ItemStack, Integer> availableItems,
                                       Set<ResourceLocation> visitedRecipes, int depth) {
        for (ItemStack targetItem : ingredient.getItems()) {
            // Find recipes that produce this item
            Collection<CraftingRecipe> recipes = recipeManager.getAllRecipesFor(RecipeType.CRAFTING);
            
            for (CraftingRecipe craftingRecipe : recipes) {
                if (ItemStack.isSameItem(craftingRecipe.getResultItem(Minecraft.getInstance().level.registryAccess()), targetItem)) {
                    // Create a temporary copy of available items for this attempt
                    Map<ItemStack, Integer> tempAvailableItems = new HashMap<>();
                    for (Map.Entry<ItemStack, Integer> entry : availableItems.entrySet()) {
                        tempAvailableItems.put(entry.getKey().copy(), entry.getValue());
                    }
                    
                    if (canCraftRecipeWithItems(craftingRecipe, tempAvailableItems, visitedRecipes, depth)) {
                        // If we can craft it, update the original available items
                        availableItems.clear();
                        availableItems.putAll(tempAvailableItems);
                        // Add the crafted item
                        addItem(availableItems, targetItem, craftingRecipe.getResultItem(Minecraft.getInstance().level.registryAccess()).getCount());
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Check if a recipe can be crafted with specific available items
     */
    private boolean canCraftRecipeWithItems(CraftingRecipe recipe, Map<ItemStack, Integer> availableItems,
                                          Set<ResourceLocation> visitedRecipes, int depth) {
        if (visitedRecipes.contains(recipe.getId())) {
            return false;
        }
        
        visitedRecipes.add(recipe.getId());
        
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) continue;
            
            boolean ingredientSatisfied = false;
            
            for (ItemStack acceptedItem : ingredient.getItems()) {
                if (hasEnoughItems(availableItems, acceptedItem, 1)) {
                    consumeItem(availableItems, acceptedItem, 1);
                    ingredientSatisfied = true;
                    break;
                }
            }
            
            if (!ingredientSatisfied) {
                ingredientSatisfied = tryToCraftIngredient(ingredient, availableItems, visitedRecipes, depth + 1);
            }
            
            if (!ingredientSatisfied) {
                visitedRecipes.remove(recipe.getId());
                return false;
            }
        }
        
        visitedRecipes.remove(recipe.getId());
        return true;
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
        for (Map.Entry<ItemStack, Integer> entry : availableItems.entrySet()) {
            if (ItemStack.isSameItem(entry.getKey(), targetItem)) {
                int newAmount = entry.getValue() - amount;
                if (newAmount <= 0) {
                    availableItems.remove(entry.getKey());
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
    
    /**
     * Clear the recipe cache
     */
    public void clearCache() {
        recipeCache.clear();
    }
}