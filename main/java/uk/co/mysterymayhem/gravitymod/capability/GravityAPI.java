package uk.co.mysterymayhem.gravitymod.capability;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.LazyOptional;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.network.GravitySyncPacket;
import uk.co.mysterymayhem.gravitymod.network.PacketHandler;

import javax.annotation.Nonnull;





public final class GravityAPI {

    
    private static final double UNSTICK_STEP = 0.05D;

    




    private static final double UNSTICK_MAX_DISTANCE = 2.5D;

    private GravityAPI() {}

    @Nonnull
    public static LazyOptional<IGravityCapability> getCapability(Entity entity) {
        return entity.getCapability(CapabilityGravityDirection.GRAVITY_CAPABILITY);
    }

    @Nonnull
    public static EnumGravityDirection getDirection(Entity entity) {
        return getCapability(entity).map(IGravityCapability::getDirection).orElse(EnumGravityDirection.DOWN);
    }

    



    public static double getStrength(Entity entity) {
        return getCapability(entity).map(IGravityCapability::getStrength).orElse(IGravityCapability.DEFAULT_STRENGTH);
    }

    



    public static void setDirection(Entity entity, @Nonnull EnumGravityDirection newDirection, boolean noTimeout) {
        EnumGravityDirection oldDirection = getDirection(entity);
        if (oldDirection == newDirection) {
            return;
        }
        getCapability(entity).ifPresent(cap -> {
            
            
            
            AxisAlignedBB oldBox = entity.getBoundingBox();
            cap.setDirection(newDirection, noTimeout);
            
            
            cap.beginTransition(oldDirection);
            applyYawCorrection(entity, oldDirection, newDirection);
            pivotForDirectionChange(entity, oldDirection, newDirection);
            
            
            
            double pivotedX = entity.getX();
            double pivotedY = entity.getY();
            double pivotedZ = entity.getZ();
            
            
            unstick(entity, oldBox, newDirection);
            
            
            
            entity.fallDistance = 0;
            
            
            
            
            
            
            if (entity.level instanceof ServerWorld) {
                syncTo(entity, newDirection, true);
                
                
                
                
                
                
                
                
                boolean moved = entity.getX() != pivotedX || entity.getY() != pivotedY || entity.getZ() != pivotedZ;
                
                if (moved && entity instanceof ServerPlayerEntity) {
                    ServerPlayerEntity player = (ServerPlayerEntity) entity;
                    player.connection.teleport(entity.getX(), entity.getY(), entity.getZ(), player.yRot, player.xRot);
                }
            }
        });
    }

    



    public static void setStrength(Entity entity, double strength) {
        if (!Double.isFinite(strength)) {
            strength = IGravityCapability.DEFAULT_STRENGTH;
        }
        strength = Math.max(IGravityCapability.MIN_STRENGTH, Math.min(IGravityCapability.MAX_STRENGTH, strength));
        final double clampedStrength = strength;
        getCapability(entity).ifPresent(cap -> {
            if (Double.compare(cap.getStrength(), clampedStrength) == 0) {
                return;
            }
            cap.setStrength(clampedStrength);
            if (entity.level instanceof ServerWorld) {
                
                
                syncTo(entity, cap.getDirection(), false);
            }
        });
    }

    








    public static void applyYawCorrection(Entity entity, @Nonnull EnumGravityDirection oldDirection,
                                          @Nonnull EnumGravityDirection newDirection) {
        float correction = EnumGravityDirection.getYawCorrection(oldDirection, newDirection);
        if (correction == 0.0F) {
            return;
        }
        entity.yRot += correction;
        entity.yRotO += correction;
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;
            living.yHeadRot += correction;
            living.yHeadRotO += correction;
            living.yBodyRot += correction;
            living.yBodyRotO += correction;
        }
    }

    
































    public static void pivotForDirectionChange(Entity entity, @Nonnull EnumGravityDirection oldDirection,
                                               @Nonnull EnumGravityDirection newDirection) {
        boolean isReversal = newDirection == oldDirection.getOpposite();
        double distance = (isReversal ? entity.getBbHeight() : entity.getBbWidth()) / 2.0D;
        
        
        double[] fromOld = oldDirection.adjustXYZValues(0.0D, distance, 0.0D);
        double[] fromNew = newDirection.adjustXYZValues(0.0D, distance, 0.0D);
        entity.setPos(
                entity.getX() + fromOld[0] - fromNew[0],
                entity.getY() + fromOld[1] - fromNew[1],
                entity.getZ() + fromOld[2] - fromNew[2]);
    }

    

















    private static void unstick(Entity entity, AxisAlignedBB oldBox, @Nonnull EnumGravityDirection direction) {
        AxisAlignedBB shape = direction.getGravityAdjustedAABB(entity.getBbWidth(), entity.getBbHeight());
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        if (isReachable(entity, oldBox, shape.move(x, y, z))) {
            return;
        }
        double[][] axes = {
                direction.adjustXYZValues(0, 1, 0),
                direction.adjustXYZValues(1, 0, 0),
                direction.adjustXYZValues(-1, 0, 0),
                direction.adjustXYZValues(0, 0, 1),
                direction.adjustXYZValues(0, 0, -1),
                direction.adjustXYZValues(0, -1, 0),
        };
        for (double distance = UNSTICK_STEP; distance <= UNSTICK_MAX_DISTANCE; distance += UNSTICK_STEP) {
            for (double[] axis : axes) {
                double candidateX = x + axis[0] * distance;
                double candidateY = y + axis[1] * distance;
                double candidateZ = z + axis[2] * distance;
                if (isReachable(entity, oldBox, shape.move(candidateX, candidateY, candidateZ))) {
                    entity.setPos(candidateX, candidateY, candidateZ);
                    return;
                }
            }
        }
        
        
    }

    
















    private static boolean isReachable(Entity entity, AxisAlignedBB oldBox, AxisAlignedBB candidate) {
        
        
        
        if (!uk.co.mysterymayhem.gravitymod.util.CubeModelCollisionHelper.noCollision(entity, candidate)) {
            return false;
        }
        double[] oldXs = {oldBox.minX, oldBox.maxX};
        double[] oldYs = {oldBox.minY, oldBox.maxY};
        double[] oldZs = {oldBox.minZ, oldBox.maxZ};
        double[] newXs = {candidate.minX, candidate.maxX};
        double[] newYs = {candidate.minY, candidate.maxY};
        double[] newZs = {candidate.minZ, candidate.maxZ};
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    Vector3d from = new Vector3d(oldXs[i], oldYs[j], oldZs[k]);
                    Vector3d to = new Vector3d(newXs[i], newYs[j], newZs[k]);
                    if (from.equals(to)) {
                        continue;
                    }
                    RayTraceContext context = new RayTraceContext(
                            from, to, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, entity);
                    if (entity.level.clip(context).getType() != RayTraceResult.Type.MISS) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    




    public static void syncTo(Entity entity, @Nonnull EnumGravityDirection direction) {
        syncTo(entity, direction, false);
    }

    










    public static void syncTo(Entity entity, @Nonnull EnumGravityDirection direction, boolean isChange) {
        GravitySyncPacket packet = new GravitySyncPacket(entity.getId(), direction, getStrength(entity), isChange);
        PacketHandler.sendToTracking(entity, packet);
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            PacketHandler.INSTANCE.send(net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }
}
