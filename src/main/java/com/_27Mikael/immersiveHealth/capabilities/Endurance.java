package com._27Mikael.immersiveHealth.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import com._27Mikael.immersiveHealth.physiology.kinesis.endurance.EnduranceHandler;

public class Endurance implements INBTSerializable<CompoundTag> {

    private EnduranceHandler handler;

    public Endurance() {
        // Initialize with default max and current endurance values
        this.handler = new EnduranceHandler(100.0);
    }

    public double getEndurance() {
        return handler.getEndurance();
    }

    public void setEndurance(double endurance) {
        handler.setEndurance(endurance);
    }

    public double getMaxEndurance() {
        return handler.getMaxEndurance();
    }

    public void setMaxEndurance(double maxEndurance) {
        handler.setMaxEndurance(maxEndurance);
    }

    /**
     * Load data from NBT compound tag.
     * If values are missing, fallback to defaults.
     */
    public void loadNBTData(CompoundTag tag) {
        double savedEndurance = tag.contains("endurance") ? tag.getDouble("endurance") : 100.0;
        double savedMaxEndurance = tag.contains("maxEndurance") ? tag.getDouble("maxEndurance") : 100.0;

        handler.setEndurance(savedEndurance);
        handler.setMaxEndurance(savedMaxEndurance);
    }

    /**
     * Save current state to NBT compound tag.
     */
    public void saveNBTData(CompoundTag tag) {
        tag.putDouble("endurance", handler.getEndurance());
        tag.putDouble("maxEndurance", handler.getMaxEndurance());
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
