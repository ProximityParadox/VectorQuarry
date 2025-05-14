package com.nicholasblue.quarrymod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.Map;

public class QuarryItemBufferScreen extends Screen {
    public Map<ResourceLocation, Integer> items;

    public QuarryItemBufferScreen(Map<Short, Integer> itemBuffer, Map<Integer, Integer> overflowBuffer) {
        super(Component.literal("Quarry Buffer"));


        //System.out.println("created screen");
        // Handle short ID buffer
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

    @Override
    protected void init() {
        System.out.println("QuarryItemBufferScreen: init()");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        System.out.println("RENDERING RENDERING RENDERING");
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);

        int x = 20;
        int y = 40;
        int i = 0;

        for (Map.Entry<ResourceLocation, Integer> entry : items.entrySet()) {
            Item item = BuiltInRegistries.ITEM.get(entry.getKey());
            if (item == null || item == Items.AIR) continue;

            ItemStack stack = new ItemStack(item, entry.getValue());
            int iconX = x + (i % 9) * 18;
            int iconY = y + (i / 9) * 18;

            graphics.renderItem(stack, iconX, iconY);
            graphics.renderItemDecorations(this.font, stack, iconX, iconY);
            i++;
        }
    }

}
