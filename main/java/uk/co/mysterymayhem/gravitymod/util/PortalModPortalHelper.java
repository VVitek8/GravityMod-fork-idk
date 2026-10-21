package uk.co.mysterymayhem.gravitymod.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3d;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;


























public final class PortalModPortalHelper {

    private PortalModPortalHelper() {}

    



    public static Direction relativeUpFacing(EnumGravityDirection direction) {
        return direction.getRelativeUp().getFacingEquivalent();
    }

    














    public static Direction toRelativeDirection(EnumGravityDirection direction, Direction world) {
        if (direction == EnumGravityDirection.DOWN) {
            return world;
        }
        Vector3d relative = direction.inverseAdjustVec(Vector3d.atLowerCornerOf(world.getNormal()));
        return Direction.getNearest(relative.x, relative.y, relative.z);
    }

    
















    public static Direction portalUpDirection(Entity player, Direction face) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(player);
        Direction relativeUp = relativeUpFacing(direction);
        if (face.getAxis() != relativeUp.getAxis()) {
            return relativeUp;
        }
        
        
        
        Direction relativeFacing = Direction.fromYRot(player.yRot);
        Vector3d world = direction.adjustVec(Vector3d.atLowerCornerOf(relativeFacing.getNormal()));
        return Direction.getNearest(world.x, world.y, world.z);
    }
}
