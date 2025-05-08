package com.nicholasblue.quarrymod.mixin;

import com.nicholasblue.quarrymod.suppression.GlobalSuppressionIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public abstract class MixinLevel{

    @Inject(method = "setBlockEntity", at = @At("HEAD"), cancellable = true)
    private void suppressSetBlockEntity(BlockEntity be, CallbackInfo ci) {
        BlockPos pos = be.getBlockPos();
        // Should always be non-null — any null here indicates a broken BlockEntity subclass
        if (pos == null) {
            throw new IllegalStateException(
                    "[QuarryMod] Encountered BlockEntity without a position during setBlockEntity(): " +
                            be.getClass().getName() +
                            ". This violates the Minecraft contract and breaks suppression logic."
            );
        }

        if (GlobalSuppressionIndex.INSTANCE.isSuppressed(pos)) {
            ci.cancel();
        }
    }


    @Inject(method = "removeBlockEntity", at = @At("HEAD"), cancellable = true)
    private void suppressRemoveBlockEntity(BlockPos pos, CallbackInfo ci) {
        if (GlobalSuppressionIndex.INSTANCE.isSuppressed(pos)) {
            ci.cancel();
        }    }
}
