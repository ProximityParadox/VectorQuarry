package com.nicholasblue.quarrymod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class QuarryPlacementScheduler {
    private static final List<ScheduledPlacer> activePlacements = new ArrayList<>();

    public static void schedule(ServerLevel level, List<BlockPos> positions, Block block) {
        activePlacements.add(new ScheduledPlacer(level, positions, block));
    }

    public static void tick() {
        Iterator<ScheduledPlacer> it = activePlacements.iterator();
        while (it.hasNext()) {
            ScheduledPlacer placer = it.next();
            placer.tick();
            if (placer.isComplete()) {
                it.remove();
            }
        }
    }
}
