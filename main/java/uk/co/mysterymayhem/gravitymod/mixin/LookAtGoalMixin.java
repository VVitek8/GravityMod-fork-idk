package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.controller.LookController;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;











@Mixin(LookAtGoal.class)
public abstract class LookAtGoalMixin {

    @Shadow
    protected Entity lookAt;

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/ai/controller/LookController;setLookAt(DDD)V"
            )
    )
    private void gravitymod$lookAtRelativeEye(LookController controller, double x, double y, double z) {
        Entity target = this.lookAt;
        if (target == null || !GravityPhysicsHelper.isAffected(target)) {
            controller.setLookAt(x, y, z);
            return;
        }
        Vector3d lookAt = GravityPhysicsHelper.getLookAtPosition(target);
        controller.setLookAt(lookAt.x, lookAt.y, lookAt.z);
    }
}
