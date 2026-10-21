package uk.co.mysterymayhem.gravitymod.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ReuseableStream;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.ModList;
import net.portalmod.common.sorted.cube.Cube;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.world.ModGameRules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;















































public final class CubeModelCollisionHelper {

    private CubeModelCollisionHelper() {}

    
    private static final double MODEL_SIZE_PIXELS = 13.0D;

    private static final double PIXELS_PER_BLOCK = 16.0D;

    





    private static final double BASE_ENTITY_SIZE = 0.8D;

    
    private static final double MINIMUM_SIZE = 1.0E-4D;

    












    public static final double MODEL_OVERHANG =
            MODEL_SIZE_PIXELS / PIXELS_PER_BLOCK * 0.5D * Math.sqrt(3.0D)
                    - BASE_ENTITY_SIZE * 0.5D
                    + (MODEL_SIZE_PIXELS / PIXELS_PER_BLOCK - BASE_ENTITY_SIZE) * 0.5D;

    



    private static final boolean PORTAL_MOD_PRESENT = detectPortalMod();

    private static boolean detectPortalMod() {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded("portalmod");
    }

    














    public static boolean usesModelCollision(Entity entity) {
        return PORTAL_MOD_PRESENT && entity != null && entity.isAlive()
                && ModGameRules.isCubePhysicsEnabled(entity.level) && Bridge.isCube(entity);
    }

    








    public static OrientedBox modelBox(Entity cube) {
        return modelBox(cube, GravityPhysicsHelper.getDirection(cube));
    }

    







    public static OrientedBox modelBox(Entity cube, EnumGravityDirection direction) {
        CubeRigidBody body = CubePhysics.orientationOf(cube);
        if (body == null) {
            return modelBox(cube, direction, facingOf(cube));
        }
        return OrientedBox.of(centreOf(cube, direction), body.axisX(), body.axisY(), body.axisZ(),
                modelHalfSide(cube), modelHalfHeight(cube), modelHalfSide(cube));
    }

    



    private static float facingOf(Entity cube) {
        return cube instanceof LivingEntity ? ((LivingEntity) cube).yBodyRot : cube.yRot;
    }

    
    public static double modelHalfSide(Entity cube) {
        return MODEL_SIZE_PIXELS / PIXELS_PER_BLOCK
                * (Math.max(MINIMUM_SIZE, cube.getBbWidth()) / BASE_ENTITY_SIZE) * 0.5D;
    }

    
    public static double modelHalfHeight(Entity cube) {
        return MODEL_SIZE_PIXELS / PIXELS_PER_BLOCK
                * (Math.max(MINIMUM_SIZE, cube.getBbHeight()) / BASE_ENTITY_SIZE) * 0.5D;
    }

    







    public static Vector3d centreOf(Entity cube, EnumGravityDirection direction) {
        return cube.position()
                .add(direction.adjustVec(new Vector3d(0.0D, modelHalfHeight(cube), 0.0D)));
    }

    
    private static OrientedBox modelBox(Entity cube, EnumGravityDirection direction, float facing) {
        double halfSide = modelHalfSide(cube);
        double halfHeight = modelHalfHeight(cube);

        double radians = Math.toRadians(facing);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        Vector3d up = direction.adjustVec(new Vector3d(0.0D, 1.0D, 0.0D));
        Vector3d turnedX = direction.adjustVec(new Vector3d(cos, 0.0D, sin));
        Vector3d turnedZ = direction.adjustVec(new Vector3d(-sin, 0.0D, cos));
        Vector3d center = cube.position().add(up.scale(halfHeight));
        return OrientedBox.of(center, turnedX, up, turnedZ, halfSide, halfHeight, halfSide);
    }

    














    public static List<OrientedBox> solidCubes(Entity entity, Vector3d movement, EnumGravityDirection direction) {
        if (!PORTAL_MOD_PRESENT) {
            return Collections.emptyList();
        }
        AxisAlignedBB region = entity.getBoundingBox().expandTowards(movement);
        if (entity.maxUpStep > 0.0F) {
            region = region.expandTowards(direction.adjustVec(new Vector3d(0.0D, entity.maxUpStep, 0.0D)));
        }
        return solidCubes(entity, region.inflate(1.0E-7D));
    }

    
    public static List<OrientedBox> solidCubes(Entity entity, AxisAlignedBB region) {
        if (!PORTAL_MOD_PRESENT) {
            return Collections.emptyList();
        }
        List<Entity> candidates = entity.level.getEntities(entity, region.inflate(MODEL_OVERHANG),
                candidate -> usesModelCollision(candidate) && !entity.hasPassenger(candidate));
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        List<OrientedBox> cubes = new ArrayList<>(candidates.size());
        for (Entity candidate : candidates) {
            OrientedBox model = modelBox(candidate);
            if (model.getBounds().intersects(region)) {
                cubes.add(model);
            }
        }
        return cubes;
    }

    













