package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;














@Mixin(Block.class)
public abstract class BlockMixin {

    @Inject(method = "updateEntityAfterFallOn", at = @At("HEAD"), cancellable = true)
    private void gravitymod$relativeLandingStop(IBlockReader reader, Entity entity, CallbackInfo ci) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        Vector3d relative = direction.inverseAdjustVec(entity.getDeltaMovement());
        entity.setDeltaMovement(direction.adjustVec(new Vector3d(relative.x, 0.0D, relative.z)));
        ci.cancel();
    }
}
