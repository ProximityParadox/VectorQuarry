package com.nicholasblue.quarrymod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class ScheduledPlacer {
    private final List<BlockPos> positions;
    private final Block block;
    private int currentIndex = 0;
    private final ServerLevel level;

    public ScheduledPlacer(ServerLevel level, List<BlockPos> positions, Block block) {
        this.positions = positions;
        this.block = block;
        this.level = level;
    }

    public void tick() {
        if (currentIndex >= positions.size()) return;

        BlockPos pos = positions.get(currentIndex++);
        level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
    }

    public boolean isComplete() {
        return currentIndex >= positions.size();
    }
}
