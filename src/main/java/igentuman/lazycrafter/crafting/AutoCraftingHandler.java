package igentuman.lazycrafter.crafting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import igentuman.lazycrafter.LazyCrafter;
import igentuman.lazycrafter.recipe.RecipeTreeBuilder;
import igentuman.lazycrafter.crafting.AutoCraftingState.CraftingPhase;
import igentuman.lazycrafter.util.CraftingGridUtils;

import java.util.List;

/**
 * Handles the execution of auto-crafting operations
 */
public class AutoCraftingHandler {
    private static final AutoCraftingHandler INSTANCE = new AutoCraftingHandler();
    
    // Current crafting state
    private AutoCraftingState currentState = null;
    
    public static AutoCraftingHandler getInstance() {
        return INSTANCE;
    }
    
    private AutoCraftingHandler() {}
    
    /**
     * Execute auto-crafting for the given recipe
     */
    public void executeAutoCrafting(CraftingRecipe targetRecipe, Player player) {
        if (currentState != null) {
            LazyCrafter.logger.warn("Auto-crafting already in progress");
            return;
        }
        
        AbstractContainerMenu containerMenu = player.containerMenu;
        if (!CraftingGridUtils.isCraftingMenu(containerMenu)) {
            LazyCrafter.logger.warn("Player is not in a supported crafting menu: {}", 
                containerMenu.getClass().getSimpleName());
            return;
        }
        
        CraftingGridUtils.CraftingGridInfo gridInfo = CraftingGridUtils.detectCraftingGrid(containerMenu);
        if (gridInfo == null) {
            LazyCrafter.logger.warn("Could not detect crafting grid for menu: {}", 
                containerMenu.getClass().getSimpleName());
            return;
        }
        
        LazyCrafter.logger.info("Starting auto-crafting with {} grid in {}", 
            gridInfo.toString(), containerMenu.getClass().getSimpleName());
        
        try {
            // Plan the crafting sequence
            CraftingPlanner planner = new CraftingPlanner(player.level().getRecipeManager());
            CraftingSequence sequence = planner.planCrafting(targetRecipe, player.getInventory());
            
            if (sequence.isEmpty()) {
                LazyCrafter.logger.warn("Could not plan crafting sequence for recipe: {}", targetRecipe.getId());
                return;
            }
            
            LazyCrafter.logger.info("Starting auto-crafting sequence with {} operations", sequence.size());
            
            // Initialize crafting state
            currentState = new AutoCraftingState(sequence, player, containerMenu);
            currentState.setCurrentPhase(CraftingPhase.CLEARING_GRID);
            
        } catch (Exception e) {
            LazyCrafter.logger.error("Error during auto-crafting initialization", e);
            currentState = null;
        }
    }
    
    /**
     * Process one tick of auto-crafting. This should be called from a client tick event.
     */
    public void onClientTick() {
        if (currentState == null) {
            return;
        }
        
        try {
            processCraftingTick();
        } catch (Exception e) {
            LazyCrafter.logger.error("Error processing crafting tick", e);
            stopCrafting();
        }
    }
    
    /**
     * Process the current crafting state
     */
    private void processCraftingTick() {
        CraftingPhase phase = currentState.getCurrentPhase();
        
        switch (phase) {
            case CLEARING_GRID:
                handleClearingGrid();
                break;
            case PLACING_INGREDIENTS:
                handlePlacingIngredients();
                break;
            case WAITING_FOR_RESULT:
                handleWaitingForResult();
                break;
            case PERFORMING_CRAFT:
                handlePerformingCraft();
                break;
            case COMPLETED:
                handleCompleted();
                break;
            case FAILED:
                handleFailed();
                break;
        }
    }
    
    /**
     * Handle clearing the crafting grid
     */
    private void handleClearingGrid() {
        LazyCrafter.logger.debug("Clearing crafting grid for: {}", currentState.getProgressInfo());
        
        clearCraftingGrid(currentState.getContainerMenu(), currentState.getPlayer(), currentState.getGridInfo());
        currentState.setCurrentPhase(CraftingPhase.PLACING_INGREDIENTS);
    }
    
