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

/**
 * Holds one Thirst object and hands it out when something asks the player for the
 * THIRST capability. Mirrors KinesisCapabilityProvider but for a single value.
 * The static THIRST handle is populated once ModCapabilities registers Thirst.class.
 */
public class ThirstProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static Capability<Thirst> THIRST = CapabilityManager.get(new CapabilityToken<Thirst>() {});

    private Thirst thirst = null;
    private final LazyOptional<Thirst> optional = LazyOptional.of(this::create);

    private Thirst create() {
        if (this.thirst == null) {
            this.thirst = new Thirst();
        }
        return this.thirst;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == THIRST) {
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
