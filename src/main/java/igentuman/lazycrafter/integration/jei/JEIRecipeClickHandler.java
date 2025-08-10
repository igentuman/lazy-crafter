package igentuman.lazycrafter.integration.jei;

import igentuman.lazycrafter.LazyCrafter;
import igentuman.lazycrafter.crafting.AutoCraftingHandler;
import igentuman.lazycrafter.crafting.CraftingPlanner;
import igentuman.lazycrafter.util.CraftingGridUtils;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;

import java.util.Collection;
import java.util.List;

/**
 * Handles JEI recipe click interactions for auto-crafting
 */
public class JEIRecipeClickHandler implements IGuiContainerHandler<AbstractContainerScreen<?>> {
    
    @Override
    public List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(AbstractContainerScreen<?> containerScreen) {
        return List.of();
    }
    
    @Override
    public Collection<IGuiClickableArea> getGuiClickableAreas(AbstractContainerScreen<?> containerScreen, double mouseX, double mouseY) {
        // We'll add clickable areas for auto-crafting if needed
        return List.of();
    }
    
    /**
     * Check if auto-crafting is available for the current recipe
     */
    public static boolean canAutoCraftCurrentRecipe(CraftingRecipe recipe) {
        Player player = Minecraft.getInstance().player;
        if (player == null || recipe == null) {
            return false;
        }
        
        try {
            // Check if player is in a supported crafting menu
            AbstractContainerMenu containerMenu = player.containerMenu;
            if (!CraftingGridUtils.isCraftingMenu(containerMenu)) {
                return false;
            }
            
            // Use the crafting planner to check if the recipe can be auto-crafted
            CraftingPlanner planner = new CraftingPlanner(player.level().getRecipeManager());
            return planner.canAutoCraft(recipe, player.getInventory());
            
        } catch (Exception e) {
            LazyCrafter.logger.debug("Error checking auto-craft possibility: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Execute auto-crafting for the given recipe
     */
    public static void executeAutoCrafting(CraftingRecipe recipe) {
        Player player = Minecraft.getInstance().player;
        if (player == null || recipe == null) {
            return;
        }
        
        try {
            LazyCrafter.logger.info("JEI auto-craft initiated for recipe: {}", recipe.getId());
            AutoCraftingHandler.getInstance().executeAutoCrafting(recipe, player);
        } catch (Exception e) {
            LazyCrafter.logger.error("Error executing auto-crafting from JEI", e);
        }
    }
}