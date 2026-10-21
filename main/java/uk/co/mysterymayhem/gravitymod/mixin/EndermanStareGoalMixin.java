package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.controller.LookController;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;









@Mixin(targets = "net.minecraft.entity.monster.EndermanEntity$StareGoal")
public abstract class EndermanStareGoalMixin {

    @Shadow
    private LivingEntity target;

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/ai/controller/LookController;setLookAt(DDD)V"
            )
    )
    private void gravitymod$lookAtRelativeEye(LookController controller, double x, double y, double z) {
        LivingEntity lookTarget = this.target;
        if (lookTarget == null || !GravityPhysicsHelper.isAffected(lookTarget)) {
            controller.setLookAt(x, y, z);
            return;
        }
        Vector3d lookAt = GravityPhysicsHelper.getLookAtPosition(lookTarget);
        controller.setLookAt(lookAt.x, lookAt.y, lookAt.z);
    }
}
