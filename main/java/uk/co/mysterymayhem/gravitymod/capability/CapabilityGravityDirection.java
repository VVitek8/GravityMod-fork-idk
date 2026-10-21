package uk.co.mysterymayhem.gravitymod.capability;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.INBTSerializable;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;

import javax.annotation.Nonnull;

public class CapabilityGravityDirection {

    @CapabilityInject(IGravityCapability.class)
    public static Capability<IGravityCapability> GRAVITY_CAPABILITY = null;

    private CapabilityGravityDirection() {}

    public static class DefaultImpl implements IGravityCapability, INBTSerializable<INBT> {
        private EnumGravityDirection direction = EnumGravityDirection.DOWN;
        private double strength = IGravityCapability.DEFAULT_STRENGTH;
        private int timeoutTicks = 0;
        private EnumGravityDirection transitionFrom = EnumGravityDirection.DOWN;
        private int transitionTicks = 0;

        @Nonnull
        @Override
        public EnumGravityDirection getDirection() {
            return this.direction;
        }

        @Override
        public void setDirection(@Nonnull EnumGravityDirection direction, boolean noTimeout) {
            this.direction = direction;
            this.timeoutTicks = noTimeout ? 0 : IGravityCapability.DEFAULT_TIMEOUT_TICKS;
        }

        @Override
        public double getStrength() {
            return this.strength;
        }

        @Override
        public void setStrength(double strength) {
            if (!Double.isFinite(strength)) {
                strength = IGravityCapability.DEFAULT_STRENGTH;
            }
            this.strength = Math.max(IGravityCapability.MIN_STRENGTH,
                    Math.min(IGravityCapability.MAX_STRENGTH, strength));
        }

        @Override
        public int getTimeoutTicks() {
            return this.timeoutTicks;
        }

        @Override
        public void setTimeoutTicks(int ticks) {
            this.timeoutTicks = ticks;
        }

        @Override
        public void beginTransition(@Nonnull EnumGravityDirection from) {
            if (from == this.direction) {
                return;
            }
            
            
            
            
            this.transitionFrom = from;
            this.transitionTicks = IGravityCapability.TRANSITION_TICKS;
        }

        @Override
        public int getTransitionTicks() {
            return this.transitionTicks;
        }

        @Nonnull
        @Override
        public EnumGravityDirection getTransitionFrom() {
            return this.transitionFrom;
        }

        @Override
        public void tick() {
            if (this.timeoutTicks > 0) {
                this.timeoutTicks--;
            }
            if (this.transitionTicks > 0) {
                this.transitionTicks--;
            }
        }

        @Override
        public INBT serializeNBT() {
            return serialize(this);
        }

        @Override
        public void deserializeNBT(INBT nbt) {
            this.direction = EnumGravityDirection.DOWN;
            this.strength = IGravityCapability.DEFAULT_STRENGTH;
            
            
            if (nbt instanceof StringNBT) {
                this.direction = parseDirection(((StringNBT) nbt).getAsString());
                return;
            }
            if (nbt instanceof CompoundNBT) {
                CompoundNBT tag = (CompoundNBT) nbt;
                this.direction = parseDirection(tag.getString("direction"));
                if (tag.contains("strength")) {
                    this.setStrength(tag.getDouble("strength"));
                }
            }
        }

        private static EnumGravityDirection parseDirection(String name) {
            for (EnumGravityDirection direction : EnumGravityDirection.values()) {
                if (direction.getSerializedName().equals(name)) {
                    return direction;
                }
            }
            return EnumGravityDirection.DOWN;
        }
    }

    private static INBT serialize(IGravityCapability instance) {
        CompoundNBT tag = new CompoundNBT();
        tag.putString("direction", instance.getDirection().getSerializedName());
        tag.putDouble("strength", instance.getStrength());
        return tag;
    }

    public static class Storage implements net.minecraftforge.common.capabilities.Capability.IStorage<IGravityCapability> {
        @Override
        public INBT writeNBT(Capability<IGravityCapability> capability, IGravityCapability instance, Direction side) {
            return serialize(instance);
        }

        @Override
        public void readNBT(Capability<IGravityCapability> capability, IGravityCapability instance, Direction side, INBT nbt) {
            if (instance instanceof DefaultImpl) {
                ((DefaultImpl) instance).deserializeNBT(nbt);
            }
        }
    }
}
