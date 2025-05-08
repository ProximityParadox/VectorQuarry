package com.nicholasblue.quarrymod;

import com.nicholasblue.quarrymod.registry.ModMenus;
import com.nicholasblue.quarrymod.menu.QuarryScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = QuarryMod.MODID,
        value = Dist.CLIENT,                 // <-- client only
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class QuarryModClient {

    private QuarryModClient() {}                              // no instantiation

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent evt) {
        evt.enqueueWork(() ->
                MenuScreens.register(ModMenus.QUARRY_MENU.get(), QuarryScreen::new)
        );
    }
}