    /**
     * Handle placing ingredients in the crafting grid
     */
    private void handlePlacingIngredients() {
        CraftingRecipe recipe = currentState.getCurrentRecipe();
        if (recipe == null) {
            currentState.setCurrentPhase(CraftingPhase.COMPLETED);
            return;
        }
        
        LazyCrafter.logger.debug("Placing ingredients for: {}", currentState.getProgressInfo());
        
        if (placeIngredientsInGrid(recipe, currentState.getContainerMenu(), currentState.getPlayer(), currentState.getGridInfo())) {
            currentState.setCurrentPhase(CraftingPhase.WAITING_FOR_RESULT);
        } else {
            LazyCrafter.logger.warn("Failed to place ingredients for recipe: {}", recipe.getId());
            currentState.setCurrentPhase(CraftingPhase.FAILED);
        }
    }
    
    /**
     * Handle waiting for the crafting result to appear
     */
    private void handleWaitingForResult() {
        CraftingRecipe recipe = currentState.getCurrentRecipe();
        if (recipe == null) {
            currentState.setCurrentPhase(CraftingPhase.COMPLETED);
            return;
        }
        
        Slot resultSlot = currentState.getContainerMenu().getSlot(currentState.getGridInfo().getResultSlot());
        
        if (resultSlot.hasItem()) {
            ItemStack resultItem = resultSlot.getItem();
            ItemStack expectedResult = recipe.getResultItem(currentState.getPlayer().level().registryAccess());
            
            // Verify that the result matches what we expect from the recipe
            if (ItemStack.isSameItem(resultItem, expectedResult)) {
                LazyCrafter.logger.debug("Crafting result appeared after {} ticks: {} x{}", 
                    currentState.getWaitTicks(), resultItem.getHoverName().getString(), resultItem.getCount());
                currentState.setCurrentPhase(CraftingPhase.PERFORMING_CRAFT);
                return;
            } else {
                LazyCrafter.logger.debug("Result item doesn't match expected recipe output. Expected: {}, Got: {}", 
                    expectedResult.getHoverName().getString(), resultItem.getHoverName().getString());
            }
        }
        
        // Increment wait ticks and check timeout
        currentState.incrementWaitTicks();
        if (currentState.getWaitTicks() > 100) { // 5 seconds timeout
            LazyCrafter.logger.warn("Timeout waiting for crafting result for recipe: {}", recipe.getId());
            currentState.setCurrentPhase(CraftingPhase.FAILED);
        }
    }
    
    /**
     * Handle performing the craft
     */
    private void handlePerformingCraft() {
        if (performCraft(currentState.getContainerMenu(), currentState.getPlayer(), currentState.getGridInfo())) {
            LazyCrafter.logger.debug("Successfully crafted: {}", currentState.getProgressInfo());
            currentState.advanceToNext();
        } else {
            LazyCrafter.logger.warn("Failed to perform craft for: {}", currentState.getProgressInfo());
            currentState.setCurrentPhase(CraftingPhase.FAILED);
        }
    }
    
    /**
     * Handle completion of all crafting operations
     */
    private void handleCompleted() {
        LazyCrafter.logger.info("Auto-crafting sequence completed successfully");
        currentState = null;
    }
    
    /**
     * Handle crafting failure
     */
    private void handleFailed() {
        LazyCrafter.logger.warn("Auto-crafting sequence failed");
        currentState = null;
    }
    
    /**
     * Stop the current crafting operation
     */
    public void stopCrafting() {
        if (currentState != null) {
            LazyCrafter.logger.info("Stopping auto-crafting sequence");
            currentState = null;
        }
    }
    
