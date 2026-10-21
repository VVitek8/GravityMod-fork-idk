package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.block.BedBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;








@Mixin(BedBlock.class)
public abstract class BedBlockMixin {

    @Inject(method = "bounceUp", at = @At("HEAD"), cancellable = true)
    private void gravitymod$relativeBounce(Entity entity, CallbackInfo ci) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        Vector3d relative = direction.inverseAdjustVec(entity.getDeltaMovement());
        if (relative.y < 0.0D) {
            double retained = entity instanceof LivingEntity ? 1.0D : 0.8D;
            entity.setDeltaMovement(direction.adjustVec(
                    new Vector3d(relative.x, -relative.y * (double) 0.66F * retained, relative.z)));
        }
        ci.cancel();
    }
}
