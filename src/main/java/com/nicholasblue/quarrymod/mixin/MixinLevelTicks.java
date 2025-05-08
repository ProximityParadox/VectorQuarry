package com.nicholasblue.quarrymod.mixin;

import com.nicholasblue.quarrymod.suppression.GlobalSuppressionIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/* ───────── LevelTicks scheduling veto ───────── */
@Mixin(LevelTicks.class)
public class MixinLevelTicks {
    @Inject(method = "schedule", at = @At("HEAD"), cancellable = true)
    private void quarrymod$onSchedule(ScheduledTick<?> tick, CallbackInfo ci) {
        BlockPos pos = tick.pos();  // or tick.getPos() depending on mappings
        if (GlobalSuppressionIndex.INSTANCE.isSuppressed(pos)) {
            ci.cancel();
        }
    }
}
