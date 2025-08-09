package igentuman.lazycrafter.recipe;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * Builds a dependency tree of recipes needed for crafting
 */
public class RecipeTreeBuilder {
    private final RecipeManager recipeManager;
    
    public RecipeTreeBuilder(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }
    
    /**
     * Builds a crafting tree for the given recipe
     */
    public CraftingTree buildCraftingTree(CraftingRecipe recipe, Inventory playerInventory) {
        Map<ItemStack, Integer> availableItems = getAvailableItems(playerInventory);
        Set<ResourceLocation> visitedRecipes = new HashSet<>();
        
        CraftingNode rootNode = buildCraftingNode(recipe, availableItems, visitedRecipes, 0);
        return new CraftingTree(rootNode);
    }
    
    /**
     * Recursively builds crafting nodes
     */
    private CraftingNode buildCraftingNode(CraftingRecipe recipe, Map<ItemStack, Integer> availableItems,
                                         Set<ResourceLocation> visitedRecipes, int depth) {
        if (depth > 10 || visitedRecipes.contains(recipe.getId())) {
            return null;
        }
        
        visitedRecipes.add(recipe.getId());
        CraftingNode node = new CraftingNode(recipe);
        
        // Check each ingredient
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) continue;
            
            boolean hasDirectly = false;
            
            // Check if we have the ingredient directly
            for (ItemStack acceptedItem : ingredient.getItems()) {
                if (hasEnoughItems(availableItems, acceptedItem, 1)) {
                    consumeItem(availableItems, acceptedItem, 1);
                    hasDirectly = true;
                    break;
                }
            }
            
            // If we don't have it directly, find a recipe to craft it
            if (!hasDirectly) {
                CraftingRecipe subRecipe = findRecipeForIngredient(ingredient);
                if (subRecipe != null) {
                    CraftingNode subNode = buildCraftingNode(subRecipe, availableItems, visitedRecipes, depth + 1);
                    if (subNode != null) {
                        node.addDependency(subNode);
                    }
                }
            }
        }
        
        visitedRecipes.remove(recipe.getId());
        return node;
    }
    
    /**
     * Find a recipe that can produce the given ingredient
     */
    private CraftingRecipe findRecipeForIngredient(Ingredient ingredient) {
        Collection<CraftingRecipe> recipes = recipeManager.getAllRecipesFor(RecipeType.CRAFTING);
        
        for (ItemStack targetItem : ingredient.getItems()) {
            for (CraftingRecipe recipe : recipes) {
                if (ItemStack.isSameItem(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()), targetItem)) {
                    return recipe;
                }
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
     * Represents a crafting tree
     */
    public static class CraftingTree {
        private final CraftingNode rootNode;
        
        public CraftingTree(CraftingNode rootNode) {
            this.rootNode = rootNode;
        }
        
        public CraftingNode getRootNode() {
            return rootNode;
        }
        
        /**
         * Get the crafting order (dependencies first)
         */
        public List<CraftingRecipe> getCraftingOrder() {
            List<CraftingRecipe> order = new ArrayList<>();
            if (rootNode != null) {
                addToOrder(rootNode, order, new HashSet<>());
            }
            return order;
        }
        
        private void addToOrder(CraftingNode node, List<CraftingRecipe> order, Set<ResourceLocation> visited) {
            if (visited.contains(node.getRecipe().getId())) {
                return;
            }
            
            visited.add(node.getRecipe().getId());
            
            // Add dependencies first
            for (CraftingNode dependency : node.getDependencies()) {
                addToOrder(dependency, order, visited);
            }
            
            // Then add this recipe
            order.add(node.getRecipe());
        }
    }
    
    /**
     * Represents a node in the crafting tree
     */
    public static class CraftingNode {
        private final CraftingRecipe recipe;
        private final List<CraftingNode> dependencies = new ArrayList<>();
        
        public CraftingNode(CraftingRecipe recipe) {
            this.recipe = recipe;
        }
        
        public CraftingRecipe getRecipe() {
            return recipe;
        }
        
        public List<CraftingNode> getDependencies() {
            return dependencies;
        }
        
        public void addDependency(CraftingNode dependency) {
            dependencies.add(dependency);
        }
    }
}