    /**
     * Clear the crafting grid by emulating player clicks with empty cursor
     */
    private void clearCraftingGrid(AbstractContainerMenu containerMenu, Player player, CraftingGridUtils.CraftingGridInfo gridInfo) {
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (gameMode == null) return;
        
        LazyCrafter.logger.debug("Clearing crafting grid...");
        
        // First, clear any ghost recipe from the recipe book
        try {
            // Access the recipe book component to clear ghost recipes
            if (Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.inventory.CraftingScreen craftingScreen) {
                net.minecraft.client.gui.screens.recipebook.RecipeBookComponent recipeBook = craftingScreen.getRecipeBookComponent();
                if (recipeBook != null) {
                    // Clear the ghost recipe by setting up an empty recipe
                    recipeBook.ghostRecipe.clear();
                    LazyCrafter.logger.debug("Cleared ghost recipe from recipe book");
                }
            }
        } catch (Exception e) {
            LazyCrafter.logger.debug("Could not clear ghost recipe: {}", e.getMessage());
        }
        
        // Small delay to let ghost recipe clearing take effect
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        
        // First, ensure the player's cursor is empty
        if (!player.containerMenu.getCarried().isEmpty()) {
            // Try to clear crafting content if the method exists (for CraftingMenu)
            try {
                if (containerMenu instanceof net.minecraft.world.inventory.CraftingMenu craftingMenu) {
                    craftingMenu.clearCraftingContent();
                }
            } catch (Exception e) {
                LazyCrafter.logger.debug("Could not clear crafting content: {}", e.getMessage());
            }
            LazyCrafter.logger.debug("Player cursor not empty, clearing it first");
            // Find an empty slot in inventory to place the carried item
            for (int invSlot = gridInfo.getFirstInventorySlot(); invSlot <= gridInfo.getLastHotbarSlot(); invSlot++) {
                if (invSlot > containerMenu.slots.size() - 1) break;
                Slot slot = containerMenu.getSlot(invSlot);
                if (!slot.hasItem()) {
                    gameMode.handleInventoryMouseClick(containerMenu.containerId, invSlot, 0, 
                        net.minecraft.world.inventory.ClickType.PICKUP, player);
                    break;
                }
            }
        }
        
        // Clear each crafting grid slot by picking up items with empty cursor
        int[] craftingSlots = CraftingGridUtils.getCraftingSlots(gridInfo);
        for (int slotIndex : craftingSlots) {
            Slot slot = containerMenu.getSlot(slotIndex);
            if (slot.hasItem()) {
                LazyCrafter.logger.debug("Clearing slot {} with item: {}", slotIndex, 
                    slot.getItem().getHoverName().getString());
                
                // Pick up the item from crafting slot
                gameMode.handleInventoryMouseClick(containerMenu.containerId, slotIndex, 0, 
                    net.minecraft.world.inventory.ClickType.PICKUP, player);
                
                // Small delay to let the pickup register
                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                
                // Find an empty slot in inventory to place the item
                boolean placed = false;
                for (int invSlot = gridInfo.getFirstInventorySlot(); invSlot <= gridInfo.getLastHotbarSlot(); invSlot++) {
                    if (invSlot > containerMenu.slots.size() - 1) break;
                    Slot invSlotObj = containerMenu.getSlot(invSlot);
                    if (!invSlotObj.hasItem() || 
                        (invSlotObj.getItem().is(player.containerMenu.getCarried().getItem()) && 
                         invSlotObj.getItem().getCount() < invSlotObj.getItem().getMaxStackSize())) {
                        
                        // Place the item in inventory
                        gameMode.handleInventoryMouseClick(containerMenu.containerId, invSlot, 0, 
                            net.minecraft.world.inventory.ClickType.PICKUP, player);
                        placed = true;
                        
                        // Small delay after placing
                        try {
                            Thread.sleep(25);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        break;
                    }
                }
                
                if (!placed) {
                    LazyCrafter.logger.warn("Could not find space in inventory to place item from crafting slot {}", slotIndex);
                    // Try to drop the item as last resort
                    gameMode.handleInventoryMouseClick(containerMenu.containerId, -999, 0, 
                        net.minecraft.world.inventory.ClickType.PICKUP, player);
                }
            }
        }
        
        // Ensure cursor is empty after clearing
        if (!player.containerMenu.getCarried().isEmpty()) {
            LazyCrafter.logger.debug("Cursor still not empty after clearing, attempting to place in inventory");
            for (int invSlot = gridInfo.getFirstInventorySlot(); invSlot <= gridInfo.getLastHotbarSlot(); invSlot++) {
                if (invSlot > containerMenu.slots.size() - 1) break;
                Slot slot = containerMenu.getSlot(invSlot);
                if (!slot.hasItem()) {
                    gameMode.handleInventoryMouseClick(containerMenu.containerId, invSlot, 0, 
                        net.minecraft.world.inventory.ClickType.PICKUP, player);
                    break;
                }
            }
        }
        
        LazyCrafter.logger.debug("Crafting grid clearing completed");
    }
    
    /**
     * Place ingredients in the crafting grid according to the recipe
     */
    private boolean placeIngredientsInGrid(CraftingRecipe recipe, AbstractContainerMenu containerMenu, Player player, CraftingGridUtils.CraftingGridInfo gridInfo) {
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (gameMode == null) return false;
        
        List<Ingredient> ingredients = recipe.getIngredients();
        
        // Handle shaped recipes properly by respecting their pattern
        if (recipe instanceof ShapedRecipe shapedRecipe) {
            return placeShapedRecipeIngredients(shapedRecipe, containerMenu, player, gameMode, gridInfo);
        } else {
            // For shapeless recipes, place ingredients sequentially
            return placeShapelessRecipeIngredients(ingredients, containerMenu, player, gameMode, gridInfo);
        }
    }
    
    /**
     * Place ingredients for a shaped recipe, respecting the recipe's pattern
     */
    private boolean placeShapedRecipeIngredients(ShapedRecipe recipe, AbstractContainerMenu containerMenu, Player player, MultiPlayerGameMode gameMode, CraftingGridUtils.CraftingGridInfo gridInfo) {
        List<Ingredient> ingredients = recipe.getIngredients();
        int recipeWidth = recipe.getRecipeWidth();
        int recipeHeight = recipe.getRecipeHeight();
        
        LazyCrafter.logger.debug("Placing shaped recipe ingredients: {}x{} pattern with {} ingredients", 
            recipeWidth, recipeHeight, ingredients.size());
        
        // Check if recipe fits in the grid
        if (!gridInfo.canFitRecipe(recipeWidth, recipeHeight)) {
            LazyCrafter.logger.error("Recipe {}x{} does not fit in {}x{} grid", 
                recipeWidth, recipeHeight, gridInfo.getWidth(), gridInfo.getHeight());
            return false;
        }
        
        // Find the best position to place the recipe in the grid
        int[] position = CraftingGridUtils.findBestRecipePosition(recipeWidth, recipeHeight, gridInfo);
        if (position == null) {
            LazyCrafter.logger.error("Could not find position for recipe in grid");
            return false;
        }
        
        int offsetX = position[0];
        int offsetY = position[1];
        
        LazyCrafter.logger.debug("Placing recipe at offset ({}, {}) in {}x{} grid", 
            offsetX, offsetY, gridInfo.getWidth(), gridInfo.getHeight());
        
        // For shaped recipes, ingredients are arranged in a pattern
        // The ingredients list corresponds to the pattern positions (left-to-right, top-to-bottom)
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ingredient = ingredients.get(i);
            if (ingredient.isEmpty()) continue;
            
            // Calculate the position in the recipe pattern
            int patternX = i % recipeWidth;
            int patternY = i / recipeWidth;
            
            // Convert pattern position to crafting grid position with offset
            int gridX = offsetX + patternX;
            int gridY = offsetY + patternY;
            
            // Get the actual crafting slot for this grid position
            int craftingSlot = gridInfo.getCraftingSlot(gridX, gridY);
            
            if (craftingSlot == -1) {
                LazyCrafter.logger.error("Invalid crafting slot for grid position ({}, {})", gridX, gridY);
                return false;
            }
            
            LazyCrafter.logger.debug("Ingredient {} at pattern position ({}, {}) -> grid position ({}, {}) -> crafting slot {}", 
                i, patternX, patternY, gridX, gridY, craftingSlot);
            
            if (!placeIngredientInSlot(ingredient, craftingSlot, containerMenu, player, gameMode, gridInfo)) {
                return false;
            }
        }
        
        // Debug: Log the final state of the crafting grid
        debugCraftingGrid(containerMenu, gridInfo);
        return true;
    }
    
