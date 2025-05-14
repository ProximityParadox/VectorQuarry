package com.nicholasblue.quarrymod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SuppressionSnapshotRequestPacket {
    public static void encode(SuppressionSnapshotRequestPacket pkt, FriendlyByteBuf buf) {}
    public static SuppressionSnapshotRequestPacket decode(FriendlyByteBuf buf) {
        return new SuppressionSnapshotRequestPacket();
    }

    public static void handle(SuppressionSnapshotRequestPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            QuarryNetwork.sendSnapshotToClient(player);
        }
        ctx.get().setPacketHandled(true);
    }
}
