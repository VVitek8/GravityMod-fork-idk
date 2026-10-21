package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPathHelper;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;

import java.util.List;











@Mixin(Path.class)
public abstract class PathMixin {

    @Shadow
    @Final
    private List<PathPoint> nodes;

    @Inject(method = "getEntityPosAtNode", at = @At("HEAD"), cancellable = true)
    private void gravitymod$getEntityPosAtNode(Entity entity, int index, CallbackInfoReturnable<Vector3d> cir) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        PathPoint node = this.nodes.get(index);
        BlockPos cell = GravityPathHelper.toRelativeBlock(direction, node.x, node.y, node.z);
        double centring = (double) ((int) (entity.getBbWidth() + 1.0F)) * 0.5D;
        cir.setReturnValue(direction.adjustVec(new Vector3d(
                (double) cell.getX() + centring,
                (double) cell.getY(),
                (double) cell.getZ() + centring)));
    }
}
