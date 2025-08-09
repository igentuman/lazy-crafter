package igentuman.lazycrafter.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import igentuman.lazycrafter.config.LazyCrafterConfig;
import igentuman.lazycrafter.gui.AutoCraftingButton;
import igentuman.lazycrafter.recipe.RecipeChecker;
import igentuman.lazycrafter.LazyCrafter;

/**
 * Mixin to add auto-crafting functionality to the Recipe Book
 */
@Mixin(RecipeBookComponent.class)
public class RecipeBookComponentMixin {
    
    @Shadow
    private Minecraft minecraft;
    
    @Unique
    private AutoCraftingButton lazyCrafter$autoCraftButton;
    
    @Unique
    private RecipeChecker lazyCrafter$recipeChecker;
    
    @Unique
    private CraftingRecipe lazyCrafter$currentRecipe;
    
    /**
     * Initialize our components when the recipe book is initialized
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void lazyCrafter$init(int pWidth, int pHeight, Minecraft pMinecraft, boolean pWidthTooNarrow, RecipeBookMenu<?> pMenu, CallbackInfo ci) {
        if (lazyCrafter$recipeChecker == null) {
            lazyCrafter$recipeChecker = new RecipeChecker(minecraft.level.getRecipeManager());
        }
        
        if (lazyCrafter$autoCraftButton == null) {
            lazyCrafter$autoCraftButton = new AutoCraftingButton(0, 0, (RecipeBookComponent)(Object)this);
        }
        
        // Position the button next to the regular craft button
        lazyCrafter$updateButtonPosition();
    }
    
    /**
     * Update button position based on recipe book layout
     */
    @Unique
    private void lazyCrafter$updateButtonPosition() {
        if (lazyCrafter$autoCraftButton != null && minecraft.screen != null) {
            // Try to get more accurate positioning based on screen type
            int buttonX, buttonY;
            
            if (minecraft.screen instanceof net.minecraft.client.gui.screens.inventory.CraftingScreen craftingScreen) {
                // Position relative to crafting screen
                int guiLeft = (craftingScreen.width - 176) / 2; // Standard GUI width
                int guiTop = (craftingScreen.height - 166) / 2; // Standard GUI height
                
                // Position next to the recipe book area
                buttonX = guiLeft + 176 + 27 + LazyCrafterConfig.getButtonOffsetX(); // Right of the crafting GUI
                buttonY = guiTop + 60 + LazyCrafterConfig.getButtonOffsetY(); // Near the top of recipe area
                
            } else if (minecraft.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen inventoryScreen) {
                // Position relative to inventory screen
                int guiLeft = (inventoryScreen.width - 176) / 2;
                int guiTop = (inventoryScreen.height - 166) / 2;
                
                // Position next to the recipe book area in inventory
                buttonX = guiLeft + 176 + 45 + LazyCrafterConfig.getButtonOffsetX();
                buttonY = guiTop + 60 + LazyCrafterConfig.getButtonOffsetY();
                
            } else {
                // Fallback positioning for other screens
                int screenWidth = minecraft.screen.width;
                int screenHeight = minecraft.screen.height;
                
                buttonX = screenWidth / 2 + 100 + LazyCrafterConfig.getButtonOffsetX();
                buttonY = screenHeight / 2 - 50 + LazyCrafterConfig.getButtonOffsetY();
            }
            
            // Ensure button stays within screen bounds
            buttonX = Math.max(0, Math.min(buttonX, minecraft.screen.width - 25));
            buttonY = Math.max(0, Math.min(buttonY, minecraft.screen.height - 18));
            
            lazyCrafter$autoCraftButton.setPosition(buttonX, buttonY);
        }
    }
    
    /**
     * Handle recipe selection to update auto-craft button
     */
    @Inject(method = "setupGhostRecipe", at = @At("HEAD"))
    private void lazyCrafter$onRecipeSelected(Recipe<?> recipe, java.util.List<net.minecraft.world.inventory.Slot> slots, CallbackInfo ci) {
        if (recipe instanceof CraftingRecipe craftingRecipe) {
            lazyCrafter$currentRecipe = craftingRecipe;
            lazyCrafter$updateAutoCraftButton();
        }
    }
    
