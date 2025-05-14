package com.nicholasblue.quarrymod.data;

import com.nicholasblue.quarrymod.item.ItemBuffer;
import com.nicholasblue.quarrymod.item.OverflowItemBuffer;
import net.minecraft.nbt.CompoundTag;

/**
 * Volatile, mutable execution state for a single running quarry.
 *
 * <p>This object is not registered or persisted during normal operation.
 * It is updated in-place by {@code CentralQuarryManager} during each server tick,
 * and only serialized when an explicit snapshot or save is triggered.</p>
 *
 * <p>Each instance is tied to a unique quarry identity via its position key
 * in the runtime state map (typically quarryPos.asLong()).</p>
 */
public final class QuarryRuntimeState {

    private int currentY;
    private int progressCounter;
    private boolean running;
    private final ItemBuffer ShortIditems;
    private final OverflowItemBuffer intIdItems;

    public QuarryRuntimeState(int currentY, int progressCounter, boolean running) {
        this.currentY = currentY;
        this.progressCounter = progressCounter;
        this.running = running;
        this.intIdItems = new OverflowItemBuffer();
        this.ShortIditems = new ItemBuffer();
    }

    /* ───────── Accessors ───────── */

    public int getCurrentY() {
        return currentY;
    }

    public void setCurrentY(int currentY) {
        this.currentY = currentY;
    }

    public int getProgressCounter() {
        return progressCounter;
    }

    public void setProgressCounter(int progressCounter) {
        this.progressCounter = progressCounter;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public ItemBuffer getItemBuffer(){
        return ShortIditems;
    }

    public OverflowItemBuffer getOverflowBuffer() {
        return intIdItems;
    }

    /* ───────── Helpers ───────── */

    public void advanceProgress() {
        this.progressCounter += 1;
    }

    public void resetProgress() {
        this.progressCounter = 0;
    }

    public void stop() {
        this.running = false;
    }

    /* ───────── Optional persistence ───────── */

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("currentY", currentY);
        tag.putInt("progress", progressCounter);
        tag.putBoolean("running", running);
        return tag;
    }

    public static QuarryRuntimeState load(CompoundTag tag) {
        int y = tag.getInt("currentY");
        int prog = tag.getInt("progress");
        boolean run = tag.getBoolean("running");
        return new QuarryRuntimeState(y, prog, run);
    }
}
