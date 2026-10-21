package uk.co.mysterymayhem.gravitymod.mixin.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.portalmod.core.math.Mat4;
import net.portalmod.core.math.Vec3;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;












































@Pseudo
@Mixin(targets = "net.portalmod.core.event.ClientEvents", remap = false)
public abstract class PortalModGrabReachMixin {

    
    @Unique
    private static final double GRAVITYMOD$ANCHOR_NUDGE = 0.2D;

    




    @Unique
    private static EnumGravityDirection gravitymod$grabberDirection = EnumGravityDirection.DOWN;

    
    @Unique
    private static Vector3d gravitymod$bodyAxisStart = Vector3d.ZERO;

    
    @Unique
    private static Vector3d gravitymod$bodyAxisEnd = Vector3d.ZERO;

    
    @Unique
    private static Vector3d gravitymod$targetPoint = Vector3d.ZERO;

    










    @Dynamic("PortalMod: ClientEvents#handleInteractKey")
    @Redirect(
            method = "handleInteractKey",
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;position()Lnet/minecraft/util/math/vector/Vector3d;"
            )
    )
    private static Vector3d gravitymod$reachOrigin(PlayerEntity grabber) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(grabber);
        Vector3d position = grabber.position();
        double[] up = direction.adjustXYZValues(0.0D, 1.0D, 0.0D);

        
        Vector3d axisStart = new Vector3d(
                position.x + up[0] * GRAVITYMOD$ANCHOR_NUDGE,
                position.y + up[1] * GRAVITYMOD$ANCHOR_NUDGE,
                position.z + up[2] * GRAVITYMOD$ANCHOR_NUDGE);

        gravitymod$grabberDirection = direction;
        gravitymod$bodyAxisStart = axisStart;
        
        
        gravitymod$bodyAxisEnd = GravityPhysicsHelper.getRelativeEyePosition(grabber);

        if (direction == EnumGravityDirection.DOWN) {
            return position;
        }
        return new Vector3d(
                axisStart.x,
                axisStart.y - GRAVITYMOD$ANCHOR_NUDGE,
                axisStart.z);
    }

    












    @Dynamic("PortalMod: ClientEvents#handleInteractKey")
    @Redirect(
            method = "handleInteractKey",
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;position()Lnet/minecraft/util/math/vector/Vector3d;"
            )
    )
    private static Vector3d gravitymod$grabTargetPoint(Entity target) {
        PlayerEntity grabber = Minecraft.getInstance().player;
        EnumGravityDirection direction = grabber == null
                ? EnumGravityDirection.DOWN : GravityPhysicsHelper.getDirection(grabber);
        Vector3d point;
        if (direction == EnumGravityDirection.DOWN && GravityPhysicsHelper.getDirection(target) == EnumGravityDirection.DOWN) {
            point = target.position();
        } else {
            Vector3d centre = target.getBoundingBox().getCenter();
            double[] up = direction.adjustXYZValues(0.0D, target.getBbHeight() / 2.0D, 0.0D);
            point = new Vector3d(centre.x - up[0], centre.y - up[1], centre.z - up[2]);
        }
        gravitymod$targetPoint = point;
        return point;
    }

    


















    @Dynamic("PortalMod: ClientEvents#handleInteractKey")
    @Redirect(
            method = "handleInteractKey",
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/portalmod/core/math/Vec3;transform(Lnet/portalmod/core/math/Mat4;)Lnet/portalmod/core/math/Vec3;",
                    ordinal = 1,
                    remap = false
            )
    )
    private static Vec3 gravitymod$capsuleReachOrigin(Vec3 reachOrigin, Mat4 matrix) {
        if (gravitymod$grabberDirection == EnumGravityDirection.DOWN) {
            return reachOrigin.transform(matrix);
        }

        Vector3d startWorld = gravitymod$bodyAxisStart;
        Vector3d endWorld = gravitymod$bodyAxisEnd;
        Vec3 start = new Vec3(startWorld.x, startWorld.y, startWorld.z).transform(matrix);
        Vec3 end = new Vec3(endWorld.x, endWorld.y, endWorld.z).transform(matrix);

        double axisX = end.x - start.x;
        double axisY = end.y - start.y;
        double axisZ = end.z - start.z;
        double axisLengthSq = axisX * axisX + axisY * axisY + axisZ * axisZ;

        double along = 0.0D;
        if (axisLengthSq > 1.0E-9D) {
            Vector3d target = gravitymod$targetPoint;
            double projection = (target.x - start.x) * axisX
                    + (target.y - start.y) * axisY
                    + (target.z - start.z) * axisZ;
            along = MathHelper.clamp(projection / axisLengthSq, 0.0D, 1.0D);
        }

        
        
        reachOrigin.x = start.x + axisX * along;
        reachOrigin.y = start.y + axisY * along;
        reachOrigin.z = start.z + axisZ * along;
        return reachOrigin;
    }
}