    public static Vector3d escapeFromCubes(List<OrientedBox> cubes, AxisAlignedBB box) {
        Vector3d deepest = Vector3d.ZERO;
        double deepestSqr = 0.0D;
        for (int i = 0; i < cubes.size(); i++) {
            Vector3d escape = cubes.get(i).escapeFrom(box);
            double lengthSqr = escape.lengthSqr();
            if (lengthSqr > deepestSqr) {
                deepestSqr = lengthSqr;
                deepest = escape;
            }
        }
        return deepest;
    }

    





















    public static Vector3d collideAgainstCubes(Entity entity, Vector3d movement, AxisAlignedBB box,
                                               ISelectionContext context, ReuseableStream<VoxelShape> potentialHits,
                                               List<OrientedBox> cubes) {
        if (cubes.isEmpty()) {
            return Entity.collideBoundingBoxHeuristically(entity, movement, box, entity.level, context, potentialHits);
        }
        ReuseableStream<VoxelShape> shapes = new ReuseableStream<>(Stream.concat(potentialHits.getStream(),
                entity.level.getBlockCollisions(entity, box.expandTowards(movement))));

        double x = movement.x;
        double y = movement.y;
        double z = movement.z;
        AxisAlignedBB moving = box;

        if (y != 0.0D) {
            y = clipAgainst(shapes, cubes, moving, Direction.Axis.Y, y);
            if (y != 0.0D) {
                moving = moving.move(0.0D, y, 0.0D);
            }
        }

        boolean zFirst = Math.abs(x) < Math.abs(z);
        if (zFirst && z != 0.0D) {
            z = clipAgainst(shapes, cubes, moving, Direction.Axis.Z, z);
            if (z != 0.0D) {
                moving = moving.move(0.0D, 0.0D, z);
            }
        }

        if (x != 0.0D) {
            x = clipAgainst(shapes, cubes, moving, Direction.Axis.X, x);
            if (!zFirst && x != 0.0D) {
                moving = moving.move(x, 0.0D, 0.0D);
            }
        }

        if (!zFirst && z != 0.0D) {
            z = clipAgainst(shapes, cubes, moving, Direction.Axis.Z, z);
        }

        return new Vector3d(x, y, z);
    }

    
    private static double clipAgainst(ReuseableStream<VoxelShape> shapes, List<OrientedBox> cubes, AxisAlignedBB box,
                                      Direction.Axis axis, double delta) {
        double allowed = VoxelShapes.collide(axis, box, shapes.getStream(), delta);
        for (int i = 0; i < cubes.size() && allowed != 0.0D; i++) {
            allowed = cubes.get(i).clipMovementOf(box, axis, allowed);
        }
        return allowed;
    }

    







    public static boolean noCollision(Entity entity, AxisAlignedBB box) {
        return entity.level.noCollision(entity, box) && !intersectsAnyModelCube(entity, box);
    }

    








    public static boolean intersectsAnyModelCube(Entity entity, AxisAlignedBB box) {
        if (!PORTAL_MOD_PRESENT) {
            return false;
        }
        for (Entity cube : entity.level.getEntities(entity, box.inflate(MODEL_OVERHANG),
                candidate -> usesModelCollision(candidate) && !candidate.isPassenger())) {
            if (modelBox(cube).intersects(box)) {
                return true;
            }
        }
        return false;
    }

    
    private static final float ROTATION_EPSILON = 1.0E-4F;

    








    private static final double ROTATION_SAMPLE_DEGREES = 5.0D;

    



    private static final int ROTATION_SEARCH_STEPS = 8;

    





















