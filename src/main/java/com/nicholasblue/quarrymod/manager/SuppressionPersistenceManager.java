package com.nicholasblue.quarrymod.manager;

import com.nicholasblue.quarrymod.data.SuppressionIndexData;

import com.nicholasblue.quarrymod.suppression.GlobalSuppressionIndex;
import com.nicholasblue.quarrymod.suppression.SuppressionSnapshotManager;
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

    private static final SuppressionSnapshotManager snapshotManager =
            new SuppressionSnapshotManager(GlobalSuppressionIndex.INSTANCE);

    public static void saveToFile(ServerLevel level) {
        SuppressionSnapshotManager.SuppressionSnapshot snapshot = snapshotManager.getSnapshot();
        SuppressionIndexData sid = new SuppressionIndexData(snapshot.map());
        CompoundTag tag = SerializationUtils.serializeSuppressionIndexData(sid);

        Path path = getFilePath(level);
        try {
            Files.createDirectories(path.getParent());
            NbtIo.writeCompressed(tag, path.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save suppression data", e);
        }
    }

    public static void loadFromFile(ServerLevel level) {
        Path path = getFilePath(level);
        if (!Files.exists(path)) return;

        try {
            CompoundTag tag = NbtIo.readCompressed(path.toFile());
            SuppressionIndexData sid = SerializationUtils.deserializeSuppressionIndexData(tag);
            snapshotManager.loadSnapshot(new SuppressionSnapshotManager.SuppressionSnapshot(sid.chunkMap));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load suppression data", e);
        }
    }

    private static Path getFilePath(ServerLevel level) {
        return level.getServer()
                .getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve(FILE_NAME);
    }
}
