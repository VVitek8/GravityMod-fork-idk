package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.ai.controller.LookController;
import net.minecraft.entity.ai.goal.BegGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;









@Mixin(BegGoal.class)
public abstract class BegGoalMixin {

    @Shadow
    private PlayerEntity player;

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/ai/controller/LookController;setLookAt(DDDFF)V"
            )
    )
    private void gravitymod$lookAtRelativeEye(LookController controller, double x, double y, double z,
                                              float yMaxRotSpeed, float xMaxRotAngle) {
        PlayerEntity target = this.player;
        if (target == null || !GravityPhysicsHelper.isAffected(target)) {
            controller.setLookAt(x, y, z, yMaxRotSpeed, xMaxRotAngle);
            return;
        }
        Vector3d lookAt = GravityPhysicsHelper.getLookAtPosition(target);
        controller.setLookAt(lookAt.x, lookAt.y, lookAt.z, yMaxRotSpeed, xMaxRotAngle);
    }
}
