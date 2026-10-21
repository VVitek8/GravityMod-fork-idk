package uk.co.mysterymayhem.gravitymod.util;

import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;





























public final class OrientedBox {

    
    private static final double DEGENERATE_AXIS_LENGTH_SQR = 1.0E-12D;

    
    private static final double DUPLICATE_AXIS_DOT = 0.999999D;

    
    private static final double PARALLEL_TOLERANCE = 1.0E-12D;

    
    private static final double COORDINATE_TOLERANCE = 1.0E-12D;

    




    private static final double CARDINAL_TOLERANCE = 1.0E-6D;

    







    private static final double CONTACT_TOLERANCE = 1.0E-7D;

    



    private static final double SLICE_TARGET_THICKNESS = 1.0D / 32.0D;

    
    private static final int MAX_SLICES_PER_SPAN = 20;

    
    private static final double SLICE_PADDING = 1.0E-7D;

    private static final Vector3d[] WORLD_AXES = {
            new Vector3d(1.0D, 0.0D, 0.0D),
            new Vector3d(0.0D, 1.0D, 0.0D),
            new Vector3d(0.0D, 0.0D, 1.0D)};

    private final Vector3d center;
    private final Vector3d axisX;
    private final Vector3d axisY;
    private final Vector3d axisZ;
    private final double halfX;
    private final double halfY;
    private final double halfZ;

    








    private final Vector3d[] separationAxes;

    private OrientedBox(Vector3d center, Vector3d axisX, Vector3d axisY, Vector3d axisZ,
                        double halfX, double halfY, double halfZ, Vector3d[] separationAxes) {
        this.center = center;
        this.axisX = axisX;
        this.axisY = axisY;
        this.axisZ = axisZ;
        this.halfX = halfX;
        this.halfY = halfY;
        this.halfZ = halfZ;
        this.separationAxes = separationAxes;
    }

    






    public static OrientedBox of(Vector3d center, Vector3d axisX, Vector3d axisY, Vector3d axisZ,
                                 double halfX, double halfY, double halfZ) {
        Vector3d y = normalizeOr(axisY, WORLD_AXES[1]);
        Vector3d x = normalizeOr(reject(normalizeOr(axisX, WORLD_AXES[0]), y), perpendicularTo(y));
        Vector3d z = normalizeOr(cross(x, y), WORLD_AXES[2]);
        if (z.dot(normalizeOr(axisZ, z)) < 0.0D) {
            z = z.scale(-1.0D);
        }
        return new OrientedBox(center, x, y, z,
                Math.max(0.0D, halfX), Math.max(0.0D, halfY), Math.max(0.0D, halfZ),
                buildSeparationAxes(x, y, z));
    }

    private static Vector3d[] buildSeparationAxes(Vector3d x, Vector3d y, Vector3d z) {
        List<Vector3d> axes = new ArrayList<>(15);
        for (Vector3d worldAxis : WORLD_AXES) {
            addAxis(axes, worldAxis);
        }
        addAxis(axes, x);
        addAxis(axes, y);
        addAxis(axes, z);
        for (Vector3d own : new Vector3d[]{x, y, z}) {
            for (Vector3d worldAxis : WORLD_AXES) {
                addAxis(axes, cross(own, worldAxis));
            }
        }
        return axes.toArray(new Vector3d[0]);
    }

    private static void addAxis(List<Vector3d> axes, Vector3d candidate) {
        if (candidate.lengthSqr() <= DEGENERATE_AXIS_LENGTH_SQR) {
            return;
        }
        Vector3d normal = candidate.normalize();
        for (Vector3d existing : axes) {
            if (Math.abs(existing.dot(normal)) >= DUPLICATE_AXIS_DOT) {
                return;
            }
        }
        axes.add(normal);
    }

    public OrientedBox move(Vector3d offset) {
        return new OrientedBox(this.center.add(offset), this.axisX, this.axisY, this.axisZ,
                this.halfX, this.halfY, this.halfZ, this.separationAxes);
    }

    public Vector3d getCenter() {
        return this.center;
    }

    




    public Vector3d getAxisX() {
        return this.axisX;
    }

    public Vector3d getAxisY() {
        return this.axisY;
    }

