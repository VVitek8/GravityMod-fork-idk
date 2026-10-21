package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.EntityPosWrapper;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;










@Mixin(EntityPosWrapper.class)
public abstract class EntityPosWrapperMixin {

    @Shadow
    @Final
    private Entity entity;

    @Shadow
    @Final
    private boolean trackEyeHeight;

    @Inject(method = "currentPosition", at = @At("HEAD"), cancellable = true)
    private void gravitymod$eyeAlongRelativeUp(CallbackInfoReturnable<Vector3d> cir) {
        if (!this.trackEyeHeight || !GravityPhysicsHelper.isAffected(this.entity)) {
            return;
        }
        cir.setReturnValue(GravityPhysicsHelper.getRelativeEyePosition(this.entity));
    }
}
