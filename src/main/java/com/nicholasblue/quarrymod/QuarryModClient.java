package com.nicholasblue.quarrymod;

import com.nicholasblue.quarrymod.client.SuppressionDebugRenderer;
import com.nicholasblue.quarrymod.network.QuarryNetwork;
import com.nicholasblue.quarrymod.registry.ModMenus;
import com.nicholasblue.quarrymod.menu.QuarryScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = QuarryMod.MODID,
        value = Dist.CLIENT,                 // <-- client only
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class QuarryModClient {

    private QuarryModClient() {}                              // no instantiation


    private static final SuppressionDebugRenderer DEBUG_RENDERER = new SuppressionDebugRenderer();

    public static SuppressionDebugRenderer getDebugRenderer() { return DEBUG_RENDERER; }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent evt) {
        evt.enqueueWork(() -> {
            System.out.println("client setting up");
            MenuScreens.register(ModMenus.QUARRY_MENU.get(), QuarryScreen::new);
            MinecraftForge.EVENT_BUS.register(DEBUG_RENDERER);
            MinecraftForge.EVENT_BUS.addListener(QuarryModClient::onClientLogin);
        });
    }

    private static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        System.out.println("Registering screen: " + ModMenus.QUARRY_MENU.get());
        System.out.println("Screen register type hash: " + System.identityHashCode(ModMenus.QUARRY_MENU.get()));

        System.out.println("client joined server, sending request for index");
        QuarryNetwork.sendBlockIndexRequest();
    }


}
