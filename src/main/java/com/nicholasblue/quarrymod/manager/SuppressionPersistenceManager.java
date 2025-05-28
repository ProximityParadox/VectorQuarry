package com.nicholasblue.quarrymod.manager;

import com.nicholasblue.quarrymod.QuarryMod; // Import the main mod class to access its logger
import com.nicholasblue.quarrymod.data.SuppressionIndexData;
import com.nicholasblue.quarrymod.suppression.GlobalSuppressionIndex;
import com.nicholasblue.quarrymod.util.SerializationUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SuppressionPersistenceManager {

    private static final String FILE_NAME = "quarrymod_suppression.dat";

    //todo: might be obsolete, but i'm just gonna upload it to github anyway just to get it off my hard drive


    public static void saveToFile(ServerLevel level) {
        QuarryMod.LOGGER.info("Attempting to save suppression data...");
        try {
            // Use the GSI's dedicated deep copy method for persistence.
            // This ensures all BitSets in the 'mapToPersist' are fresh copies
            // from the ISP's snapshot pool. SerializationUtils then reads their bits.
            var mapToPersist = GlobalSuppressionIndex.INSTANCE.deepCopySnapshotForPersistence();
            SuppressionIndexData sid = new SuppressionIndexData(mapToPersist);

            // SerializationUtils itself will convert this map of ChunkMasks (with pooled BitSets)
            // into NBT, where BitSet.toLongArray() just extracts the bit data.
            CompoundTag tag = SerializationUtils.serializeSuppressionIndexData(sid);

            Path path = getFilePath(level);
            Files.createDirectories(path.getParent());
            NbtIo.writeCompressed(tag, path.toFile());
            QuarryMod.LOGGER.info("Successfully saved suppression data to {}", path);
        } catch (IOException e) {
            QuarryMod.LOGGER.error("Failed to save suppression data", e);
            // Rethrow as runtime exception as this is a critical failure that should crash server for debugging
            throw new RuntimeException("Failed to save suppression data", e);
        } catch (Exception e) {
            // Catch any other unexpected exceptions during the save process
            QuarryMod.LOGGER.error("An unexpected error occurred during suppression data save", e);
            throw new RuntimeException("An unexpected error occurred during suppression data save", e);
        }
    }

    public static void loadFromFile(ServerLevel level) {
        Path path = getFilePath(level);
        if (!Files.exists(path)) {
            QuarryMod.LOGGER.info("No existing suppression data file found at {}. Skipping load.", path);
            return;
        }

        QuarryMod.LOGGER.info("Attempting to load suppression data from {}...", path);
        try {
            CompoundTag tag = NbtIo.readCompressed(path.toFile());

            SuppressionIndexData sid = SerializationUtils.deserializeSuppressionIndexData(tag);

            GlobalSuppressionIndex.INSTANCE.loadSnapshotFromPersistence(sid.chunkMap);
            QuarryMod.LOGGER.info("Successfully loaded suppression data.");
        } catch (IOException e) {
            QuarryMod.LOGGER.error("Failed to load suppression data", e);
            // Rethrow as runtime exception as this is a critical failure that should crash server for debugging
            throw new RuntimeException("Failed to load suppression data", e);
        } catch (Exception e) {
            // Catch any other unexpected exceptions during the load process
            QuarryMod.LOGGER.error("An unexpected error occurred during suppression data load", e);
            throw new RuntimeException("An unexpected error occurred during suppression data load", e);
        }
    }

    private static Path getFilePath(ServerLevel level) {
        return level.getServer()
                .getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve(FILE_NAME);
    }
}