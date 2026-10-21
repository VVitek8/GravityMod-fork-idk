package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityKnockbackHelper;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;








@Mixin(MobEntity.class)
public abstract class MobEntityMixin {

    @Redirect(
            method = "doHurtTarget",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;knockback(FDD)V")
    )
    private void gravitymod$mobAttackKnockback(LivingEntity victim, float strength, double ratioX, double ratioZ) {
        GravityKnockbackHelper.fromAttackerFacing((MobEntity) (Object) this, victim, strength, ratioX, ratioZ);
    }

    @Shadow
    private float rotlerp(float current, float target, float maxDelta) {
        throw new AssertionError("mixin stub");
    }

    









    @Inject(method = "lookAt(Lnet/minecraft/entity/Entity;FF)V", at = @At("HEAD"), cancellable = true)
    private void gravitymod$lookAtRelativeEye(Entity target, float maxYRotIncrease, float maxXRotIncrease,
                                              CallbackInfo ci) {
        MobEntity self = (MobEntity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN && !GravityPhysicsHelper.isAffected(target)) {
            return;
        }
        Vector3d lookAt = GravityPhysicsHelper.getLookAtPosition(target);
        Vector3d eye = GravityPhysicsHelper.getRelativeEyePosition(self);
        Vector3d offset = direction.inverseAdjustVec(lookAt.subtract(eye));
        double horizontal = (double) MathHelper.sqrt(offset.x * offset.x + offset.z * offset.z);
        float yRot = (float) (MathHelper.atan2(offset.z, offset.x) * (double) (180F / (float) Math.PI)) - 90.0F;
        float xRot = (float) (-(MathHelper.atan2(offset.y, horizontal) * (double) (180F / (float) Math.PI)));
        self.xRot = this.rotlerp(self.xRot, xRot, maxXRotIncrease);
        self.yRot = this.rotlerp(self.yRot, yRot, maxYRotIncrease);
        ci.cancel();
    }
}
