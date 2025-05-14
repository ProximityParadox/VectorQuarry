package com.nicholasblue.quarrymod.network;

import com.nicholasblue.quarrymod.client.ClientBlockIndex;
import com.nicholasblue.quarrymod.data.BlockIndexer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class BlockIdMapPacket {

    private final Map<Integer, ResourceLocation> idMap;

    public BlockIdMapPacket(Map<Integer, ResourceLocation> idMap) {
        this.idMap = idMap;
    }

    public static void encode(BlockIdMapPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.idMap.size());
        for (Map.Entry<Integer, ResourceLocation> entry : pkt.idMap.entrySet()) {
            buf.writeVarInt(entry.getKey());
            buf.writeResourceLocation(entry.getValue());
        }
    }

    public static BlockIdMapPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<Integer, ResourceLocation> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            int id = buf.readVarInt();
            ResourceLocation key = buf.readResourceLocation();
            map.put(id, key);
        }
        return new BlockIdMapPacket(map);
    }

    public static void handle(BlockIdMapPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().level == null) {
                System.err.println("[BlockIdMapPacket] Received packet before world was ready.");
                return;
            }

            ClientBlockIndex.accept(pkt.getIdMap());
            System.out.println("[BlockIdMapPacket] Block ID map received and stored. Entries: " + pkt.getIdMap().size());
        });
        ctx.get().setPacketHandled(true);
    }


    public Map<Integer, ResourceLocation> getIdMap() {
        return idMap;
    }
}
