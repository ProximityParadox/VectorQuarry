package com.nicholasblue.quarrymod.events;

import com.nicholasblue.quarrymod.suppression.GlobalSuppressionIndex;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "quarrymod")
public class TickInterceptor {

    @SubscribeEvent
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (GlobalSuppressionIndex.INSTANCE.isSuppressed(event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (GlobalSuppressionIndex.INSTANCE.isSuppressed(event.getPos())) {
            event.setCanceled(true);
        }
    }


}

