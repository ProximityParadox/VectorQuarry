package com.nicholasblue.quarrymod.manager;

import com.nicholasblue.quarrymod.ISP.ImmutableStatePool;
import com.nicholasblue.quarrymod.QuarryMod;
import com.nicholasblue.quarrymod.data.BlockIndexer;
import com.nicholasblue.quarrymod.data.QuarryBlockData;
import com.nicholasblue.quarrymod.data.QuarryRuntimeState;
import com.nicholasblue.quarrymod.data.QuarrySuppressionSavedData;
import com.nicholasblue.quarrymod.item.ItemBuffer;
import com.nicholasblue.quarrymod.suppression.GlobalSuppressionIndex;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
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

    public boolean registerQuarry(QuarryBlockData data, ServerLevel level) {
        long key = data.quarryPos.asLong();
        QuarryMod.LOGGER.debug("[CQM] Attempting to register quarry at {}. Key: {}", data.quarryPos, key);

        // Attempt to register in the thread-safe registry.
        // QuarryRegistry.register() already handles the "already present" case atomically.
        boolean registeredInRegistry = registry.register(data);

        if (!registeredInRegistry) {
            QuarryMod.LOGGER.warn("[CQM] Failed to register quarry at {} in QuarryRegistry (key {}). Likely already present.", data.quarryPos, key);
            return false;
        }

        // If successfully registered in QuarryRegistry, proceed with runtime state and suppression.
        // These operations are on CQM's own data structures or global state,
        // assumed to be called from a safe context (server thread, during tick or controlled setup like onPlace).
        QuarryRuntimeState runtime = new QuarryRuntimeState(data.startY, 0, true);
        runtimeStates.put(key, runtime); // runtimeStates is managed by CQM, assumed to be mutated on server thread.

        // CRITICAL: This call adds the initial suppression. Only for new quarries.
        GlobalSuppressionIndex.INSTANCE.addFullShellLayer(data.origin, data.xSize, data.zSize, data.startY);
        QuarrySuppressionSavedData.get(level).setDirty();



        QuarryMod.LOGGER.info("[CQM] Successfully registered quarry and initialized runtime/suppression for {}. Key: {}", data.quarryPos, key);
        return true;
    }

    // Getter for QuarryRegistry (if QuarryStatePersistenceManager needs it - it does)
    public QuarryRegistry getRegistry() {
        return registry;
    }

    // Getter for runtimeStates (if QuarryStatePersistenceManager needs it - it does)
    public Long2ObjectOpenHashMap<QuarryRuntimeState> getRuntimeStates() {
        return runtimeStates;
    }

    // Setter for runtimeStates (for QuarryStatePersistenceManager)
    public void restoreRuntimeStates(Long2ObjectOpenHashMap<QuarryRuntimeState> restoredRuntimes) {
        this.runtimeStates.clear();
        this.runtimeStates.putAll(restoredRuntimes);
    }


    public void unregisterQuarry(BlockPos quarryPos) {
        long key = quarryPos.asLong();

        QuarryBlockData removedConfig = registry.unregister(key);
        QuarryRuntimeState removedState = runtimeStates.remove(key); // Remove runtime state regardless of config state

        if (removedConfig != null) { // Only attempt suppression removal if config was actually there
            int currentYToRemoveFrom = removedConfig.startY; // Default to startY if no runtime state
            if (removedState != null) {
                currentYToRemoveFrom = removedState.getCurrentY();
            } else {
                // If runtime state was null, it implies it might have been a quarry that never ran,
                // or its runtime state wasn't persisted/loaded.
                // We should still attempt to clean up suppression based on its potential full depth.
                QuarryMod.LOGGER.warn("[CQM] Unregistering quarry {} which had config but no active runtime state. Using startY for suppression removal.", quarryPos);
            }
            GlobalSuppressionIndex.INSTANCE.removeShell(
                    removedConfig.origin,
                    removedConfig.xSize,
                    removedConfig.zSize,
                    currentYToRemoveFrom, // The Y level it was currently at or about to mine
                    removedConfig.startY  // The original starting Y level
            );
            QuarryMod.LOGGER.info("[CQM] Unregistered quarry at {}. Removed suppression shell.", quarryPos);
        } else {
            QuarryMod.LOGGER.debug("[CQM] Attempted to unregister quarry at {}, but no config found.", quarryPos);
        }
    }

    /* ─────────────────────── Server‑tick hook  ───────────────────── */

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {

        if (evt.phase != TickEvent.Phase.END) return;  // run once per tick
        int currentTick = (int) (evt.getServer().getTickCount() & 0x7FFFFFFF); // Clamp to 31 bits
        ImmutableStatePool.INSTANCE.setCurrentTick(currentTick);
        INSTANCE.tickAll(evt.getServer().overworld(), currentTick); // main world only
        ImmutableStatePool.INSTANCE.releaseAllThisTick(); // Finalize tick-local state
    }

    private void tickAll(ServerLevel level, int currentTick) {
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

            processQuarryTick(level, key, config, runtime, currentTick);
        }
    }

    @Nullable
    public QuarryRuntimeState getRuntimeState(BlockPos pos) {
        return runtimeStates.get(pos.asLong());
    }



    /* ─────────────────────── Internal per‑quarry logic  ───────────────────── */

    /**
     * Execute one logical unit of work for a single quarry.
     *
     * Returns number of blocks mined this tick (0 or 1 in current design).
     */
    private void processQuarryTick(ServerLevel level, long quarryKey,
                                   QuarryBlockData config, QuarryRuntimeState runtime, int currentTick) {

        if (!runtime.isRunning()) return;

        int progress = runtime.getProgressCounter();
        int dx = progress % config.xSize;
        int dz = progress / config.xSize;

        if (dz >= config.zSize) {

            int currentY = runtime.getCurrentY();
            int nextY = currentY - 1;

            if (nextY < level.getMinBuildHeight()) {
                QuarryMod.LOGGER.info("[CQM processQuarryTick] Quarry {} Reached bottom at Y={}. Stopping. Config xS:{} zS:{}",
                        quarryKey, currentY, config.xSize, config.zSize);

                // Final suppression removal for the entire quarry column
                GlobalSuppressionIndex.INSTANCE.removeShell(
                        config.origin, config.xSize, config.zSize,
                        // Y level it was trying to mine (or minBuildHeight if below)
                        Math.max(nextY, level.getMinBuildHeight()),
                        config.startY
                );
                QuarrySuppressionSavedData.get(level).setDirty(); // Ensure saved
                runtime.stop();

            } else {
                // Not at world bottom, so descend to the next layer
                //QuarryMod.LOGGER.info("[CQM processQuarryTick] Quarry {} Descending Layer: currentY={}, nextY={}. Progress was {}. Config xS:{} zS:{}", quarryKey, currentY, nextY, progress, config.xSize, config.zSize);

                GlobalSuppressionIndex.INSTANCE.descendShell(
                        config.origin, config.xSize, config.zSize,
                        currentY, nextY
                );
                QuarrySuppressionSavedData.get(level).setDirty();

                runtime.setCurrentY(nextY);
                runtime.resetProgress();
            }
            return;
        }

        BlockPos.MutableBlockPos scratch = ImmutableStatePool.INSTANCE.unsafeMutablePos();
        scratch.set(config.origin.getX() + dx, runtime.getCurrentY(), config.origin.getZ() + dz);
        BlockPos target = scratch.immutable();

        BlockState state = level.getBlockState(target);
        Block targetBlock = state.getBlock();

        if (!state.isAir() && targetBlock != Blocks.BEDROCK) {

            BlockEntity blockEntity = level.getBlockEntity(target);

            boolean liquid = !state.getFluidState().isEmpty();

            boolean isComplexBlock = blockEntity != null;
            if (!GlobalSuppressionIndex.INSTANCE.isSuppressed(target)){
                System.out.println("SUPPRESSION FAILED BEFORE: " + target);
            };
            level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS, 0);
            if (!GlobalSuppressionIndex.INSTANCE.isSuppressed(target)){
                System.out.println("SUPPRESSION FAILED AFTER: " + target);
            };
            if (isComplexBlock || liquid) {
                // Logic to get the ItemStack *with* NBT data for 'targetBlock' at 'target'
            } else {
                short id = BlockIndexer.tryGetShortId(targetBlock);
                if(!(id==-1)){
                    ItemBuffer buf = runtime.getItemBuffer();
                    buf.tick(currentTick);
                    buf.add(id, currentTick);

                }
                else{
                    runtime.getOverflowBuffer().add(BlockIndexer.getIntId(targetBlock), currentTick);
                }
            }
        }

        runtime.advanceProgress(); // mutate in-place
    }


}
