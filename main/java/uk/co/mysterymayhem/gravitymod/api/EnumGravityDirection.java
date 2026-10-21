package uk.co.mysterymayhem.gravitymod.api;

import net.minecraft.util.Direction;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;









public enum EnumGravityDirection implements IStringSerializable {
    UP("up", Direction.UP) {
        @Override
        public double[] adjustXYZValues(double x, double y, double z) {
            return new double[]{-x, -y, z};
        }

        @Override
        public EnumGravityDirection getInverseAdjustmentFromDOWNDirection() {
            return this;
        }

        @Override
        public EnumGravityDirection getOpposite() {
            return DOWN;
        }

        @Override
        public EnumGravityDirection getRelativePositiveX() {
            return WEST;
        }

        @Override
        public EnumGravityDirection getRelativePositiveZ() {
            return SOUTH;
        }
    },
    DOWN("down", Direction.DOWN) {
        @Override
        public double[] adjustXYZValues(double x, double y, double z) {
            return new double[]{x, y, z};
        }

        @Override
        public EnumGravityDirection getInverseAdjustmentFromDOWNDirection() {
            return this;
        }

        @Override
        public EnumGravityDirection getOpposite() {
            return UP;
        }

        @Override
        public EnumGravityDirection getRelativePositiveX() {
            return EAST;
        }

        @Override
        public EnumGravityDirection getRelativePositiveZ() {
            return SOUTH;
        }
    },
    NORTH("north", Direction.NORTH) {
        @Override
        public double[] adjustXYZValues(double x, double y, double z) {
            return new double[]{x, -z, y};
        }

        @Override
        public EnumGravityDirection getInverseAdjustmentFromDOWNDirection() {
            return SOUTH;
        }

        @Override
        public EnumGravityDirection getOpposite() {
            return SOUTH;
        }

        @Override
        public EnumGravityDirection getRelativePositiveX() {
            return EAST;
        }

        @Override
        public EnumGravityDirection getRelativePositiveZ() {
            return DOWN;
        }
    },
    EAST("east", Direction.EAST) {
        @Override
        public double[] adjustXYZValues(double x, double y, double z) {
            return new double[]{-y, x, z};
        }

        @Override
        public EnumGravityDirection getInverseAdjustmentFromDOWNDirection() {
            return WEST;
        }

        @Override
        public EnumGravityDirection getOpposite() {
            return WEST;
        }

        @Override
        public EnumGravityDirection getRelativePositiveX() {
            return UP;
        }

        @Override
        public EnumGravityDirection getRelativePositiveZ() {
            return SOUTH;
        }
    },
    SOUTH("south", Direction.SOUTH) {
        @Override
        public double[] adjustXYZValues(double x, double y, double z) {
            return new double[]{x, z, -y};
        }

        @Override
        public EnumGravityDirection getInverseAdjustmentFromDOWNDirection() {
            return NORTH;
        }

        @Override
        public EnumGravityDirection getOpposite() {
            return NORTH;
        }

        @Override
        public EnumGravityDirection getRelativePositiveX() {
            return EAST;
        }

        @Override
        public EnumGravityDirection getRelativePositiveZ() {
            return UP;
        }
    },
    WEST("west", Direction.WEST) {
        @Override
        public double[] adjustXYZValues(double x, double y, double z) {
            return new double[]{y, -x, z};
        }

        @Override
        public EnumGravityDirection getInverseAdjustmentFromDOWNDirection() {
            return EAST;
        }

        @Override
        public EnumGravityDirection getOpposite() {
            return EAST;
        }

        @Override
        public EnumGravityDirection getRelativePositiveX() {
            return DOWN;
        }

        @Override
        public EnumGravityDirection getRelativePositiveZ() {
            return SOUTH;
        }
    };

    private final String name;
    private final Direction facingEquivalent;

    EnumGravityDirection(String name, Direction facingEquivalent) {
        this.name = name;
        this.facingEquivalent = facingEquivalent;
    }

    public static EnumGravityDirection fromDirection(Direction direction) {
        switch (direction) {
            case DOWN:
                return DOWN;
            case UP:
                return UP;
            case NORTH:
                return NORTH;
            case SOUTH:
                return SOUTH;
            case WEST:
                return WEST;
            default: 
                return EAST;
        }
    }

    


    public static EnumGravityDirection getSafeDirectionFromOrdinal(int ordinal) {
        EnumGravityDirection[] values = values();
        if (ordinal >= 0 && ordinal < values.length) {
            return values[ordinal];
        }
        return DOWN;
    }

    



    public abstract double[] adjustXYZValues(double x, double y, double z);

    public double[] adjustXYZValuesMaintainSigns(double x, double y, double z) {
        double[] values = this.adjustXYZValues(x, y, z);
        double[] signs = this.adjustXYZValues(1, 1, 1);
        return new double[]{values[0] * signs[0], values[1] * signs[1], values[2] * signs[2]};
    }

    public Vector3d adjustVec(Vector3d input) {
        double[] d = this.adjustXYZValues(input.x, input.y, input.z);
        return new Vector3d(d[0], d[1], d[2]);
    }

    



