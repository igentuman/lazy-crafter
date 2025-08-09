package igentuman.lazycrafter.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import igentuman.lazycrafter.LazyCrafter;

/**
 * Utility methods for crafting operations
 */
public class CraftingUtils {
    
    /**
     * Check if two ItemStacks are the same item (ignoring count)
     */
    public static boolean isSameItem(ItemStack stack1, ItemStack stack2) {
        return ItemStack.isSameItem(stack1, stack2);
    }
    
    /**
     * Check if an ItemStack matches an Ingredient
     */
    public static boolean matchesIngredient(ItemStack stack, Ingredient ingredient) {
        return ingredient.test(stack);
    }
    
    /**
     * Get a human-readable description of a recipe
     */
    public static String getRecipeDescription(CraftingRecipe recipe) {
        StringBuilder sb = new StringBuilder();
        sb.append("Recipe[").append(recipe.getId()).append("] -> ");
        
        try {
            ItemStack result = recipe.getResultItem(null);
            if (!result.isEmpty()) {
                sb.append(result.getCount()).append("x ").append(result.getHoverName().getString());
            } else {
                sb.append("Unknown Result");
            }
        } catch (Exception e) {
            sb.append("Error getting result");
        }
        
        return sb.toString();
    }
    
    /**
     * Log recipe information for debugging
     */
    public static void logRecipeInfo(CraftingRecipe recipe) {
        LazyCrafter.logger.debug("Recipe Info: {}", getRecipeDescription(recipe));
        
        int ingredientCount = 0;
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (!ingredient.isEmpty()) {
                ingredientCount++;
                LazyCrafter.logger.debug("  Ingredient {}: {} possible items", 
                    ingredientCount, ingredient.getItems().length);
            }
        }
    }
    
    /**
     * Check if a recipe is valid and has ingredients
     */
    public static boolean isValidRecipe(CraftingRecipe recipe) {
        if (recipe == null) return false;
        
        try {
            // Check if recipe has a result
            ItemStack result = recipe.getResultItem(null);
            if (result.isEmpty()) return false;
            
            // Check if recipe has at least one ingredient
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (!ingredient.isEmpty()) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            LazyCrafter.logger.warn("Error validating recipe {}: {}", recipe.getId(), e.getMessage());
            return false;
        }
    }
}