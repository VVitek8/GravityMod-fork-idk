package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.block.SlimeBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;



















@Mixin(SlimeBlock.class)
public abstract class SlimeBlockMixin {

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
                    new Vector3d(relative.x, -relative.y * retained, relative.z)));
        }
        ci.cancel();
    }

    @Inject(method = "stepOn", at = @At("HEAD"), cancellable = true)
    private void gravitymod$relativeStepOn(World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        Vector3d relative = direction.inverseAdjustVec(entity.getDeltaMovement());
        double approach = Math.abs(relative.y);
        if (approach < 0.1D && !entity.isSteppingCarefully()) {
            double damping = 0.4D + approach * 0.2D;
            entity.setDeltaMovement(direction.adjustVec(
                    new Vector3d(relative.x * damping, relative.y, relative.z * damping)));
        }
        ci.cancel();
    }
}
