package com.nicholasblue.quarrymod.network;

import com.nicholasblue.quarrymod.QuarryMod;
import com.nicholasblue.quarrymod.QuarryModClient;
import com.nicholasblue.quarrymod.client.SuppressionDebugRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SuppressionSnapshotPacket {
    private final List<AABB> bounds;
    private final List<Integer> yLevels;

    public SuppressionSnapshotPacket(List<AABB> bounds, List<Integer> yLevels) {
        this.bounds = bounds;
        this.yLevels = yLevels;
    }

    public static void encode(SuppressionSnapshotPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.bounds.size());
        for (int i = 0; i < pkt.bounds.size(); i++) {
            AABB b = pkt.bounds.get(i);
            buf.writeDouble(b.minX);
            buf.writeDouble(b.minZ);
            buf.writeDouble(b.maxX);
            buf.writeDouble(b.maxZ);
            buf.writeInt(pkt.yLevels.get(i));
        }
    }

    public static SuppressionSnapshotPacket decode(FriendlyByteBuf buf) {
        int n = buf.readInt();
        List<AABB> bounds = new ArrayList<>(n);
        List<Integer> yLevels = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double minX = buf.readDouble();
            double minZ = buf.readDouble();
            double maxX = buf.readDouble();
            double maxZ = buf.readDouble();
            int y = buf.readInt();

            bounds.add(new AABB(minX, 0, minZ, maxX, 0, maxZ));
            yLevels.add(y);
        }
        return new SuppressionSnapshotPacket(bounds, yLevels);
    }

    public static void handle(SuppressionSnapshotPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            QuarryModClient.getDebugRenderer().loadSnapshot(pkt.bounds, pkt.yLevels);
        });
        ctx.get().setPacketHandled(true);
    }
}
