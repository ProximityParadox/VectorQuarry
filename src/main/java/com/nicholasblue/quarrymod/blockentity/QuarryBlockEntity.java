package com.nicholasblue.quarrymod.blockentity;

import com.nicholasblue.quarrymod.QuarryMod;
import com.nicholasblue.quarrymod.capability.SQEnergy;
import com.nicholasblue.quarrymod.data.QuarryBlockData;
import com.nicholasblue.quarrymod.manager.CentralQuarryManager;
import com.nicholasblue.quarrymod.menu.QuarryMenu;
import com.nicholasblue.quarrymod.multiThreadedMadness.MultiThreadedCentralQuarryManager;
import com.nicholasblue.quarrymod.registry.ModBlockEntities;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;

/**
 * Non-ticking, spatially-bound block entity that anchors a Quarry block in worldspace.
 *
 * <p>Tracks configuration and activation state. All execution state is delegated to the
 * {@link CentralQuarryManager} once confirmed.</p>
 */
public class QuarryBlockEntity extends BlockEntity {

    /** Null ⇒ not yet configured (waiting for marker input). */
    @Nullable
    private QuarryConfigData pendingConfig;
    /** True once registered with the CentralQuarryManager. */
    private boolean isRegistered = false;

    /** TEMPORARY: Skip marker logic and use default config if false. */
    private static final boolean CONFIG_CODE_EXISTS = false;

