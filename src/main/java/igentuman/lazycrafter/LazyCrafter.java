package igentuman.lazycrafter;

import igentuman.lazycrafter.config.LazyCrafterConfig;
import igentuman.lazycrafter.recipe.AsyncRecipeProcessor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(LazyCrafter.MODID)
@Mod.EventBusSubscriber
public class LazyCrafter
{
    public static final String MODID = "lazycrafter";
    public static final Logger logger = LogManager.getLogger();

    public LazyCrafter() {
        this(FMLJavaModLoadingContext.get());
    }

    public LazyCrafter(FMLJavaModLoadingContext context) {
        logger.info("LazyCrafter mod initializing...");
        
        // Register configuration
        LazyCrafterConfig.register();
        
        // Register event handlers
        MinecraftForge.EVENT_BUS.register(this);
        
        if (FMLEnvironment.dist == Dist.CLIENT) {
            logger.info("Client-side initialization complete");
            
            // Add shutdown hook for async processor cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down async recipe processor...");
                AsyncRecipeProcessor.getInstance().shutdown();
            }));
        }
        
        logger.info("LazyCrafter mod initialized successfully");
    }
}
