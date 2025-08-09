package igentuman.lazycrafter;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        igentuman.lazycrafter.LazyCrafter.MODID
)
@Mod.EventBusSubscriber
public class LazyCrafter
{
    public static final String MODID = "lazycrafter";
    public static final Logger logger = LogManager.getLogger();

    public LazyCrafter() {
        this(FMLJavaModLoadingContext.get());
    }

    public LazyCrafter(FMLJavaModLoadingContext context) {

    }

}
