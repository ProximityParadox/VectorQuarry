package com.nicholasblue.quarrymod.events;

import com.nicholasblue.quarrymod.QuarryMod;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.nicholasblue.quarrymod.command.SimpleQuarryCommand;

@Mod.EventBusSubscriber(modid = QuarryMod.MODID)
public final class CommandEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SimpleQuarryCommand.register(event.getDispatcher());
    }
}