    public static void clampFacing(Entity cube) {
        if (!usesModelCollision(cube) || !(cube instanceof LivingEntity)) {
            return;
        }
        LivingEntity living = (LivingEntity) cube;
        float previous = living.yBodyRotO;
        double turn = MathHelper.wrapDegrees(living.yBodyRot - previous);
        if (Math.abs(turn) < ROTATION_EPSILON) {
            return;
        }

        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(cube);
        List<AxisAlignedBB> obstacles = rotationObstacles(cube, direction, living.yBodyRot);
        if (obstacles.isEmpty() || !isFacingFree(cube, direction, previous, obstacles)) {
            return;
        }

        int samples = Math.max(1, (int) Math.ceil(Math.abs(turn) / ROTATION_SAMPLE_DEGREES));
        double free = 0.0D;
        double blocked = -1.0D;
        for (int sample = 1; sample <= samples; sample++) {
            double fraction = (double) sample / samples;
            if (isFacingFree(cube, direction, (float) (previous + turn * fraction), obstacles)) {
                free = fraction;
            }
            else {
                blocked = fraction;
                break;
            }
        }
        if (blocked < 0.0D) {
            
            return;
        }

        for (int step = 0; step < ROTATION_SEARCH_STEPS; step++) {
            double middle = (free + blocked) * 0.5D;
            if (isFacingFree(cube, direction, (float) (previous + turn * middle), obstacles)) {
                free = middle;
            }
            else {
                blocked = middle;
            }
        }

        float clamped = (float) (previous + turn * free);
        living.yRot = clamped;
        living.yBodyRot = clamped;
        living.yHeadRot = clamped;
    }

    





    private static List<AxisAlignedBB> rotationObstacles(Entity cube, EnumGravityDirection direction, float facing) {
        double sweep = modelHalfSide(cube) * (Math.sqrt(2.0D) - 1.0D) + 1.0E-7D;
        AxisAlignedBB region = modelBox(cube, direction, facing).getBounds().inflate(sweep);
        List<AxisAlignedBB> obstacles = new ArrayList<>();
        cube.level.getBlockCollisions(cube, region).forEach(shape -> collect(obstacles, region, shape));
        collectOtherCubes(cube, region, obstacles);
        return obstacles;
    }

    private static boolean isFacingFree(Entity cube, EnumGravityDirection direction, float facing,
                                        List<AxisAlignedBB> obstacles) {
        OrientedBox box = modelBox(cube, direction, facing);
        for (AxisAlignedBB obstacle : obstacles) {
            if (box.intersects(obstacle)) {
                return false;
            }
        }
        return true;
    }

    


















    public static Vector3d collide(Entity cube, Vector3d movement, EnumGravityDirection direction) {
        OrientedBox model = modelBox(cube, direction);
        List<AxisAlignedBB> obstacles = collectObstacles(cube, model, movement, direction);

        Vector3d base = movement.lengthSqr() == 0.0D ? movement : solve(model, movement, obstacles);
        if (cube.maxUpStep <= 0.0F) {
            return base;
        }

        
        
        
        
        Vector3d relIntended = direction.inverseAdjustVec(movement);
        Vector3d relBase = direction.inverseAdjustVec(base);
        boolean blockedHorizontally = relIntended.x != relBase.x || relIntended.z != relBase.z;
        boolean blockedVertically = relIntended.y != relBase.y;
        boolean groundedOrFalling = cube.isOnGround() || (blockedVertically && relIntended.y < 0.0D);
        if (!groundedOrFalling || !blockedHorizontally) {
            return base;
        }

        Vector3d horizontalMove = direction.adjustVec(new Vector3d(relIntended.x, 0.0D, relIntended.z));
        Vector3d upStep = direction.adjustVec(new Vector3d(0.0D, cube.maxUpStep, 0.0D));
        Vector3d lift = solve(model, upStep, obstacles);
        Vector3d stepped = solve(model.move(lift), horizontalMove, obstacles).add(lift);
        if (relativeHorizontalDistanceSqr(direction, stepped) <= relativeHorizontalDistanceSqr(direction, base)) {
            return base;
        }
        
        double settle = -direction.inverseAdjustVec(stepped).y + relIntended.y;
        Vector3d settleMove = direction.adjustVec(new Vector3d(0.0D, settle, 0.0D));
        return stepped.add(solve(model.move(stepped), settleMove, obstacles));
    }

    private static double relativeHorizontalDistanceSqr(EnumGravityDirection direction, Vector3d worldVec) {
        Vector3d relative = direction.inverseAdjustVec(worldVec);
        return relative.x * relative.x + relative.z * relative.z;
    }

    






