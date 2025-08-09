package igentuman.lazycrafter.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Configuration for the Lazy Crafter mod
 */
public class LazyCrafterConfig {
    
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    // Configuration values
    public static final ForgeConfigSpec.IntValue MAX_RECURSION_DEPTH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_AUTO_CRAFTING;
    public static final ForgeConfigSpec.BooleanValue SHOW_TOOLTIPS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CACHING;
    public static final ForgeConfigSpec.IntValue BUTTON_OFFSET_X;
    public static final ForgeConfigSpec.IntValue BUTTON_OFFSET_Y;
    
    static {
        BUILDER.push("General Settings");
        
        ENABLE_AUTO_CRAFTING = BUILDER
            .comment("Enable auto-crafting functionality")
            .define("enableAutoCrafting", true);
        
        MAX_RECURSION_DEPTH = BUILDER
            .comment("Maximum recursion depth for nested recipes (higher values may impact performance)")
            .defineInRange("maxRecursionDepth", 10, 1, 50);
        
        ENABLE_CACHING = BUILDER
            .comment("Enable recipe checking cache for better performance")
            .define("enableCaching", true);
        
        BUILDER.pop();
        
        BUILDER.push("GUI Settings");
        
        SHOW_TOOLTIPS = BUILDER
            .comment("Show tooltips for the auto-craft button")
            .define("showTooltips", true);
        
        BUTTON_OFFSET_X = BUILDER
            .comment("Horizontal offset for the auto-craft button position")
            .defineInRange("buttonOffsetX", 0, -100, 100);
        
        BUTTON_OFFSET_Y = BUILDER
            .comment("Vertical offset for the auto-craft button position")
            .defineInRange("buttonOffsetY", 0, -100, 100);
        
        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
    
    /**
     * Register the configuration
     */
    public static void register() {
        // For now, we'll skip config registration to avoid deprecated API issues
        // The config will use default values
        // TODO: Update to newer config registration method when available
    }
    
    /**
     * Check if auto-crafting is enabled
     */
    public static boolean isAutoCraftingEnabled() {
        try {
            return ENABLE_AUTO_CRAFTING.get();
        } catch (Exception e) {
            return true; // Default value
        }
    }
    
    /**
     * Get the maximum recursion depth
     */
    public static int getMaxRecursionDepth() {
        try {
            return MAX_RECURSION_DEPTH.get();
        } catch (Exception e) {
            return 10; // Default value
        }
    }
    
    /**
     * Check if tooltips should be shown
     */
    public static boolean shouldShowTooltips() {
        try {
            return SHOW_TOOLTIPS.get();
        } catch (Exception e) {
            return true; // Default value
        }
    }
    
    /**
     * Check if caching is enabled
     */
    public static boolean isCachingEnabled() {
        try {
            return ENABLE_CACHING.get();
        } catch (Exception e) {
            return true; // Default value
        }
    }
    
    /**
     * Get button X offset
     */
    public static int getButtonOffsetX() {
        try {
            return BUTTON_OFFSET_X.get();
        } catch (Exception e) {
            return 0; // Default value
        }
    }
    
    /**
     * Get button Y offset
     */
    public static int getButtonOffsetY() {
        try {
            return BUTTON_OFFSET_Y.get();
        } catch (Exception e) {
            return 0; // Default value
        }
    }
}