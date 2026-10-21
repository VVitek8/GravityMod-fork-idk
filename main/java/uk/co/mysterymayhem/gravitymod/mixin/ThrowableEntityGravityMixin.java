package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.capability.GravityAPI;











@Mixin(ThrowableEntity.class)
public abstract class ThrowableEntityGravityMixin {

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/projectile/ThrowableEntity;setDeltaMovement(DDD)V"
            )
    )
    private void gravitymod$applyRelativeThrowableGravity(ThrowableEntity projectile, double vanillaX,
                                                           double vanillaY, double vanillaZ) {
        EnumGravityDirection direction = GravityAPI.getDirection(projectile);
        double strength = GravityAPI.getStrength(projectile);
        if (direction == EnumGravityDirection.DOWN && Double.compare(strength, 1.0D) == 0) {
            projectile.setDeltaMovement(vanillaX, vanillaY, vanillaZ);
            return;
        }

        Vector3d motionAfterDrag = projectile.getDeltaMovement();
        
        
        double gravity = motionAfterDrag.y - vanillaY;
        projectile.setDeltaMovement(motionAfterDrag.add(
                direction.adjustVec(new Vector3d(0.0D, -gravity * strength, 0.0D))));
    }
}
