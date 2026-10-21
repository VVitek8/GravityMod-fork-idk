package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;











@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {

    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/vector/Vector3d;add(DDD)Lnet/minecraft/util/math/vector/Vector3d;"
            )
    )
    private Vector3d gravitymod$flyImpulseRelativeToGravity(Vector3d deltaMovement, double x, double y, double z) {
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return deltaMovement.add(x, y, z);
        }
        return deltaMovement.add(direction.adjustVec(new Vector3d(x, y, z)));
    }

    


















    @Inject(method = "moveTowardsClosestSpace(DD)V", at = @At("HEAD"), cancellable = true)
    private void gravitymod$skipWallUnstickWhenGravityAffected(double x, double z, CallbackInfo ci) {
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        if (GravityPhysicsHelper.getDirection(self) != EnumGravityDirection.DOWN) {
            ci.cancel();
        }
    }
}
