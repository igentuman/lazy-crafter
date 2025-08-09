package igentuman.lazycrafter.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import igentuman.lazycrafter.LazyCrafter;
import igentuman.lazycrafter.config.LazyCrafterConfig;
import igentuman.lazycrafter.crafting.AutoCraftingHandler;

/**
 * Button for auto-crafting nested recipes
 */
public class AutoCraftingButton extends Button {
    private static final ResourceLocation RECIPE_BUTTON_LOCATION = 
        ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/recipe_button.png");
    
    private final RecipeBookComponent recipeBookComponent;
    private CraftingRecipe currentRecipe;
    private boolean visible = false;
    
    public AutoCraftingButton(int x, int y, RecipeBookComponent recipeBookComponent) {
        super(x, y, 25, 18, Component.translatable("gui.lazycrafter.auto_craft"), 
              button -> ((AutoCraftingButton) button).onPress(), DEFAULT_NARRATION);
        this.recipeBookComponent = recipeBookComponent;
    }
    
    @Override
    public void onPress() {
        if (currentRecipe != null && Minecraft.getInstance().player != null) {
            try {
                AutoCraftingHandler.getInstance().executeAutoCrafting(
                    currentRecipe, 
                    Minecraft.getInstance().player
                );
            } catch (Exception e) {
                LazyCrafter.logger.error("Error during auto-crafting", e);
            }
        }
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        
        RenderSystem.setShaderTexture(0, RECIPE_BUTTON_LOCATION);
        
        // Determine button state
        int textureY = 0;
        if (!active) {
            textureY = 36; // Disabled state
        } else if (isHoveredOrFocused()) {
            textureY = 18; // Hovered state
        }
        
        // Render button background
        guiGraphics.blit(RECIPE_BUTTON_LOCATION, getX(), getY(), 0, textureY, width, height);
        
        // Render auto-craft icon (you might want to create a custom texture)
        // For now, we'll use a simple overlay
        if (active) {
            guiGraphics.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, 0x8000FF00);
        }
    }
    
    public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (visible && isHoveredOrFocused() && LazyCrafterConfig.shouldShowTooltips()) {
            Component tooltip;
            if (currentRecipe != null) {
                tooltip = Component.translatable("gui.lazycrafter.auto_craft.tooltip", 
                    currentRecipe.getResultItem(Minecraft.getInstance().level.registryAccess()).getHoverName());
            } else {
                tooltip = Component.translatable("gui.lazycrafter.auto_craft.tooltip.generic");
            }
            guiGraphics.renderTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
        }
    }
    
    /**
     * Update the button state based on the current recipe
     */
    public void updateForRecipe(CraftingRecipe recipe, boolean canAutoCraft) {
        this.currentRecipe = recipe;
        this.visible = canAutoCraft;
        this.active = canAutoCraft;
    }
    
    /**
     * Hide the button
     */
    public void hide() {
        this.visible = false;
        this.active = false;
        this.currentRecipe = null;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * Set the position of the button
     */
    public void setPosition(int x, int y) {
        this.setX(x);
        this.setY(y);
    }
}