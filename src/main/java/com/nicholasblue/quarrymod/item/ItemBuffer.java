package com.nicholasblue.quarrymod.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        void addEntry(short itemId, int expiryTick) {
            if (size >= SLAB_CAPACITY) {
                throw new IllegalStateException("Attempted to add entry to full slab");
            }
            itemIds[size] = itemId;
            counts[size] = 1; // New entries start with count 1
            expiryTicks[size] = expiryTick;
            indexMap.put(itemId, size);
            size++;
        }

        void removeAt(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("Index " + index + " out of bounds for size " + size);
            }
            short itemIdToRemove = itemIds[index];
            indexMap.remove(itemIdToRemove);

            int last = --size;
            if (index != last) {
                itemIds[index] = itemIds[last];
                counts[index] = counts[last];
                expiryTicks[index] = expiryTicks[last];
                indexMap.put(itemIds[index], index); // Update map for the moved item
            }
        }

        boolean isEmpty() {
            return size == 0;
        }

        void reset() {
            size = 0;
            indexMap.clear();
        }
    }

    // ────────── Main Buffer State ──────────
    private final List<Slab> slabs = new ArrayList<>();
    private Slab activeSlab;

    public ItemBuffer() {
        this.slabs.clear(); // Ensure fresh start
        this.activeSlab = SlabPool.acquire();
        slabs.add(activeSlab);
    }

    public void add(short itemId, int currentTick) {
        Integer existingIndex = activeSlab.indexMap.get(itemId);
        if (existingIndex != null) {
            int index = existingIndex;
            if ((activeSlab.counts[index] & 0xFF) < 255) {
                activeSlab.counts[index]++;
                if (RESET_TIMER_ON_ADD) { 
                    activeSlab.expiryTicks[index] = currentTick + DEFAULT_EXPIRY_TICKS;
                }
                return;
            } else {
                activeSlab.indexMap.remove(itemId);
            }
        }

        if (activeSlab.isFull()) {
            activeSlab = SlabPool.acquire();
            slabs.add(activeSlab);
        }
        activeSlab.addEntry(itemId, currentTick + DEFAULT_EXPIRY_TICKS);
    }

    public void tick(int currentTick) {
        for (int s = 0; s < slabs.size(); s++) {
            Slab slab = slabs.get(s);

            if (slab.isEmpty()) {
                if (slab != activeSlab) { // Avoid deallocating/pooling the active slab
                    SlabPool.release(slabs.remove(s)); // Release to the pool
                    s--; // Decrement slab index
                }
            }
        }
    }


    public void flushAll() {
        for (Slab slab : slabs) {
            if (slab == activeSlab) continue;
            for (int i = 0; i < slab.size; i++) {
                expel(slab.itemIds[i], slab.counts[i]);
            }
            SlabPool.release(slab);
        }
        slabs.clear();

        if (activeSlab != null) {
            for (int i = 0; i < activeSlab.size; i++) {
                expel(activeSlab.itemIds[i], activeSlab.counts[i]);
            }
            activeSlab.reset();
            slabs.add(activeSlab);
        } else {
            activeSlab = SlabPool.acquire();
            slabs.add(activeSlab);
        }
    }

    public ListTag save() {
        ListTag bufferNbt = new ListTag();
        for (Slab slab : slabs) {
            for (int i = 0; i < slab.size; i++) {
                CompoundTag itemEntryTag = new CompoundTag();
                itemEntryTag.putShort("id", slab.itemIds[i]);
                itemEntryTag.putByte("count", slab.counts[i]);
                itemEntryTag.putInt("expiry", slab.expiryTicks[i]);
                bufferNbt.add(itemEntryTag);
            }
        }
        return bufferNbt;
    }

    public void load(ListTag bufferNbt) {
        // Reset internal state without expelling (return slabs to pool)
        for (Slab slab : slabs) {
            SlabPool.release(slab);
        }
        slabs.clear();
        activeSlab = SlabPool.acquire();
        slabs.add(activeSlab);

        if (bufferNbt.isEmpty()) {
            return;
        }

        for (int i = 0; i < bufferNbt.size(); i++) {
            CompoundTag itemEntryTag = bufferNbt.getCompound(i);
            short itemId = itemEntryTag.getShort("id");
            byte countByte = itemEntryTag.getByte("count");
            int expiryTick = itemEntryTag.getInt("expiry");

            if (activeSlab.isFull()) {
                activeSlab = SlabPool.acquire();
                slabs.add(activeSlab);
            }

            int slabIndex = activeSlab.size;
            activeSlab.itemIds[slabIndex] = itemId;
            activeSlab.counts[slabIndex] = countByte;
            activeSlab.expiryTicks[slabIndex] = expiryTick;
            activeSlab.indexMap.put(itemId, slabIndex); // Assumes itemId is unique per slab for map
            activeSlab.size++;
        }
    }

    public Map<Short, Integer> getItemSummary() {
        Map<Short, Integer> summary = new HashMap<>();
        for (Slab slab : slabs) {
            for (int i = 0; i < slab.size; i++) {
                summary.merge(slab.itemIds[i], (int) (slab.counts[i] & 0xFF), Integer::sum);
            }
        }
        return summary;
    }

    public boolean isEmpty() {
        return slabs.size() == 1 && activeSlab.isEmpty();
    }

    public int getTotalBufferedEntries() {
        int total = 0;
        for (Slab slab : slabs) {
            total += slab.size;
        }
        return total;
    }

    private void expel(short itemId, byte count) {
        System.out.printf("ItemBuffer: Expelling itemId=%d, count=%d%n", itemId, (count & 0xFF));
    }
}
