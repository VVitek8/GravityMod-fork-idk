package uk.co.mysterymayhem.gravitymod.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.portalmod.core.math.Mat4;












public final class PortalModGeometryHelper {

    private PortalModGeometryHelper() {}

    public static void setupMatrix(MatrixStack stack, Direction direction, Direction upVector, Vector3d center) {
        Matrix4f matrix = setupMatrix(direction, upVector, center).to4f();
        stack.last().pose().multiply(matrix);
    }

    public static Mat4 setupMatrix(Direction direction, Direction upVector, Vector3d center) {
        Direction effectiveUp = upVector;
        Vec right = cross(vector(effectiveUp), vector(direction));

        
        
        if (right.isZero()) {
            Direction.Axis axis = direction.getAxis();
            effectiveUp = Direction.fromAxisAndDirection(
                    axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X,
                    Direction.AxisDirection.POSITIVE);
            right = cross(vector(effectiveUp), vector(direction));
        }

        Vec up = vector(effectiveUp);
        Vec forward = vector(direction);
        Mat4 basis = new Mat4(
                right.x, up.x, forward.x, 0.0D,
                right.y, up.y, forward.y, 0.0D,
                right.z, up.z, forward.z, 0.0D,
                0.0D, 0.0D, 0.0D, 1.0D);

        return Mat4.identity()
                .translate(center.x, center.y, center.z)
                .mul(basis)
                .translate(-0.5D, -1.0D, 0.5D);
    }

    private static Vec vector(Direction direction) {
        return new Vec(
                direction.getNormal().getX(),
                direction.getNormal().getY(),
                direction.getNormal().getZ());
    }

    private static Vec cross(Vec left, Vec right) {
        return new Vec(
                left.y * right.z - left.z * right.y,
                left.z * right.x - left.x * right.z,
                left.x * right.y - left.y * right.x);
    }

    private static final class Vec {
        final double x;
        final double y;
        final double z;

        Vec(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        boolean isZero() {
            return this.x == 0.0D && this.y == 0.0D && this.z == 0.0D;
        }
    }
}
