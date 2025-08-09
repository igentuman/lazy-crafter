package igentuman.lazycrafter.util;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import igentuman.lazycrafter.LazyCrafter;

/**
 * Utility class for detecting and working with different crafting grid types
 */
public class CraftingGridUtils {
    
    /**
     * Information about a crafting grid
     */
    public static class CraftingGridInfo {
        private final int width;
        private final int height;
        private final int firstCraftingSlot;
        private final int resultSlot;
        private final int firstInventorySlot;
        private final int lastInventorySlot;
        private final int firstHotbarSlot;
        private final int lastHotbarSlot;
        
        public CraftingGridInfo(int width, int height, int firstCraftingSlot, int resultSlot,
                               int firstInventorySlot, int lastInventorySlot, 
                               int firstHotbarSlot, int lastHotbarSlot) {
            this.width = width;
            this.height = height;
            this.firstCraftingSlot = firstCraftingSlot;
            this.resultSlot = resultSlot;
            this.firstInventorySlot = firstInventorySlot;
            this.lastInventorySlot = lastInventorySlot;
            this.firstHotbarSlot = firstHotbarSlot;
            this.lastHotbarSlot = lastHotbarSlot;
        }
        
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int getSize() { return width * height; }
        public int getFirstCraftingSlot() { return firstCraftingSlot; }
        public int getResultSlot() { return resultSlot; }
        public int getFirstInventorySlot() { return firstInventorySlot; }
        public int getLastInventorySlot() { return lastInventorySlot; }
        public int getFirstHotbarSlot() { return firstHotbarSlot; }
        public int getLastHotbarSlot() { return lastHotbarSlot; }
        
        /**
         * Get the crafting slot index for a given grid position
         */
        public int getCraftingSlot(int x, int y) {
            if (x < 0 || x >= width || y < 0 || y >= height) {
                return -1;
            }
            return firstCraftingSlot + (y * width) + x;
        }
        
        /**
         * Check if a recipe can fit in this grid
         */
        public boolean canFitRecipe(int recipeWidth, int recipeHeight) {
            return recipeWidth <= width && recipeHeight <= height;
        }
        
        @Override
        public String toString() {
            return String.format("CraftingGrid[%dx%d, slots=%d-%d, result=%d]", 
                width, height, firstCraftingSlot, firstCraftingSlot + getSize() - 1, resultSlot);
        }
    }
    
    /**
     * Detect the crafting grid properties for a given container menu
     */
    public static CraftingGridInfo detectCraftingGrid(AbstractContainerMenu menu) {
        if (menu instanceof CraftingMenu) {
            // 3x3 crafting table
            // Slot layout: 0=result, 1-9=crafting grid, 10-36=main inventory, 37-45=hotbar
            return new CraftingGridInfo(3, 3, 1, 0, 10, 36, 37, 45);
        } else if (menu instanceof InventoryMenu) {
            // 2x2 player inventory crafting
            // Slot layout: 0=result, 1-4=crafting grid, 5-8=armor, 9-35=main inventory, 36-44=hotbar, 45=offhand
            return new CraftingGridInfo(2, 2, 1, 0, 9, 35, 36, 44);
        } else {
            // Try to detect custom crafting grids by analyzing the menu structure
            return detectCustomCraftingGrid(menu);
        }
    }
    
