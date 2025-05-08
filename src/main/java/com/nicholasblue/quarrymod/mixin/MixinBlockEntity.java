package com.nicholasblue.quarrymod.mixin;

import com.nicholasblue.quarrymod.suppression.GlobalSuppressionIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class MixinBlockEntity {

    @Inject(method = "setChanged", at = @At("HEAD"), cancellable = true)
    private void suppressSetChanged(CallbackInfo ci) {
        BlockEntity be = (BlockEntity) (Object) this;  // gaslight the compiler, assert dominance
        BlockPos pos = be.getBlockPos();
        if (GlobalSuppressionIndex.INSTANCE.isSuppressed(pos)) {
            ci.cancel();
        }
    }
}
