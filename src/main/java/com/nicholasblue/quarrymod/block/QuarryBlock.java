package com.nicholasblue.quarrymod.block;

import com.nicholasblue.quarrymod.blockentity.QuarryBlockEntity;
import com.nicholasblue.quarrymod.data.BlockIndexer;
import com.nicholasblue.quarrymod.data.QuarryRuntimeState;
import com.nicholasblue.quarrymod.manager.CentralQuarryManager;
import com.nicholasblue.quarrymod.menu.QuarryMenu;
import com.nicholasblue.quarrymod.menu.QuarryMenuProvider;
import com.nicholasblue.quarrymod.network.QuarryItemBufferPacket;
import com.nicholasblue.quarrymod.network.QuarryNetwork;
import com.nicholasblue.quarrymod.registry.ModBlockEntities;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;               // ← pay attention to import
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Map;

public class QuarryBlock extends Block implements EntityBlock {

    /** Supplier used by the DeferredRegister */
    public QuarryBlock() {
        super(Properties.copy(Blocks.IRON_BLOCK)                 // hardness, sound, tool-tags …
                .strength(3.5F)
                .requiresCorrectToolForDrops());
    }

    /* ---------- block entity hooks ---------- */

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new com.nicholasblue.quarrymod.blockentity.QuarryBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!world.isClientSide && player instanceof ServerPlayer serverPlayer) {
            System.out.println("[SERVER] openScreen called from thread: " + Thread.currentThread().getName());
            QuarryRuntimeState runtime = CentralQuarryManager.INSTANCE.getRuntimeState(pos);
            if (runtime == null) return InteractionResult.CONSUME;

            Map<Short, Integer> itemBuffer = runtime.getItemBuffer().getItemSummary();
            Map<Integer, Integer> overflowBuffer = runtime.getOverflowBuffer().getItemSummary();

            MenuProvider provider = new QuarryMenuProvider(pos, itemBuffer, overflowBuffer);

            NetworkHooks.openScreen(
                    serverPlayer,
                    provider,
                    (FriendlyByteBuf buf) -> {
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
            );
        }

        return InteractionResult.SUCCESS;
    }



}
