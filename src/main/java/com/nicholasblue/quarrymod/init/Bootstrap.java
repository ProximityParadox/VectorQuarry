package com.nicholasblue.quarrymod.init;


import com.nicholasblue.quarrymod.data.BlockIndexer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import static com.nicholasblue.quarrymod.QuarryMod.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Bootstrap {
    @SubscribeEvent
    public static void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(BlockIndexer::buildIndex);
    }
}