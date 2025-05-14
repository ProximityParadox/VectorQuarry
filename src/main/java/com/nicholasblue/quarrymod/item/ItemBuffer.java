package com.nicholasblue.quarrymod.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * High-performance buffer for batching mined item outputs with full fidelity
 * (itemId, count, expiryTick), using pooled, struct-of-arrays slabs.
 */
public final class ItemBuffer {

    private static final int SLAB_CAPACITY = 256;
    static final int DEFAULT_EXPIRY_TICKS = 20;
    static final boolean RESET_TIMER_ON_ADD = false;

    // ────────── Internal Slab Definition ──────────

    static final class Slab {
        final short[] itemIds = new short[SLAB_CAPACITY];
        final byte[] counts = new byte[SLAB_CAPACITY];
        final int[] expiryTicks = new int[SLAB_CAPACITY];
        int size = 0;

        private final Map<Short, Integer> indexMap = new HashMap<>();

        boolean isFull() {
            return size >= SLAB_CAPACITY;
        }

        // Called by ItemBuffer.add when adding a NEW entry
        void addEntry(short itemId, int expiryTick) {
            // This assumes the map check was already done and no existing entry was found

            //System.out.println("added new entry");
            if (size >= SLAB_CAPACITY) {
                // Should not happen if called correctly after isFull() check in ItemBuffer
                throw new IllegalStateException("Attempted to add entry to full slab");
            }

            itemIds[size] = itemId;
            counts[size] = 1;
            expiryTicks[size] = expiryTick;

            // Add to map AFTER checking isFull and setting values, BEFORE incrementing size
            indexMap.put(itemId, size);

            size++;
        }

        // Called by ItemBuffer.tick or flushAll when removing an entry
        void removeAt(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("Index " + index + " out of bounds for size " + size);
            }

            // Remove from map BEFORE swapping or decrementing size
            short itemIdToRemove = itemIds[index];
            indexMap.remove(itemIdToRemove);

            int last = --size; // Decrement size first as it's the index of the last element

            // If the element being removed was not the last one
            if (index != last) {
                // Move the last element to the removed position
                itemIds[index] = itemIds[last];
                counts[index] = counts[last];
                expiryTicks[index] = expiryTicks[last];

                // Update the map for the element that was MOVED
                short movedItemId = itemIds[index];
                indexMap.put(movedItemId, index); // Update the index for the item that was moved
            }
            // If index == last, the removed element was the last, no swap needed.
            // size is already decremented, and map entry was removed.
        }

        boolean isEmpty() {
            return size == 0;
        }

        // Added method to clear the map when releasing the slab
        void reset() {
            size = 0;
            indexMap.clear(); // Explicitly clear the map
        }
    }

    // ────────── Main Buffer State ──────────

    private final List<Slab> slabs = new ArrayList<>();
    private Slab activeSlab;

    public ItemBuffer() {
        this.activeSlab = SlabPool.acquire();
        slabs.add(activeSlab);
    }

    /**
     * Inserts a new item or increments an existing one.
     * Uses a map lookup for fast checking within the active slab.
     */
    public void add(short itemId, int currentTick) {
        // --- Check if item already exists in the active slab using the map ---
        Integer existingIndex = activeSlab.indexMap.get(itemId);

        if (existingIndex != null) {
            // Item found in active slab, increment count
            int index = existingIndex; // Auto-unboxing

            // Check if count is already maxed (255)
            if ((activeSlab.counts[index] & 0xFF) < 255) {
                activeSlab.counts[index]++;
                if (RESET_TIMER_ON_ADD) {
                    activeSlab.expiryTicks[index] = currentTick + DEFAULT_EXPIRY_TICKS;
                }
                return;
            } else {
                // Entry has saturated — remove from indexMap to allow future re-adding
                activeSlab.indexMap.remove(itemId);
            }

        }

        // --- Item does NOT exist in the active slab ---

        // Check if active slab is full
        if (activeSlab.isFull()) {
            // Active slab is full, acquire a new one
            activeSlab = SlabPool.acquire();
            slabs.add(activeSlab); // Add the new slab to the list of slabs
        }

        // Add the new item entry to the active slab
        activeSlab.addEntry(itemId, currentTick + DEFAULT_EXPIRY_TICKS);
    }

    /**
     * Flushes expired or saturated entries and returns empty slabs to the pool.
     */
    public void tick(int currentTick) {
        // Iterate slabs using traditional for loop to handle removals
        for (int s = 0; s < slabs.size(); s++) {
            Slab slab = slabs.get(s);
            int i = 0; // Index for iterating within the slab

            // Iterate entries within the slab using while loop to handle removals
            while (i < slab.size) {
                boolean expired = currentTick >= slab.expiryTicks[i];
                boolean maxed = (slab.counts[i] & 0xFF) == 255;

                //if (expired || maxed) {
                    // Entry needs to be expelled
                    //expel(slab.itemIds[i], slab.counts[i]);

                    // Remove the entry from the slab. removeAt updates arrays and map.
                    //slab.removeAt(i);
                //} else {
                    // Entry is not expired and not maxed, move to the next entry
                    i++;
                //}
            }

            // After processing all entries in the slab, check if the slab is empty
            if (slab.isEmpty()) {
                // Avoid deallocating/pooling the active slab even if it's empty.
                // It will be reused for new additions.
                if (slab != activeSlab) {
                    slabs.remove(s); // Remove from the list of slabs
                    SlabPool.release(slab); // Release to the pool (clears the map inside release)
                    s--; // Decrement slab index because the next slab is now at the current index 's'
                }
            }
        }
    }

    /**
     * Immediately flushes all items and resets the buffer.
     */
    public void flushAll() {
        // Iterate slabs using traditional for loop to handle removals (implicitly, by clearing)
        for (int s = 0; s < slabs.size(); s++) {
            Slab slab = slabs.get(s);
            // Expel all items currently in this slab
            for (int i = 0; i < slab.size; i++) {
                expel(slab.itemIds[i], slab.counts[i]);
            }
            // Reset the slab (clears size and map)
            slab.reset(); // Use the new reset method

            // Pool the slab if it's not the active one
            if (slab != activeSlab) {
                // No need to remove from slabs list here immediately if we clear the whole list next.
                // But releasing them one by one is also fine.
                SlabPool.release(slab); // Release to the pool (clears map)
            }
        }
        slabs.clear(); // Clear the list of slabs

        activeSlab = SlabPool.acquire();
        slabs.add(activeSlab);
    }

    public Map<Short, Integer> getItemSummary() {
        Map<Short, Integer> summary = new HashMap<>();
        for (Slab slab : slabs) {
            for (int i = 0; i < slab.size; i++) {
                short id = slab.itemIds[i];
                int count = slab.counts[i] & 0xFF; // Unsigned byte

                //System.out.println("itemid: " + id + " count: " + count);

                summary.merge(id, count, Integer::sum);
            }
        }

        return summary;
    }


    public boolean isEmpty() {

        if (slabs.size() > 1) {
            return false; // More than just the active slab means items are buffered
        }

        return slabs.size() == 1 && activeSlab.size == 0; // Reverting to original faster check
    }
    public int getTotalBufferedEntries() {
        int total = 0;
        for (Slab slab : slabs) {
            total += slab.size;
        }
        return total;
    }

    private void expel(short itemId, byte count) {
        // Replace with actual drop, transfer, or item routing logic
        System.out.printf("Expelling itemId=%d, count=%d%n", itemId, (count & 0xFF));
    }
}
