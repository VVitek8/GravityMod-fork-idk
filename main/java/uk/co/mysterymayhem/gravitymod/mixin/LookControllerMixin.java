package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.controller.LookController;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;


















@Mixin(LookController.class)
public abstract class LookControllerMixin {

    @Shadow
    @Final
    protected MobEntity mob;

    @Shadow
    protected double wantedX;

    @Shadow
    protected double wantedY;

    @Shadow
    protected double wantedZ;

    @Shadow
    public abstract void setLookAt(double x, double y, double z, float yMaxRotSpeed, float xMaxRotAngle);

    @Inject(
            method = "setLookAt(Lnet/minecraft/entity/Entity;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void gravitymod$lookAtRelativeEye(Entity target, float yMaxRotSpeed, float xMaxRotAngle, CallbackInfo ci) {
        if (!GravityPhysicsHelper.isAffected(target)) {
            return;
        }
        Vector3d lookAt = GravityPhysicsHelper.getLookAtPosition(target);
        this.setLookAt(lookAt.x, lookAt.y, lookAt.z, yMaxRotSpeed, xMaxRotAngle);
        ci.cancel();
    }

    








    @Unique
    private Vector3d gravitymod$wantedOffsetInMobFrame(EnumGravityDirection direction) {
        Vector3d eye = GravityPhysicsHelper.getRelativeEyePosition(this.mob);
        return direction.inverseAdjustVec(
                new Vector3d(this.wantedX - eye.x, this.wantedY - eye.y, this.wantedZ - eye.z));
    }

    





    @Inject(method = "getXRotD", at = @At("HEAD"), cancellable = true)
    private void gravitymod$relativeXRot(CallbackInfoReturnable<Float> cir) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(this.mob);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        Vector3d offset = this.gravitymod$wantedOffsetInMobFrame(direction);
        double horizontal = (double) MathHelper.sqrt(offset.x * offset.x + offset.z * offset.z);
        cir.setReturnValue((float) (-(MathHelper.atan2(offset.y, horizontal) * (double) (180F / (float) Math.PI))));
    }

    




    @Inject(method = "getYRotD", at = @At("HEAD"), cancellable = true)
    private void gravitymod$relativeYRot(CallbackInfoReturnable<Float> cir) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(this.mob);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        Vector3d offset = this.gravitymod$wantedOffsetInMobFrame(direction);
        cir.setReturnValue(
                (float) (MathHelper.atan2(offset.z, offset.x) * (double) (180F / (float) Math.PI)) - 90.0F);
    }
}
