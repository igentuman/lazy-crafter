package igentuman.lazycrafter.integration;

import igentuman.lazycrafter.LazyCrafter;
import net.minecraftforge.fml.ModList;

/**
 * Manages JEI integration for LazyCrafter
 */
public class JEIIntegration {
    
    private static final String JEI_MOD_ID = "jei";
    private static boolean jeiLoaded = false;
    
    /**
     * Initialize JEI integration if JEI is present
     */
    public static void init() {
        if (ModList.get().isLoaded(JEI_MOD_ID)) {
            jeiLoaded = true;
            LazyCrafter.logger.info("JEI detected, enabling JEI integration");
            
            try {
                // JEI integration is handled by the @JeiPlugin annotation
                // The LazyCrafterJEIPlugin will be automatically discovered and loaded
                LazyCrafter.logger.info("JEI integration initialized successfully");
            } catch (Exception e) {
                LazyCrafter.logger.error("Failed to initialize JEI integration", e);
                jeiLoaded = false;
            }
        } else {
            LazyCrafter.logger.info("JEI not detected, skipping JEI integration");
        }
    }
    
    /**
     * Check if JEI is loaded and integration is active
     */
    public static boolean isJEILoaded() {
        return jeiLoaded;
    }
}