    /**
     * Attempt to detect custom crafting grids from modded containers
     */
    private static CraftingGridInfo detectCustomCraftingGrid(AbstractContainerMenu menu) {
        LazyCrafter.logger.debug("Attempting to detect custom crafting grid for menu type: {}", 
            menu.getClass().getSimpleName());
        
        // Look for patterns that suggest a crafting grid
        int totalSlots = menu.slots.size();
        
        // Common patterns:
        // - Result slot at index 0
        // - Crafting grid starting at index 1
        // - Inventory slots after crafting grid
        
        // Try to find a result slot (usually has special behavior)
        int resultSlot = -1;
        int craftingStart = -1;
        int gridSize = 0;
        
        // Check if slot 0 looks like a result slot (common pattern)
        if (totalSlots > 0) {
            Slot slot0 = menu.getSlot(0);
            // Result slots are typically not directly accessible for placement
            // This is a heuristic and might not work for all mods
            resultSlot = 0;
            craftingStart = 1;
        }
        
        if (craftingStart != -1) {
            // Try to determine grid size by looking at slot arrangement
            // Check for common grid sizes: 2x2 (4 slots), 3x3 (9 slots), 4x4 (16 slots), 5x5 (25 slots)
            int[] possibleGridSizes = {4, 9, 16, 25};
            
            for (int size : possibleGridSizes) {
                if (craftingStart + size <= totalSlots) {
                    int width = (int) Math.sqrt(size);
                    if (width * width == size) {
                        // Found a square grid
                        gridSize = size;
                        
                        // Estimate inventory slots (everything after crafting grid)
                        int firstInventorySlot = craftingStart + gridSize;
                        int inventorySize = Math.min(27, totalSlots - firstInventorySlot - 9); // Assume 27 main + 9 hotbar
                        int lastInventorySlot = firstInventorySlot + inventorySize - 1;
                        int firstHotbarSlot = lastInventorySlot + 1;
                        int lastHotbarSlot = Math.min(firstHotbarSlot + 8, totalSlots - 1);
                        
                        LazyCrafter.logger.info("Detected custom {}x{} crafting grid in {}", 
                            width, width, menu.getClass().getSimpleName());
                        
                        return new CraftingGridInfo(width, width, craftingStart, resultSlot,
                            firstInventorySlot, lastInventorySlot, firstHotbarSlot, lastHotbarSlot);
                    }
                }
            }
        }
        
        LazyCrafter.logger.warn("Could not detect crafting grid for menu type: {}", 
            menu.getClass().getSimpleName());
        return null;
    }
    
    /**
     * Check if a container menu supports crafting
     */
    public static boolean isCraftingMenu(AbstractContainerMenu menu) {
        return detectCraftingGrid(menu) != null;
    }
    
    /**
     * Get all crafting slot indices for a given grid
     */
    public static int[] getCraftingSlots(CraftingGridInfo gridInfo) {
        int[] slots = new int[gridInfo.getSize()];
        for (int i = 0; i < gridInfo.getSize(); i++) {
            slots[i] = gridInfo.getFirstCraftingSlot() + i;
        }
        return slots;
    }
    
    /**
     * Convert inventory index to menu slot index for a given grid
     */
    public static int inventoryIndexToMenuSlot(int inventoryIndex, CraftingGridInfo gridInfo) {
        if (inventoryIndex < 9) {
            // Hotbar slots
            return gridInfo.getFirstHotbarSlot() + inventoryIndex;
        } else {
            // Main inventory slots
            int mainInventoryIndex = inventoryIndex - 9;
            return gridInfo.getFirstInventorySlot() + mainInventoryIndex;
        }
    }
    
    /**
     * Check if a recipe can be placed in the given grid at a specific offset
     */
    public static boolean canPlaceRecipeAt(int recipeWidth, int recipeHeight, 
                                          int offsetX, int offsetY, CraftingGridInfo gridInfo) {
        return offsetX >= 0 && offsetY >= 0 && 
               offsetX + recipeWidth <= gridInfo.getWidth() && 
               offsetY + recipeHeight <= gridInfo.getHeight();
    }
    
    /**
     * Find the best position to place a recipe in the grid
     * Returns {x, y} offset or null if recipe doesn't fit
     */
    public static int[] findBestRecipePosition(int recipeWidth, int recipeHeight, CraftingGridInfo gridInfo) {
        // Try to center the recipe in the grid
        int centerX = (gridInfo.getWidth() - recipeWidth) / 2;
        int centerY = (gridInfo.getHeight() - recipeHeight) / 2;
        
        if (canPlaceRecipeAt(recipeWidth, recipeHeight, centerX, centerY, gridInfo)) {
            return new int[]{centerX, centerY};
        }
        
        // If centering doesn't work, try top-left
        if (canPlaceRecipeAt(recipeWidth, recipeHeight, 0, 0, gridInfo)) {
            return new int[]{0, 0};
        }
        
        // Recipe doesn't fit
        return null;
    }
}