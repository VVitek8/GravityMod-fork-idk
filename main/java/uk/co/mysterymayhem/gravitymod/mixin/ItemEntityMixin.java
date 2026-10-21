package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.capability.GravityAPI;
import uk.co.mysterymayhem.gravitymod.capability.IGravityCapability;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;
















@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {

    



    private ItemEntityMixin(EntityType<?> type, World level) {
        super(type, level);
    }

    






    @Inject(method = "tick", at = @At("HEAD"))
    private void gravitymod$tickGravityCapability(CallbackInfo ci) {
        GravityAPI.getCapability(this).ifPresent(IGravityCapability::tick);
    }

    



    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/vector/Vector3d;add(DDD)Lnet/minecraft/util/math/vector/Vector3d;"
            )
    )
    private Vector3d gravitymod$relativeItemGravity(Vector3d motion, double x, double y, double z) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(this);
        double strength = GravityPhysicsHelper.getStrength(this);
        if (direction == EnumGravityDirection.DOWN) {
            return motion.add(x * strength, y * strength, z * strength);
        }
        return motion.add(direction.adjustVec(new Vector3d(x * strength, y * strength, z * strength)));
    }

    













    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/vector/Vector3d;multiply(DDD)Lnet/minecraft/util/math/vector/Vector3d;",
                    ordinal = 0
            )
    )
    private Vector3d gravitymod$relativeItemFriction(Vector3d motion, double frictionX, double drag, double frictionZ) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(this);
        if (direction == EnumGravityDirection.DOWN) {
            return motion.multiply(frictionX, drag, frictionZ);
        }
        Vector3d relative = direction.inverseAdjustVec(motion);
        double relativeY = relative.y * drag;
        if (this.onGround && relativeY < 0.0D) {
            relativeY *= -0.5D;
        }
        return direction.adjustVec(new Vector3d(relative.x * frictionX, relativeY, relative.z * frictionZ));
    }

    




    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/vector/Vector3d;multiply(DDD)Lnet/minecraft/util/math/vector/Vector3d;",
                    ordinal = 1
            )
    )
    private Vector3d gravitymod$suppressWorldAxisLandingDamp(Vector3d motion, double x, double y, double z) {
        return GravityPhysicsHelper.isAffected(this) ? motion : motion.multiply(x, y, z);
    }

    






    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"
            )
    )
    private BlockState gravitymod$relativeSlipperinessBlock(World level, BlockPos pos) {
        return level.getBlockState(GravityPhysicsHelper.isAffected(this) ? this.getOnPos() : pos);
    }

    















    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/item/ItemEntity;getHorizontalDistanceSqr(Lnet/minecraft/util/math/vector/Vector3d;)D"
            )
    )
    private double gravitymod$relativeSlideDistance(Vector3d motion) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(this);
        if (direction == EnumGravityDirection.DOWN) {
            return motion.x * motion.x + motion.z * motion.z;
        }
        Vector3d relative = direction.inverseAdjustVec(motion);
        return relative.x * relative.x + relative.z * relative.z;
    }

    




    @Inject(method = "setUnderwaterMovement", at = @At("HEAD"), cancellable = true)
    private void gravitymod$relativeUnderwaterMovement(CallbackInfo ci) {
        gravitymod$applyRelativeFluidMovement(0.99F, ci);
    }

    


    @Inject(method = "setUnderLavaMovement", at = @At("HEAD"), cancellable = true)
    private void gravitymod$relativeUnderLavaMovement(CallbackInfo ci) {
        gravitymod$applyRelativeFluidMovement(0.95F, ci);
    }

    @org.spongepowered.asm.mixin.Unique
    private void gravitymod$applyRelativeFluidMovement(double horizontalDamping, CallbackInfo ci) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(this);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        Vector3d relative = direction.inverseAdjustVec(this.getDeltaMovement());
        
        double relativeY = relative.y + (relative.y < (double) 0.06F ? 5.0E-4D : 0.0D);
        this.setDeltaMovement(direction.adjustVec(
                new Vector3d(relative.x * horizontalDamping, relativeY, relative.z * horizontalDamping)));
        ci.cancel();
    }
}
