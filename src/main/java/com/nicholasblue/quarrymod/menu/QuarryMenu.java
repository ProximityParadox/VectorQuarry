package com.nicholasblue.quarrymod.menu;

import ca.weblite.objc.Client;
import com.nicholasblue.quarrymod.client.ClientBlockIndex;
import com.nicholasblue.quarrymod.client.ClientItemTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import com.nicholasblue.quarrymod.registry.ModMenus;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class QuarryMenu extends AbstractContainerMenu {

    public final BlockPos pos;



    public QuarryMenu(int windowId, Inventory playerInventory, BlockPos pos,
                      Map<Short, Integer> itemBuffer,
                      Map<Integer, Integer> overflowBuffer) {
        super(ModMenus.QUARRY_MENU.get(), windowId);
        this.pos = pos;
        addPlayerInventory(playerInventory);

        ClientItemTracker.INSTANCE.updateInternals(itemBuffer, overflowBuffer);

    }

    public QuarryMenu(int windowId, Inventory playerInventory, FriendlyByteBuf buf) {
        super(ModMenus.QUARRY_MENU.get(), windowId);
        this.pos = buf.readBlockPos();

        Map<Short, Integer> itemBuffer = new HashMap<>();
        Map<Integer, Integer> overflowBuffer = new HashMap<>();

        int itemBufferSize = buf.readVarInt();
        for (int i = 0; i < itemBufferSize; i++) {
            short key = buf.readShort();
            int value = buf.readVarInt();
            itemBuffer.put(key, value);
        }

        int overflowBufferSize = buf.readVarInt();
        for (int i = 0; i < overflowBufferSize; i++) {
            int key = buf.readVarInt();
            int value = buf.readVarInt();
            overflowBuffer.put(key, value);
        }

        ClientItemTracker.INSTANCE.updateInternals(itemBuffer, overflowBuffer);

        addPlayerInventory(playerInventory);
    }


    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // You can add more logic later
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        return ItemStack.EMPTY; // Shift-clicking can be implemented later
    }
}
