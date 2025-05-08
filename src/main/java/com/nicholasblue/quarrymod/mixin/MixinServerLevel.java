package com.nicholasblue.quarrymod.mixin;

import com.nicholasblue.quarrymod.suppression.GlobalSuppressionIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/* ───────── ServerLevel execution veto ───────── */
@Mixin(ServerLevel.class)
public class MixinServerLevel {

    @Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true)
    private void suppressBlockTick(BlockPos pos, Block block, CallbackInfo ci) {
        if (GlobalSuppressionIndex.INSTANCE.isSuppressed(pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "tickFluid", at = @At("HEAD"), cancellable = true)
    private void suppressFluidTick(BlockPos pos, Fluid fluid, CallbackInfo ci) {
        if (GlobalSuppressionIndex.INSTANCE.isSuppressed(pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "updateNeighborsAt", at = @At("HEAD"), cancellable = true)
    private void suppressUpdateNeighbors(BlockPos pos, Block block, CallbackInfo ci) {
        if (GlobalSuppressionIndex.INSTANCE.isSuppressed(pos)) {
            ci.cancel();
        }

    }

    @Inject(method = "neighborChanged(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;)V",
            at = @At("HEAD"), cancellable = true)
    private void suppressNeighborChangedShort(BlockPos pos, Block block, BlockPos from, CallbackInfo ci) {
        if (GlobalSuppressionIndex.INSTANCE.isSuppressed(pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "neighborChanged(Lnet/minecraft/world/level/block/state/BlockState;" +
            "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;" +
            "Lnet/minecraft/core/BlockPos;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void suppressNeighborChangedFull(BlockState state, BlockPos pos, Block block, BlockPos from, boolean moving, CallbackInfo ci) {
        if (GlobalSuppressionIndex.INSTANCE.isSuppressed(pos)) {
            ci.cancel();
        }
    }
}


