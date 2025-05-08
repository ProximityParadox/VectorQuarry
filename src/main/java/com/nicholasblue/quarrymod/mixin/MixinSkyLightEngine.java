package com.nicholasblue.quarrymod.mixin;

import com.nicholasblue.quarrymod.suppression.GlobalSuppressionIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.SkyLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyLightEngine.class)
public abstract class MixinSkyLightEngine {
    @Inject(method = "checkNode", at = @At("HEAD"), cancellable = true)
    private void suppressSkyLightCheck(long packedPos, CallbackInfo ci) {
        BlockPos pos = BlockPos.of(packedPos);
        if (GlobalSuppressionIndex.INSTANCE.isSuppressed(pos)) {
            ci.cancel();
        }
    }
}
