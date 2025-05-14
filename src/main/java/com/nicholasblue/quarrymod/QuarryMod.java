package com.nicholasblue.quarrymod;

import com.mojang.logging.LogUtils;
import com.nicholasblue.quarrymod.client.SuppressionDebugRenderer;
import com.nicholasblue.quarrymod.data.BlockIndexer;
import com.nicholasblue.quarrymod.item.ModItems;
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
    private static final int SUPPRESSION_SAVE_INTERVAL_TICKS = 1800; // 90 seconds
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