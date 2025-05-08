package com.nicholasblue.quarrymod.data;

import com.nicholasblue.quarrymod.suppression.GlobalSuppressionIndex.ChunkMask;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

public final class SuppressionIndexData {

    public final Long2ObjectMap<ChunkMask> chunkMap;

    public SuppressionIndexData(Long2ObjectMap<ChunkMask> chunkMap) {
        this.chunkMap = chunkMap;
    }

}