    /**
     * Place ingredients for a shapeless recipe
     */
    private boolean placeShapelessRecipeIngredients(List<Ingredient> ingredients, AbstractContainerMenu containerMenu, Player player, MultiPlayerGameMode gameMode, CraftingGridUtils.CraftingGridInfo gridInfo) {
        LazyCrafter.logger.debug("Placing shapeless recipe ingredients: {} ingredients", ingredients.size());
        
        for (int i = 0; i < ingredients.size() && i < gridInfo.getSize(); i++) {
            Ingredient ingredient = ingredients.get(i);
            if (ingredient.isEmpty()) continue;
            
            // For shapeless recipes, place ingredients sequentially
            int craftingSlot = gridInfo.getFirstCraftingSlot() + i;
            
            if (!placeIngredientInSlot(ingredient, craftingSlot, containerMenu, player, gameMode, gridInfo)) {
                return false;
            }
        }
        
        // Debug: Log the final state of the crafting grid
        debugCraftingGrid(containerMenu, gridInfo);
        return true;
    }
    
    /**
     * Place a single ingredient in the specified crafting slot
     */
    private boolean placeIngredientInSlot(Ingredient ingredient, int craftingSlot, AbstractContainerMenu containerMenu, Player player, MultiPlayerGameMode gameMode, CraftingGridUtils.CraftingGridInfo gridInfo) {
        // Find the ingredient in player inventory
        int inventorySlot = findIngredientInInventory(ingredient, player, gridInfo);
        if (inventorySlot == -1) {
            LazyCrafter.logger.warn("Could not find ingredient in inventory: {}", 
                ingredient.getItems().length > 0 ? ingredient.getItems()[0].getItem() : "unknown");
            return false;
        }
        
        // Move item from inventory to crafting grid
        LazyCrafter.logger.debug("Moving item from inventory slot {} to crafting slot {}", 
            inventorySlot, craftingSlot);
        
        // Method 1: Try pickup and place approach
        boolean placed = false;
        
        // First, try to pick up the item stack
        gameMode.handleInventoryMouseClick(containerMenu.containerId, inventorySlot, 0, 
            net.minecraft.world.inventory.ClickType.PICKUP, player);
        
        // Small delay to let the pickup register
        try {
            Thread.sleep(25);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        LazyCrafter.logger.debug("After pickup, carried: {}", 
            player.containerMenu.getCarried().isEmpty() ? "empty" : 
            player.containerMenu.getCarried().getHoverName().getString());
        
        if (!player.containerMenu.getCarried().isEmpty()) {
            // Place one item in crafting grid using right-click
            gameMode.handleInventoryMouseClick(containerMenu.containerId, craftingSlot, 1, 
                net.minecraft.world.inventory.ClickType.PICKUP, player);
            
            // Small delay to let the placement register
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            LazyCrafter.logger.debug("After placing, carried: {}", 
                player.containerMenu.getCarried().isEmpty() ? "empty" : 
                player.containerMenu.getCarried().getHoverName().getString());
            
            // Put remaining items back in inventory
            if (!player.containerMenu.getCarried().isEmpty()) {
                gameMode.handleInventoryMouseClick(containerMenu.containerId, inventorySlot, 0, 
                    net.minecraft.world.inventory.ClickType.PICKUP, player);
                
                // Small delay after putting items back
                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // Check if item was placed
            Slot targetSlot = containerMenu.getSlot(craftingSlot);
            placed = targetSlot.hasItem();
            
            if (placed) {
                LazyCrafter.logger.debug("Successfully placed ingredient in crafting slot {}: {}", 
                    craftingSlot, targetSlot.getItem().getHoverName().getString());
            }
        }
        
        // If pickup method failed, try QUICK_MOVE as fallback
        if (!placed) {
            LazyCrafter.logger.debug("Pickup method failed, trying QUICK_MOVE");
            gameMode.handleInventoryMouseClick(containerMenu.containerId, inventorySlot, 0, 
                net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
            
            Slot targetSlot = containerMenu.getSlot(craftingSlot);
            placed = targetSlot.hasItem();
        }
        
        if (!placed) {
            LazyCrafter.logger.error("Failed to place ingredient in crafting slot {}", craftingSlot);
            return false;
        }

        // Small delay between placing ingredients
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        
        return true;
    }
    
    /**
     * Debug method to log the current state of the crafting grid
     */
    private void debugCraftingGrid(AbstractContainerMenu containerMenu, CraftingGridUtils.CraftingGridInfo gridInfo) {
        LazyCrafter.logger.debug("=== Crafting Grid State ===");
        int[] craftingSlots = CraftingGridUtils.getCraftingSlots(gridInfo);
        for (int slotIndex : craftingSlots) {
            Slot slot = containerMenu.getSlot(slotIndex);
            if (slot.hasItem()) {
                ItemStack item = slot.getItem();
                LazyCrafter.logger.debug("Slot {}: {} x{}", slotIndex, 
                    item.getHoverName().getString(), item.getCount());
            } else {
                LazyCrafter.logger.debug("Slot {}: empty", slotIndex);
            }
        }
        LazyCrafter.logger.debug("=== End Grid State ===");
    }
    
    /**
     * Find an ingredient in the player's inventory
     */
    private int findIngredientInInventory(Ingredient ingredient, Player player, CraftingGridUtils.CraftingGridInfo gridInfo) {
        LazyCrafter.logger.debug("Looking for ingredient: {}", 
            ingredient.getItems().length > 0 ? ingredient.getItems()[0].getItem() : "unknown");
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && ingredient.test(stack)) {
                // Convert inventory index to menu slot index using the grid info
                int menuSlot = CraftingGridUtils.inventoryIndexToMenuSlot(i, gridInfo);
                
                LazyCrafter.logger.debug("Found ingredient {} in inventory slot {} (menu slot {}): {} x{}", 
                    stack.getItem(), i, menuSlot, stack.getHoverName().getString(), stack.getCount());
                
                return menuSlot;
            }
        }
        
        LazyCrafter.logger.debug("Ingredient not found in inventory");
        return -1;
    }
    
    /**
     * Perform the actual crafting operation
     */
    private boolean performCraft(AbstractContainerMenu containerMenu, Player player, CraftingGridUtils.CraftingGridInfo gridInfo) {
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (gameMode == null) {
            LazyCrafter.logger.debug("GameMode is null, cannot perform craft");
            return false;
        }
        
        try {
            Slot resultSlot = containerMenu.getSlot(gridInfo.getResultSlot());
            
            // At this point, we should already have a result item (verified by handleWaitingForResult)
            if (resultSlot.hasItem()) {
                ItemStack resultItem = resultSlot.getItem();
                LazyCrafter.logger.debug("Crafting result item: {} x{}", 
                    resultItem.getHoverName().getString(), resultItem.getCount());
                
                // Click the result slot to craft the item
                gameMode.handleInventoryMouseClick(containerMenu.containerId, gridInfo.getResultSlot(), 0, 
                    net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
                
                LazyCrafter.logger.debug("Clicked result slot to craft item");
                return true;
            } else {
                LazyCrafter.logger.warn("No result item found when trying to perform craft - this should not happen");
                return false;
            }
            
        } catch (Exception e) {
            LazyCrafter.logger.error("Error performing craft", e);
            return false;
        }
    }
    
    /**
     * Check if auto-crafting is currently in progress
     */
    public boolean isAutoCraftingInProgress() {
        return currentState != null;
    }
    
    /**
     * Get information about the current crafting grid
     */
    public String getCurrentGridInfo() {
        if (currentState != null) {
            return currentState.getGridInfo().toString();
        }
        return "No active crafting session";
    }
    
    /**
     * Check if a recipe can be crafted in the current grid
     */
    public boolean canCraftRecipeInCurrentGrid(CraftingRecipe recipe, Player player) {
        AbstractContainerMenu containerMenu = player.containerMenu;
        CraftingGridUtils.CraftingGridInfo gridInfo = CraftingGridUtils.detectCraftingGrid(containerMenu);
        
        if (gridInfo == null) {
            return false;
        }
        
        // For shaped recipes, check if they fit in the grid
        if (recipe instanceof ShapedRecipe shapedRecipe) {
            int recipeWidth = shapedRecipe.getRecipeWidth();
            int recipeHeight = shapedRecipe.getRecipeHeight();
            
            if (!gridInfo.canFitRecipe(recipeWidth, recipeHeight)) {
                LazyCrafter.logger.debug("Recipe {}x{} does not fit in {}x{} grid", 
                    recipeWidth, recipeHeight, gridInfo.getWidth(), gridInfo.getHeight());
                return false;
            }
        }
        
        // For shapeless recipes, check if we have enough slots
        int ingredientCount = (int) recipe.getIngredients().stream()
            .filter(ingredient -> !ingredient.isEmpty())
            .count();
            
        if (ingredientCount > gridInfo.getSize()) {
            LazyCrafter.logger.debug("Recipe needs {} ingredients but grid only has {} slots", 
                ingredientCount, gridInfo.getSize());
            return false;
        }
        
        return true;
    }
}