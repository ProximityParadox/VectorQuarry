package com.nicholasblue.quarrymod.network;

import com.nicholasblue.quarrymod.data.BlockIndexer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

public class RequestQuarryItemBufferPacket {

    private final BlockPos pos;

    public RequestQuarryItemBufferPacket(BlockPos pos) {
        this.pos = pos;
    }

    public RequestQuarryItemBufferPacket(FriendlyByteBuf buf) {
        pos = buf.readBlockPos();
    }


    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            QuarryNetwork.SendItemSnapshot(pos, player);
        });
        ctx.get().setPacketHandled(true);
    }
}
