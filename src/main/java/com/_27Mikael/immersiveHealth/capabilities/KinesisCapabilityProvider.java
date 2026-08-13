package com._27Mikael.immersiveHealth.capabilities;

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

import com._27Mikael.immersiveHealth.physiology.kinesis.stamina.StaminaHandler;
import com._27Mikael.immersiveHealth.physiology.kinesis.endurance.EnduranceHandler;


public class KinesisCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static Capability<Endurance> ENDURANCE = CapabilityManager.get(new CapabilityToken<Endurance>() {});
    public static Capability<Stamina> STAMINA = CapabilityManager.get(new CapabilityToken<Stamina>() {});

    private Stamina stamina = null;
    private Endurance endurance = null;

    private final LazyOptional<Stamina> optionalStamina = LazyOptional.of(this::createStamina);
    private final LazyOptional<Endurance> optionalEndurance = LazyOptional.of(this::createEndurance);

    private Stamina createStamina() {
        if (this.stamina == null) {
            this.stamina = new Stamina();
        }
        return this.stamina;
    }

    private Endurance createEndurance() {
        if (this.endurance == null) {
            this.endurance = new Endurance();
        }
        return this.endurance;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == STAMINA) {
            return optionalStamina.cast();
        }
        if (cap == ENDURANCE) {
            return optionalEndurance.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createStamina().saveNBTData(nbt);
        createEndurance().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createStamina().loadNBTData(nbt);
        createEndurance().loadNBTData(nbt);
    }
}

