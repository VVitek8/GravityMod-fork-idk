package uk.co.mysterymayhem.gravitymod.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.capability.GravityAPI;
import uk.co.mysterymayhem.gravitymod.capability.IGravityCapability;






















public final class GravityRenderHelper {

    




    private static final Quaternion[] ROTATIONS = buildRotations();

    
    private static final double SLERP_LINEAR_THRESHOLD = 0.9995D;

    private GravityRenderHelper() {}

    private static Quaternion[] buildRotations() {
        EnumGravityDirection[] directions = EnumGravityDirection.values();
        Quaternion[] rotations = new Quaternion[directions.length];
        for (int i = 0; i < directions.length; i++) {
            float[] degrees = directions[i].getCameraRollDegrees();
            Quaternion rotation = Quaternion.ONE.copy();
            if (degrees[0] != 0) {
                rotation.mul(Vector3f.XP.rotationDegrees(degrees[0]));
            }
            if (degrees[1] != 0) {
                rotation.mul(Vector3f.YP.rotationDegrees(degrees[1]));
            }
            if (degrees[2] != 0) {
                rotation.mul(Vector3f.ZP.rotationDegrees(degrees[2]));
            }
            rotations[i] = rotation;
        }
        return rotations;
    }

    



    private static IGravityCapability capabilityOf(Entity entity) {
        try {
            return GravityAPI.getCapability(entity).orElse(null);
        }
        catch (RuntimeException e) {
            
            
            return null;
        }
    }

    




    public static boolean isIdentity(Entity entity, float partialTicks) {
        IGravityCapability capability = capabilityOf(entity);
        return capability == null
                || (capability.getDirection() == EnumGravityDirection.DOWN && capability.getTransitionTicks() <= 0);
    }

    







    public static Quaternion getRotation(Entity entity, float partialTicks) {
        IGravityCapability capability = capabilityOf(entity);
        if (capability == null) {
            return Quaternion.ONE.copy();
        }
        EnumGravityDirection direction = capability.getDirection();
        float progress = progressOf(capability, partialTicks);
        if (progress >= 1.0F) {
            return ROTATIONS[direction.ordinal()].copy();
        }
        return slerp(startFrame(capability.getTransitionFrom(), direction), ROTATIONS[direction.ordinal()], ease(progress));
    }

    










    private static Quaternion startFrame(EnumGravityDirection from, EnumGravityDirection to) {
        float correction = EnumGravityDirection.getYawCorrection(from, to);
        Quaternion start = ROTATIONS[from.ordinal()].copy();
        if (correction != 0.0F) {
            
            
            start.mul(Vector3f.YP.rotationDegrees(correction));
        }
        return start;
    }

    












    public static Quaternion getDirectionRotation(EnumGravityDirection direction) {
        return ROTATIONS[direction.ordinal()].copy();
    }

    





    public static Quaternion getInverseRotation(Entity entity, float partialTicks) {
        Quaternion rotation = getRotation(entity, partialTicks);
        rotation.conj();
        return rotation;
    }

    








    public static double[] getRelativeUp(Entity entity, float partialTicks) {
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        up.transform(getRotation(entity, partialTicks));
        return new double[]{up.x(), up.y(), up.z()};
    }

    


    public static Vector3d inverseRotate(Entity entity, Vector3d worldSpace, float partialTicks) {
        Vector3f vector = new Vector3f((float) worldSpace.x, (float) worldSpace.y, (float) worldSpace.z);
        vector.transform(getInverseRotation(entity, partialTicks));
        return new Vector3d(vector.x(), vector.y(), vector.z());
    }

    



    public static float getProgress(Entity entity, float partialTicks) {
        IGravityCapability capability = capabilityOf(entity);
        return capability == null ? 1.0F : progressOf(capability, partialTicks);
    }

    private static float progressOf(IGravityCapability capability, float partialTicks) {
        int remaining = capability.getTransitionTicks();
        if (remaining <= 0) {
            return 1.0F;
        }
        
        
        float elapsed = IGravityCapability.TRANSITION_TICKS - remaining + partialTicks;
        return clamp(elapsed / IGravityCapability.TRANSITION_TICKS);
    }

    



    private static float ease(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    private static float clamp(float t) {
        return t < 0.0F ? 0.0F : (t > 1.0F ? 1.0F : t);
    }

    







    private static Quaternion slerp(Quaternion a, Quaternion b, float t) {
        double ai = a.i(), aj = a.j(), ak = a.k(), ar = a.r();
        double bi = b.i(), bj = b.j(), bk = b.k(), br = b.r();
        double dot = ai * bi + aj * bj + ak * bk + ar * br;
        if (dot < 0.0D) {
            dot = -dot;
            bi = -bi;
            bj = -bj;
            bk = -bk;
            br = -br;
        }
        double weightA;
        double weightB;
        if (dot > SLERP_LINEAR_THRESHOLD) {
            weightA = 1.0D - t;
            weightB = t;
        }
        else {
            
            
            double theta = Math.acos(dot);
            double sinTheta = Math.sin(theta);
            weightA = Math.sin((1.0D - t) * theta) / sinTheta;
            weightB = Math.sin(t * theta) / sinTheta;
        }
        double ri = ai * weightA + bi * weightB;
        double rj = aj * weightA + bj * weightB;
        double rk = ak * weightA + bk * weightB;
        double rr = ar * weightA + br * weightB;
        
        
        
        
        
        double norm = Math.sqrt(ri * ri + rj * rj + rk * rk + rr * rr);
        if (norm > 1.0E-9D) {
            ri /= norm;
            rj /= norm;
            rk /= norm;
            rr /= norm;
        }
        return new Quaternion((float) ri, (float) rj, (float) rk, (float) rr);
    }
}
