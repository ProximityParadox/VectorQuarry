package com.nicholasblue.quarrymod.network;

import com.nicholasblue.quarrymod.data.BlockIndexer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

public class RequestBlockIndexPacket {
    public RequestBlockIndexPacket() {}

    public RequestBlockIndexPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            System.out.println("server recieved request to send cached resourceloc map");
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Map<Integer, ResourceLocation> map = BlockIndexer.getCachedIdToResourceMap();
            QuarryNetwork.sendBlockIdMap(player, map);
        });
        ctx.get().setPacketHandled(true);
    }
}
