package uk.co.mysterymayhem.gravitymod.mixin.compat;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;




















@Pseudo
@Mixin(targets = "net.portalmod.common.sorted.turret.TurretEntity", remap = false)
public abstract class PortalModTurretEntityMixin {

    


















    @Dynamic("PortalMod: TurretEntity#tick")
    @ModifyArgs(
            method = {"tick", "func_70071_h_"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/vector/Vector3d;<init>(DDD)V",
                    remap = false
            )
    )
    private void gravitymod$measureTipMotionInGravityFrame(Args args) {
        Entity self = (Entity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        Vector3d relative = direction.inverseAdjustVec(self.getDeltaMovement());
        args.set(0, relative.x);
        
        args.set(1, 0.0D);
        args.set(2, relative.z);
    }

    












    @Dynamic("PortalMod: TurretEntity#tick")
    @Redirect(
            method = {"tick", "func_70071_h_"},
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/vector/Vector3d;yRot(F)Lnet/minecraft/util/math/vector/Vector3d;"
            )
    )
    private Vector3d gravitymod$turnSidewaysAboutRelativeUp(Vector3d worldLook, float degrees) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection((Entity) (Object) this);
        if (direction == EnumGravityDirection.DOWN) {
            return worldLook.yRot(degrees);
        }
        return direction.adjustVec(direction.inverseAdjustVec(worldLook).yRot(degrees));
    }

    













    @Dynamic("PortalMod: TurretEntity#turretView")
    @Redirect(
            method = "turretView",
            at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/util/math/vector/Vector3d;", ordinal = 0)
    )
    private Vector3d gravitymod$traceFromRealEye(double x, double y, double z) {
        return GravityPhysicsHelper.getRelativeEyePosition((Entity) (Object) this);
    }

    




    @Dynamic("PortalMod: TurretEntity#turretView")
    @Redirect(
            method = "turretView",
            at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/util/math/vector/Vector3d;", ordinal = 1)
    )
    private Vector3d gravitymod$traceToRealEye(double x, double y, double z, Entity targetEntity) {
        return GravityPhysicsHelper.getRelativeEyePosition(targetEntity);
    }
}
