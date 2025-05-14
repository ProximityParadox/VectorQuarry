package com.nicholasblue.quarrymod.network;

import com.nicholasblue.quarrymod.QuarryMod;
import com.nicholasblue.quarrymod.data.QuarryRuntimeState;
import com.nicholasblue.quarrymod.manager.CentralQuarryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuarryNetwork {
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
        CHANNEL.registerMessage(
                nextId(),
                QuarryItemBufferPacket.class,
                QuarryItemBufferPacket::toBytes,
                QuarryItemBufferPacket::new,
                QuarryItemBufferPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                BlockIdMapPacket.class,
                BlockIdMapPacket::encode,
                BlockIdMapPacket::decode,
                BlockIdMapPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                RequestBlockIndexPacket.class,
                RequestBlockIndexPacket::toBytes,
                RequestBlockIndexPacket::new,
                RequestBlockIndexPacket::handle
        );
        CHANNEL.registerMessage(
                nextId(),
                RequestQuarryItemBufferPacket.class,
                RequestQuarryItemBufferPacket::toBytes,
                RequestQuarryItemBufferPacket::new,
                RequestQuarryItemBufferPacket::handle
        );

    }

    private static int nextId() {
        return packetId++;
    }


    public static void requestSnapshot() {
        CHANNEL.sendToServer(new SuppressionSnapshotRequestPacket());
    }

    public static void SendItemSnapshot(BlockPos pos, ServerPlayer player){

        QuarryRuntimeState runtime = CentralQuarryManager.INSTANCE.getRuntimeState(pos);

        QuarryItemBufferPacket pkt = new QuarryItemBufferPacket(pos, runtime.getItemBuffer().getItemSummary(), runtime.getOverflowBuffer().getItemSummary());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), pkt);

    }

    public static void sendBlockIdMap(ServerPlayer player, Map<Integer, ResourceLocation> idMap) {
        System.out.println("sending block id map to client");
        BlockIdMapPacket pkt = new BlockIdMapPacket(idMap);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), pkt);
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

    public static void RequestItemSnapshot(BlockPos pos){
        CHANNEL.sendToServer(new RequestQuarryItemBufferPacket(pos));
    }

    public static void sendBlockIndexRequest() {
        CHANNEL.sendToServer(new RequestBlockIndexPacket());
    }


}