    public Vector3d getAxisZ() {
        return this.axisZ;
    }

    public double getHalfX() {
        return this.halfX;
    }

    public double getHalfY() {
        return this.halfY;
    }

    public double getHalfZ() {
        return this.halfZ;
    }

    
    public Vector3d[] getCorners() {
        Vector3d x = this.axisX.scale(this.halfX);
        Vector3d y = this.axisY.scale(this.halfY);
        Vector3d z = this.axisZ.scale(this.halfZ);
        return new Vector3d[]{
                this.center.subtract(x).subtract(y).subtract(z),
                this.center.add(x).subtract(y).subtract(z),
                this.center.add(x).subtract(y).add(z),
                this.center.subtract(x).subtract(y).add(z),
                this.center.subtract(x).add(y).subtract(z),
                this.center.add(x).add(y).subtract(z),
                this.center.add(x).add(y).add(z),
                this.center.subtract(x).add(y).add(z)};
    }

    
    public AxisAlignedBB getBounds() {
        double radiusX = projectionRadius(WORLD_AXES[0]);
        double radiusY = projectionRadius(WORLD_AXES[1]);
        double radiusZ = projectionRadius(WORLD_AXES[2]);
        return new AxisAlignedBB(
                this.center.x - radiusX, this.center.y - radiusY, this.center.z - radiusZ,
                this.center.x + radiusX, this.center.y + radiusY, this.center.z + radiusZ);
    }

    



    public double projectionRadius(Vector3d axis) {
        return Math.abs(axis.dot(this.axisX)) * this.halfX
                + Math.abs(axis.dot(this.axisY)) * this.halfY
                + Math.abs(axis.dot(this.axisZ)) * this.halfZ;
    }

    
    public double maxAlong(Vector3d axis) {
        return this.center.dot(axis) + projectionRadius(axis);
    }

    
    public double minAlong(Vector3d axis) {
        return this.center.dot(axis) - projectionRadius(axis);
    }

    










