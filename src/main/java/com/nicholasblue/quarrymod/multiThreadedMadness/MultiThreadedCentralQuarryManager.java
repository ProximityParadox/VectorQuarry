package com.nicholasblue.quarrymod.multiThreadedMadness;

import com.nicholasblue.quarrymod.ISP.ImmutableStatePool;
import com.nicholasblue.quarrymod.data.QuarryBlockData;
import com.nicholasblue.quarrymod.data.QuarryRuntimeState;
import com.nicholasblue.quarrymod.manager.QuarryRegistry;
import com.nicholasblue.quarrymod.suppression.GlobalSuppressionIndex;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Centralized global controller for every quarry in the world.
 *
 * <p>Responsibilities</p>
 * <ul>
 *   <li>Maintains a registry of active quarries and their minimal runtime state.</li>
 *   <li>Schedules and executes batched quarry ticks once per server tick.</li>
 *   <li>Coordinates all suppression‑layer updates through {@link }.</li>
 *   <li>Performs periodic persistence snapshots for crash‑safe recovery.</li>
 *   <li>Publishes progress / suppression updates to nearby clients.</li>
 * </ul>
 *
 * <p>This class does <strong>not</strong> touch NBT storage for individual
 * {@code QuarryBlockEntity}s; instead, entities delegate to CQM and act as
 * dumb anchors for UI interaction or block removal.</p>
 */








//basically, tried cooking some multi-threading, but the current workload of low work - high counts make the context switch more painful than any work saved
@Mod.EventBusSubscriber(modid = "quarrymod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MultiThreadedCentralQuarryManager {

    public static final MultiThreadedCentralQuarryManager INSTANCE =
            new MultiThreadedCentralQuarryManager();

    private final QuarryRegistry registry = new QuarryRegistry();
    private final Long2ObjectOpenHashMap<QuarryRuntimeState> runtimeStates = new Long2ObjectOpenHashMap<>();

    private final ParallelQuarryPlanner planner =
            new ParallelQuarryPlanner(Runtime.getRuntime().availableProcessors());

    private MultiThreadedCentralQuarryManager() {}

    public void registerQuarry(QuarryBlockData data) {
        long key = data.quarryPos.asLong();
        if (!registry.register(data)) return;

        GlobalSuppressionIndex.INSTANCE.addFullShellLayer(data.origin, data.xSize, data.zSize, data.startY);
        runtimeStates.put(key, new QuarryRuntimeState(data.startY, 0, true));
    }

    public void unregisterQuarry(BlockPos quarryPos) {
        long key = quarryPos.asLong();
        QuarryBlockData removedConfig = registry.unregister(key);
        QuarryRuntimeState removedState = runtimeStates.remove(key);
        if (removedConfig != null && removedState != null) {
            GlobalSuppressionIndex.INSTANCE.removeShell(
                    removedConfig.origin, removedConfig.xSize, removedConfig.zSize,
                    removedState.getCurrentY(), removedConfig.startY);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) return;
        INSTANCE.tickAll(evt.getServer().overworld());
        ImmutableStatePool.INSTANCE.releaseAllThisTick();
    }

    private void tickAll(ServerLevel level) {
        int currentTick = (int) (level.getServer().getTickCount() & 0x7FFFFFFF);
        ImmutableStatePool.INSTANCE.setCurrentTick(currentTick);

        Long2ObjectOpenHashMap<QuarryBlockData> configSnapshot = registry.snapshot();

        List<QuarryAction> actions = planner.planAll(configSnapshot, runtimeStates, level.getMinBuildHeight());
        applyActions(actions, configSnapshot, level);
    }

    private void applyActions(List<QuarryAction> actions,
                              Long2ObjectOpenHashMap<QuarryBlockData> configSnapshot,
                              ServerLevel level) {
        for (QuarryAction action : actions) {
            long key = action.quarryKey();
            QuarryBlockData config = configSnapshot.get(key);
            QuarryRuntimeState runtime = runtimeStates.get(key);

            if (config == null || runtime == null) continue;

            if (action.shouldMine() && action.target() != null) {
                //System.out.printf("[Mine] Quarry %d mining block at %s\n", key, action.target());
                BlockState state = level.getBlockState(action.target());
                Block targetBlock = state.getBlock();

                //System.out.println("block name: " + state.getBlock().getName());

                if (!state.isAir() && targetBlock != Blocks.BEDROCK) {
                    level.setBlock(action.target(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                runtime.advanceProgress();
            }

            if (action.layerComplete()) {
                if (action.stopQuarry()) {
                    //System.out.printf("[Apply] Quarry %d stopped at Y=%d\n", key, runtime.getCurrentY());
                    GlobalSuppressionIndex.INSTANCE.removeShell(
                            config.origin, config.xSize, config.zSize,
                            Math.max(action.nextY(), level.getMinBuildHeight()), config.startY);
                    runtime.stop();
                } else {
                    //System.out.printf("[Apply] Quarry %d descending to Y=%d\n", key, action.nextY());

                    GlobalSuppressionIndex.INSTANCE.descendShell(
                            config.origin, config.xSize, config.zSize,
                            runtime.getCurrentY(), action.nextY()
                    );
                    runtime.setCurrentY(action.nextY());
                    runtime.resetProgress();
                }
            }
        }
    }

    public void shutdown() {
        planner.shutdown();
    }
}

