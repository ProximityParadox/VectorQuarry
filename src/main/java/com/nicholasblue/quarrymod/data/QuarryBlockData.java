package com.nicholasblue.quarrymod.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * Immutable snapshot of one quarry’s configuration + runtime cursor.
 *
 * <p>All fields are public and final to keep the record mechanically simple,
 * encourage value‑semantics, and minimise accessor overhead.  Mutations are
 * performed by constructing a new instance (copy‑on‑write) and replacing the
 * reference inside {@code CentralQuarryManager.stateRef} via CAS.</p>
 */
public final class QuarryBlockData {

    /* ───────── configuration (set once on registration) ───────── */
    public final BlockPos quarryPos;   // location of the quarry block itself
    public final BlockPos origin;      // (quarryPos + 1,0,1) – same semantics as before
    public final int xSize;
    public final int zSize;
    public final int startY;           // first Y layer to mine (origin.y‑1)


    /* ───────── constructors ───────── */

    /** Initial snapshot produced at registration time. */
    public QuarryBlockData(BlockPos quarryPos, int xSize, int zSize) {
        this.quarryPos  = quarryPos.immutable();
        this.origin     = quarryPos.offset(1, 0, 1).immutable();
        this.xSize      = xSize;
        this.zSize      = zSize;
        this.startY     = origin.getY() - 1;

    }

    /** Private full constructor used by the copy‑builder. */
    private QuarryBlockData(BlockPos quarryPos, BlockPos origin,
                            int xSize, int zSize, int startY) {

        this.quarryPos       = quarryPos;
        this.origin          = origin;
        this.xSize           = xSize;
        this.zSize           = zSize;
        this.startY          = startY;

    }


    /* ───────── NBT (de)serialisation helpers for SavedData ────── */

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("quarryPos", quarryPos.asLong());
        tag.putInt("xSize", xSize);
        tag.putInt("zSize", zSize);

        tag.putInt("startY", startY);
        return tag;
    }

    public static QuarryBlockData load(CompoundTag tag) {
        BlockPos qPos = BlockPos.of(tag.getLong("quarryPos"));
        int x = tag.getInt("xSize");
        int z = tag.getInt("zSize");

        int startY  = tag.getInt("startY");

        BlockPos origin = qPos.offset(1, 0, 1).immutable();
        return new QuarryBlockData(
                qPos.immutable(), origin,
                x, z, startY);
    }
}
