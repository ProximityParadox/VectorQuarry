package com.nicholasblue.quarrymod;

import com.mojang.logging.LogUtils;
import com.nicholasblue.quarrymod.client.SuppressionDebugRenderer;
import com.nicholasblue.quarrymod.item.ModItems;
import com.nicholasblue.quarrymod.manager.SuppressionPersistenceManager;
import com.nicholasblue.quarrymod.network.SuppressionNetwork;
import com.nicholasblue.quarrymod.registry.ModBlockEntities;
import com.nicholasblue.quarrymod.registry.ModBlocks;
import com.nicholasblue.quarrymod.registry.ModMenus;
import com.nicholasblue.quarrymod.menu.QuarryScreen;
import com.nicholasblue.quarrymod.suppression.GlobalSuppressionIndex;
import com.nicholasblue.quarrymod.suppression.SuppressionDiagnostics;
import com.nicholasblue.quarrymod.util.QuarryPlacementScheduler;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(QuarryMod.MODID)
public class QuarryMod {
    public static final String MODID = "quarrymod";
    private static final int SUPPRESSION_SAVE_INTERVAL_TICKS = 1800; // 90 seconds
    private static int suppressionSaveCountdown = SUPPRESSION_SAVE_INTERVAL_TICKS;
    public static final Logger LOGGER = LogUtils.getLogger(); // net.minecraftforge.fml.util
    public static final SuppressionDiagnostics SUPPRESSION_DIAGNOSTICS = new SuppressionDiagnostics(GlobalSuppressionIndex.INSTANCE);

    public QuarryMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModBlocks.register();
        ModItems.register();
        ModMenus.register();
        ModBlockEntities.register();
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onClientSetup);
        SuppressionNetwork.init(); // <-- REGISTER PACKETS
        MinecraftForge.EVENT_BUS.register(this);



        // client-only hook
        MinecraftForge.EVENT_BUS.addListener(this::clientSetup);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Example external tick call
        QuarryPlacementScheduler.tick();

        // Correct way to get MinecraftServer
        MinecraftServer server = event.getServer();

        // From server, get the overworld (or iterate all ServerLevels if needed)
        ServerLevel overworld = server.overworld();

        // Perform the suppression save
        if (--suppressionSaveCountdown <= 0) {
            suppressionSaveCountdown = SUPPRESSION_SAVE_INTERVAL_TICKS;
            SuppressionPersistenceManager.saveToFile(overworld);
        }
    }



    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        SuppressionPersistenceManager.loadFromFile(level);
    }






    private void clientSetup(FMLClientSetupEvent evt) {
        MenuScreens.register(ModMenus.QUARRY_MENU.get(), QuarryScreen::new);
    }
    public String getModid(){
        return MODID;
    }
    private static final SuppressionDebugRenderer DEBUG_RENDERER = new SuppressionDebugRenderer();
    public static SuppressionDebugRenderer getDebugRenderer() { return DEBUG_RENDERER; }

    private void onClientSetup(FMLClientSetupEvent evt) {
        MinecraftForge.EVENT_BUS.register(DEBUG_RENDERER);
    }

}