    private static List<AxisAlignedBB> collectObstacles(Entity cube, OrientedBox model, Vector3d movement,
                                                        EnumGravityDirection direction) {
        AxisAlignedBB bounds = model.getBounds();
        AxisAlignedBB query = bounds.expandTowards(movement);
        if (cube.maxUpStep > 0.0F) {
            
            
            
            query = query.expandTowards(direction.adjustVec(new Vector3d(0.0D, cube.maxUpStep, 0.0D)));
        }
        query = query.inflate(1.0E-7D);
        List<AxisAlignedBB> obstacles = new ArrayList<>();
        AxisAlignedBB region = query;
        cube.level.getBlockCollisions(cube, query).forEach(shape -> collect(obstacles, region, shape));
        cube.level.getEntityCollisions(cube, query, entity -> true).forEach(shape -> collect(obstacles, region, shape));
        collectOtherCubes(cube, region, obstacles);
        
        
        
        
        PortalModCollisionHelper.additionalCollisions(cube, movement, direction)
                .forEach(shape -> collect(obstacles, region, shape));

        
        
        VoxelShape border = cube.level.getWorldBorder().getCollisionShape();
        if (!VoxelShapes.joinIsNotEmpty(border, VoxelShapes.create(bounds.deflate(1.0E-7D)), IBooleanFunction.AND)) {
            collect(obstacles, region, border);
        }
        return obstacles;
    }

    
















    private static void collectOtherCubes(Entity cube, AxisAlignedBB region, List<AxisAlignedBB> obstacles) {
        for (Entity other : cube.level.getEntities(cube, region.inflate(MODEL_OVERHANG), Bridge::isCube)) {
            if (usesModelCollision(other)) {
                for (AxisAlignedBB box : modelBox(other).toAxisAlignedBoxes()) {
                    clampInto(obstacles, region, box);
                }
            }
            else {
                clampInto(obstacles, region, other.getBoundingBox());
            }
        }
    }

    


















    private static void collect(List<AxisAlignedBB> obstacles, AxisAlignedBB region, VoxelShape shape) {
        for (AxisAlignedBB box : shape.toAabbs()) {
            clampInto(obstacles, region, box);
        }
    }

    
    private static void clampInto(List<AxisAlignedBB> obstacles, AxisAlignedBB region, AxisAlignedBB box) {
        double minX = Math.max(box.minX, region.minX);
        double minY = Math.max(box.minY, region.minY);
        double minZ = Math.max(box.minZ, region.minZ);
        double maxX = Math.min(box.maxX, region.maxX);
        double maxY = Math.min(box.maxY, region.maxY);
        double maxZ = Math.min(box.maxZ, region.maxZ);
        
        
        if (minX < maxX && minY < maxY && minZ < maxZ) {
            obstacles.add(new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
        }
    }

    







    private static Vector3d solve(OrientedBox box, Vector3d movement, List<AxisAlignedBB> obstacles) {
        double x = movement.x;
        double y = movement.y;
        double z = movement.z;
        OrientedBox moving = box;

        if (y != 0.0D) {
            y = clip(moving, obstacles, Direction.Axis.Y, y);
            if (y != 0.0D) {
                moving = moving.move(new Vector3d(0.0D, y, 0.0D));
            }
        }

        boolean zFirst = Math.abs(x) < Math.abs(z);
        if (zFirst && z != 0.0D) {
            z = clip(moving, obstacles, Direction.Axis.Z, z);
            if (z != 0.0D) {
                moving = moving.move(new Vector3d(0.0D, 0.0D, z));
            }
        }

        if (x != 0.0D) {
            x = clip(moving, obstacles, Direction.Axis.X, x);
            if (!zFirst && x != 0.0D) {
                moving = moving.move(new Vector3d(x, 0.0D, 0.0D));
            }
        }

        if (!zFirst && z != 0.0D) {
            z = clip(moving, obstacles, Direction.Axis.Z, z);
        }

        return new Vector3d(x, y, z);
    }

    
    private static double clip(OrientedBox box, List<AxisAlignedBB> obstacles, Direction.Axis axis, double delta) {
        double allowed = delta;
        for (AxisAlignedBB obstacle : obstacles) {
            allowed = box.clipMovement(obstacle, axis, allowed);
            if (allowed == 0.0D) {
                return 0.0D;
            }
        }
        return allowed;
    }

    



    private static final class Bridge {

        private Bridge() {}

        static boolean isCube(Entity entity) {
            
            
            
            return Cube.class.isInstance(entity);
        }
    }
}
