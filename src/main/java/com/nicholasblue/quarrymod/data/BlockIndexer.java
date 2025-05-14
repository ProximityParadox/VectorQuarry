package com.nicholasblue.quarrymod.data;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Items;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class BlockIndexer {

    // Internal maps will store and use int IDs
    private static final Int2ObjectOpenHashMap<Block> ID_TO_BLOCK_INT = new Int2ObjectOpenHashMap<>();
    private static final Object2IntOpenHashMap<ResourceLocation> BLOCK_TO_ID_INT = new Object2IntOpenHashMap<>();

    //used to send to players
    private static Map<Integer, ResourceLocation> cachedIdMap;

    // Flag to indicate if indexing has been completed
    private static boolean indexBuilt = false;

    // The maximum ID that can fit in a short (inclusive)
    private static final int MAX_SHORT_ID = Short.MAX_VALUE; // 32767

    // Sentinel value to indicate a block cannot be represented by a short ID
    // We use -1 because valid assigned IDs are >= 0
    public static final short SHORT_ID_UNAVAILABLE = -1;


    private static void buildIndex() {
        if (indexBuilt) {
            System.out.println("BlockIndexer: Index already built.");
            return;
        }

        long unixTime1 = System.currentTimeMillis();

        List<Block> candidates = BuiltInRegistries.BLOCK.stream()
                .filter(BlockIndexer::hasStandardBlockItem)
                .sorted(Comparator.comparing(BuiltInRegistries.BLOCK::getKey))
                .collect(Collectors.toCollection(ArrayList::new));

        int id = 0;
        for (Block b : candidates) {
            ID_TO_BLOCK_INT.put(id, b);
            BLOCK_TO_ID_INT.put(BuiltInRegistries.BLOCK.getKey(b), id);

            ++id;
        }

        indexBuilt = true;

        long unixTime2 = System.currentTimeMillis();
        System.out.println("BlockIndexer: Index built in " + (unixTime2-unixTime1) + " ms. Indexed " + ID_TO_BLOCK_INT.size() + " blocks.");
        if (ID_TO_BLOCK_INT.size() > MAX_SHORT_ID + 1) {
            System.out.println("BlockIndexer: Warning: Total indexed blocks (" + ID_TO_BLOCK_INT.size() + ") exceeds the short ID range (" + (MAX_SHORT_ID + 1) + "). Some blocks will require int IDs.");
        }
    }

    public static void initalize(){
        buildIndex();
        cachedIdMap = buildIdToResourceMap();
    }

    private static boolean hasStandardBlockItem(Block b) {
        Item item = Item.BY_BLOCK.get(b);
        return item instanceof BlockItem && item != Items.AIR;
    }

    // --- Public methods for getting INT IDs and Blocks ---

    // Returns the full int ID for a block as an OptionalInt
    // Returns OptionalInt.empty() if the block is not indexed.
    public static OptionalInt tryGetIntId(Block block) {
        if (!indexBuilt) {
            System.err.println("BlockIndexer: Attempted to get ID before index was built!");
            return OptionalInt.empty();
        }
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        // Use getInt and handle FastUtil's default/missing behavior if needed
        return BLOCK_TO_ID_INT.containsKey(key)
                ? OptionalInt.of(BLOCK_TO_ID_INT.getInt(key))
                : OptionalInt.empty();
    }

    // Returns the full int ID for a block.
    // Throws IllegalArgumentException if the block is not indexed.
    public static int getIntId(Block block) {
        if (!indexBuilt) {
            System.err.println("BlockIndexer: Attempted to get ID before index was built!");
            throw new IllegalStateException("BlockIndexer: Index not built yet.");
        }
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        if (!BLOCK_TO_ID_INT.containsKey(key)) {
            throw new IllegalArgumentException("Block not indexed: " + key);
        }
        return BLOCK_TO_ID_INT.getInt(key);
    }

    // Returns the Block for a given int ID as an Optional.
    // Returns Optional.empty() if the ID is not found.
    public static Optional<Block> tryGetBlockFromIntId(int id) {
        if (!indexBuilt) {
            System.err.println("BlockIndexer: Attempted to get Block before index was built!");
            return Optional.empty();
        }
        return Optional.ofNullable(ID_TO_BLOCK_INT.get(id));
    }

    // Returns the Block for a given int ID.
    // Returns null if the ID is not found.
    public static Block getBlockFromIntId(int id) {
        if (!indexBuilt) {
            System.err.println("BlockIndexer: Attempted to get Block before index was built!");
            return null;
        }
        return ID_TO_BLOCK_INT.get(id);
    }


    // --- Public methods for getting SHORT IDs (for the shortBuffer) ---

    /**
     * Attempts to get a short ID for a block.
     * Returns a short ID (0 to 32767) if the block is indexed and its int ID is within that range.
     * Returns BlockIndexer.SHORT_ID_UNAVAILABLE (-1) otherwise (either not indexed or ID > 32767).
     */
    public static short tryGetShortId(Block block) {
        // We need the int ID first to check the range.
        OptionalInt intIdOpt = tryGetIntId(block); // Use the safe method

        if (intIdOpt.isPresent()) {
            int intId = intIdOpt.getAsInt();
            // Check if the int ID is within the valid range for a short (0 to 32767)
            if (intId >= 0 && intId <= MAX_SHORT_ID) {
                return (short) intId;
            }
        }

        // Block not indexed, or ID is outside the short range
        return SHORT_ID_UNAVAILABLE;
    }

    /**
     * Attempts to get a Block for a given short ID.
     * Returns an Optional<Block> if the short ID is valid (0 to 32767) and corresponds to an indexed block.
     * Returns Optional.empty() otherwise (invalid short ID, or ID not found).
     */
    public static Optional<Block> tryGetBlockFromShortId(short id) {
        // Check if the short ID is a valid ID value (0 to 32767).
        // SHORT_ID_UNAVAILABLE (-1) is not a valid block ID.
        if (id >= 0 && id <= MAX_SHORT_ID) {
            // Look up using the int map, as short IDs are just a subset of int IDs
            return tryGetBlockFromIntId(id);
        } else {
            return Optional.empty(); // Invalid short ID or sentinel value
        }
    }

    private static Map<Integer, ResourceLocation> buildIdToResourceMap() {
        Map<Integer, ResourceLocation> result = new HashMap<>();
        for (Int2ObjectMap.Entry<Block> entry : ID_TO_BLOCK_INT.int2ObjectEntrySet()) {
            int id = entry.getIntKey();
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(entry.getValue());
            result.put(id, key);
        }
        return result;
    }

    public static Map<Integer, ResourceLocation> getCachedIdToResourceMap() {
        if (cachedIdMap == null) throw new IllegalStateException("BlockIndexer not initialized yet");
        return cachedIdMap;
    }



    // --- Other Utility Methods ---

    public static int size() {
        return ID_TO_BLOCK_INT.size();
    }

    public static Stream<ResourceLocation> keyStream() {
        return BLOCK_TO_ID_INT.keySet().stream();
    }

    // Helper to check if an int ID fits in a short ID range
    private static boolean isShortId(int id) {
        return id >= 0 && id <= MAX_SHORT_ID;
    }

    // Method for your buffer logic to check if a block needs an overflow buffer
    public static boolean requiresOverflow(Block block) {
        if (!indexBuilt) {
            System.err.println("BlockIndexer: requiresOverflow called before index built. Assuming overflow required.");
            return true;
        }
        OptionalInt intIdOpt = tryGetIntId(block);
        if (!intIdOpt.isPresent()) {
            // Block isn't indexed at all (e.g., maybe it doesn't have a standard item)
            System.err.println("BlockIndexer: Block '" + BuiltInRegistries.BLOCK.getKey(block) + "' is not indexed. Treating as requiring overflow.");
            return true;
        }
        int id = intIdOpt.getAsInt();
        return !isShortId(id); // Requires overflow if the ID is > 32767
    }
}