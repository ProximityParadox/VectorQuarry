package com.nicholasblue.quarrymod.network;

import com.nicholasblue.quarrymod.client.ClientItemTracker;
import com.nicholasblue.quarrymod.client.QuarryItemBufferScreen;
import com.nicholasblue.quarrymod.menu.QuarryMenu;
import com.nicholasblue.quarrymod.menu.QuarryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class QuarryItemBufferPacket {
    private final BlockPos pos;
    private final Map<Short, Integer> itemBuffer;
    private final Map<Integer, Integer> overflowBuffer;

    public QuarryItemBufferPacket(BlockPos pos, Map<Short, Integer> itemBuffer, Map<Integer, Integer> overflowBuffer) {
        this.pos = pos;
        this.itemBuffer = itemBuffer;
        this.overflowBuffer = overflowBuffer;
    }

    public QuarryItemBufferPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();

        int itemSize = buf.readVarInt();
        this.itemBuffer = new HashMap<>(itemSize);
        for (int i = 0; i < itemSize; i++) {
            short key = buf.readShort();
            int val = buf.readVarInt();
            itemBuffer.put(key, val);
        }

        int overflowSize = buf.readVarInt();
        this.overflowBuffer = new HashMap<>(overflowSize);
        for (int i = 0; i < overflowSize; i++) {
            int key = buf.readVarInt();
            int val = buf.readVarInt();
            overflowBuffer.put(key, val);
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);

        buf.writeVarInt(itemBuffer.size());
        for (Map.Entry<Short, Integer> entry : itemBuffer.entrySet()) {
            buf.writeShort(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }

        buf.writeVarInt(overflowBuffer.size());
        for (Map.Entry<Integer, Integer> entry : overflowBuffer.entrySet()) {
            buf.writeVarInt(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) return;

            ClientItemTracker.INSTANCE.updateInternals(itemBuffer, overflowBuffer);


            //mc.setScreen(new QuarryItemBufferScreen(itemBuffer, overflowBuffer));

            //System.out.println("set up screen");

        });
        ctx.get().setPacketHandled(true);
    }

}
