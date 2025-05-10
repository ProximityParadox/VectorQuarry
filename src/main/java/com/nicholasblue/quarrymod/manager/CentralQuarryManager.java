package com.nicholasblue.quarrymod.manager;

import com.nicholasblue.quarrymod.ISP.ImmutableStatePool;
import com.nicholasblue.quarrymod.data.QuarryBlockData;
import com.nicholasblue.quarrymod.data.QuarryRuntimeState;
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

import java.util.concurrent.atomic.AtomicReference;

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
@Mod.EventBusSubscriber(modid = "quarrymod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CentralQuarryManager {

    /* ─────────────────────── Singleton wiring ─────────────────────── */

    public static final CentralQuarryManager INSTANCE = new CentralQuarryManager();
    private CentralQuarryManager() {}
    private final QuarryRegistry registry = new QuarryRegistry();
    private final Long2ObjectOpenHashMap<QuarryRuntimeState> runtimeStates = new Long2ObjectOpenHashMap<>();


    /* ─────────────────────── Global registry (lock‑free) ───────────────────── */

    public void registerQuarry(QuarryBlockData data) {
        long key = data.quarryPos.asLong();
        boolean inserted = registry.register(data);

        if (!inserted) return;

        // Initialize runtime state in parallel with config
        QuarryRuntimeState runtime = new QuarryRuntimeState(data.startY, 0, true);
        runtimeStates.put(key, runtime);

        //System.out.printf("[CQM] Registering quarry: origin=%s, xSize=%d, zSize=%d, startY=%d\n", data.origin, data.xSize, data.zSize, data.startY);

        GlobalSuppressionIndex.INSTANCE.addFullShellLayer(data.origin, data.xSize, data.zSize, data.startY);
    }


    public void unregisterQuarry(BlockPos quarryPos) {
        long key = quarryPos.asLong();

        // Remove both config and runtime entries
        QuarryBlockData removedConfig = registry.unregister(key);
        QuarryRuntimeState removedState = runtimeStates.remove(key);

        if (removedConfig != null && removedState != null) {
            GlobalSuppressionIndex.INSTANCE.removeShell(
                    removedConfig.origin,
                    removedConfig.xSize,
                    removedConfig.zSize,
                    removedState.getCurrentY(),
                    removedConfig.startY
            );
        }
    }

    /**
     * CAS‑swapped reference holding the current snapshot of <quarryId → state>.
     * The key is the block‑pos long for the Quarry block’s location.
     */
    private final AtomicReference<Long2ObjectOpenHashMap<QuarryBlockData>> stateRef =
            new AtomicReference<>(new Long2ObjectOpenHashMap<>());


    /* ─────────────────────── Server‑tick hook  ───────────────────── */

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) return;  // run once per tick
        int currentTick = (int) (evt.getServer().getTickCount() & 0x7FFFFFFF); // Clamp to 31 bits
        ImmutableStatePool.INSTANCE.setCurrentTick(currentTick);
        INSTANCE.tickAll(evt.getServer().overworld()); // main world only
        ImmutableStatePool.INSTANCE.releaseAllThisTick(); // Finalize tick-local state
    }

    private void tickAll(ServerLevel level) {
        Long2ObjectOpenHashMap<QuarryBlockData> configSnapshot = registry.snapshot();
        long[] keys = configSnapshot.keySet().toLongArray();
        java.util.Arrays.sort(keys);

        for (long key : keys) {
            QuarryBlockData config = configSnapshot.get(key);
            QuarryRuntimeState runtime = runtimeStates.get(key);

            // Defensive: ensure both parts are non-null and quarry is active
            if (config == null || runtime == null || !runtime.isRunning()) {
                continue;
            }

            processQuarryTick(level, key, config, runtime);
        }
    }



    /* ─────────────────────── Internal per‑quarry logic  ───────────────────── */

    /**
     * Execute one logical unit of work for a single quarry.
     *
     * Returns number of blocks mined this tick (0 or 1 in current design).
     */
    private void processQuarryTick(ServerLevel level, long quarryKey,
                                   QuarryBlockData config, QuarryRuntimeState runtime) {

        if (!runtime.isRunning()) return;

        int progress = runtime.getProgressCounter();
        int dx = progress % config.xSize;
        int dz = progress / config.xSize;

        // Check for layer completion
        if (dz >= config.zSize) {
            int currentY = runtime.getCurrentY();
            int nextY = currentY - 1;

            if (nextY < level.getMinBuildHeight()) {
                int clampedY = Math.max(nextY, level.getMinBuildHeight());

                GlobalSuppressionIndex.INSTANCE.removeShell(
                        config.origin, config.xSize, config.zSize,
                        clampedY, config.startY
                );

                runtime.stop();
            } else {
                // Perform batch suppression descent
                GlobalSuppressionIndex.INSTANCE.descendShell(
                        config.origin, config.xSize, config.zSize,
                        currentY, nextY
                );

                runtime.setCurrentY(nextY);
                runtime.resetProgress();
            }
            return;
        }


        // Compute mining target
        BlockPos.MutableBlockPos scratch = ImmutableStatePool.INSTANCE.unsafeMutablePos();
        scratch.set(config.origin.getX() + dx, runtime.getCurrentY(), config.origin.getZ() + dz);
        BlockPos target = scratch.immutable();

        BlockState state = level.getBlockState(target);
        Block targetBlock = state.getBlock();

        if (!state.isAir() && targetBlock != Blocks.BEDROCK) {
            level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            // Optionally, insert mining effects or item drops here
        }

        runtime.advanceProgress(); // mutate in-place
    }


}
