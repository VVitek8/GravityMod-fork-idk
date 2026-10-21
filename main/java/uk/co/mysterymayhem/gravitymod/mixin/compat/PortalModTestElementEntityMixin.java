package uk.co.mysterymayhem.gravitymod.mixin.compat;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;
import uk.co.mysterymayhem.gravitymod.util.GravityRenderHelper;





























































@Pseudo
@Mixin(targets = "net.portalmod.common.entities.TestElementEntity", remap = false)
public abstract class PortalModTestElementEntityMixin {

    
















    @Dynamic("PortalMod: TestElementEntity#rideTick")
    @ModifyArgs(
            method = {"rideTick", "func_70098_U"},
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/vector/Vector3d;add(DDD)Lnet/minecraft/util/math/vector/Vector3d;"
            )
    )
    private void gravitymod$reorientHoldOffset(Args args) {
        EnumGravityDirection cubeDirection = GravityPhysicsHelper.getDirection((Entity) (Object) this);
        if (cubeDirection == EnumGravityDirection.DOWN) {
            return;
        }
        double[] world = cubeDirection.adjustXYZValues(
                (Double) args.get(0),
                (Double) args.get(1),
                (Double) args.get(2)
        );
        args.set(0, world[0]);
        args.set(1, world[1]);
        args.set(2, world[2]);
    }

    









    @Dynamic("PortalMod: TestElementEntity#rideTick")
    @ModifyArgs(
            method = {"rideTick", "func_70098_U"},
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/portalmod/core/math/Vec3;sub(DDD)Lnet/portalmod/core/math/Vec3;",
                    ordinal = 0,
                    remap = false
            )
    )
    private void gravitymod$reorientEyeHeightRemoval(Args args) {
        gravitymod$reorientOffset(args);
    }

    
    @Dynamic("PortalMod: TestElementEntity#rideTick")
    @ModifyArgs(
            method = {"rideTick", "func_70098_U"},
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/portalmod/core/math/Vec3;add(DDD)Lnet/portalmod/core/math/Vec3;",
                    remap = false
            )
    )
    private void gravitymod$reorientOldEyeHeight(Args args) {
        gravitymod$reorientOffset(args);
    }

    

















    @Dynamic("PortalMod: TestElementEntity#rideTick")
    @Redirect(
            method = {"rideTick", "func_70098_U"},
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/vector/Vector3f;rotationDegrees(F)Lnet/minecraft/util/math/vector/Quaternion;",
                    ordinal = 1
            )
    )
    private Quaternion gravitymod$reorientRidingVersor(Vector3f axis, float degrees) {
        Quaternion yaw = axis.rotationDegrees(degrees);
        EnumGravityDirection direction = gravitymod$holderDirection();
        if (direction == EnumGravityDirection.DOWN) {
            
            return yaw;
        }
        Quaternion rotation = GravityRenderHelper.getDirectionRotation(direction);
        
        rotation.mul(yaw);
        return rotation;
    }

    



    @Unique
    private static final double GRAVITYMOD$FACING_EPSILON = 1.0E-4D;

    
    @Unique
    private float gravitymod$lastFacingYaw = Float.NaN;

    































    @Dynamic("PortalMod: TestElementEntity#rideTick")
    @Redirect(
            method = {"rideTick", "func_70098_U"},
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;getYHeadRot()F"
            )
    )
    private float gravitymod$heldFacingYaw(Entity holder) {
        float headYaw = holder.getYHeadRot();
        EnumGravityDirection holderDirection = GravityPhysicsHelper.getDirection(holder);
        EnumGravityDirection cubeDirection = GravityPhysicsHelper.getDirection((Entity) (Object) this);
        if (holderDirection == cubeDirection) {
            
            
            return headYaw;
        }

        double radians = Math.toRadians(headYaw);
        Vector3d holderLook = new Vector3d(-Math.sin(radians), 0.0D, Math.cos(radians));
        Vector3d cubeLook = cubeDirection.inverseAdjustVec(holderDirection.adjustVec(holderLook));

        if (Math.abs(cubeLook.x) < GRAVITYMOD$FACING_EPSILON && Math.abs(cubeLook.z) < GRAVITYMOD$FACING_EPSILON) {
            return Float.isNaN(gravitymod$lastFacingYaw)
                    ? ((Entity) (Object) this).yRot : gravitymod$lastFacingYaw;
        }

        float yaw = (float) -Math.toDegrees(Math.atan2(cubeLook.x, cubeLook.z));
        gravitymod$lastFacingYaw = yaw;
        return yaw;
    }

    







    private void gravitymod$reorientOffset(Args args) {
        EnumGravityDirection direction = gravitymod$holderDirection();
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        double[] world = direction.adjustXYZValues(
                (Double) args.get(0),
                (Double) args.get(1),
                (Double) args.get(2)
        );
        args.set(0, world[0]);
        args.set(1, world[1]);
        args.set(2, world[2]);
    }

    






    private EnumGravityDirection gravitymod$holderDirection() {
        Entity holder = ((Entity) (Object) this).getVehicle();
        if (holder == null) {
            return EnumGravityDirection.DOWN;
        }
        return GravityPhysicsHelper.getDirection(holder);
    }
}
