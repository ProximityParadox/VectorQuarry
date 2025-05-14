package com.nicholasblue.quarrymod.menu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.nicholasblue.quarrymod.QuarryMod;
import com.nicholasblue.quarrymod.client.ClientItemTracker;
import com.nicholasblue.quarrymod.network.QuarryNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.Set;

public class QuarryScreen extends AbstractContainerScreen<QuarryMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(QuarryMod.MODID, "textures/gui/quarry_gui.png");

    public QuarryScreen(QuarryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176; // Standard GUI width
        this.imageHeight = 166; // Standard GUI height
    }

    short tick = 0;
    short TicksbetweenUpdates = 120;

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int x = this.leftPos + 8;
        int y = this.topPos + 18;
        int i = 0;

        Set<Map.Entry<ResourceLocation, Integer>> entryset = ClientItemTracker.INSTANCE.getItems().entrySet();

        for (Map.Entry<ResourceLocation, Integer> entry : entryset) {
            ResourceLocation itemId = entry.getKey();
            int count = entry.getValue();


            // Look up item from registry
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == Items.AIR || item == null) {
                continue; // Skip invalid/unresolvable items
            }

            // Create ItemStack with correct count (for tooltip/rendering)
            ItemStack stack = new ItemStack(item);
            stack.setCount(count);

            int iconX = x + (i % 9) * 18;  // 18px spacing horizontally
            int iconY = y + (i / 9) * 18;  // 18px spacing vertically

            guiGraphics.renderItem(stack, iconX, iconY);
            guiGraphics.renderItemDecorations(this.font, stack, iconX, iconY);

            i++;

            if (mouseX >= iconX && mouseX < iconX + 16 && mouseY >= iconY && mouseY < iconY + 16) {
                guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY);
            }
        }

        if(tick>=TicksbetweenUpdates){
            tick = 0;
            QuarryNetwork.RequestItemSnapshot(menu.pos);
            //System.out.println("sent request");
        }
        else {
            tick++;
        }


    }



    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
    }
}
