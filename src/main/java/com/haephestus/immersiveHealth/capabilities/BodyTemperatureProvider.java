package com.haephestus.immersiveHealth.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Hands out the BodyTemperature capability for a player. Same pattern as ThirstProvider. */
public class BodyTemperatureProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static Capability<BodyTemperature> BODY_TEMPERATURE =
            CapabilityManager.get(new CapabilityToken<BodyTemperature>() {});

    private BodyTemperature bodyTemperature = null;
    private final LazyOptional<BodyTemperature> optional = LazyOptional.of(this::create);

    private BodyTemperature create() {
        if (this.bodyTemperature == null) {
            this.bodyTemperature = new BodyTemperature();
        }
        return this.bodyTemperature;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == BODY_TEMPERATURE) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        create().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        create().loadNBTData(nbt);
    }
}
