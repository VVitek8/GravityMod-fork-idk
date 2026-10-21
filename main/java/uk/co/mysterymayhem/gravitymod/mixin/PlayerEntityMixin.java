package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.CubeModelCollisionHelper;
import uk.co.mysterymayhem.gravitymod.util.GravityDamageSources;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;
import uk.co.mysterymayhem.gravitymod.util.IGravityBackedOffMoveHolder;
import uk.co.mysterymayhem.gravitymod.util.PortalModFlightHelper;













@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Shadow
    @Final
    public PlayerAbilities abilities;

    @Shadow
    protected abstract boolean isStayingOnGroundSurface();

    





















    
    
    
    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;getHorizontalDistanceSqr(Lnet/minecraft/util/math/vector/Vector3d;)D"
            )
    )
    private double gravitymod$relativeViewBobSpeed(Vector3d velocity) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(this);
        Vector3d measured = direction == EnumGravityDirection.DOWN ? velocity : direction.inverseAdjustVec(velocity);
        return measured.x * measured.x + measured.z * measured.z;
    }

    








    @Redirect(
            method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;knockback(FDD)V")
    )
    private void gravitymod$attackKnockback(LivingEntity victim, float strength, double ratioX, double ratioZ) {
        uk.co.mysterymayhem.gravitymod.util.GravityKnockbackHelper.fromAttackerFacing(
                (PlayerEntity) (Object) this, victim, strength, ratioX, ratioZ);
    }

    










    @Redirect(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/server/ServerWorld;sendParticles(Lnet/minecraft/particles/IParticleData;DDDIDDDD)I")
    )
    private <T extends net.minecraft.particles.IParticleData> int gravitymod$damageIndicatorAtBodyCentre(
            net.minecraft.world.server.ServerWorld world, T type, double x, double y, double z,
            int count, double xDist, double yDist, double zDist, double speed, Entity target) {
        if (!GravityPhysicsHelper.isAffected(target)) {
            return world.sendParticles(type, x, y, z, count, xDist, yDist, zDist, speed);
        }
        Vector3d centre = GravityPhysicsHelper.getRelativeBodyCenter(target);
        return world.sendParticles(type, centre.x, centre.y, centre.z, count, xDist, yDist, zDist, speed);
    }

    















    @Inject(method = "sweepAttack", at = @At("HEAD"), cancellable = true)
    private void gravitymod$relativeSweepAttack(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }

        float yawRad = this.yRot * ((float) Math.PI / 180F);
        Vector3d forward = direction.adjustVec(new Vector3d(
                -MathHelper.sin(yawRad), 0.0D, MathHelper.cos(yawRad)));

        if (this.level instanceof net.minecraft.world.server.ServerWorld) {
            Vector3d origin = GravityPhysicsHelper.getRelativeBodyCenter(self).add(forward);
            ((net.minecraft.world.server.ServerWorld) this.level).sendParticles(
                    net.minecraft.particles.ParticleTypes.SWEEP_ATTACK,
                    origin.x, origin.y, origin.z,
                    0, forward.x, forward.y, forward.z, 0.0D);
        }
        ci.cancel();
    }

    























    @org.spongepowered.asm.mixin.Unique
    private boolean gravitymod$wasFlyingLastTravel = false;

    



    @org.spongepowered.asm.mixin.Unique
    private Vector3d gravitymod$velocityAtFlightEnd = null;

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void gravitymod$flightTravel(Vector3d travelVector, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            this.gravitymod$wasFlyingLastTravel = false;
            this.gravitymod$velocityAtFlightEnd = null;
            return;
        }
        if (!this.abilities.flying || self.isPassenger()) {
            return;
        }

        double relativeVerticalBefore = direction.inverseAdjustVec(self.getDeltaMovement()).y;

        float previousFlyingSpeed = this.flyingSpeed;
        this.flyingSpeed = this.abilities.getFlyingSpeed() * (float) (self.isSprinting() ? 2 : 1);

        
        
        BlockPos belowPos = this.getBlockPosBelowThatAffectsMyMovement();
        float slipperiness = this.level.getBlockState(belowPos).getSlipperiness(this.level, belowPos, self);
        float friction = this.onGround ? slipperiness * 0.91F : 0.91F;
        float speed = this.onGround ? self.getSpeed() * (0.21600002F / (slipperiness * slipperiness * slipperiness)) : this.flyingSpeed;

        self.moveRelative(speed, travelVector);
        self.move(MoverType.SELF, self.getDeltaMovement());

        Vector3d relativeAfter = direction.inverseAdjustVec(self.getDeltaMovement());
        Vector3d damped = new Vector3d(relativeAfter.x * (double) friction, relativeVerticalBefore * 0.6D, relativeAfter.z * (double) friction);
        self.setDeltaMovement(direction.adjustVec(damped));

        this.flyingSpeed = previousFlyingSpeed;
        this.fallDistance = 0.0F;
        this.setSharedFlag(7, false);

        
        
        
        
        
        self.calculateEntityAnimation(self, false);

        this.gravitymod$wasFlyingLastTravel = true;
        this.gravitymod$velocityAtFlightEnd = self.getDeltaMovement();
        ci.cancel();
    }

    























    @Inject(method = "travel", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;travel(Lnet/minecraft/util/math/vector/Vector3d;)V"))
    private void gravitymod$repairPortalModFlightEnd(Vector3d travelVector, CallbackInfo ci) {
        if (!this.gravitymod$wasFlyingLastTravel || this.abilities.flying) {
            return;
        }
        this.gravitymod$wasFlyingLastTravel = false;

        Vector3d flightMomentum = this.gravitymod$velocityAtFlightEnd;
        this.gravitymod$velocityAtFlightEnd = null;
        if (flightMomentum == null || !PortalModFlightHelper.stompsFlightEndVelocity()) {
            return;
        }

        PlayerEntity self = (PlayerEntity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }

        
        
        net.minecraft.entity.ai.attributes.ModifiableAttributeInstance gravityAttribute =
                self.getAttribute(net.minecraftforge.common.ForgeMod.ENTITY_GRAVITY.get());
        double gravity = gravityAttribute == null ? 0.08D : gravityAttribute.getValue();

        Vector3d relative = direction.inverseAdjustVec(flightMomentum);
        self.setDeltaMovement(direction.adjustVec(new Vector3d(relative.x, -gravity, relative.z)));
    }

    





    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isSwimming()Z"))
    private boolean gravitymod$suppressVanillaSwimSteering(PlayerEntity self) {
        return self.isSwimming() && !GravityPhysicsHelper.isAffected(self);
    }

    









    @Inject(method = "travel", at = @At("HEAD"))
    private void gravitymod$swimTravel(Vector3d travelVector, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN || !self.isSwimming() || self.isPassenger()) {
            return;
        }
        double lookPitch = direction.inverseAdjustVec(self.getLookAngle()).y;
        double ease = lookPitch < -0.2D ? 0.085D : 0.06D;
        
        
        BlockPos aboveHead = new BlockPos(self.position().add(direction.adjustVec(new Vector3d(0.0D, 1.0D - 0.1D, 0.0D))));
        if (lookPitch <= 0.0D || this.jumping || !this.level.getBlockState(aboveHead).getFluidState().isEmpty()) {
            Vector3d relative = direction.inverseAdjustVec(self.getDeltaMovement());
            Vector3d eased = new Vector3d(relative.x, relative.y + (lookPitch - relative.y) * ease, relative.z);
            self.setDeltaMovement(direction.adjustVec(eased));
        }
    }

    





    @Inject(method = "tick", at = @At("HEAD"))
    private void gravitymod$oxygenDeath(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (self.level.isClientSide || !self.isAlive()) {
            return;
        }
        if (GravityPhysicsHelper.getDirection(self) != EnumGravityDirection.UP) {
            return;
        }
        if (self.getY() > 400.0D && !self.isSpectator() && !this.abilities.instabuild) {
            self.hurt(GravityDamageSources.LACK_OF_OXYGEN, Float.MAX_VALUE);
        }
    }

    

















    @Inject(method = "drop(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void gravitymod$dropFromRelativeEye(ItemStack stack, boolean dropAround, boolean includeName,
                                                CallbackInfoReturnable<ItemEntity> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN || stack.isEmpty()) {
            return;
        }
        if (this.level.isClientSide) {
            self.swing(Hand.MAIN_HAND);
        }

        
        Vector3d spawn = GravityPhysicsHelper.getRelativeEyePosition(self)
                .subtract(direction.adjustVec(new Vector3d(0.0D, 0.3D, 0.0D)));
        ItemEntity itemEntity = new ItemEntity(this.level, spawn.x, spawn.y, spawn.z, stack);
        itemEntity.setPickUpDelay(40);
        if (includeName) {
            itemEntity.setThrower(self.getUUID());
        }

        
        
        
        if (dropAround) {
            float f = this.random.nextFloat() * 0.5F;
            float f1 = this.random.nextFloat() * ((float) Math.PI * 2F);
            itemEntity.setDeltaMovement(direction.adjustVec(new Vector3d(
                    (double) (-MathHelper.sin(f1) * f),
                    (double) 0.2F,
                    (double) (MathHelper.cos(f1) * f))));
        }
        else {
            float f8 = MathHelper.sin(self.xRot * ((float) Math.PI / 180F));
            float f2 = MathHelper.cos(self.xRot * ((float) Math.PI / 180F));
            float f3 = MathHelper.sin(self.yRot * ((float) Math.PI / 180F));
            float f4 = MathHelper.cos(self.yRot * ((float) Math.PI / 180F));
            float f5 = this.random.nextFloat() * ((float) Math.PI * 2F);
            float f6 = 0.02F * this.random.nextFloat();
            itemEntity.setDeltaMovement(direction.adjustVec(new Vector3d(
                    (double) (-f3 * f2 * 0.3F) + Math.cos((double) f5) * (double) f6,
                    (double) (-f8 * 0.3F + 0.1F + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F),
                    (double) (f4 * f2 * 0.3F) + Math.sin((double) f5) * (double) f6)));
        }

        cir.setReturnValue(itemEntity);
    }

    @Inject(method = "maybeBackOffFromEdge", at = @At("HEAD"), cancellable = true)
    private void gravitymod$backOffFromEdge(Vector3d vec, MoverType mover, CallbackInfoReturnable<Vector3d> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        if (this.abilities.flying || (mover != MoverType.SELF && mover != MoverType.PLAYER)
                || !this.isStayingOnGroundSurface() || !this.gravitymod$isAboveGround(direction)) {
            return;
        }

        Vector3d relative = direction.inverseAdjustVec(vec);
        double d0 = relative.x;
        double d1 = relative.z;
        double step = 0.05D;

        while (d0 != 0.0D && this.gravitymod$nothingToStandOn(self, this.gravitymod$relativeMove(direction, d0, -(double) this.maxUpStep, 0.0D))) {
            if (d0 < step && d0 >= -step) {
                d0 = 0.0D;
            }
            else if (d0 > 0.0D) {
                d0 -= step;
            }
            else {
                d0 += step;
            }
        }

        while (d1 != 0.0D && this.gravitymod$nothingToStandOn(self, this.gravitymod$relativeMove(direction, 0.0D, -(double) this.maxUpStep, d1))) {
            if (d1 < step && d1 >= -step) {
                d1 = 0.0D;
            }
            else if (d1 > 0.0D) {
                d1 -= step;
            }
            else {
                d1 += step;
            }
        }

        while (d0 != 0.0D && d1 != 0.0D && this.gravitymod$nothingToStandOn(self, this.gravitymod$relativeMove(direction, d0, -(double) this.maxUpStep, d1))) {
            if (d0 < step && d0 >= -step) {
                d0 = 0.0D;
            }
            else if (d0 > 0.0D) {
                d0 -= step;
            }
            else {
                d0 += step;
            }

            if (d1 < step && d1 >= -step) {
                d1 = 0.0D;
            }
            else if (d1 > 0.0D) {
                d1 -= step;
            }
            else {
                d1 += step;
            }
        }

        Vector3d backedOff = direction.adjustVec(new Vector3d(d0, relative.y, d1));
        
        
        
        ((IGravityBackedOffMoveHolder) self).gravitymod$setBackedOffMoveDelta(backedOff);
        cir.setReturnValue(backedOff);
    }

    




    private boolean gravitymod$isAboveGround(EnumGravityDirection direction) {
        return this.onGround || this.fallDistance < this.maxUpStep
                && !this.gravitymod$nothingToStandOn((PlayerEntity) (Object) this,
                this.gravitymod$relativeMove(direction, 0.0D, (double) this.fallDistance - (double) this.maxUpStep, 0.0D));
    }

    








    private boolean gravitymod$nothingToStandOn(PlayerEntity self, AxisAlignedBB box) {
        return CubeModelCollisionHelper.noCollision(self, box);
    }

    private AxisAlignedBB gravitymod$relativeMove(EnumGravityDirection direction, double relX, double relY, double relZ) {
        return this.getBoundingBox().move(direction.adjustVec(new Vector3d(relX, relY, relZ)));
    }
}
