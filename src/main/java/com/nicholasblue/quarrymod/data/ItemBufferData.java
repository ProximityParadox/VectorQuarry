package com.nicholasblue.quarrymod.data;

public class ItemBufferData {
    private short itemId;
    private byte count;
    private short timer;

    public ItemBufferData(short itemId, byte count, short timer) {
        this.itemId = itemId;
        this.count = count;
        this.timer = timer;
    }

    public short getItemId() {
        return itemId;
    }

    public void setItemId(short itemId) {
        this.itemId = itemId;
    }

    public byte getCount() {
        return count;
    }

    public void setCount(byte count) {
        this.count = count;
    }

    public short getTimer() {
        return timer;
    }

    public void setTimer(short timer) {
        this.timer = timer;
    }

    public void decrementTimer() {
        if (timer > 0) {
            timer--;
        }
    }

    public boolean isReadyToExpel() {
        return timer == 0 && count > 0;
    }

    @Override
    public String toString() {
        return "ItemBufferData{" +
                "itemId=" + itemId +
                ", count=" + count +
                ", timer=" + timer +
                '}';
    }
}
