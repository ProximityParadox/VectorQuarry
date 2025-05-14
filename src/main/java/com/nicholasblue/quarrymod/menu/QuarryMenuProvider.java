package com.nicholasblue.quarrymod.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.Collections;
import java.util.Map;

public class QuarryMenuProvider implements MenuProvider  {
    private final BlockPos pos;
    private final Map<Short, Integer> itemBuffer;
    private final Map<Integer, Integer> overflowBuffer;

    public QuarryMenuProvider(BlockPos pos, Map<Short, Integer> itemBuffer, Map<Integer, Integer> overflowBuffer) {
        this.pos = pos;
        this.itemBuffer = itemBuffer;
        this.overflowBuffer = overflowBuffer;
    }


    public Component getDisplayName() {
        return Component.literal("Quarry");
    }

    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        // client-side only
        return new QuarryMenu(windowId, playerInventory, pos, itemBuffer, overflowBuffer);
    }

}
