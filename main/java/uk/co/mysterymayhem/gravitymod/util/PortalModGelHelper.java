package uk.co.mysterymayhem.gravitymod.util;

import net.minecraft.entity.Entity;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.Property;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;

import javax.annotation.Nullable;














public final class PortalModGelHelper {

    private PortalModGelHelper() {}

    



    public static Vector3d relativeUp(EnumGravityDirection direction) {
        double[] up = direction.adjustXYZValues(0.0D, 1.0D, 0.0D);
        return new Vector3d(up[0], up[1], up[2]);
    }

    








    public static double relativeHeight(EnumGravityDirection direction, double x, double y, double z) {
        Vector3d up = relativeUp(direction);
        return up.x * x + up.y * y + up.z * z;
    }

    public static double relativeHeight(EnumGravityDirection direction, Vector3d position) {
        return relativeHeight(direction, position.x, position.y, position.z);
    }

    









    public static double blockRelativeFloor(EnumGravityDirection direction, BlockPos pos) {
        Vector3d up = relativeUp(direction);
        if (up.x != 0.0D) {
            return up.x > 0.0D ? pos.getX() : -(pos.getX() + 1);
        }
        if (up.y != 0.0D) {
            return up.y > 0.0D ? pos.getY() : -(pos.getY() + 1);
        }
        return up.z > 0.0D ? pos.getZ() : -(pos.getZ() + 1);
    }

    







    public static double heightWithinBlock(EnumGravityDirection direction, Entity entity) {
        double height = relativeHeight(direction, entity.position());
        return height - Math.floor(height);
    }

    










    public static Direction remapFace(EnumGravityDirection direction, Direction roleFace) {
        Vector3i normal = roleFace.getNormal();
        double[] world = direction.adjustXYZValues(normal.getX(), normal.getY(), normal.getZ());
        return Direction.getNearest(world[0], world[1], world[2]);
    }

    






    @Nullable
    public static BooleanProperty remapFaceProperty(EnumGravityDirection direction, Property<?> property) {
        Direction roleFace = faceOf(property);
        if (roleFace == null) {
            return null;
        }
        return facePropertyOf(remapFace(direction, roleFace));
    }

    @Nullable
    private static Direction faceOf(Property<?> property) {
        if (property == BlockStateProperties.DOWN) return Direction.DOWN;
        if (property == BlockStateProperties.UP) return Direction.UP;
        if (property == BlockStateProperties.NORTH) return Direction.NORTH;
        if (property == BlockStateProperties.SOUTH) return Direction.SOUTH;
        if (property == BlockStateProperties.WEST) return Direction.WEST;
        if (property == BlockStateProperties.EAST) return Direction.EAST;
        return null;
    }

    private static BooleanProperty facePropertyOf(Direction face) {
        switch (face) {
            case DOWN: return BlockStateProperties.DOWN;
            case UP: return BlockStateProperties.UP;
            case NORTH: return BlockStateProperties.NORTH;
            case SOUTH: return BlockStateProperties.SOUTH;
            case WEST: return BlockStateProperties.WEST;
            default: return BlockStateProperties.EAST;
        }
    }

    





    private static final double SURFACE_PROBE = 1.0E-4D;

    

















    public static BlockPos blockAtContactSurface(EnumGravityDirection direction, Entity entity) {
        Vector3d probe = entity.position().add(relativeUp(direction).scale(SURFACE_PROBE));
        return new BlockPos(probe);
    }

    







    public static Vector3d flattenToRelativeHorizontal(EnumGravityDirection direction, Vector3d worldSpace) {
        Vector3d relative = direction.inverseAdjustVec(worldSpace);
        return direction.adjustVec(relative.multiply(1.0D, 0.0D, 1.0D));
    }

    





    public static Vector3d boundingBoxCenter(EnumGravityDirection direction, Entity entity) {
        return direction.getGravityAdjustedAABB(entity.getBbWidth(), entity.getBbHeight())
                .move(entity.getX(), entity.getY(), entity.getZ())
                .getCenter();
    }

    




    public static double extentAlong(EnumGravityDirection direction, Entity entity, Direction.Axis axis) {
        AxisAlignedBB box = direction.getGravityAdjustedAABB(entity.getBbWidth(), entity.getBbHeight());
        switch (axis) {
            case X: return box.getXsize();
            case Y: return box.getYsize();
            default: return box.getZsize();
        }
    }
}
