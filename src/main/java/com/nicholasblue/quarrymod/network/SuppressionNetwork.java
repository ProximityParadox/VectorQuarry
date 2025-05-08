package com.nicholasblue.quarrymod.network;

import com.nicholasblue.quarrymod.QuarryMod;
import com.nicholasblue.quarrymod.suppression.GlobalSuppressionIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SuppressionNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final ResourceLocation CHANNEL_ID = new ResourceLocation("quarrymod", "suppression");
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            CHANNEL_ID,
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    private static int packetId = 0;

    public static void init() {
        CHANNEL.registerMessage(
                nextId(),
                SuppressionSnapshotPacket.class,
                SuppressionSnapshotPacket::encode,
                SuppressionSnapshotPacket::decode,
                SuppressionSnapshotPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                SuppressionSnapshotRequestPacket.class,
                SuppressionSnapshotRequestPacket::encode,
                SuppressionSnapshotRequestPacket::decode,
                SuppressionSnapshotRequestPacket::handle
        );


    }

    private static int nextId() {
        return packetId++;
    }


    public static void requestSnapshot() {
        CHANNEL.sendToServer(new SuppressionSnapshotRequestPacket());
    }


    public static void sendSnapshotToClient(ServerPlayer player) {
        Map<Integer, List<AABB>> shells = QuarryMod.SUPPRESSION_DIAGNOSTICS.computeConnectedShells(null);

        List<AABB> bounds = new ArrayList<>();
        List<Integer> yLevels = new ArrayList<>();

        for (Map.Entry<Integer, List<AABB>> e : shells.entrySet()) {
            int y = e.getKey();
            for (AABB box : e.getValue()) {
                bounds.add(box);
                yLevels.add(y);
            }
        }

        SuppressionSnapshotPacket pkt = new SuppressionSnapshotPacket(bounds, yLevels);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), pkt);

    }

}