    public boolean intersects(AxisAlignedBB other) {
        if (!isFinite(other)) {
            return false;
        }
        Vector3d delta = this.center.subtract(other.getCenter());
        double otherHalfX = other.getXsize() * 0.5D;
        double otherHalfY = other.getYsize() * 0.5D;
        double otherHalfZ = other.getZsize() * 0.5D;
        for (Vector3d axis : this.separationAxes) {
            double overlap = projectionRadius(axis)
                    + boxProjectionRadius(axis, otherHalfX, otherHalfY, otherHalfZ)
                    - Math.abs(delta.dot(axis));
            if (overlap <= CONTACT_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    























    public Vector3d escapeFrom(AxisAlignedBB mover) {
        if (!isFinite(mover)) {
            return Vector3d.ZERO;
        }
        Vector3d gap = this.center.subtract(mover.getCenter());
        double moverHalfX = mover.getXsize() * 0.5D;
        double moverHalfY = mover.getYsize() * 0.5D;
        double moverHalfZ = mover.getZsize() * 0.5D;

        double shallowest = Double.POSITIVE_INFINITY;
        Vector3d escapeAxis = null;
        double escapeSign = 1.0D;
        for (Vector3d axis : this.separationAxes) {
            double offset = gap.dot(axis);
            double overlap = projectionRadius(axis)
                    + boxProjectionRadius(axis, moverHalfX, moverHalfY, moverHalfZ)
                    - Math.abs(offset);
            if (overlap <= CONTACT_TOLERANCE) {
                
                return Vector3d.ZERO;
            }
            if (overlap < shallowest) {
                shallowest = overlap;
                escapeAxis = axis;
                
                escapeSign = offset < 0.0D ? 1.0D : -1.0D;
            }
        }
        return escapeAxis == null ? Vector3d.ZERO
                : escapeAxis.scale(escapeSign * (shallowest + CONTACT_TOLERANCE));
    }

    





























    public double clipMovement(AxisAlignedBB obstacle, Direction.Axis moveAxis, double delta) {
        if (delta == 0.0D || !isFinite(obstacle)) {
            return delta;
        }
        Vector3d travel = WORLD_AXES[axisIndex(moveAxis)];
        double distance = Math.abs(delta);
        double sign = delta > 0.0D ? 1.0D : -1.0D;
        Vector3d gap = this.center.subtract(obstacle.getCenter());
        double otherHalfX = obstacle.getXsize() * 0.5D;
        double otherHalfY = obstacle.getYsize() * 0.5D;
        double otherHalfZ = obstacle.getZsize() * 0.5D;

        double enter = Double.NEGATIVE_INFINITY;
        double exit = Double.POSITIVE_INFINITY;
        
        
        double shallowest = Double.POSITIVE_INFINITY;
        Vector3d escapeAxis = null;
        double escapeSign = 1.0D;
        for (Vector3d axis : this.separationAxes) {
            double reach = projectionRadius(axis) + boxProjectionRadius(axis, otherHalfX, otherHalfY, otherHalfZ);
            double offset = gap.dot(axis);
            double overlap = reach - Math.abs(offset);
            if (overlap < shallowest) {
                shallowest = overlap;
                escapeAxis = axis;
                
                escapeSign = offset < 0.0D ? -1.0D : 1.0D;
            }
            double speed = travel.dot(axis) * sign;
            if (Math.abs(speed) <= PARALLEL_TOLERANCE) {
                
                
                if (Math.abs(offset) >= reach) {
                    return delta;
                }
                continue;
            }
            double first = (-reach - offset) / speed;
            double second = (reach - offset) / speed;
            enter = Math.max(enter, Math.min(first, second));
            exit = Math.min(exit, Math.max(first, second));
            if (enter >= exit) {
                return delta;
            }
        }

        if (enter > distance || exit <= 0.0D) {
            return delta;
        }
        if (enter < -CONTACT_TOLERANCE) {
            
            
            if (escapeAxis != null && travel.dot(escapeAxis) * sign * escapeSign < -PARALLEL_TOLERANCE) {
                return 0.0D;
            }
            return delta;
        }
        return sign * Math.max(0.0D, enter);
    }

    















    public double clipMovementOf(AxisAlignedBB mover, Direction.Axis moveAxis, double delta) {
        double allowed = -clipMovement(mover, moveAxis, -delta);
        
        return allowed == 0.0D ? 0.0D : allowed;
    }

    













    public List<AxisAlignedBB> toAxisAlignedBoxes() {
        AxisAlignedBB bounds = getBounds();
        int flatAxis = cardinalAxis(this.axisY);
        if (flatAxis < 0 || (cardinalAxis(this.axisX) >= 0 && cardinalAxis(this.axisZ) >= 0)) {
            
            
            
            
            
            
            
            return Collections.singletonList(bounds);
        }
        int axisA = flatAxis == 0 ? 1 : 0;
        int axisB = flatAxis == 2 ? 1 : 2;

        double[][] polygon = crossSection(axisA, axisB);
        double[] corners = {polygon[0][0], polygon[1][0], polygon[2][0], polygon[3][0]};
        java.util.Arrays.sort(corners);

        List<AxisAlignedBB> slabs = new ArrayList<>();
        for (int span = 0; span < corners.length - 1; span++) {
            double spanStart = corners[span];
            double spanEnd = corners[span + 1];
            double width = spanEnd - spanStart;
            if (width <= SLICE_PADDING) {
                continue;
            }
            double[] startExtent = extentAt(polygon, spanStart);
            double[] endExtent = extentAt(polygon, spanEnd);
            if (startExtent == null || endExtent == null) {
                continue;
            }
            double rise = Math.max(Math.abs(endExtent[0] - startExtent[0]), Math.abs(endExtent[1] - startExtent[1]));
            int steps = (int) Math.ceil(Math.max(width, rise) / SLICE_TARGET_THICKNESS);
            steps = Math.max(1, Math.min(MAX_SLICES_PER_SPAN, steps));
            for (int step = 0; step < steps; step++) {
                double slabStart = spanStart + width * ((double) step / steps);
                double slabEnd = spanStart + width * ((double) (step + 1) / steps);
                double[] low = step == 0 ? startExtent : extentAt(polygon, slabStart);
                double[] high = step == steps - 1 ? endExtent : extentAt(polygon, slabEnd);
                if (low == null || high == null) {
                    continue;
                }
                
                
                slabs.add(slabBox(bounds, axisA, axisB,
                        slabStart - SLICE_PADDING, slabEnd + SLICE_PADDING,
                        Math.min(low[0], high[0]), Math.max(low[1], high[1])));
            }
        }
        return slabs.isEmpty() ? Collections.singletonList(bounds) : slabs;
    }

    
    private double[][] crossSection(int axisA, int axisB) {
        return new double[][]{
                projectCorner(1.0D, 1.0D, axisA, axisB),
                projectCorner(-1.0D, 1.0D, axisA, axisB),
                projectCorner(-1.0D, -1.0D, axisA, axisB),
                projectCorner(1.0D, -1.0D, axisA, axisB)};
    }

    private double[] projectCorner(double signX, double signZ, int axisA, int axisB) {
        Vector3d corner = this.center
                .add(this.axisX.scale(signX * this.halfX))
                .add(this.axisZ.scale(signZ * this.halfZ));
        return new double[]{component(corner, axisA), component(corner, axisB)};
    }

    



    private static double[] extentAt(double[][] polygon, double at) {
        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < polygon.length; i++) {
            double[] from = polygon[i];
            double[] to = polygon[(i + 1) % polygon.length];
            if (Math.abs(from[0] - at) <= COORDINATE_TOLERANCE) {
                low = Math.min(low, from[1]);
                high = Math.max(high, from[1]);
            }
            if ((from[0] < at && to[0] > at) || (to[0] < at && from[0] > at)) {
                double along = (at - from[0]) / (to[0] - from[0]);
                double crossing = from[1] + (to[1] - from[1]) * along;
                low = Math.min(low, crossing);
                high = Math.max(high, crossing);
            }
        }
        return low > high ? null : new double[]{low, high};
    }

    private static AxisAlignedBB slabBox(AxisAlignedBB bounds, int axisA, int axisB,
                                         double minA, double maxA, double minB, double maxB) {
        double[] min = {bounds.minX, bounds.minY, bounds.minZ};
        double[] max = {bounds.maxX, bounds.maxY, bounds.maxZ};
        min[axisA] = minA;
        max[axisA] = maxA;
        min[axisB] = minB;
        max[axisB] = maxB;
        return new AxisAlignedBB(min[0], min[1], min[2], max[0], max[1], max[2]);
    }

    
    private static int cardinalAxis(Vector3d axis) {
        double x = Math.abs(axis.x);
        double y = Math.abs(axis.y);
        double z = Math.abs(axis.z);
        double largest = Math.max(x, Math.max(y, z));
        if (largest < 1.0D - CARDINAL_TOLERANCE) {
            return -1;
        }
        return x >= y && x >= z ? 0 : (y >= z ? 1 : 2);
    }

    private static int axisIndex(Direction.Axis axis) {
        switch (axis) {
            case X:
                return 0;
            case Y:
                return 1;
            default:
                return 2;
        }
    }

    private static double component(Vector3d vector, int axis) {
        switch (axis) {
            case 0:
                return vector.x;
            case 1:
                return vector.y;
            default:
                return vector.z;
        }
    }

    private static double boxProjectionRadius(Vector3d axis, double halfX, double halfY, double halfZ) {
        return Math.abs(axis.x) * halfX + Math.abs(axis.y) * halfY + Math.abs(axis.z) * halfZ;
    }

    










    private static boolean isFinite(AxisAlignedBB box) {
        return Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ)
                && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ);
    }

    private static Vector3d cross(Vector3d first, Vector3d second) {
        return first.cross(second);
    }

    private static Vector3d reject(Vector3d vector, Vector3d axis) {
        return vector.subtract(axis.scale(vector.dot(axis)));
    }

    private static Vector3d perpendicularTo(Vector3d axis) {
        Vector3d reference = Math.abs(axis.y) < 0.75D ? WORLD_AXES[1] : WORLD_AXES[0];
        return normalizeOr(cross(reference, axis), WORLD_AXES[2]);
    }

    private static Vector3d normalizeOr(Vector3d vector, Vector3d fallback) {
        return vector == null || vector.lengthSqr() <= DEGENERATE_AXIS_LENGTH_SQR ? fallback : vector.normalize();
    }
}
