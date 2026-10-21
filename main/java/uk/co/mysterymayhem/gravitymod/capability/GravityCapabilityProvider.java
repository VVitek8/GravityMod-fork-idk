package uk.co.mysterymayhem.gravitymod.capability;

import net.minecraft.nbt.INBT;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GravityCapabilityProvider implements ICapabilitySerializable<INBT> {

    private final IGravityCapability instance;
    private final LazyOptional<IGravityCapability> optional;

    public GravityCapabilityProvider() {
        this.instance = new CapabilityGravityDirection.DefaultImpl();
        this.optional = LazyOptional.of((NonNullSupplier<IGravityCapability>) () -> this.instance);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable net.minecraft.util.Direction side) {
        if (cap == CapabilityGravityDirection.GRAVITY_CAPABILITY) {
            return this.optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public INBT serializeNBT() {
        return ((CapabilityGravityDirection.DefaultImpl) this.instance).serializeNBT();
    }

    @Override
    public void deserializeNBT(INBT nbt) {
        ((CapabilityGravityDirection.DefaultImpl) this.instance).deserializeNBT(nbt);
    }

    public IGravityCapability getInstance() {
        return this.instance;
    }
}
