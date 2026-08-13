package com.haephestus.immersiveHealth.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Per-player THIRST (hydration) data. Same idea as Stamina, but written in the
 * simpler "value lives directly in this class" style (no separate Handler), which
 * is the cleaner pattern to copy for future systems.
 *
 * hydration: 0 (dying of thirst) .. maxHydration (fully hydrated). Starts full.
 * Saved to disk via saveNBTData/loadNBTData (Forge calls these through ThirstProvider).
 */
public class Thirst implements INBTSerializable<CompoundTag> {

    private double hydration = 100.0;
    private double maxHydration = 100.0;

    public double getHydration() {
        return hydration;
    }

    /** Clamps to [0, maxHydration] so callers never have to bounds-check. */
    public void setHydration(double hydration) {
        this.hydration = Math.max(0, Math.min(maxHydration, hydration));
    }

    public double getMaxHydration() {
        return maxHydration;
    }

    public void setMaxHydration(double maxHydration) {
        this.maxHydration = maxHydration;
    }

    public void saveNBTData(CompoundTag tag) {
        tag.putDouble("hydration", hydration);
        tag.putDouble("maxHydration", maxHydration);
    }

    public void loadNBTData(CompoundTag tag) {
        this.maxHydration = tag.contains("maxHydration") ? tag.getDouble("maxHydration") : 100.0;
        this.hydration = tag.contains("hydration") ? tag.getDouble("hydration") : 100.0;
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
