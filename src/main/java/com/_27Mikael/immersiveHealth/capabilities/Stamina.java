package com._27Mikael.immersiveHealth.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import com._27Mikael.immersiveHealth.physiology.kinesis.stamina.StaminaHandler;

/**
 * ============================ ANNOTATION (read me) ============================
 * This IS the per-player stamina data that now actually lives on the player and
 * saves to disk. The working slice reads/writes it via getStamina()/setStamina().
 *
 * It stores the value inside a StaminaHandler purely as a container (getStamina /
 * setStamina / clamp). The DRAIN/REGEN methods on StaminaHandler (SprintState,
 * JumpingState, etc.) are NOT used by the slice — that logic currently lives in
 * events/KinesisEvents.onPlayerTick instead. Keep that in mind so you don't edit
 * StaminaHandler expecting it to change in-game behaviour yet.
 *
 * saveNBTData/loadNBTData are what make stamina survive quitting and reloading the
 * world — Forge calls them through KinesisCapabilityProvider's NBT methods.
 * ============================================================================
 */
public class Stamina implements INBTSerializable<CompoundTag> {

    private StaminaHandler handler;

    public Stamina() {
        // Initialize with default max and current stamina values
        this.handler = new StaminaHandler(100.0);
    }

    public double getStamina() {
        return handler.getStamina();
    }

    public void setStamina(double stamina) {
        handler.setStamina(stamina);
    }

    public double getMaxStamina() {
        return handler.getMaxStamina();
    }

    public void setMaxStamina(double maxStamina) {
        handler.setMaxStamina(maxStamina);
    }

    /**
     * Load data from NBT compound tag.
     * If values are missing, fallback to defaults.
     */
    public void loadNBTData(CompoundTag tag) {
        double savedStamina = tag.contains("stamina") ? tag.getDouble("stamina") : 100.0;
        double savedMaxStamina = tag.contains("maxStamina") ? tag.getDouble("maxStamina") : 100.0;

        handler.setStamina(savedStamina);
        handler.setMaxStamina(savedMaxStamina);
    }

    /**
     * Save current state to NBT compound tag.
     */
    public void saveNBTData(CompoundTag tag) {
        tag.putDouble("stamina", handler.getStamina());
        tag.putDouble("maxStamina", handler.getMaxStamina());
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

