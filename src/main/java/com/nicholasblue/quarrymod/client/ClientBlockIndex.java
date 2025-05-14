package com.nicholasblue.quarrymod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.HashMap;
import java.util.Map;

public class ClientBlockIndex {

    private static final Map<Integer, Block> idToBlock = new HashMap<>();

    public static void accept(Map<Integer, ResourceLocation> idMap) {
        idToBlock.clear();
        for (Map.Entry<Integer, ResourceLocation> entry : idMap.entrySet()) {
            Block block = BuiltInRegistries.BLOCK.get(entry.getValue());
            if (block != null) {
                idToBlock.put(entry.getKey(), block);
            } else {
                System.err.println("[ClientBlockIndex] Unknown block for key: " + entry.getValue());
            }
        }
        System.out.println("accepted valid resourceloc map");
    }

    public static Block getBlockFromIntId(int id) {
        Block block = idToBlock.get(id);
        if (block == null) {
            System.err.println("[ClientBlockIndex] Block ID not found: " + id);
        }
        return block;
    }

    public static Block getBlockFromShortId(short id) {
        if (id < 0) {
            System.err.println("[ClientBlockIndex] Invalid short ID: " + id);
            return null;
        }
        return idToBlock.get((int) id);
    }

    public static boolean contains(int id) {
        return idToBlock.containsKey(id);
    }

    public static int size() {
        return idToBlock.size();
    }
}
