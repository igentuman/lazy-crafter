package igentuman.lazycrafter.crafting;

import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.ItemStack;

/**
 * Represents a single crafting operation in the auto-crafting sequence
 */
public class CraftingOperation {
    private final CraftingRecipe recipe;
    private final int quantity;
    private final int priority; // Lower numbers = higher priority (craft first)
    
    public CraftingOperation(CraftingRecipe recipe, int quantity, int priority) {
        this.recipe = recipe;
        this.quantity = quantity;
        this.priority = priority;
    }
    
    public CraftingRecipe getRecipe() {
        return recipe;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public ItemStack getResult() {
        return recipe.getResultItem(null);
    }
    
    @Override
    public String toString() {
        return "CraftingOperation{" +
                "recipe=" + recipe.getId() +
                ", quantity=" + quantity +
                ", priority=" + priority +
                '}';
    }
}