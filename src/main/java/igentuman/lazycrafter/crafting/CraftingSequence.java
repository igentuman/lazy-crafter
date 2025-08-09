package igentuman.lazycrafter.crafting;

import net.minecraft.world.item.crafting.CraftingRecipe;
import igentuman.lazycrafter.LazyCrafter;

import java.util.*;

/**
 * Manages a sequence of crafting operations for auto-crafting
 */
public class CraftingSequence {
    private final List<CraftingOperation> operations = new ArrayList<>();
    private final Set<CraftingRecipe> processedRecipes = new HashSet<>();
    
    /**
     * Add a crafting operation to the sequence
     */
    public void addOperation(CraftingRecipe recipe, int quantity, int priority) {
        if (!processedRecipes.contains(recipe)) {
            operations.add(new CraftingOperation(recipe, quantity, priority));
            processedRecipes.add(recipe);
            LazyCrafter.logger.debug("Added crafting operation: {} x{} (priority: {})", 
                recipe.getId(), quantity, priority);
        }
    }
    
    /**
     * Get the operations sorted by priority (dependencies first)
     */
    public List<CraftingOperation> getSortedOperations() {
        List<CraftingOperation> sorted = new ArrayList<>(operations);
        sorted.sort(Comparator.comparingInt(CraftingOperation::getPriority));
        return sorted;
    }
    
    /**
     * Get all operations
     */
    public List<CraftingOperation> getOperations() {
        return new ArrayList<>(operations);
    }
    
    /**
     * Check if the sequence is empty
     */
    public boolean isEmpty() {
        return operations.isEmpty();
    }
    
    /**
     * Get the total number of operations
     */
    public int size() {
        return operations.size();
    }
    
    /**
     * Clear all operations
     */
    public void clear() {
        operations.clear();
        processedRecipes.clear();
    }
    
    /**
     * Check if a recipe is already in the sequence
     */
    public boolean contains(CraftingRecipe recipe) {
        return processedRecipes.contains(recipe);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CraftingSequence{\n");
        for (CraftingOperation op : getSortedOperations()) {
            sb.append("  ").append(op).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}