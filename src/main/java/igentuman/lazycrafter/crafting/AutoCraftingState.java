package igentuman.lazycrafter.crafting;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import igentuman.lazycrafter.util.CraftingGridUtils;

import java.util.List;

/**
 * Holds the state of an ongoing auto-crafting operation
 */
public class AutoCraftingState {
    private final CraftingSequence sequence;
    private final Player player;
    private final AbstractContainerMenu containerMenu;
    private final CraftingGridUtils.CraftingGridInfo gridInfo;
    
    private int currentOperationIndex = 0;
    private int currentQuantityIndex = 0;
    private CraftingPhase currentPhase = CraftingPhase.IDLE;
    private int waitTicks = 0;
    
    public enum CraftingPhase {
        IDLE,
        CLEARING_GRID,
        PLACING_INGREDIENTS,
        WAITING_FOR_RESULT,
        PERFORMING_CRAFT,
        COMPLETED,
        FAILED
    }
    
    public AutoCraftingState(CraftingSequence sequence, Player player, AbstractContainerMenu containerMenu) {
        this.sequence = sequence;
        this.player = player;
        this.containerMenu = containerMenu;
        this.gridInfo = CraftingGridUtils.detectCraftingGrid(containerMenu);
        
        if (this.gridInfo == null) {
            throw new IllegalArgumentException("Container menu does not support crafting: " + containerMenu.getClass().getSimpleName());
        }
    }
    
    public CraftingSequence getSequence() {
        return sequence;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public AbstractContainerMenu getContainerMenu() {
        return containerMenu;
    }
    
    public CraftingGridUtils.CraftingGridInfo getGridInfo() {
        return gridInfo;
    }
    
    /**
     * @deprecated Use getContainerMenu() instead
     */
    @Deprecated
    public AbstractContainerMenu getCraftingMenu() {
        return containerMenu;
    }
    
    public int getCurrentOperationIndex() {
        return currentOperationIndex;
    }
    
    public void setCurrentOperationIndex(int currentOperationIndex) {
        this.currentOperationIndex = currentOperationIndex;
    }
    
    public int getCurrentQuantityIndex() {
        return currentQuantityIndex;
    }
    
    public void setCurrentQuantityIndex(int currentQuantityIndex) {
        this.currentQuantityIndex = currentQuantityIndex;
    }
    
    public CraftingPhase getCurrentPhase() {
        return currentPhase;
    }
    
    public void setCurrentPhase(CraftingPhase currentPhase) {
        this.currentPhase = currentPhase;
        this.waitTicks = 0; // Reset wait ticks when phase changes
    }
    
    public int getWaitTicks() {
        return waitTicks;
    }
    
    public void incrementWaitTicks() {
        this.waitTicks++;
    }
    
    public void resetWaitTicks() {
        this.waitTicks = 0;
    }
    
    /**
     * Get the current crafting operation
     */
    public CraftingOperation getCurrentOperation() {
        List<CraftingOperation> operations = sequence.getSortedOperations();
        if (currentOperationIndex >= operations.size()) {
            return null;
        }
        return operations.get(currentOperationIndex);
    }
    
    /**
     * Get the current recipe being crafted
     */
    public CraftingRecipe getCurrentRecipe() {
        CraftingOperation operation = getCurrentOperation();
        return operation != null ? operation.getRecipe() : null;
    }
    
    /**
     * Check if all operations are completed
     */
    public boolean isCompleted() {
        return currentOperationIndex >= sequence.getSortedOperations().size();
    }
    
    /**
     * Move to the next quantity or operation
     */
    public void advanceToNext() {
        CraftingOperation operation = getCurrentOperation();
        if (operation == null) {
            setCurrentPhase(CraftingPhase.COMPLETED);
            return;
        }
        
        currentQuantityIndex++;
        if (currentQuantityIndex >= operation.getQuantity()) {
            // Move to next operation
            currentOperationIndex++;
            currentQuantityIndex = 0;
        }
        
        if (isCompleted()) {
            setCurrentPhase(CraftingPhase.COMPLETED);
        } else {
            setCurrentPhase(CraftingPhase.CLEARING_GRID);
        }
    }
    
    /**
     * Get progress information for logging
     */
    public String getProgressInfo() {
        CraftingOperation operation = getCurrentOperation();
        if (operation == null) {
            return "Completed";
        }
        
        return String.format("Operation %d/%d, Quantity %d/%d - %s", 
            currentOperationIndex + 1, 
            sequence.getSortedOperations().size(),
            currentQuantityIndex + 1,
            operation.getQuantity(),
            operation.getRecipe().getId());
    }
}