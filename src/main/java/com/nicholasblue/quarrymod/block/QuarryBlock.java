package com.nicholasblue.quarrymod.block;

import com.nicholasblue.quarrymod.blockentity.QuarryBlockEntity;
import com.nicholasblue.quarrymod.registry.ModBlockEntities;
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

import javax.annotation.Nullable;

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
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof com.nicholasblue.quarrymod.blockentity.QuarryBlockEntity qbe) {
                player.openMenu(qbe);                            // opens your menu
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

}