    /**
     * Update the auto-craft button based on current recipe
     */
    @Unique
    private void lazyCrafter$updateAutoCraftButton() {
        if (lazyCrafter$autoCraftButton == null || lazyCrafter$currentRecipe == null || minecraft.player == null) {
            if (lazyCrafter$autoCraftButton != null) {
                lazyCrafter$autoCraftButton.hide();
            }
            return;
        }
        
        // Check if the recipe can be auto-crafted
        boolean canAutoCraft = lazyCrafter$recipeChecker.canCraftRecipe(
            lazyCrafter$currentRecipe, 
            minecraft.player.getInventory()
        );
        
        // Only show the button if auto-crafting is possible and regular crafting is not
        boolean showButton = canAutoCraft && !lazyCrafter$canCraftDirectly();
        
        lazyCrafter$autoCraftButton.updateForRecipe(lazyCrafter$currentRecipe, showButton);
    }
    
    /**
     * Check if the recipe can be crafted directly (without nested crafting)
     */
    @Unique
    private boolean lazyCrafter$canCraftDirectly() {
        if (lazyCrafter$currentRecipe == null || minecraft.player == null) {
            return false;
        }
        
        // Simple check - if all ingredients are directly available
        return lazyCrafter$currentRecipe.getIngredients().stream().allMatch(ingredient -> {
            if (ingredient.isEmpty()) return true;
            
            return minecraft.player.getInventory().items.stream().anyMatch(stack -> 
                !stack.isEmpty() && ingredient.test(stack)
            );
        });
    }
    
    /**
     * Render the auto-craft button
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void lazyCrafter$render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (lazyCrafter$autoCraftButton != null && lazyCrafter$autoCraftButton.isVisible()) {
            // Update position before rendering in case layout changed
            lazyCrafter$updateButtonPosition();
            
            // Render the button with proper z-ordering
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 100); // Render on top
            lazyCrafter$autoCraftButton.render(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.pose().popPose();
            
            // Render tooltip separately to ensure it's on top
            if (lazyCrafter$autoCraftButton.isHoveredOrFocused()) {
                lazyCrafter$autoCraftButton.renderTooltip(guiGraphics, mouseX, mouseY);
            }
        }
    }
    
    /**
     * Handle mouse clicks for the auto-craft button
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void lazyCrafter$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (lazyCrafter$autoCraftButton != null && lazyCrafter$autoCraftButton.isVisible()) {
            if (lazyCrafter$autoCraftButton.mouseClicked(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
            }
        }
    }
    
    /**
     * Clear recipe checker cache when recipes change
     */
    @Inject(method = "recipesUpdated", at = @At("HEAD"))
    private void lazyCrafter$recipesUpdated(CallbackInfo ci) {
        if (lazyCrafter$recipeChecker != null) {
            lazyCrafter$recipeChecker.clearCache();
        }
    }
    
    /**
     * Update button position when layout changes
     */
    @Inject(method = "initFilterButtonTextures", at = @At("TAIL"))
    private void lazyCrafter$updateLayout(CallbackInfo ci) {
        lazyCrafter$updateButtonPosition();
    }
    
    /**
     * Update button position when the recipe book is resized or repositioned
     */
    @Inject(method = "updateScreenPosition", at = @At("TAIL"))
    private void lazyCrafter$updateScreenPosition(int screenWidth, int screenHeight, CallbackInfo ci) {
        lazyCrafter$updateButtonPosition();
    }
    
    /**
     * Hide button when recipe book is not visible
     */
    @Inject(method = "setVisible", at = @At("HEAD"))
    private void lazyCrafter$setVisible(boolean visible, CallbackInfo ci) {
        if (lazyCrafter$autoCraftButton != null && !visible) {
            lazyCrafter$autoCraftButton.hide();
        }
    }
}