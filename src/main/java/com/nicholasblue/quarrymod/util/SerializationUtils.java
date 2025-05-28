package com.nicholasblue.quarrymod.util;

import com.nicholasblue.quarrymod.data.SuppressionIndexData;
import com.nicholasblue.quarrymod.suppression.GlobalSuppressionIndex;
import com.nicholasblue.quarrymod.suppression.SuppressionSnapshotManager.SuppressionSnapshot;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.BitSet;

/**
 * Centralized serialization logic for suppression snapshot and persistent index data.
 * Converts between in-memory data structures and NBT-compatible representations.
 */
public final class SerializationUtils {

    private SerializationUtils() {}

    //todo: might also be obsolete - check later

    /* ───────────────────────── SuppressionSnapshot ↔ CompoundTag ───────────────────────── */

    public static CompoundTag serializeSuppressionSnapshot(SuppressionSnapshot snapshot) {
        CompoundTag root = new CompoundTag();
        ListTag chunkList = new ListTag();

        for (Long2ObjectMap.Entry<GlobalSuppressionIndex.ChunkMask> entry : snapshot.map().long2ObjectEntrySet()) {
            long chunkKey = entry.getLongKey();
            GlobalSuppressionIndex.ChunkMask cm = entry.getValue();

            CompoundTag chunkTag = new CompoundTag();
            chunkTag.putLong("pos", chunkKey);
            chunkTag.putInt("sliceCount", cm.nonEmptySliceCount);

            ListTag sliceList = new ListTag();
            for (int y = 0; y < cm.slices.length; y++) {
                BitSet slice = cm.slices[y];
                if (slice == null || slice.isEmpty()) continue;

                CompoundTag sliceTag = new CompoundTag();
                sliceTag.putInt("y", y);
                sliceTag.putLongArray("bits", slice.toLongArray());
                sliceList.add(sliceTag);
            }

            if (!sliceList.isEmpty()) {
                chunkTag.put("slices", sliceList);
                chunkList.add(chunkTag);
            }
        }

        root.put("chunks", chunkList);
        return root;
    }

    public static SuppressionSnapshot deserializeSuppressionSnapshot(CompoundTag tag) {
        ListTag chunkList = tag.getList("chunks", Tag.TAG_COMPOUND);
        Long2ObjectOpenHashMap<GlobalSuppressionIndex.ChunkMask> map = new Long2ObjectOpenHashMap<>();

        for (Tag rawChunk : chunkList) {
            CompoundTag chunkTag = (CompoundTag) rawChunk;
            long chunkKey = chunkTag.getLong("pos");

            GlobalSuppressionIndex.ChunkMask cm = new GlobalSuppressionIndex.ChunkMask();
            cm.nonEmptySliceCount = chunkTag.getInt("sliceCount"); // <-- LOAD THE COUNT
            ListTag sliceList = chunkTag.getList("slices", Tag.TAG_COMPOUND);

            for (Tag rawSlice : sliceList) {
                CompoundTag sliceTag = (CompoundTag) rawSlice;
                int y = sliceTag.getInt("y");
                BitSet bs = BitSet.valueOf(sliceTag.getLongArray("bits"));
                cm.slices[y] = bs;
            }

            map.put(chunkKey, cm);
        }

        return new SuppressionSnapshot(map);
    }

    /* ───────────────────────── SuppressionIndexData ↔ CompoundTag ───────────────────────── */

    public static CompoundTag serializeSuppressionIndexData(SuppressionIndexData sid) {
        return serializeSuppressionSnapshot(new SuppressionSnapshot(sid.chunkMap));
    }

    public static SuppressionIndexData deserializeSuppressionIndexData(CompoundTag tag) {
        SuppressionSnapshot snap = deserializeSuppressionSnapshot(tag);
        return new SuppressionIndexData(snap.map());
    }
}
