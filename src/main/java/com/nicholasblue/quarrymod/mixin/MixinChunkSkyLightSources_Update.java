package com.nicholasblue.quarrymod.mixin;

import com.nicholasblue.quarrymod.suppression.GlobalSuppressionIndex;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSkyLightSources.class)
public abstract class MixinChunkSkyLightSources_Update {

    //todo: scrub the memories of trying to debug this nonsensical light update call

    @Inject(method = "update(Lnet/minecraft/world/level/BlockGetter;III)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void quarrymod$suppressUpdate(BlockGetter level, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {

        BlockPos changedBlockPos = new BlockPos(x, y, z);

        if (GlobalSuppressionIndex.INSTANCE.isSuppressed(changedBlockPos)) {
            cir.setReturnValue(false); // update() returns boolean, false means no change/no update needed
            // Or rather, it means "the lowest source Y was NOT updated by this call"
            // This should effectively prevent the strange updateEdge calls.
        }
    }
}