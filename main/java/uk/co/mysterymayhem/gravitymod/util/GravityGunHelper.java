package uk.co.mysterymayhem.gravitymod.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;

import java.util.List;
import java.util.Random;











public final class GravityGunHelper {

    private GravityGunHelper() {}

    
    public static final double MAX_RANGE = 20.0D;

    




    public static final double PUNT_REACH = 4.0D;

    




    private static final double AIM_COS = 0.978D;

    



    private static final double HOLD_DISTANCE = 1.5D;

    








    private static final double GRAB_EYE_DISTANCE = 2.2D;

    









    private static final double GRAB_HOLD_SLACK = 0.5D;

    
    private static final double MIN_PULL_SPEED = 0.175D;
    private static final double MAX_PULL_SPEED = 0.45D;

    
    private static final double PUNT_SPEED = 1.68D;

    
    private static final double PUNT_SPIN = 0.55D;

    private static final Random RANDOM = new Random();

    







    public static Entity findTarget(PlayerEntity player) {
        return findTarget(player, MAX_RANGE);
    }

    
    public static Entity findTarget(PlayerEntity player, double maxRange) {
        if (!PortalModCarryHelper.isAvailable()) {
            return null;
        }
        Vector3d eye = player.getEyePosition(1.0F);
        Vector3d look = player.getLookAngle();
        AxisAlignedBB search = player.getBoundingBox().expandTowards(look.scale(maxRange)).inflate(2.0D);

        Entity best = null;
        double bestCos = AIM_COS;
        List<Entity> candidates = player.level.getEntities(player, search, PortalModCarryHelper::canCarry);
        for (int i = 0; i < candidates.size(); i++) {
            Entity candidate = candidates.get(i);
            Vector3d toCube = centreOf(candidate).subtract(eye);
            double distance = toCube.length();
            if (distance > maxRange || distance < 1.0E-4D) {
                continue;
            }
            double cos = toCube.scale(1.0D / distance).dot(look);
            if (cos > bestCos && hasLineOfSight(player, eye, candidate)) {
                bestCos = cos;
                best = candidate;
            }
        }
        return best;
    }

    











    public static boolean pull(PlayerEntity player, Entity cube) {
        Vector3d centre = centreOf(cube);
        Vector3d eye = player.getEyePosition(1.0F);
        Vector3d toHold = holdPoint(player).subtract(centre);
        double distance = toHold.length();
        double eyeDistance = eye.distanceTo(centre);

        
        
        if (distance <= GRAB_HOLD_SLACK && eyeDistance <= GRAB_EYE_DISTANCE) {
            return true;
        }
        if (distance < 1.0E-4D) {
            return true;
        }

        double nearness = 1.0D - Math.min(1.0D, distance / MAX_RANGE);
        double speed = MIN_PULL_SPEED + nearness * (MAX_PULL_SPEED - MIN_PULL_SPEED);
        
        speed = Math.min(speed, distance);

        cube.setDeltaMovement(toHold.scale(speed / distance));
        cube.hasImpulse = true;
        wake(cube);
        return false;
    }

    
    public static boolean grab(PlayerEntity player, Entity cube) {
        return PortalModCarryHelper.grab(cube, player);
    }

    








    public static void punt(PlayerEntity player, Entity cube) {
        PortalModCarryHelper.release(cube);

        Vector3d look = player.getLookAngle();
        cube.setDeltaMovement(look.scale(PUNT_SPEED).add(player.getDeltaMovement()));
        cube.hasImpulse = true;

        CubeRigidBody body = CubePhysics.bodyOf(cube, true);
        if (body != null) {
            body.addSpin(randomSpin());
        }
    }

    



    public static void drop(PlayerEntity player, Entity cube) {
        PortalModCarryHelper.release(cube);
        cube.setDeltaMovement(player.getDeltaMovement());
        cube.hasImpulse = true;
        wake(cube);
    }

    








    public static boolean canKeepPulling(PlayerEntity player, Entity cube) {
        if (!PortalModCarryHelper.canCarry(cube)) {
            return false;
        }
        Vector3d eye = player.getEyePosition(1.0F);
        return eye.distanceTo(centreOf(cube)) <= MAX_RANGE && hasLineOfSight(player, eye, cube);
    }

    
    private static Vector3d holdPoint(PlayerEntity player) {
        return player.getEyePosition(1.0F).add(player.getLookAngle().scale(HOLD_DISTANCE));
    }

    



    private static Vector3d centreOf(Entity cube) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(cube);
        return CubePhysics.centreOf(cube, direction);
    }

    
    public static boolean hasLineOfSight(PlayerEntity player, Vector3d eye, Entity cube) {
        RayTraceContext context = new RayTraceContext(eye, centreOf(cube), RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE, player);
        BlockRayTraceResult hit = player.level.clip(context);
        return hit.getType() == RayTraceResult.Type.MISS;
    }

    
    private static Vector3d randomSpin() {
        
        Vector3d axis;
        double lengthSqr;
        do {
            axis = new Vector3d(RANDOM.nextDouble() * 2.0D - 1.0D,
                    RANDOM.nextDouble() * 2.0D - 1.0D,
                    RANDOM.nextDouble() * 2.0D - 1.0D);
            lengthSqr = axis.lengthSqr();
        } while (lengthSqr < 1.0E-6D || lengthSqr > 1.0D);

        double magnitude = PUNT_SPIN * (0.5D + RANDOM.nextDouble() * 0.5D);
        return axis.scale(magnitude / Math.sqrt(lengthSqr));
    }

    
    private static void wake(Entity cube) {
        CubeRigidBody body = CubePhysics.bodyOf(cube, false);
        if (body != null) {
            body.wake();
        }
    }
}
