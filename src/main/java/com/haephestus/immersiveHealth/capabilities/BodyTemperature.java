package com.haephestus.immersiveHealth.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Per-player BODY temperature — but ONLY used as a fallback when Cold Sweat is not
 * installed. When Cold Sweat is present, we read its BODY trait instead (see
 * ColdSweatCompat) and this stored value is ignored.
 *
 * Scale matches Cold Sweat's BODY scale so the rest of the mod behaves identically
 * either way: 0 = neutral, positive = hot (fever range), negative = cold.
 */
public class BodyTemperature implements INBTSerializable<CompoundTag> {

    // Reasonable clamp mirroring Cold Sweat's hurt thresholds (~ +/-100).
    public static final double MIN = -100.0;
    public static final double MAX = 100.0;

    private double temperature = 0.0; // start neutral

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = Math.max(MIN, Math.min(MAX, temperature));
    }

    public void saveNBTData(CompoundTag tag) {
        tag.putDouble("bodyTemperature", temperature);
    }

    public void loadNBTData(CompoundTag tag) {
        this.temperature = tag.contains("bodyTemperature") ? tag.getDouble("bodyTemperature") : 0.0;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        saveNBTData(tag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        loadNBTData(nbt);
    }
}
