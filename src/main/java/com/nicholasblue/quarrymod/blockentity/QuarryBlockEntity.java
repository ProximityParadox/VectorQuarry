package com.nicholasblue.quarrymod.blockentity;

import com.nicholasblue.quarrymod.data.QuarryBlockData;
import com.nicholasblue.quarrymod.manager.CentralQuarryManager;
import com.nicholasblue.quarrymod.menu.QuarryMenu;
import com.nicholasblue.quarrymod.registry.ModBlockEntities;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
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

import javax.annotation.Nullable;

/**
 * Non-ticking, spatially-bound block entity that anchors a Quarry block in worldspace.
 *
 * <p>Tracks configuration and activation state. All execution state is delegated to the
 * {@link CentralQuarryManager} once confirmed.</p>
 */
public class QuarryBlockEntity extends BlockEntity implements MenuProvider {

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

    @Override
    public void onLoad() {
        if (level != null && !level.isClientSide && !isRegistered) {
            if (!CONFIG_CODE_EXISTS) {
                // TEMPORARY default behavior: auto-register as 9x9
                QuarryBlockData defaultData = new QuarryBlockData(getBlockPos(), 9, 9);
                CentralQuarryManager.INSTANCE.registerQuarry(defaultData);
                isRegistered = true;
                setChanged();
            }
            // Else wait for config → confirm logic as usual
        }
    }


    @Override
    public void setRemoved() {
        if (!level.isClientSide && isRegistered) {
            CentralQuarryManager.INSTANCE.unregisterQuarry(this.getBlockPos());
        }
        super.setRemoved();
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

    /**
     * Called by GUI or player action to finalize and activate the quarry.
     * Transitions Phase 2 → 3.
     */
    public boolean confirmAndActivate() {
        if (isRegistered || pendingConfig == null || level == null || level.isClientSide) return false;

        QuarryBlockData data = new QuarryBlockData(
                getBlockPos(),
                pendingConfig.xSize(),
                pendingConfig.zSize()
        );

        CentralQuarryManager.INSTANCE.registerQuarry(data);
        isRegistered = true;
        pendingConfig = null;
        setChanged();
        return true;
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

    /* ───────── UI Interface ───────── */

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.quarrymod.quarry_block");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new QuarryMenu(id, inv,
                new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(getBlockPos()));
    }

    /* ───────── Config Data Record ───────── */

    public record QuarryConfigData(BlockPos origin, int xSize, int zSize) {}
}
