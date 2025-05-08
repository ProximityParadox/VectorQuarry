package com.nicholasblue.quarrymod.events;

import net.minecraft.core.BlockPos;
import net.minecraftforge.eventbus.api.Event;

import java.util.List;

public class SuppressionLayerAddedEvent extends Event {
    private final List<BlockPos> affectedPositions;

    public SuppressionLayerAddedEvent(List<BlockPos> affectedPositions) {
        this.affectedPositions = affectedPositions;
    }

    public List<BlockPos> getAffectedPositions() {
        return affectedPositions;
    }
}
