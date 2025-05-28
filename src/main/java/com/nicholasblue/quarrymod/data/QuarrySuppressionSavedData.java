package com.nicholasblue.quarrymod.data;

import com.nicholasblue.quarrymod.QuarryMod;
import com.nicholasblue.quarrymod.suppression.GlobalSuppressionIndex;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.BitSet;

public class QuarrySuppressionSavedData extends SavedData {

    // It will fetch the data from GSI directly when Minecraft calls save().

    private static final String DATA_NAME = QuarryMod.MODID + "_suppression_index";

    public QuarrySuppressionSavedData() {
        // QuarryMod.LOGGER.info("QuarrySuppressionSavedData: Instance created/loaded.");
    }

    // The load method is for initializing GSI from disk, not for live updates.
    // So, it will still populate a map to be passed to GSI.
    public static QuarrySuppressionSavedData load(CompoundTag nbt) {
        // QuarryMod.LOGGER.info("QuarrySuppressionSavedData: Static load method called from NBT...");
        Long2ObjectOpenHashMap<GlobalSuppressionIndex.ChunkMask> mapForGSIInitialization = new Long2ObjectOpenHashMap<>();
        ListTag chunkList = nbt.getList("chunks", Tag.TAG_COMPOUND);

        for (Tag rawChunk : chunkList) {
            CompoundTag chunkTag = (CompoundTag) rawChunk;
            long chunkKey = chunkTag.getLong("pos");
            GlobalSuppressionIndex.ChunkMask cm = new GlobalSuppressionIndex.ChunkMask();
            cm.nonEmptySliceCount = chunkTag.getInt("sliceCount");

            if (chunkTag.contains("slices", Tag.TAG_LIST)) {
                ListTag sliceList = chunkTag.getList("slices", Tag.TAG_COMPOUND);
                for (Tag rawSlice : sliceList) {
                    CompoundTag sliceTag = (CompoundTag) rawSlice;
                    int yIndex = sliceTag.getInt("y");
                    if (yIndex >= 0 && yIndex < cm.slices.length) {
                        cm.slices[yIndex] = BitSet.valueOf(sliceTag.getLongArray("bits"));
                    } else {
                        QuarryMod.LOGGER.warn("QuarrySuppressionSavedData: Invalid slice Y-index {} during load. Skipping.", yIndex);
                    }
                }
            }
            mapForGSIInitialization.put(chunkKey, cm);
        }
        // QuarryMod.LOGGER.info("QuarrySuppressionSavedData: Map for GSI initialization created with {} chunks.", mapForGSIInitialization.size());

        // This instance itself doesn't store the map long-term for frequent updates.
        // We return a new instance. The map it constructs is for GSI to consume ONCE on load.
        QuarrySuppressionSavedData loadedData = new QuarrySuppressionSavedData();
        return loadedData; // This instance is mostly a shell; its `save` method does the work.
    }

    // This method is called by Minecraft when it's time to save this SavedData to disk.
    @Override
    public CompoundTag save(CompoundTag nbt) {
        QuarryMod.LOGGER.info("QuarrySuppressionSavedData: save() called by Minecraft. Fetching live data from GSI...");

        // Fetch the MOST RECENT state from GSI at the moment of saving.
        // GSI.deepCopySnapshotForPersistence() is excellent here because it gives a clean,
        // pooled copy suitable for serialization without holding up GSI's live operations... should be anyway
        Long2ObjectMap<GlobalSuppressionIndex.ChunkMask> currentGsiState =
                GlobalSuppressionIndex.INSTANCE.deepCopySnapshotForPersistence();

        ListTag chunkList = new ListTag();
        for (Long2ObjectMap.Entry<GlobalSuppressionIndex.ChunkMask> entry : currentGsiState.long2ObjectEntrySet()) {
            long chunkKey = entry.getLongKey();
            GlobalSuppressionIndex.ChunkMask cm = entry.getValue();
            CompoundTag chunkTag = new CompoundTag();
            chunkTag.putLong("pos", chunkKey);
            chunkTag.putInt("sliceCount", cm.nonEmptySliceCount);

            ListTag sliceList = new ListTag();
            if (cm.slices != null) {
                for (int y = 0; y < cm.slices.length; y++) {
                    BitSet slice = cm.slices[y];
                    if (slice == null || slice.isEmpty()) continue;
                    CompoundTag sliceTag = new CompoundTag();
                    sliceTag.putInt("y", y);
                    sliceTag.putLongArray("bits", slice.toLongArray());
                    sliceList.add(sliceTag);
                }
            }

            if (!sliceList.isEmpty() || cm.nonEmptySliceCount > 0) {
                if (!sliceList.isEmpty()) {
                    chunkTag.put("slices", sliceList);
                }
                chunkList.add(chunkTag);
            }
        }
        nbt.put("chunks", chunkList);
        QuarryMod.LOGGER.info("QuarrySuppressionSavedData: Data serialized from GSI ({} chunks).", currentGsiState.size());
        return nbt;
    }

