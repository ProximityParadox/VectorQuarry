package com.nicholasblue.quarrymod.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

public class ClientItemTracker {

    public static final ClientItemTracker INSTANCE = new ClientItemTracker();
    private ClientItemTracker() {}

    private Map<ResourceLocation, Integer> items = new HashMap<>();

    public void updateInternals(Map<Short, Integer> itemBuffer, Map<Integer, Integer> overflowBuffer){
        // Handle short ID buffer

        this.items.clear();

        for (Map.Entry<Short, Integer> entry : itemBuffer.entrySet()) {
            short shortId = entry.getKey();
            int count = entry.getValue();

            Block block = ClientBlockIndex.getBlockFromShortId(shortId);
            if (block == null) continue;

            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
            items.merge(key, count, Integer::sum);
        }

        // Handle overflow int ID buffer
        for (Map.Entry<Integer, Integer> entry : overflowBuffer.entrySet()) {
            int intId = entry.getKey();
            int count = entry.getValue();

            Block block = ClientBlockIndex.getBlockFromIntId(intId);
            if (block == null) continue;

            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
            items.merge(key, count, Integer::sum);
        }
    }

    public Map<ResourceLocation, Integer> getItems() {
        return items;
    }
}
