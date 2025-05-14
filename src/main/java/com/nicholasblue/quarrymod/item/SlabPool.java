package com.nicholasblue.quarrymod.item;

import java.util.ArrayDeque;
import java.util.Deque;

public final class SlabPool {
    private static final int MAX_REUSABLE_SLABS = 4096;
    private static final Deque<ItemBuffer.Slab> pool = new ArrayDeque<>();

    public static ItemBuffer.Slab acquire() {
        // Use the Slab's new reset method upon acquisition to ensure it's clean
        ItemBuffer.Slab slab = pool.isEmpty() ? new ItemBuffer.Slab() : pool.pop();
        slab.reset(); // Ensure size is 0 and map is clear
        return slab;
    }

    public static void release(ItemBuffer.Slab slab) {
        // Use the Slab's new reset method before returning to pool
        slab.reset(); // Ensure size is 0 and map is clear
        if (pool.size() < MAX_REUSABLE_SLABS) {
            pool.push(slab);
        }
    }

    public static int size() {
        return pool.size();
    }
}