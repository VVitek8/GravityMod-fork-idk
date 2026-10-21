package uk.co.mysterymayhem.gravitymod.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.capability.GravityAPI;










public final class GravityPhysicsHelper {

    private GravityPhysicsHelper() {}

    public static EnumGravityDirection getDirection(Entity entity) {
        try {
            return GravityAPI.getDirection(entity);
        }
        catch (RuntimeException e) {
            
            
            
            
            return EnumGravityDirection.DOWN;
        }
    }

    



    public static double getStrength(Entity entity) {
        try {
            return GravityAPI.getStrength(entity);
        }
        catch (RuntimeException e) {
            return uk.co.mysterymayhem.gravitymod.capability.IGravityCapability.DEFAULT_STRENGTH;
        }
    }

    


    public static boolean hasDefaultGravity(Entity entity) {
        return getDirection(entity) == EnumGravityDirection.DOWN
                && getStrength(entity) == uk.co.mysterymayhem.gravitymod.capability.IGravityCapability.DEFAULT_STRENGTH;
    }

    public static boolean isAffected(Entity entity) {
        return getDirection(entity) != EnumGravityDirection.DOWN;
    }

    







    public static Vector3d getRelativeEyePosition(Entity entity) {
        EnumGravityDirection direction = getDirection(entity);
        double[] relativeUp = direction.adjustXYZValues(0.0D, 1.0D, 0.0D);
        double eyeHeight = entity.getEyeHeight();
        return new Vector3d(
                entity.getX() + relativeUp[0] * eyeHeight,
                entity.getY() + relativeUp[1] * eyeHeight,
                entity.getZ() + relativeUp[2] * eyeHeight);
    }

    
















    public static Vector3d getRelativeBodyPoint(Entity entity, double widthScaleX, double heightScale, double widthScaleZ) {
        EnumGravityDirection direction = getDirection(entity);
        double width = entity.getBbWidth();
        Vector3d offset = direction.adjustVec(
                new Vector3d(width * widthScaleX, entity.getBbHeight() * heightScale, width * widthScaleZ));
        return entity.position().add(offset);
    }

    




    public static Vector3d getRelativeBodyCenter(Entity entity) {
        return getRelativeBodyPoint(entity, 0.0D, 0.5D, 0.0D);
    }

    











    public static Vector3d getLookAtPosition(Entity target) {
        if (target instanceof LivingEntity) {
            return getRelativeEyePosition(target);
        }
        AxisAlignedBB box = target.getBoundingBox();
        return new Vector3d(target.getX(), (box.minY + box.maxY) / 2.0D, target.getZ());
    }
}