    public abstract EnumGravityDirection getInverseAdjustmentFromDOWNDirection();

    public Vector3d inverseAdjustVec(Vector3d worldSpace) {
        return this.getInverseAdjustmentFromDOWNDirection().adjustVec(worldSpace);
    }

    public abstract EnumGravityDirection getOpposite();

    public abstract EnumGravityDirection getRelativePositiveX();

    public abstract EnumGravityDirection getRelativePositiveZ();

    public EnumGravityDirection getRelativeNegativeX() {
        return this.getRelativePositiveX().getOpposite();
    }

    public EnumGravityDirection getRelativeNegativeZ() {
        return this.getRelativePositiveZ().getOpposite();
    }

    public EnumGravityDirection getRelativeUp() {
        return this.getOpposite();
    }

    public EnumGravityDirection getRelativeDown() {
        return this;
    }

    public Direction getFacingEquivalent() {
        return this.facingEquivalent;
    }

    



    public float[] getCameraRollDegrees() {
        switch (this) {
            case UP:
                return new float[]{0, 0, 180};
            case NORTH:
                return new float[]{90, 0, 0};
            case EAST:
                return new float[]{0, 0, 90};
            case SOUTH:
                return new float[]{-90, 0, 0};
            case WEST:
                return new float[]{0, 0, -90};
            default: 
                return new float[]{0, 0, 0};
        }
    }

    



















    public static float getYawCorrection(EnumGravityDirection from, EnumGravityDirection to) {
        double[] upOld = from.adjustXYZValues(0, 1, 0);
        double[] upNew = to.adjustXYZValues(0, 1, 0);
        
        double[] axis = {
                upOld[1] * upNew[2] - upOld[2] * upNew[1],
                upOld[2] * upNew[0] - upOld[0] * upNew[2],
                upOld[0] * upNew[1] - upOld[1] * upNew[0]};
        double length = Math.sqrt(axis[0] * axis[0] + axis[1] * axis[1] + axis[2] * axis[2]);
        if (length < 1.0E-6D) {
            
            
            return 0.0F;
        }
        axis[0] /= length;
        axis[1] /= length;
        axis[2] /= length;
        
        
        double[] forward = from.adjustXYZValues(0, 0, 1);
        double dot = axis[0] * forward[0] + axis[1] * forward[1] + axis[2] * forward[2];
        double[] turned = {
                axis[1] * forward[2] - axis[2] * forward[1] + axis[0] * dot,
                axis[2] * forward[0] - axis[0] * forward[2] + axis[1] * dot,
                axis[0] * forward[1] - axis[1] * forward[0] + axis[2] * dot};
        
        
        Vector3d relative = to.inverseAdjustVec(new Vector3d(turned[0], turned[1], turned[2]));
        
        return (float) Math.toDegrees(Math.atan2(-relative.x, relative.z));
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    















    private static final double ANCHOR_EPSILON = 1.0E-6D;

    








    public AxisAlignedBB getGravityAdjustedAABB(float width, float height) {
        double halfWidth = width / 2.0;
        double e = ANCHOR_EPSILON;
        switch (this) {
            case DOWN:
                return new AxisAlignedBB(-halfWidth, 0, -halfWidth, halfWidth, height, halfWidth);
            case UP:
                return new AxisAlignedBB(-halfWidth, -height + e, -halfWidth, halfWidth, e, halfWidth);
            case NORTH:
                return new AxisAlignedBB(-halfWidth, -halfWidth, 0, halfWidth, halfWidth, height);
            case SOUTH:
                return new AxisAlignedBB(-halfWidth, -halfWidth, -height + e, halfWidth, halfWidth, e);
            case WEST:
                return new AxisAlignedBB(0, -halfWidth, -halfWidth, height, halfWidth, halfWidth);
            default: 
                return new AxisAlignedBB(-height + e, -halfWidth, -halfWidth, e, halfWidth, halfWidth);
        }
    }

    








    public Vector3d getAnchorPosition(AxisAlignedBB box) {
        double e = ANCHOR_EPSILON;
        switch (this) {
            case UP:
                return new Vector3d((box.minX + box.maxX) / 2.0D, box.maxY - e, (box.minZ + box.maxZ) / 2.0D);
            case NORTH:
                return new Vector3d((box.minX + box.maxX) / 2.0D, (box.minY + box.maxY) / 2.0D, box.minZ);
            case SOUTH:
                return new Vector3d((box.minX + box.maxX) / 2.0D, (box.minY + box.maxY) / 2.0D, box.maxZ - e);
            case WEST:
                return new Vector3d(box.minX, (box.minY + box.maxY) / 2.0D, (box.minZ + box.maxZ) / 2.0D);
            case EAST:
                return new Vector3d(box.maxX - e, (box.minY + box.maxY) / 2.0D, (box.minZ + box.maxZ) / 2.0D);
            default: 
                return new Vector3d((box.minX + box.maxX) / 2.0D, box.minY, (box.minZ + box.maxZ) / 2.0D);
        }
    }
}
