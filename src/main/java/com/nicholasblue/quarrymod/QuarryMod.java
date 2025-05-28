package com.nicholasblue.quarrymod;

import com.mojang.logging.LogUtils;
import com.nicholasblue.quarrymod.client.SuppressionDebugRenderer;
import com.nicholasblue.quarrymod.data.BlockIndexer;
import com.nicholasblue.quarrymod.data.QuarrySuppressionSavedData;
import com.nicholasblue.quarrymod.item.ModItems;
import com.nicholasblue.quarrymod.manager.CentralQuarryManager;
import com.nicholasblue.quarrymod.manager.QuarryStatePersistenceManager;
import com.nicholasblue.quarrymod.manager.SuppressionPersistenceManager;
import com.nicholasblue.quarrymod.network.QuarryNetwork;
import com.nicholasblue.quarrymod.registry.ModBlockEntities;
import com.nicholasblue.quarrymod.registry.ModBlocks;
import com.nicholasblue.quarrymod.registry.ModMenus;
import com.nicholasblue.quarrymod.menu.QuarryScreen;
import com.nicholasblue.quarrymod.suppression.GlobalSuppressionIndex;
import com.nicholasblue.quarrymod.suppression.SuppressionDiagnostics;
import com.nicholasblue.quarrymod.util.QuarryPlacementScheduler;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

@Mod(QuarryMod.MODID)
public class QuarryMod {
    public static final String MODID = "quarrymod";
    private static final int SUPPRESSION_SAVE_INTERVAL_TICKS = 180; // 9 seconds
    private static int suppressionSaveCountdown = SUPPRESSION_SAVE_INTERVAL_TICKS;
    public static final Logger LOGGER = LogUtils.getLogger(); // net.minecraftforge.fml.util
    public static final SuppressionDiagnostics SUPPRESSION_DIAGNOSTICS = new SuppressionDiagnostics(GlobalSuppressionIndex.INSTANCE);

    public QuarryMod() {
        System.out.println("Mixin config resource: " + QuarryMod.class.getClassLoader().getResource("mixin.quarrymod.json"));
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModBlocks.register();
        ModItems.register();
        ModMenus.register();
        ModBlockEntities.register();
        QuarryNetwork.init(); // <-- REGISTER PACKETS
        MinecraftForge.EVENT_BUS.register(this);


    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        QuarryPlacementScheduler.tick();

        MinecraftServer server = event.getServer();

        ServerLevel overworld = server.overworld();

    }



    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            if (level.dimension() == ServerLevel.OVERWORLD) { // Or your relevant dimension
                QuarryMod.LOGGER.info("Overworld loading. Initializing Quarry Suppression Data via SavedData system for level {}.", level.dimension().location());
                QuarryStatePersistenceManager.loadQuarryStates(level, CentralQuarryManager.INSTANCE);

                // This call to QuarrySuppressionSavedData.get() will trigger either
                // loadAndInitializeGSI (if data exists) or createNewAndInitializeGSI (if not).
                // In both cases, GSI will be appropriately populated or reset.
                QuarrySuppressionSavedData.get(level);

            }
        }
    }


    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Server stopping. Attempting to save Quarry Mod data.");
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld(); // CQM operates based on overworld

        if (overworld != null) {
            //todo double check persistM for state and suppression. Think i unified them so they're both saveddata but different strategies.
            QuarryStatePersistenceManager.saveQuarryStates(overworld, CentralQuarryManager.INSTANCE);

        }
    }



    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            System.out.println("player logged in, starting to send index via onplayerlogin");
            Map<Integer, ResourceLocation> map = BlockIndexer.getCachedIdToResourceMap();
            QuarryNetwork.sendBlockIdMap(player, map);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        System.out.println("player logged in, starting to send index");

        QuarryNetwork.sendBlockIdMap(player, BlockIndexer.getCachedIdToResourceMap());
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        System.out.println("client joind server, sending request for index");
        QuarryNetwork.sendBlockIndexRequest();
    }


    public String getModid(){
        return MODID;
    }




}