package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.CubePhysics;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;
import uk.co.mysterymayhem.gravitymod.util.IGravityKnockbackHolder;
import uk.co.mysterymayhem.gravitymod.util.PortalModFlingHelper;







@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements IGravityKnockbackHolder {

    




    @org.spongepowered.asm.mixin.Unique
    private static final float GRAVITYMOD$CUBE_AIR_DRAG = 0.98F;

    




    @Unique
    private static final double LOW_GRAVITY_GROUND_PROBE_STRENGTH = 0.04D;

    
    @Unique
    private static final double LOW_GRAVITY_GROUND_PROBE_SPEED = 0.003D;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    




    @Unique
    private Vector3d gravitymod$knockbackDirection;

    @Override
    public Vector3d gravitymod$getKnockbackDirection() {
        return this.gravitymod$knockbackDirection;
    }

    @Override
    public void gravitymod$setKnockbackDirection(Vector3d worldDirection) {
        this.gravitymod$knockbackDirection = worldDirection;
    }

    @Shadow
    public abstract boolean isEffectiveAi();

    @Shadow
    public abstract boolean isFallFlying();

    @Shadow
    protected abstract SoundEvent getFallDamageSound(int distance);

    @Shadow
    protected abstract boolean isAffectedByFluids();

    @Shadow
    protected abstract float getWaterSlowDown();

    @Shadow
    protected abstract float getJumpPower();

    @Shadow
    public abstract void calculateEntityAnimation(LivingEntity entity, boolean flying);

    @Shadow
    public abstract float getSpeed();

    @Shadow
    public float flyingSpeed;

    

















    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;setDeltaMovement(DDD)V"
            )
    )
    private void gravitymod$preserveGroundContactAtLowGravity(LivingEntity self, double x, double y, double z) {
        double strength = GravityPhysicsHelper.getStrength(self);
        if (strength <= 0.0D || strength >= LOW_GRAVITY_GROUND_PROBE_STRENGTH || self.isNoGravity()
                || (self instanceof net.minecraft.entity.player.PlayerEntity
                && ((net.minecraft.entity.player.PlayerEntity) self).abilities.flying)) {
            self.setDeltaMovement(x, y, z);
            return;
        }

        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        Vector3d relative = direction.inverseAdjustVec(new Vector3d(x, y, z));
        if (relative.y == 0.0D) {
            relative = new Vector3d(relative.x, -LOW_GRAVITY_GROUND_PROBE_SPEED, relative.z);
        }
        self.setDeltaMovement(direction.adjustVec(relative));
    }

    


    @Unique
    private Vector3d gravitymod$frameDisplacement(LivingEntity self) {
        return new Vector3d(self.getX() - this.xo, self.getY() - this.yo, self.getZ() - this.zo);
    }

    





























    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getX()D", ordinal = 0)
    )
    private double gravitymod$bodyRotationDeltaX(LivingEntity self) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return self.getX();
        }
        return this.xo + direction.inverseAdjustVec(this.gravitymod$frameDisplacement(self)).x;
    }

    


    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getZ()D", ordinal = 0)
    )
    private double gravitymod$bodyRotationDeltaZ(LivingEntity self) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return self.getZ();
        }
        return this.zo + direction.inverseAdjustVec(this.gravitymod$frameDisplacement(self)).z;
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void gravitymod$travel(Vector3d relativeInput, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        
        
        
        
        
        
        
        boolean rigidBody = CubePhysics.controls(self);
        if (GravityPhysicsHelper.hasDefaultGravity(self) && !rigidBody) {
            return;
        }
        if (!(this.isEffectiveAi() || this.isControlledByLocalInstance())) {
            return;
        }
        
        
        
        
        if (self instanceof net.minecraft.entity.player.PlayerEntity && ((net.minecraft.entity.player.PlayerEntity) self).abilities.flying) {
            return;
        }

        double gravityAccel = 0.08D * GravityPhysicsHelper.getStrength(self);

        
        
        
        
        boolean falling = direction.inverseAdjustVec(self.getDeltaMovement()).y <= 0.0D;
        FluidState fluidState = this.level.getFluidState(this.blockPosition());
        if (this.isAffectedByFluids() && !self.canStandOnFluid(fluidState.getType())) {
            if (this.isInWater()) {
                this.gravitymod$waterTravel(self, direction, relativeInput, gravityAccel, falling);
                this.calculateEntityAnimation(self, false);
                ci.cancel();
                return;
            }
            if (this.isInLava()) {
                this.gravitymod$lavaTravel(self, direction, relativeInput, gravityAccel, falling);
                this.calculateEntityAnimation(self, false);
                ci.cancel();
                return;
            }
        }

        
        
        if (this.isFallFlying()) {
            this.gravitymod$elytraTravel(self, direction, gravityAccel);
            this.calculateEntityAnimation(self, false);
            ci.cancel();
            return;
        }

        
        
        
        
        
        
        boolean zeroGravity = gravityAccel == 0.0D;

        BlockPos belowPos = this.getBlockPosBelowThatAffectsMyMovement();
        float slipperiness = this.level.getBlockState(belowPos).getSlipperiness(this.level, belowPos, self);
        float friction = this.onGround ? slipperiness * 0.91F : (zeroGravity ? 1.0F : 0.91F);
        if (rigidBody) {
            
            
            
            
            friction = zeroGravity ? 1.0F : GRAVITYMOD$CUBE_AIR_DRAG;
        }
        
        
        
        
        float speed = this.onGround ? this.gravitymod$getFrictionInfluencedSpeed(slipperiness) : this.flyingSpeed;

        self.moveRelative(speed, relativeInput);
        self.move(MoverType.SELF, self.getDeltaMovement());

        Vector3d postCollision = self.getDeltaMovement();
        Vector3d relative = direction.inverseAdjustVec(postCollision);
        double relativeY = relative.y;
        if (!self.isNoGravity()) {
            relativeY -= gravityAccel;
        }
        
        
        
        
        
        boolean dragFreeFling = !this.horizontalCollision && !this.verticalCollision
                && PortalModFlingHelper.isFlinging(self);
        
        
        
        double verticalDrag = zeroGravity ? 1.0D : 0.98D;
        Vector3d newRelative = dragFreeFling
                ? new Vector3d(relative.x, relativeY, relative.z)
                : new Vector3d(relative.x * (double) friction, relativeY * verticalDrag, relative.z * (double) friction);
        self.setDeltaMovement(direction.adjustVec(newRelative));

        this.calculateEntityAnimation(self, false);
        ci.cancel();
    }

    


















    @Unique
    private void gravitymod$elytraTravel(LivingEntity self, EnumGravityDirection direction, double gravityAccel) {
        Vector3d velocity = direction.inverseAdjustVec(self.getDeltaMovement());
        if (velocity.y > -0.5D) {
            this.fallDistance = 1.0F;
        }

        Vector3d look = direction.inverseAdjustVec(self.getLookAngle());
        float pitch = self.xRot * ((float) Math.PI / 180F);
        double lookHorizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        double speedHorizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        double lookLength = look.length();
        float lift = MathHelper.cos(pitch);
        lift = (float) ((double) lift * (double) lift * Math.min(1.0D, lookLength / 0.4D));

        
        velocity = velocity.add(0.0D, gravityAccel * (-1.0D + (double) lift * 0.75D), 0.0D);
        
        if (velocity.y < 0.0D && lookHorizontal > 0.0D) {
            double traded = velocity.y * -0.1D * (double) lift;
            velocity = velocity.add(look.x * traded / lookHorizontal, traded, look.z * traded / lookHorizontal);
        }
        
        if (pitch < 0.0F && lookHorizontal > 0.0D) {
            double traded = speedHorizontal * (double) (-MathHelper.sin(pitch)) * 0.04D;
            velocity = velocity.add(-look.x * traded / lookHorizontal, traded * 3.2D, -look.z * traded / lookHorizontal);
        }
        
        if (lookHorizontal > 0.0D) {
            velocity = velocity.add(
                    (look.x / lookHorizontal * speedHorizontal - velocity.x) * 0.1D,
                    0.0D,
                    (look.z / lookHorizontal * speedHorizontal - velocity.z) * 0.1D);
        }

        velocity = velocity.multiply((double) 0.99F, (double) 0.98F, (double) 0.99F);
        self.setDeltaMovement(direction.adjustVec(velocity));
        self.move(MoverType.SELF, self.getDeltaMovement());

        
        
        
        if (this.horizontalCollision && !this.level.isClientSide) {
            Vector3d after = direction.inverseAdjustVec(self.getDeltaMovement());
            double speedAfter = Math.sqrt(after.x * after.x + after.z * after.z);
            float damage = (float) ((speedHorizontal - speedAfter) * 10.0D - 3.0D);
            if (damage > 0.0F) {
                self.playSound(this.getFallDamageSound((int) damage), 1.0F, 1.0F);
                self.hurt(DamageSource.FLY_INTO_WALL, damage);
            }
        }

        if (this.onGround && !this.level.isClientSide) {
            this.setSharedFlag(7, false);
        }
    }

    













    @Unique
    private void gravitymod$waterTravel(LivingEntity self, EnumGravityDirection direction, Vector3d relativeInput,
                                        double gravityAccel, boolean falling) {
        double relativeYBefore = direction.inverseAdjustVec(self.position()).y;

        float drag = this.isSprinting() ? 0.9F : this.getWaterSlowDown();
        float speed = 0.02F;
        float depthStrider = (float) EnchantmentHelper.getDepthStrider(self);
        if (depthStrider > 3.0F) {
            depthStrider = 3.0F;
        }
        if (!this.onGround) {
            depthStrider *= 0.5F;
        }
        if (depthStrider > 0.0F) {
            drag += (0.54600006F - drag) * depthStrider / 3.0F;
            speed += (this.getSpeed() - speed) * depthStrider / 3.0F;
        }
        if (self.hasEffect(Effects.DOLPHINS_GRACE)) {
            drag = 0.96F;
        }
        speed *= (float) self.getAttribute(ForgeMod.SWIM_SPEED.get()).getValue();

        self.moveRelative(speed, relativeInput);
        self.move(MoverType.SELF, self.getDeltaMovement());

        Vector3d relative = direction.inverseAdjustVec(self.getDeltaMovement());
        if (this.horizontalCollision && self.onClimbable()) {
            relative = new Vector3d(relative.x, 0.2D, relative.z);
        }
        relative = new Vector3d(relative.x * (double) drag, relative.y * (double) 0.8F, relative.z * (double) drag);
        relative = this.gravitymod$applyFluidBuoyancy(self, relative, gravityAccel, falling);
        self.setDeltaMovement(direction.adjustVec(relative));

        
        
        
        double relativeYAfter = direction.inverseAdjustVec(self.position()).y;
        if (this.horizontalCollision && this.gravitymod$isRelativelyFree(self, direction,
                relative.x, relative.y + 0.6D - relativeYAfter + relativeYBefore, relative.z)) {
            self.setDeltaMovement(direction.adjustVec(new Vector3d(relative.x, 0.3D, relative.z)));
        }
    }

    




    @Unique
    private void gravitymod$lavaTravel(LivingEntity self, EnumGravityDirection direction, Vector3d relativeInput,
                                       double gravityAccel, boolean falling) {
        double relativeYBefore = direction.inverseAdjustVec(self.position()).y;

        self.moveRelative(0.02F, relativeInput);
        self.move(MoverType.SELF, self.getDeltaMovement());

        Vector3d relative = direction.inverseAdjustVec(self.getDeltaMovement());
        if (this.getFluidHeight(FluidTags.LAVA) <= this.getFluidJumpThreshold()) {
            relative = new Vector3d(relative.x * 0.5D, relative.y * (double) 0.8F, relative.z * 0.5D);
            relative = this.gravitymod$applyFluidBuoyancy(self, relative, gravityAccel, falling);
        }
        else {
            relative = relative.scale(0.5D);
        }

        if (!this.isNoGravity()) {
            relative = new Vector3d(relative.x, relative.y - gravityAccel / 4.0D, relative.z);
        }
        self.setDeltaMovement(direction.adjustVec(relative));

        double relativeYAfter = direction.inverseAdjustVec(self.position()).y;
        if (this.horizontalCollision && this.gravitymod$isRelativelyFree(self, direction,
                relative.x, relative.y + 0.6D - relativeYAfter + relativeYBefore, relative.z)) {
            self.setDeltaMovement(direction.adjustVec(new Vector3d(relative.x, 0.3D, relative.z)));
        }
    }

    





    @Unique
    private Vector3d gravitymod$applyFluidBuoyancy(LivingEntity self, Vector3d relative, double gravityAccel, boolean falling) {
        if (self.isNoGravity() || this.isSprinting() || gravityAccel == 0.0D) {
            return relative;
        }
        double relativeY;
        if (falling && Math.abs(relative.y - 0.005D) >= 0.003D && Math.abs(relative.y - gravityAccel / 16.0D) < 0.003D) {
            relativeY = -0.003D;
        }
        else {
            relativeY = relative.y - gravityAccel / 16.0D;
        }
        return new Vector3d(relative.x, relativeY, relative.z);
    }

    



    @Unique
    private boolean gravitymod$isRelativelyFree(LivingEntity self, EnumGravityDirection direction,
                                                double relX, double relY, double relZ) {
        AxisAlignedBB box = self.getBoundingBox().move(direction.adjustVec(new Vector3d(relX, relY, relZ)));
        
        
        return uk.co.mysterymayhem.gravitymod.util.CubeModelCollisionHelper.noCollision(self, box)
                && !this.level.containsAnyLiquid(box);
    }

    





    @Inject(method = "goDownInWater", at = @At("HEAD"), cancellable = true)
    private void gravitymod$goDownInWater(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        double impulse = (double) -0.04F * self.getAttribute(ForgeMod.SWIM_SPEED.get()).getValue();
        self.setDeltaMovement(self.getDeltaMovement().add(direction.adjustVec(new Vector3d(0.0D, impulse, 0.0D))));
        ci.cancel();
    }

    




    @Inject(method = "jumpInLiquid", at = @At("HEAD"), cancellable = true)
    private void gravitymod$jumpInLiquid(ITag<Fluid> fluidTag, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        double impulse = (double) 0.04F * self.getAttribute(ForgeMod.SWIM_SPEED.get()).getValue();
        self.setDeltaMovement(self.getDeltaMovement().add(direction.adjustVec(new Vector3d(0.0D, impulse, 0.0D))));
        ci.cancel();
    }

    




    private float gravitymod$getFrictionInfluencedSpeed(float friction) {
        LivingEntity self = (LivingEntity) (Object) this;
        return this.onGround ? self.getSpeed() * (0.21600002F / (friction * friction * friction)) : self.getSpeed();
    }

    
















    @Inject(method = "calculateEntityAnimation", at = @At("HEAD"), cancellable = true)
    private void gravitymod$calculateEntityAnimation(LivingEntity entity, boolean flying, CallbackInfo ci) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        Vector3d worldDelta = new Vector3d(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
        Vector3d relativeDelta = direction.inverseAdjustVec(worldDelta);
        double d0 = relativeDelta.x;
        double d1 = flying ? relativeDelta.y : 0.0D;
        double d2 = relativeDelta.z;
        float f = MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2) * 4.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }
        entity.animationSpeedOld = entity.animationSpeed;
        entity.animationSpeed += (f - entity.animationSpeed) * 0.4F;
        entity.animationPosition += entity.animationSpeed;
        ci.cancel();
    }

    














    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
    private void gravitymod$knockback(float strength, double ratioX, double ratioZ, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        
        
        Vector3d supplied = this.gravitymod$knockbackDirection;
        this.gravitymod$knockbackDirection = null;

        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }

        net.minecraftforge.event.entity.living.LivingKnockBackEvent event =
                net.minecraftforge.common.ForgeHooks.onLivingKnockBack(self, strength, ratioX, ratioZ);
        if (event.isCanceled()) {
            ci.cancel();
            return;
        }
        
        
        boolean eventChangedDirection = event.getRatioX() != ratioX || event.getRatioZ() != ratioZ;
        strength = event.getStrength();
        ratioX = event.getRatioX();
        ratioZ = event.getRatioZ();

        strength = (float) ((double) strength * (1.0D - self.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));
        ci.cancel();
        if (strength <= 0.0F) {
            return;
        }

        this.hasImpulse = true;
        Vector3d worldPush = supplied != null && !eventChangedDirection && supplied.lengthSqr() > 1.0E-4D
                ? supplied
                : new Vector3d(ratioX, 0.0D, ratioZ);

        Vector3d velocity = direction.inverseAdjustVec(self.getDeltaMovement());
        Vector3d push = direction.inverseAdjustVec(worldPush);
        
        
        
        Vector3d flat = new Vector3d(push.x, 0.0D, push.z).normalize().scale((double) strength);

        double relativeY = this.onGround ? Math.min(0.4D, velocity.y / 2.0D + (double) strength) : velocity.y;
        self.setDeltaMovement(direction.adjustVec(new Vector3d(
                velocity.x / 2.0D - flat.x,
                relativeY,
                velocity.z / 2.0D - flat.z)));
    }

    




    @Redirect(
            method = "hurt",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;knockback(FDD)V")
    )
    private void gravitymod$hurtKnockback(LivingEntity victim, float strength, double ratioX, double ratioZ,
                                          DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker == null || GravityPhysicsHelper.getDirection(victim) == EnumGravityDirection.DOWN) {
            victim.knockback(strength, ratioX, ratioZ);
            return;
        }
        uk.co.mysterymayhem.gravitymod.util.GravityKnockbackHelper.fromWorldDisplacement(
                victim, attacker.position().subtract(victim.position()), strength, ratioX, ratioZ);
    }

    
    @Redirect(
            method = "blockedByShield",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;knockback(FDD)V")
    )
    private void gravitymod$shieldKnockback(LivingEntity victim, float strength, double ratioX, double ratioZ) {
        uk.co.mysterymayhem.gravitymod.util.GravityKnockbackHelper.fromWorldDisplacement(
                victim, victim.position().subtract(this.position()), strength, ratioX, ratioZ);
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void gravitymod$jump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        float jumpPower = this.getJumpPower();
        Vector3d relative = direction.inverseAdjustVec(self.getDeltaMovement());
        Vector3d newRelative = new Vector3d(relative.x, jumpPower, relative.z);
        self.setDeltaMovement(direction.adjustVec(newRelative));

        if (this.isSprinting()) {
            float yawRad = self.yRot * ((float) Math.PI / 180F);
            Vector3d boost = direction.adjustVec(new Vector3d((double) (-MathHelper.sin(yawRad) * 0.2F), 0.0D, (double) (MathHelper.cos(yawRad) * 0.2F)));
            self.setDeltaMovement(self.getDeltaMovement().add(boost));
        }

        self.hasImpulse = true;
        net.minecraftforge.common.ForgeHooks.onLivingJump(self);
        ci.cancel();
    }
}
