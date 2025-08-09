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
        super(x, y, 10, 10, Component.translatable("gui.lazycrafter.auto_craft"),
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
        
        // Determine button state colors
        int backgroundColor, borderColor, iconColor;
        
        if (!active) {
            backgroundColor = 0x80404040; // Disabled - dark gray
            borderColor = 0x80606060;
            iconColor = 0x80808080;
        } else if (isHoveredOrFocused()) {
            backgroundColor = 0xC0004080; // Hovered - bright blue
            borderColor = 0xFF0060C0;
            iconColor = 0xFFFFFFFF;
        } else {
            backgroundColor = 0xA0003060; // Normal - darker blue
            borderColor = 0xFF004080;
            iconColor = 0xFFE0E0E0;
        }
        
        // Render button background with rounded corners effect
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, backgroundColor);
        
        // Render border
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + 1, borderColor); // Top
        guiGraphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor); // Bottom
        guiGraphics.fill(getX(), getY(), getX() + 1, getY() + height, borderColor); // Left
        guiGraphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor); // Right
        
        // Render auto-craft icon - a stylized "A" for Auto
        if (active || !active) { // Always show icon
            int centerX = getX() + width / 2;
            int centerY = getY() + height / 2;
            
            // Draw a simple "A" shape (smaller)
            // Vertical lines
            guiGraphics.fill(centerX - 2, centerY - 3, centerX - 1, centerY + 3, iconColor); // Left line
            guiGraphics.fill(centerX + 1, centerY - 3, centerX + 2, centerY + 3, iconColor); // Right line
            
            // Top horizontal line
            guiGraphics.fill(centerX - 1, centerY - 3, centerX + 1, centerY - 2, iconColor);
            
            // Middle horizontal line
            guiGraphics.fill(centerX - 1, centerY, centerX + 1, centerY + 1, iconColor);
        }
        
        // Add a subtle glow effect when hovered
        if (isHoveredOrFocused() && active) {
            guiGraphics.fill(getX() - 1, getY() - 1, getX() + width + 1, getY(), 0x40FFFFFF); // Top glow
            guiGraphics.fill(getX() - 1, getY() + height, getX() + width + 1, getY() + height + 1, 0x40FFFFFF); // Bottom glow
            guiGraphics.fill(getX() - 1, getY(), getX(), getY() + height, 0x40FFFFFF); // Left glow
            guiGraphics.fill(getX() + width, getY(), getX() + width + 1, getY() + height, 0x40FFFFFF); // Right glow
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