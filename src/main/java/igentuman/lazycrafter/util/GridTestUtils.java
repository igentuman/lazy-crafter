package igentuman.lazycrafter.util;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import igentuman.lazycrafter.LazyCrafter;

/**
 * Utility class for testing grid detection functionality
 */
public class GridTestUtils {
    
    /**
     * Test grid detection for different menu types
     */
    public static void testGridDetection() {
        LazyCrafter.logger.info("=== Testing Grid Detection ===");
        
        // Test cases would go here in a real test environment
        // For now, just log the supported grid types
        
        LazyCrafter.logger.info("Supported grid types:");
        LazyCrafter.logger.info("- CraftingMenu: 3x3 grid (crafting table)");
        LazyCrafter.logger.info("- InventoryMenu: 2x2 grid (player inventory)");
        LazyCrafter.logger.info("- Custom modded grids: 2x2, 3x3, 4x4, 5x5 square grids");
        
        LazyCrafter.logger.info("=== Grid Detection Test Complete ===");
    }
    
    /**
     * Log grid information for debugging
     */
    public static void logGridInfo(AbstractContainerMenu menu) {
        CraftingGridUtils.CraftingGridInfo gridInfo = CraftingGridUtils.detectCraftingGrid(menu);
        
        if (gridInfo != null) {
            LazyCrafter.logger.info("Detected grid: {} for menu type: {}", 
                gridInfo.toString(), menu.getClass().getSimpleName());
            
            LazyCrafter.logger.info("Grid details:");
            LazyCrafter.logger.info("  Size: {}x{} ({} slots)", 
                gridInfo.getWidth(), gridInfo.getHeight(), gridInfo.getSize());
            LazyCrafter.logger.info("  Crafting slots: {}-{}", 
                gridInfo.getFirstCraftingSlot(), 
                gridInfo.getFirstCraftingSlot() + gridInfo.getSize() - 1);
            LazyCrafter.logger.info("  Result slot: {}", gridInfo.getResultSlot());
            LazyCrafter.logger.info("  Inventory slots: {}-{}", 
                gridInfo.getFirstInventorySlot(), gridInfo.getLastInventorySlot());
            LazyCrafter.logger.info("  Hotbar slots: {}-{}", 
                gridInfo.getFirstHotbarSlot(), gridInfo.getLastHotbarSlot());
        } else {
            LazyCrafter.logger.warn("Could not detect grid for menu type: {}", 
                menu.getClass().getSimpleName());
        }
    }
    
    /**
     * Test recipe fitting in different grid sizes
     */
    public static void testRecipeFitting() {
        LazyCrafter.logger.info("=== Testing Recipe Fitting ===");
        
        // Simulate different grid sizes
        CraftingGridUtils.CraftingGridInfo grid2x2 = new CraftingGridUtils.CraftingGridInfo(
            2, 2, 1, 0, 9, 35, 36, 44);
        CraftingGridUtils.CraftingGridInfo grid3x3 = new CraftingGridUtils.CraftingGridInfo(
            3, 3, 1, 0, 10, 36, 37, 45);
        
        // Test different recipe sizes
        int[][] recipeSizes = {{1, 1}, {2, 1}, {1, 2}, {2, 2}, {3, 1}, {1, 3}, {3, 2}, {2, 3}, {3, 3}};
        
        for (int[] size : recipeSizes) {
            int width = size[0];
            int height = size[1];
            
            boolean fits2x2 = grid2x2.canFitRecipe(width, height);
            boolean fits3x3 = grid3x3.canFitRecipe(width, height);
            
            LazyCrafter.logger.info("Recipe {}x{}: fits in 2x2={}, fits in 3x3={}", 
                width, height, fits2x2, fits3x3);
            
            if (fits2x2) {
                int[] pos2x2 = CraftingGridUtils.findBestRecipePosition(width, height, grid2x2);
                if (pos2x2 != null) {
                    LazyCrafter.logger.info("  Best position in 2x2: ({}, {})", pos2x2[0], pos2x2[1]);
                }
            }
            
            if (fits3x3) {
                int[] pos3x3 = CraftingGridUtils.findBestRecipePosition(width, height, grid3x3);
                if (pos3x3 != null) {
                    LazyCrafter.logger.info("  Best position in 3x3: ({}, {})", pos3x3[0], pos3x3[1]);
                }
            }
        }
        
        LazyCrafter.logger.info("=== Recipe Fitting Test Complete ===");
    }
}