    // Static getter method - needs to handle initial GSI load
    public static QuarrySuppressionSavedData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(
                (nbt) -> QuarrySuppressionSavedData.loadAndInitializeGSI(nbt, level), // For loading existing data
                () -> QuarrySuppressionSavedData.createNewAndInitializeGSI(level),     // For creating new data
                DATA_NAME
        );
    }

    // Helper for loading existing data and initializing GSI
    private static QuarrySuppressionSavedData loadAndInitializeGSI(CompoundTag nbt, ServerLevel level) {
        QuarryMod.LOGGER.info("QuarrySuppressionSavedData: Loading existing data for GSI in level {}...", level.dimension().location());
        Long2ObjectOpenHashMap<GlobalSuppressionIndex.ChunkMask> mapForGSI = new Long2ObjectOpenHashMap<>();
        ListTag chunkList = nbt.getList("chunks", Tag.TAG_COMPOUND);

        for (Tag rawChunk : chunkList) {
            CompoundTag chunkTag = (CompoundTag) rawChunk;
            long chunkKey = chunkTag.getLong("pos");
            GlobalSuppressionIndex.ChunkMask cm = new GlobalSuppressionIndex.ChunkMask();
            cm.nonEmptySliceCount = chunkTag.getInt("sliceCount");

            if (chunkTag.contains("slices", Tag.TAG_LIST)) {
                ListTag sliceList = chunkTag.getList("slices", Tag.TAG_COMPOUND);
                for (Tag rawSlice : sliceList) {
                    CompoundTag sliceTag = (CompoundTag) rawSlice;
                    int yIndex = sliceTag.getInt("y");
                    if (yIndex >= 0 && yIndex < cm.slices.length) {
                        cm.slices[yIndex] = BitSet.valueOf(sliceTag.getLongArray("bits"));
                    }
                }
            }
            mapForGSI.put(chunkKey, cm);
        }

        // Initialize GSI with this loaded map
        GlobalSuppressionIndex.INSTANCE.loadSnapshotFromPersistence(mapForGSI);
        QuarryMod.LOGGER.info("GSI initialized from loaded SavedData ({} chunks) for level {}.", mapForGSI.size(), level.dimension().location());
        return new QuarrySuppressionSavedData(); // Return a new "shell" instance
    }

    // Helper for creating new (empty) data and initializing GSI (if needed, or GSI starts empty)
    private static QuarrySuppressionSavedData createNewAndInitializeGSI(ServerLevel level) {
        QuarryMod.LOGGER.info("QuarrySuppressionSavedData: Creating new (empty) data for GSI in level {}...", level.dimension().location());
        GlobalSuppressionIndex.INSTANCE.loadSnapshotFromPersistence(new Long2ObjectOpenHashMap<>()); // Ensure GSI is empty
        QuarryMod.LOGGER.info("GSI initialized as empty for new SavedData for level {}.", level.dimension().location());
        QuarrySuppressionSavedData newData = new QuarrySuppressionSavedData();
        newData.setDirty(); // Mark new data as dirty so an empty file is created on first save
        return newData;
    }
}