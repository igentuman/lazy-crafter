package igentuman.lazycrafter.integration.jei;

import igentuman.lazycrafter.LazyCrafter;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Main JEI plugin for LazyCrafter integration
 * Registers GUI handlers and recipe transfer handlers for auto-crafting functionality
 */
@JeiPlugin
public class LazyCrafterJEIPlugin implements IModPlugin {
    
    private static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath(
        LazyCrafter.MODID, "jei_plugin"
    );
    
    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }
    
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        LazyCrafter.logger.info("Registering JEI GUI handlers for auto-crafting");
        
        // Register overlay handler for crafting screen
        registration.addGuiContainerHandler(CraftingScreen.class, new JEIAutoCraftingOverlay());
        
        // Register overlay handler for inventory screen (player crafting)
        registration.addGuiContainerHandler(InventoryScreen.class, new JEIAutoCraftingOverlay());
        
        LazyCrafter.logger.debug("JEI GUI handlers registered successfully");
    }
    
    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        LazyCrafter.logger.info("Registering JEI recipe transfer handlers for auto-crafting");
        
        try {
            // Register transfer handler for crafting table
            registration.addRecipeTransferHandler(
                new AutoCraftingTransferHandler<>(CraftingMenu.class),
                mezz.jei.api.constants.RecipeTypes.CRAFTING
            );
            
            // Register transfer handler for player inventory crafting
            registration.addRecipeTransferHandler(
                new AutoCraftingTransferHandler<>(InventoryMenu.class),
                mezz.jei.api.constants.RecipeTypes.CRAFTING
            );
            
            LazyCrafter.logger.debug("JEI recipe transfer handlers registered successfully");
        } catch (Exception e) {
            LazyCrafter.logger.error("Failed to register JEI recipe transfer handlers", e);
        }
    }
}