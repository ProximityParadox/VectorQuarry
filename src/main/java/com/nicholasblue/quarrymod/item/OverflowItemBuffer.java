package com.nicholasblue.quarrymod.item;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

import com.nicholasblue.quarrymod.data.BlockIndexer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ItemStack; // Needed in expel for creating item stacks


public final class OverflowItemBuffer {

    // ────────── Internal Entry Definition for Overflow ──────────
    // Represents a single stack/entry of an item *without NBT* in the overflow buffer
    static final class OverflowEntry {
        final int blockIntId; // The Block ID
        byte count; // Max 255 per entry
        int expiryTick;

        OverflowEntry(int blockIntId, int expiryTick) {
            this.blockIntId = blockIntId;
            this.count = 1; // Starts with 1
            this.expiryTick = expiryTick;
        }

        // Method to increment count, handles max limit
        boolean tryIncrementCount() {
            if ((this.count & 0xFF) < 255) { // Use & 0xFF for unsigned comparison
                this.count++;
                return true; // Increment successful
            }
            return false; // Count is already maxed
        }
    }

    // ────────── Main Buffer State ──────────

    // Map from int Block ID to a list of entries for that block ID
    // This stores items by ID only, for blocks *without relevant NBT*.
    private final Int2ObjectOpenHashMap<List<OverflowEntry>> bufferMap = new Int2ObjectOpenHashMap<>();

    private static final int DEFAULT_EXPIRY_TICKS = ItemBuffer.DEFAULT_EXPIRY_TICKS;
    private static final boolean RESET_TIMER_ON_ADD = ItemBuffer.RESET_TIMER_ON_ADD;


    public OverflowItemBuffer() {
        // FastUtil map initialized
    }

    /**
     * Adds an item with an int ID to the overflow buffer or increments an existing entry.
     * Assumes the Block has already been classified as requiring an int ID and does NOT
     * require storing NBT for stacking.
     * Attempts to increment an existing entry in the list first. If all existing entries
     * are full (count 255), or if no entry exists for this ID, a new entry is created.
     *
     * @param blockIntId The int ID of the block.
     * @param currentTick The current game tick.
     */
    public void add(int blockIntId, int currentTick) {
        List<OverflowEntry> entries = bufferMap.get(blockIntId);

        if (entries == null) {
            // No entries yet for this block ID, create a new list and the first entry
            entries = new ArrayList<>();
            bufferMap.put(blockIntId, entries);
        }

        // Attempt to find an existing entry that can be incremented
        boolean incrementedExisting = false;
        // Iterate through existing entries (backwards is often slightly faster for lists modified at end)
        for (int i = entries.size() - 1; i >= 0; i--) {
            OverflowEntry entry = entries.get(i);
            // No NBT comparison needed here, as NBT items go to the tertiary buffer
            if (entry.tryIncrementCount()) {
                // Successfully incremented an existing entry
                if (RESET_TIMER_ON_ADD) {
                    entry.expiryTick = currentTick + DEFAULT_EXPIRY_TICKS;
                }
                incrementedExisting = true;
                break; // Found and incremented, done with this add
            }
        }

        if (!incrementedExisting) {
            // No existing entry could be incremented (either none existed or all were full)
            // Create a new entry
            OverflowEntry newEntry = new OverflowEntry(blockIntId, currentTick + DEFAULT_EXPIRY_TICKS);
            entries.add(newEntry);
        }
    }

    /**
     * Ticks the buffer, expelling expired or saturated entries.
     *
     * @param currentTick The current game tick.
     */
    public void tick(int currentTick) {
        // Iterate over the entries in the buffer map
        // Use an iterator over the values (the lists of entries) to safely remove lists if they become empty
        Iterator<List<OverflowEntry>> listIterator = bufferMap.values().iterator();

        while (listIterator.hasNext()) {
            List<OverflowEntry> entries = listIterator.next();

            // Iterate over the entries within the list using an iterator to safely remove expired/full entries
            Iterator<OverflowEntry> entryIterator = entries.iterator();
            while (entryIterator.hasNext()) {
                OverflowEntry entry = entryIterator.next();

                boolean expired = currentTick >= entry.expiryTick;
                boolean maxed = (entry.count & 0xFF) == 255; // Use & 0xFF for unsigned comparison

                if (expired || maxed) {
                    // Entry needs to be expelled
                    expel(entry.blockIntId, entry.count); // No NBT to pass
                    entryIterator.remove(); // Safely remove the entry from the list
                }
            }

            // After processing entries in this list, check if the list is now empty
            if (entries.isEmpty()) {
                listIterator.remove(); // Safely remove the list from the bufferMap
            }
        }
    }

    /**
     * Immediately flushes all items and resets the buffer.
     */
    public void flushAll() {
        // Iterate over all lists and all entries to expel them
        Iterator<List<OverflowEntry>> listIterator = bufferMap.values().iterator();
        while (listIterator.hasNext()) {
            List<OverflowEntry> entries = listIterator.next();
            Iterator<OverflowEntry> entryIterator = entries.iterator();
            while (entryIterator.hasNext()) {
                OverflowEntry entry = entryIterator.next();
                expel(entry.blockIntId, entry.count); // Expel the entry
                entryIterator.remove(); // Remove entry from list
            }
            // No need to remove the list from bufferMap here, clear() will handle it
        }

        // Clear the map, removing all lists
        bufferMap.clear();
    }

    public Map<Integer, Integer> getItemSummary() {
        Map<Integer, Integer> summary = new HashMap<>();
        for (Map.Entry<Integer, List<OverflowEntry>> entry : bufferMap.entrySet()) {
            int id = entry.getKey();
            List<OverflowEntry> entries = entry.getValue();

            int totalCount = 0;
            for (OverflowEntry oe : entries) {
                totalCount += (oe.count & 0xFF); // Unsigned byte
            }

            summary.put(id, totalCount);
        }
        return summary;
    }


    /**
     * Checks if the buffer is empty.
     *
     * @return true if the buffer contains no items, false otherwise.
     */
    public boolean isEmpty() {
        return bufferMap.isEmpty();
    }

    /**
     * Gets the total number of *entries* (stacks) in the buffer.
     */
    public int getTotalBufferedEntries() {
        int totalEntries = 0;
        for (List<OverflowEntry> entries : bufferMap.values()) {
            totalEntries += entries.size();
        }
        return totalEntries;
    }

    /**
     * Gets the total count of all items across all entries in the buffer.
     */
    public int getTotalItemCount() {
        int totalItems = 0;
        for (List<OverflowEntry> entries : bufferMap.values()) {
            for (OverflowEntry entry : entries) {
                totalItems += (entry.count & 0xFF); // Use & 0xFF to treat byte as unsigned
            }
        }
        return totalItems;
    }


    // This method needs to be implemented or linked to your actual item expulsion logic
    private void expel(int blockIntId, byte count) {
        // This is where you use the BlockIndexer to get the Block object from the int ID
        Optional<Block> blockOpt = BlockIndexer.tryGetBlockFromIntId(blockIntId);

        if (blockOpt.isPresent()) {
            Block block = blockOpt.get();
            int itemCount = (count & 0xFF); // Ensure count is treated as unsigned

            // --- Your actual item handling logic goes here ---


        } else {
            // Error: Buffered ID doesn't map to a block (should not happen if IDs come from indexer. If it does, we are, in fact, cooked)
            System.err.printf("Overflow Buffer Error: Buffered int ID %d does not map to a block! Count: %d. Cannot expel.%n",
                    blockIntId, (count & 0xFF));
        }
    }
}