    public QuarryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUARRY_BLOCK_ENTITY.get(), pos, state);
    }

    private final SQEnergy energyStorage = new SQEnergy(100000, 1000, 0, 0); // Adjust as needed



    // todo: this fucking mess was caused by persistance issues that might have been solved in my refractor of CQM tick logic, check later
    public void handlePlacementConfiguration() {
        if (level == null || level.isClientSide()) return;

        // This method is called when the block is placed.
        // If CONFIG_CODE_EXISTS is false, we attempt to auto-register.
        if (!CONFIG_CODE_EXISTS) {
            // Check if already known to CQM to prevent issues if onPlace logic is complex
            // or if this method gets called unexpectedly after CQM might already know about it.
            if (!CentralQuarryManager.INSTANCE.getRegistry().snapshot().containsKey(this.worldPosition.asLong())) {
                QuarryMod.LOGGER.debug("QuarryBlockEntity at {}: Auto-registering due to onPlace and CONFIG_CODE_EXISTS=false.", this.worldPosition);
                QuarryBlockData defaultData = new QuarryBlockData(this.worldPosition, 9, 9);

                boolean success = CentralQuarryManager.INSTANCE.registerQuarry(defaultData, (ServerLevel) level);
                if (success) {
                    this.isRegistered = true; // Mark as registered in the BE state
                    this.pendingConfig = null; // No pending config for auto-registered quarries
                    setChanged(); // Save the BE state (isRegistered = true, pendingConfig = null)
                    QuarryMod.LOGGER.debug("QuarryBlockEntity at {}: Auto-registration successful. isRegistered=true.", this.worldPosition);
                } else {
                    QuarryMod.LOGGER.warn("QuarryBlockEntity at {}: Auto-registration with CQM failed (possibly already present or other CQM internal issue). isRegistered remains {}.", this.worldPosition, this.isRegistered);
                    // isRegistered remains its current value. onLoad will later sync with CQM.
                }
            } else {
                QuarryMod.LOGGER.debug("QuarryBlockEntity at {}: onPlace auto-registration skipped, already known to CQM.", this.worldPosition);
                // If it's already known to CQM, onLoad will handle syncing `isRegistered`.
            }
        } else {
            QuarryMod.LOGGER.debug("QuarryBlockEntity at {}: onPlace, CONFIG_CODE_EXISTS=true. Awaiting manual configuration. isRegistered={}", this.worldPosition, this.isRegistered);
            // isRegistered should be false here for a new placement. pendingConfig is null. Waits for attemptConfigure().
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            // Persistence loading (QuarryStatePersistenceManager) for CQM runs on LevelEvent.Load,
            // which typically precedes BlockEntity.onLoad for existing BEs in loaded chunks.
            // New placements would have run handlePlacementConfiguration() before or around onLoad if !CONFIG_CODE_EXISTS.

            boolean knownToCQM = CentralQuarryManager.INSTANCE.getRegistry().snapshot().containsKey(this.worldPosition.asLong());
            // boolean previousIsRegisteredNBT = this.isRegistered; // Value loaded from NBT by super.load()

            if (knownToCQM) {
                // Quarry is active/known in CQM. BE should reflect this.
                if (!this.isRegistered) {
                    this.isRegistered = true;
                    QuarryMod.LOGGER.info("QuarryBlockEntity at {}: Loaded. Known to CQM. Synced BE.isRegistered from false to true.", this.worldPosition);
                    setChanged(); // Persist the corrected state
                } else {
                    QuarryMod.LOGGER.debug("QuarryBlockEntity at {}: Loaded. Known to CQM. BE.isRegistered already true. Consistent.", this.worldPosition);
                }
                // If it's known to CQM, it means it's past the pendingConfig stage.
                // If pendingConfig somehow exists from NBT but it's registered, clear pendingConfig.
                if (this.pendingConfig != null) {
                    QuarryMod.LOGGER.warn("QuarryBlockEntity at {}: Loaded. Known to CQM but had pendingConfig from NBT. Clearing pendingConfig as it's active.", this.worldPosition);
                    this.pendingConfig = null;
                    setChanged();
                }
            } else {
                // Quarry is NOT active/known in CQM.
                // This could be a new BE awaiting configuration (if CONFIG_CODE_EXISTS=true),
                // or a BE whose CQM entry was lost, or !CONFIG_CODE_EXISTS and onPlace registration failed.
                if (this.isRegistered) {
                    QuarryMod.LOGGER.warn("QuarryBlockEntity at {}: Loaded. NOT known to CQM, but BE.isRegistered was true (from NBT). Resetting BE.isRegistered to false (CQM is master).", this.worldPosition);
                    this.isRegistered = false;
                    // pendingConfig might still be relevant if CONFIG_CODE_EXISTS=true and user wants to re-attempt configuration.
                    // If it was truly active and CQM lost it, then this is a data loss scenario.
                    setChanged();
                } else {
                    QuarryMod.LOGGER.debug("QuarryBlockEntity at {}: Loaded. Not known to CQM. BE.isRegistered already false. Consistent for inactive/unconfigured.", this.worldPosition);
                }
                // The auto-registration for !CONFIG_CODE_EXISTS was moved to onPlace/handlePlacementConfiguration.
                // So, if !knownToCQM here, it's either:
                // 1. CONFIG_CODE_EXISTS = true, awaiting manual configuration (pendingConfig may or may not be set from NBT).
                // 2. CONFIG_CODE_EXISTS = false, but onPlace registration failed or this is an orphaned BE not picked up by onPlace.
                // No direct action to register it from onLoad anymore.
            }
            QuarryMod.LOGGER.debug("QuarryBlockEntity at {}: onLoad processed. Final BE state: isRegistered={}, pendingConfigPresent={}", this.worldPosition, this.isRegistered, this.pendingConfig != null);
        }
    }

    // confirmAndActivate: This is for player-driven activation, should be fine.
    // It correctly sets isRegistered and clears pendingConfig.
    public boolean confirmAndActivate() {
        if (isRegistered || pendingConfig == null || level == null || level.isClientSide) return false;

        QuarryBlockData data = new QuarryBlockData(
                getBlockPos(),
                pendingConfig.xSize(),
                pendingConfig.zSize()
        );

        boolean success = CentralQuarryManager.INSTANCE.registerQuarry(data, (ServerLevel) level); // Or MTCQM if it handles suppression

        if (success) {
            isRegistered = true;
            pendingConfig = null;
            setChanged();
            QuarryMod.LOGGER.info("QuarryBlockEntity at {}: Confirmed and activated. Registered with CQM.", this.worldPosition);
            return true;
        } else {
            QuarryMod.LOGGER.warn("QuarryBlockEntity at {}: confirmAndActivate failed to register with CQM.", this.worldPosition);
            return false;
        }
    }

    //233/68000

    @Override
    public void setRemoved() {
        QuarryMod.LOGGER.debug("QuarryBlockEntity at {} setRemoved() called. No explicit unregistration from CQM here.", this.worldPosition);
        super.setRemoved();
    }

    private final LazyOptional<SQEnergy> energyCapability = LazyOptional.of(() -> energyStorage);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCapability.cast();
        }
        return super.getCapability(cap, side);
    }


    /* ───────── Configuration Logic ───────── */

    /**
     * Called by marker logic or GUI once a valid region has been identified.
     */
    public void attemptConfigure(BlockPos origin, int xSize, int zSize) {
        if (isRegistered) return; // too late to reconfigure
        this.pendingConfig = new QuarryConfigData(origin.immutable(), xSize, zSize);
        setChanged();
    }



    public boolean isConfigured() {
        return pendingConfig != null;
    }

    public boolean isActive() {
        return isRegistered;
    }

    @Nullable
    public QuarryConfigData getPendingConfig() {
        return pendingConfig;
    }

    /* ───────── Persistence ───────── */

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("isRegistered", isRegistered);
        if (pendingConfig != null) {
            tag.putInt("cfgXSize", pendingConfig.xSize());
            tag.putInt("cfgZSize", pendingConfig.zSize());
            tag.putLong("cfgOrigin", pendingConfig.origin().asLong());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        isRegistered = tag.getBoolean("isRegistered");
        if (tag.contains("cfgOrigin")) {
            BlockPos origin = BlockPos.of(tag.getLong("cfgOrigin"));
            int x = tag.getInt("cfgXSize");
            int z = tag.getInt("cfgZSize");
            pendingConfig = new QuarryConfigData(origin, x, z);
        }
    }



    /* ───────── Config Data Record ───────── */

    public record QuarryConfigData(BlockPos origin, int xSize, int zSize) {}
}
