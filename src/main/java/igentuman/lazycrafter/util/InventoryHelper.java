package igentuman.lazycrafter.util;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for inventory operations
 */
public class InventoryHelper {
    
    /**
     * Count how many of a specific item are available in the inventory
     */
    public static int countItems(Inventory inventory, ItemStack targetItem) {
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (ItemStack.isSameItem(stack, targetItem)) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    /**
     * Count how many items matching an ingredient are available
     */
    public static int countIngredient(Inventory inventory, Ingredient ingredient) {
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && ingredient.test(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    /**
     * Get a map of all items in the inventory with their counts
     */
    public static Map<ItemStack, Integer> getInventoryContents(Inventory inventory) {
        Map<ItemStack, Integer> contents = new HashMap<>();
        
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                // Check if we already have this item type
                boolean found = false;
                for (Map.Entry<ItemStack, Integer> entry : contents.entrySet()) {
                    if (ItemStack.isSameItem(entry.getKey(), stack)) {
                        entry.setValue(entry.getValue() + stack.getCount());
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    contents.put(stack.copy(), stack.getCount());
                }
            }
        }
        
        return contents;
    }
    
    /**
     * Find the first slot containing an item that matches the ingredient
     */
    public static int findIngredientSlot(Inventory inventory, Ingredient ingredient) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && ingredient.test(stack)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Check if the inventory has enough of an ingredient
     */
    public static boolean hasEnoughIngredient(Inventory inventory, Ingredient ingredient, int needed) {
        return countIngredient(inventory, ingredient) >= needed;
    }
    
    /**
     * Simulate consuming items from inventory (for planning purposes)
     */
    public static Map<ItemStack, Integer> simulateConsumption(Map<ItemStack, Integer> inventory, 
                                                            ItemStack item, int amount) {
        Map<ItemStack, Integer> result = new HashMap<>(inventory);
        
        for (Map.Entry<ItemStack, Integer> entry : result.entrySet()) {
            if (ItemStack.isSameItem(entry.getKey(), item)) {
                int newAmount = entry.getValue() - amount;
                if (newAmount <= 0) {
                    result.remove(entry.getKey());
                } else {
                    entry.setValue(newAmount);
                }
                break;
            }
        }
        
        return result;
    }
    
    /**
     * Simulate adding items to inventory (for planning purposes)
     */
    public static Map<ItemStack, Integer> simulateAddition(Map<ItemStack, Integer> inventory, 
                                                         ItemStack item, int amount) {
        Map<ItemStack, Integer> result = new HashMap<>(inventory);
        
        boolean found = false;
        for (Map.Entry<ItemStack, Integer> entry : result.entrySet()) {
            if (ItemStack.isSameItem(entry.getKey(), item)) {
                entry.setValue(entry.getValue() + amount);
                found = true;
                break;
            }
        }
        
        if (!found) {
            result.put(item.copy(), amount);
        }
        
        return result;
    }
}