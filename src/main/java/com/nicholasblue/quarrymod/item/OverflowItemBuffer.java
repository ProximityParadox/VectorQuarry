// --- START OF OverflowItemBuffer.java ---
package com.nicholasblue.quarrymod.item;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;


import java.util.*;

public final class OverflowItemBuffer {

    static final class OverflowEntry {
        final int blockIntId;
        byte count;
        int expiryTick;

        OverflowEntry(int blockIntId, int expiryTick) {
            this.blockIntId = blockIntId;
            this.count = 1;
            this.expiryTick = expiryTick;
        }

        boolean tryIncrementCount() {
            if ((this.count & 0xFF) < 255) {
                this.count++;
                return true;
            }
            return false;
        }
    }

    private final Int2ObjectOpenHashMap<List<OverflowEntry>> bufferMap = new Int2ObjectOpenHashMap<>();
    private static final int DEFAULT_EXPIRY_TICKS = ItemBuffer.DEFAULT_EXPIRY_TICKS; 
    private static final boolean RESET_TIMER_ON_ADD = ItemBuffer.RESET_TIMER_ON_ADD; 

    public OverflowItemBuffer() {}

    public void add(int blockIntId, int currentTick) {
        List<OverflowEntry> entries = bufferMap.get(blockIntId);
        if (entries == null) {
            entries = new ArrayList<>();
            bufferMap.put(blockIntId, entries);
        }

        for (int i = entries.size() - 1; i >= 0; i--) {
            OverflowEntry entry = entries.get(i);
            if (entry.tryIncrementCount()) {
                if (RESET_TIMER_ON_ADD) {
                    entry.expiryTick = currentTick + DEFAULT_EXPIRY_TICKS;
                }
                return; // Incremented existing
            }
        }
        // Add new entry
        OverflowEntry newEntry = new OverflowEntry(blockIntId, currentTick + DEFAULT_EXPIRY_TICKS);
        entries.add(newEntry);
    }

    public void tick(int currentTick) {
        ObjectIterator<Int2ObjectMap.Entry<List<OverflowEntry>>> mapIterator = bufferMap.int2ObjectEntrySet().fastIterator();
        while (mapIterator.hasNext()) {
            Int2ObjectMap.Entry<List<OverflowEntry>> mapEntry = mapIterator.next();
            List<OverflowEntry> entriesList = mapEntry.getValue(); 


            if (entriesList.isEmpty()) {
                mapIterator.remove();
            }
        }
    }

    public void flushAll() {
        // This method DOES expel all items.
        ObjectIterator<Int2ObjectMap.Entry<List<OverflowEntry>>> mapIterator = bufferMap.int2ObjectEntrySet().fastIterator();
        while (mapIterator.hasNext()) {
            Int2ObjectMap.Entry<List<OverflowEntry>> mapEntry = mapIterator.next();
            List<OverflowEntry> entries = mapEntry.getValue();
            for (OverflowEntry entry : entries) {
                expel(entry.blockIntId, entry.count);
            }
        }
        bufferMap.clear();
    }

    private void clearInternal() {
        bufferMap.clear();
    }


    public CompoundTag save() {
        CompoundTag overflowNbt = new CompoundTag();
        for (Int2ObjectMap.Entry<List<OverflowEntry>> mapEntry : bufferMap.int2ObjectEntrySet()) {
            int blockIntId = mapEntry.getIntKey();
            List<OverflowEntry> entriesList = mapEntry.getValue();

            if (entriesList.isEmpty()) continue;

            ListTag entriesNbt = new ListTag();
            for (OverflowEntry entry : entriesList) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putByte("count", entry.count);
                entryTag.putInt("expiry", entry.expiryTick);
                entriesNbt.add(entryTag);
            }
            overflowNbt.put(String.valueOf(blockIntId), entriesNbt);
        }
        return overflowNbt;
    }

    public void load(CompoundTag overflowNbt) {
        clearInternal(); // Use the non-expelling clear

        for (String key : overflowNbt.getAllKeys()) {
            try {
                int blockIntId = Integer.parseInt(key);
                if (!overflowNbt.contains(key, Tag.TAG_LIST)) {
                    System.err.printf("OverflowItemBuffer.load: Key %s is not a ListTag.%n", key);
                    continue;
                }
                ListTag entriesNbt = overflowNbt.getList(key, Tag.TAG_COMPOUND);

                List<OverflowEntry> entriesList = new ArrayList<>();
                for (int i = 0; i < entriesNbt.size(); i++) {
                    CompoundTag entryTag = entriesNbt.getCompound(i);
                    byte count = entryTag.getByte("count");
                    int expiryTick = entryTag.getInt("expiry");

                    OverflowEntry loadedEntry = new OverflowEntry(blockIntId, expiryTick);
                    loadedEntry.count = count;
                    entriesList.add(loadedEntry);
                }

                if (!entriesList.isEmpty()) {
                    bufferMap.put(blockIntId, entriesList);
                }
            } catch (NumberFormatException e) {
                System.err.printf("OverflowItemBuffer.load: Failed to parse blockIntId from key '%s'%n", key);
            }
        }
    }

    public Map<Integer, Integer> getItemSummary() {
        Map<Integer, Integer> summary = new HashMap<>();
        for (Map.Entry<Integer, List<OverflowEntry>> entry : bufferMap.entrySet()) {
            int totalCount = 0;
            for (OverflowEntry oe : entry.getValue()) {
                totalCount += (oe.count & 0xFF);
            }
            summary.put(entry.getKey(), totalCount);
        }
        return summary;
    }

    public boolean isEmpty() {
        return bufferMap.isEmpty();
    }

    public int getTotalBufferedEntries() {
        int totalEntries = 0;
        for (List<OverflowEntry> entries : bufferMap.values()) {
            totalEntries += entries.size();
        }
        return totalEntries;
    }

    public int getTotalItemCount() {
        int totalItems = 0;
        for (List<OverflowEntry> entries : bufferMap.values()) {
            for (OverflowEntry entry : entries) {
                totalItems += (entry.count & 0xFF);
            }
        }
        return totalItems;
    }

    private void expel(int blockIntId, byte count) {

        System.out.printf("OverflowItemBuffer: Expelling blockIntId=%d, count=%d%n", blockIntId, (count & 0xFF));
        // Actual expulsion logic
    }
}
