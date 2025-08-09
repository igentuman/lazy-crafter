package igentuman.lazycrafter.event;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import igentuman.lazycrafter.LazyCrafter;

/**
 * Client-side event handler for the Lazy Crafter mod
 */
@Mod.EventBusSubscriber(modid = LazyCrafter.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandler {
    
    /**
     * Welcome message when player joins the world
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                // Send a welcome message to let the player know the mod is loaded
                minecraft.player.sendSystemMessage(
                    Component.translatable("chat.lazycrafter.welcome")
                );
                
                LazyCrafter.logger.info("Lazy Crafter mod is active for player: {}", 
                    minecraft.player.getName().getString());
            }
        }
    }
}