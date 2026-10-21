package uk.co.mysterymayhem.gravitymod.mixin.compat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;























@Pseudo
@Mixin(targets = "net.portalmod.core.injectors.LivingEntityInjector", remap = false)
public abstract class PortalModLivingEntityInjectorMixin {

    







    @Dynamic("PortalMod: LivingEntityInjector#onPreTick")
    @Redirect(
            method = "onPreTick",
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;getDeltaMovement()Lnet/minecraft/util/math/vector/Vector3d;"
            )
    )
    private static Vector3d gravitymod$relativeDescentCheck(LivingEntity entity) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        if (direction == EnumGravityDirection.DOWN) {
            return entity.getDeltaMovement();
        }
        return direction.inverseAdjustVec(entity.getDeltaMovement());
    }
}
