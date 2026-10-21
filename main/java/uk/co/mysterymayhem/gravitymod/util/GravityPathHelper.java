package uk.co.mysterymayhem.gravitymod.util;

import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;






















public final class GravityPathHelper {

    private GravityPathHelper() {}

    
    public static BlockPos toWorldBlock(EnumGravityDirection direction, int relX, int relY, int relZ) {
        Vector3d centre = direction.adjustVec(new Vector3d(relX + 0.5D, relY + 0.5D, relZ + 0.5D));
        return new BlockPos(MathHelper.floor(centre.x), MathHelper.floor(centre.y), MathHelper.floor(centre.z));
    }

    
    public static BlockPos toRelativeBlock(EnumGravityDirection direction, int x, int y, int z) {
        Vector3d centre = direction.inverseAdjustVec(new Vector3d(x + 0.5D, y + 0.5D, z + 0.5D));
        return new BlockPos(MathHelper.floor(centre.x), MathHelper.floor(centre.y), MathHelper.floor(centre.z));
    }

    public static BlockPos toRelativeBlock(EnumGravityDirection direction, BlockPos pos) {
        return toRelativeBlock(direction, pos.getX(), pos.getY(), pos.getZ());
    }

    













    public static BlockPos toRelativeCell(EnumGravityDirection direction, Vector3d worldPoint) {
        Vector3d relative = direction.inverseAdjustVec(worldPoint);
        return new BlockPos(MathHelper.floor(relative.x), MathHelper.floor(relative.y), MathHelper.floor(relative.z));
    }

    




    public static AxisAlignedBB toWorldBox(EnumGravityDirection direction, AxisAlignedBB relative) {
        Vector3d a = direction.adjustVec(new Vector3d(relative.minX, relative.minY, relative.minZ));
        Vector3d b = direction.adjustVec(new Vector3d(relative.maxX, relative.maxY, relative.maxZ));
        return new AxisAlignedBB(
                Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z),
                Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
    }

    






    public static double relativeUpExtent(EnumGravityDirection direction, VoxelShape shape) {
        if (shape.isEmpty()) {
            return 0.0D;
        }
        double[] up = direction.adjustXYZValues(0.0D, 1.0D, 0.0D);
        if (up[0] != 0.0D) {
            return up[0] > 0.0D ? shape.max(Direction.Axis.X) : 1.0D - shape.min(Direction.Axis.X);
        }
        if (up[1] != 0.0D) {
            return up[1] > 0.0D ? shape.max(Direction.Axis.Y) : 1.0D - shape.min(Direction.Axis.Y);
        }
        return up[2] > 0.0D ? shape.max(Direction.Axis.Z) : 1.0D - shape.min(Direction.Axis.Z);
    }

    




    public static double relativeFloorLevel(IBlockReader reader, EnumGravityDirection direction,
                                            int relX, int relY, int relZ) {
        BlockPos below = toWorldBlock(direction, relX, relY - 1, relZ);
        VoxelShape shape = reader.getBlockState(below).getCollisionShape(reader, below);
        return (double) (relY - 1) + relativeUpExtent(direction, shape);
    }
}
