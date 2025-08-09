package igentuman.lazycrafter.events;

import igentuman.lazycrafter.crafting.AutoCraftingHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles client-side events for the LazyCrafter mod
 */
@Mod.EventBusSubscriber(modid = "lazycrafter", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandler {
    
    /**
     * Handle client tick events to process auto-crafting
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // Only process on the end phase to avoid double processing
        if (event.phase == TickEvent.Phase.END) {
            AutoCraftingHandler.getInstance().onClientTick();
        }
    }
}