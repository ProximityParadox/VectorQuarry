package com.nicholasblue.quarrymod.multiThreadedMadness;

import net.minecraft.core.BlockPos;

public record QuarryAction(
        long quarryKey,
        BlockPos target,
        boolean shouldMine,
        boolean layerComplete,
        int nextY,
        boolean stopQuarry
) {}
