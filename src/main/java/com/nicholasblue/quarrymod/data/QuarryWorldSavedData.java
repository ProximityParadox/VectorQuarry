package com.nicholasblue.quarrymod.data;
import com.nicholasblue.quarrymod.QuarryMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.nbt.Tag; // Import for NBT type constants
public class QuarryWorldSavedData extends SavedData {

    private static final String DATA_NAME = QuarryMod.MODID + "_QuarryStates"; // Consistent naming

    private ListTag quarryConfigsNBT = new ListTag();
    private CompoundTag quarryRuntimeStatesNBT = new CompoundTag();

    public QuarryWorldSavedData() {
        super();
        // QuarryMod.LOGGER.debug("QuarryWorldSavedData: New instance created (default constructor).");
    }

    // Constructor used by computeIfAbsent when loading from NBT
    public static QuarryWorldSavedData load(CompoundTag tag) {
        QuarryWorldSavedData data = new QuarryWorldSavedData();
        if (tag.contains("QuarryConfigs", Tag.TAG_LIST)) {
            data.quarryConfigsNBT = tag.getList("QuarryConfigs", Tag.TAG_COMPOUND);
        }
        if (tag.contains("QuarryRuntimeStates", Tag.TAG_COMPOUND)) {
            data.quarryRuntimeStatesNBT = tag.getCompound("QuarryRuntimeStates");
        }
        // QuarryMod.LOGGER.debug("QuarryWorldSavedData.load: Loaded {} configs, {} runtime states.", data.quarryConfigsNBT.size(), data.quarryRuntimeStatesNBT.size());
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag) {
        compoundTag.put("QuarryConfigs", this.quarryConfigsNBT);
        compoundTag.put("QuarryRuntimeStates", this.quarryRuntimeStatesNBT);
        // QuarryMod.LOGGER.debug("QuarryWorldSavedData.save: Saving {} configs, {} runtime states.", this.quarryConfigsNBT.size(), this.quarryRuntimeStatesNBT.size());
        return compoundTag;
    }

    public static QuarryWorldSavedData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(QuarryWorldSavedData::load, QuarryWorldSavedData::new, DATA_NAME);
    }

    public ListTag getQuarryConfigsNBT() {
        return quarryConfigsNBT;
    }

    public void setQuarryConfigsNBT(ListTag quarryConfigsNBT) {
        this.quarryConfigsNBT = quarryConfigsNBT;
        // QuarryMod.LOGGER.debug("QuarryWorldSavedData.setQuarryConfigsNBT: Set {} configs. Marking dirty.", quarryConfigsNBT.size());
        setDirty(true);
    }

    public CompoundTag getQuarryRuntimeStatesNBT() {
        return quarryRuntimeStatesNBT;
    }

    public void setQuarryRuntimeStatesNBT(CompoundTag quarryRuntimeStatesNBT) {
        this.quarryRuntimeStatesNBT = quarryRuntimeStatesNBT;
        // QuarryMod.LOGGER.debug("QuarryWorldSavedData.setQuarryRuntimeStatesNBT: Set {} runtime states. Marking dirty.", quarryRuntimeStatesNBT.size());
        setDirty(true);
    }
}