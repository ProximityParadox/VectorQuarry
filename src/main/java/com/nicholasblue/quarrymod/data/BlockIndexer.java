package com.nicholasblue.quarrymod.data;

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

    private static final Int2ObjectOpenHashMap<Block> ID_TO_BLOCK = new Int2ObjectOpenHashMap<>();
    private static final Object2IntOpenHashMap<ResourceLocation> BLOCK_TO_ID = new Object2IntOpenHashMap<>();

    public static void buildIndex() {
        long unixTime1 = System.currentTimeMillis();
        List<Block> candidates = BuiltInRegistries.BLOCK.stream()
                .filter(BlockIndexer::hasStandardBlockItem).sorted(Comparator.comparing(BuiltInRegistries.BLOCK::getKey)).collect(Collectors.toCollection(ArrayList::new));

        int id = 0;
        for (Block b : candidates) {
            ID_TO_BLOCK.put(id, b);
            BLOCK_TO_ID.put(BuiltInRegistries.BLOCK.getKey(b), id);
            ++id;
        }
        long unixTime2 = System.currentTimeMillis();
        System.out.println(unixTime2-unixTime1);
    }

    private static boolean hasStandardBlockItem(Block b) {
        Item item = Item.BY_BLOCK.get(b);
        return item instanceof BlockItem && item != Items.AIR;
    }

    public static OptionalInt tryGetId(Block block) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        return BLOCK_TO_ID.containsKey(key)
                ? OptionalInt.of(BLOCK_TO_ID.getInt(key))
                : OptionalInt.empty();
    }

    public static Optional<Block> tryGetBlock(int id) {
        return Optional.ofNullable(ID_TO_BLOCK.get(id));
    }

    public static int id(Block block) {
        return tryGetId(block).orElseThrow(() ->
                new IllegalArgumentException("Block not indexed: " + block));
    }


    public static Block block(int id) {
        return ID_TO_BLOCK.get(id);
    }
    public static int size() {
        return ID_TO_BLOCK.size();
    }
    public static Stream<ResourceLocation> keyStream() {
        return BLOCK_TO_ID.keySet().stream();
    }


}
