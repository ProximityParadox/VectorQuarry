// Create this file, e.g., in com.nicholasblue.quarrymod.manager
package com.nicholasblue.quarrymod.manager;

import com.nicholasblue.quarrymod.QuarryMod;
import com.nicholasblue.quarrymod.data.QuarryBlockData;
import com.nicholasblue.quarrymod.data.QuarryRuntimeState;
import com.nicholasblue.quarrymod.data.QuarryWorldSavedData;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;

public class QuarryStatePersistenceManager {

    //todo: ensure that state is safely reconstructed from crash.

    public static void saveQuarryStates(ServerLevel level, CentralQuarryManager cqm) {
        if (level.isClientSide()) return;

        QuarryWorldSavedData savedData = QuarryWorldSavedData.get(level);

        // 1. Save QuarryRegistry (QuarryBlockData instances)
        Long2ObjectOpenHashMap<QuarryBlockData> configSnapshot = cqm.getRegistry().snapshot();
        ListTag registryListNBT = new ListTag();
        for (QuarryBlockData data : configSnapshot.values()) {
            registryListNBT.add(data.save());
        }
        savedData.setQuarryConfigsNBT(registryListNBT);

        // 2. Save QuarryRuntimeStates
        Long2ObjectOpenHashMap<QuarryRuntimeState> runtimeSnapshot = cqm.getRuntimeStates();
        CompoundTag runtimeStatesMapNBT = new CompoundTag();
        for (Long2ObjectOpenHashMap.Entry<QuarryRuntimeState> entry : runtimeSnapshot.long2ObjectEntrySet()) {
            long quarryKey = entry.getLongKey();
            QuarryRuntimeState runtimeState = entry.getValue();
            runtimeStatesMapNBT.put(String.valueOf(quarryKey), runtimeState.save());
        }
        savedData.setQuarryRuntimeStatesNBT(runtimeStatesMapNBT);

        QuarryMod.LOGGER.info("Saving Quarry states for dimension: {}. {} configs, {} runtimes.",
                level.dimension().location(), configSnapshot.size(), runtimeSnapshot.size());
    }

    public static void loadQuarryStates(ServerLevel level, CentralQuarryManager cqm) {
        if (level.isClientSide()) return;

        QuarryWorldSavedData savedData = QuarryWorldSavedData.get(level);

        // 1. Load and restore QuarryRegistry
        ListTag registryListNBT = savedData.getQuarryConfigsNBT();
        Long2ObjectOpenHashMap<QuarryBlockData> restoredConfigs = new Long2ObjectOpenHashMap<>();
        for (int i = 0; i < registryListNBT.size(); i++) {
            CompoundTag dataTag = registryListNBT.getCompound(i);
            QuarryBlockData data = QuarryBlockData.load(dataTag);
            if (data != null) {
                restoredConfigs.put(data.quarryPos.asLong(), data);
            }
        }
        cqm.getRegistry().restore(restoredConfigs);

        // 2. Load and restore QuarryRuntimeStates
        CompoundTag runtimeStatesMapNBT = savedData.getQuarryRuntimeStatesNBT();
        Long2ObjectOpenHashMap<QuarryRuntimeState> restoredRuntimes = new Long2ObjectOpenHashMap<>();
        for (String keyStr : runtimeStatesMapNBT.getAllKeys()) {
            try {
                long quarryKey = Long.parseLong(keyStr);
                CompoundTag stateTag = runtimeStatesMapNBT.getCompound(keyStr);
                QuarryRuntimeState runtimeState = QuarryRuntimeState.load(stateTag);

                if (runtimeState != null) {
                    // IMPORTANT: Only load runtime state if its corresponding config was also loaded.
                    // This prevents orphaned runtime states if a config was somehow lost or corrupted.
                    if (restoredConfigs.containsKey(quarryKey)) {
                        restoredRuntimes.put(quarryKey, runtimeState);
                    } else {
                        QuarryMod.LOGGER.warn("Found orphaned runtime state for quarry key {} during load. Discarding.", quarryKey);
                    }
                }
            } catch (NumberFormatException e) {
                QuarryMod.LOGGER.error("Failed to parse quarry key from NBT: {} during runtime state loading.", keyStr, e);
            }
        }
        cqm.restoreRuntimeStates(restoredRuntimes);

        QuarryMod.LOGGER.info("Loaded Quarry states for dimension: {}. {} configs, {} runtimes (matched to configs).",
                level.dimension().location(), restoredConfigs.size(), restoredRuntimes.size());
    }
}