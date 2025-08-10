package igentuman.lazycrafter.integration.jei;

import igentuman.lazycrafter.LazyCrafter;
import igentuman.lazycrafter.crafting.AutoCraftingHandler;
import igentuman.lazycrafter.crafting.CraftingPlanner;
import igentuman.lazycrafter.util.CraftingGridUtils;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Custom recipe transfer handler that integrates auto-crafting with JEI's recipe transfer system
 */
public class AutoCraftingTransferHandler<C extends AbstractContainerMenu> implements IRecipeTransferHandler<C, CraftingRecipe> {
    
    private final Class<? extends C> containerClass;
    
    public AutoCraftingTransferHandler(Class<? extends C> containerClass) {
        this.containerClass = containerClass;
    }
    
    @Override
    public Class<? extends C> getContainerClass() {
        return containerClass;
    }
    
    @Override
    public mezz.jei.api.recipe.RecipeType<CraftingRecipe> getRecipeType() {
        return mezz.jei.api.constants.RecipeTypes.CRAFTING;
    }
    
    @Override
    public Optional<net.minecraft.world.inventory.MenuType<C>> getMenuType() {
        // Return empty to let JEI handle menu type detection
        return Optional.empty();
    }
    
    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(C container, CraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, 
                                             Player player, boolean maxTransfer, boolean doTransfer) {
        
        // Validate that we can handle this container
        if (!CraftingGridUtils.isCraftingMenu(container)) {
            return createError("gui.lazycrafter.jei.transfer.error.unsupported_container");
        }
        
        // First, check if the player can craft directly (normal JEI behavior)
        if (canCraftDirectly(recipe, player)) {
            // Let JEI handle normal recipe transfer
            return null;
        }
        
        // Check if auto-crafting is possible for recipes that can't be crafted directly
        if (!canAutoCraftRecipe(recipe, player)) {
            return createError("gui.lazycrafter.jei.transfer.error.cannot_autocraft");
        }
        
        // If this is just a check (not actual transfer), return success
        if (!doTransfer) {
            return null; // No error, transfer is possible
        }
        
        // Perform the auto-crafting
        try {
            LazyCrafter.logger.info("JEI recipe transfer initiated auto-crafting for: {}", recipe.getId());
            AutoCraftingHandler.getInstance().executeAutoCrafting(recipe, player);
            return null; // Success
            
        } catch (Exception e) {
            LazyCrafter.logger.error("Error during JEI recipe transfer auto-crafting", e);
            return createError("gui.lazycrafter.jei.transfer.error.execution_failed");
        }
    }
    
    /**
     * Check if the recipe can be crafted directly (without nested crafting)
     */
    private boolean canCraftDirectly(CraftingRecipe recipe, Player player) {
        // Simple check - if all ingredients are directly available
        return recipe.getIngredients().stream().allMatch(ingredient -> {
            if (ingredient.isEmpty()) return true;
            
            return player.getInventory().items.stream().anyMatch(stack -> 
                !stack.isEmpty() && ingredient.test(stack)
            );
        });
    }
    
    /**
     * Check if the recipe can be auto-crafted
     */
    private boolean canAutoCraftRecipe(CraftingRecipe recipe, Player player) {
        try {
            CraftingPlanner planner = new CraftingPlanner(player.level().getRecipeManager());
            return planner.canAutoCraft(recipe, player.getInventory());
        } catch (Exception e) {
            LazyCrafter.logger.debug("Error checking auto-craft possibility in transfer handler: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Create a transfer error with the given message key
     */
    private IRecipeTransferError createError(String messageKey) {
        return new IRecipeTransferError() {
            public Type getType() {
                return Type.USER_FACING;
            }
            
            public void showError(int mouseX, int mouseY, IRecipeSlotsView recipeSlotsView, int guiLeft, int guiTop) {
                // Show error tooltip
                Component errorMessage = Component.translatable(messageKey);
                // Use GuiGraphics for rendering tooltip in newer versions
                try {
                    if (Minecraft.getInstance().screen != null) {
                        // This is a simplified approach - in practice, JEI handles error display
                        LazyCrafter.logger.warn("Recipe transfer error: {}", errorMessage.getString());
                    }
                } catch (Exception e) {
                    LazyCrafter.logger.debug("Error showing transfer error tooltip", e);
                }
            }
        };
    }
}