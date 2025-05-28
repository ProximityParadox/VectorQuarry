package com.nicholasblue.quarrymod.manager;

import com.nicholasblue.quarrymod.data.QuarryBlockData;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe, copy-on-write registry for tracking active quarries in the world.
 *
 * <p>This registry uses a CAS-synchronized {@code Long2ObjectOpenHashMap} to maintain a
 * minimal snapshot of per-quarry state, keyed by {@link QuarryBlockData#quarryPos} (as long).
 * All updates are atomic and versioned; readers may safely snapshot the current state
 * without locks or external coordination.</p>
 *
 * <p>All mutation operations follow a copy-on-write model: the current state is
 * cloned and mutated, then atomically swapped back into {@code stateRef}. This avoids
 * concurrency hazards and supports consistent view semantics under contention.</p>
 *
 * <p>This class is agnostic of tick or suppression behavior. It only enforces registry
 * consistency and delta-safety.</p>
 */
public final class QuarryRegistry {

    private final AtomicReference<Long2ObjectOpenHashMap<QuarryBlockData>> stateRef =
            new AtomicReference<>(new Long2ObjectOpenHashMap<>());

    public QuarryRegistry() {}

    /**
     * Atomically registers a new quarry, if one is not already present at the given key.
     * Returns {@code true} if registration succeeded, {@code false} if already registered.
     */
    public boolean register(QuarryBlockData data) {
        long key = data.quarryPos.asLong();
        while (true) {
            Long2ObjectOpenHashMap<QuarryBlockData> oldMap = stateRef.get();
            if (oldMap.containsKey(key)) return false;
            Long2ObjectOpenHashMap<QuarryBlockData> newMap = new Long2ObjectOpenHashMap<>(oldMap);
            newMap.put(key, data);
            if (stateRef.compareAndSet(oldMap, newMap)) return true;
        }
    }

    /**
     * Atomically removes the quarry at the given position, if present.
     * Returns the removed {@code QuarryBlockData}, or {@code null} if no entry was present.
     */
    public QuarryBlockData unregister(long key) {
        while (true) {
            Long2ObjectOpenHashMap<QuarryBlockData> oldMap = stateRef.get();
            QuarryBlockData existing = oldMap.get(key);
            if (existing == null) return null;
            Long2ObjectOpenHashMap<QuarryBlockData> newMap = new Long2ObjectOpenHashMap<>(oldMap);
            newMap.remove(key);
            if (stateRef.compareAndSet(oldMap, newMap)) return existing;
        }
    }

    /**
     * Returns a snapshot of the current registry state.
     * The returned map is safe for iteration and inspection but must not be mutated.
     */
    public Long2ObjectOpenHashMap<QuarryBlockData> snapshot() {
        return stateRef.get();
    }

    /**
     * Replaces the internal state entirely (e.g., during snapshot load).
     * Assumes full ownership of the passed map; it is not copied.
     */
    public void restore(Long2ObjectOpenHashMap<QuarryBlockData> restored) {
        stateRef.set(restored);
    }

    /**
     * Atomically clears all entries from the registry.
     */
    public void clear() {
        while (true) {
            Long2ObjectOpenHashMap<QuarryBlockData> oldMap = stateRef.get();
            if (oldMap.isEmpty()) {
                return; // Already empty, nothing to do
            }
            if (stateRef.compareAndSet(oldMap, new Long2ObjectOpenHashMap<>())) {
                return; // Successfully cleared
            }
            // If CAS failed, another thread modified it, so loop and retry
        }
    }
}