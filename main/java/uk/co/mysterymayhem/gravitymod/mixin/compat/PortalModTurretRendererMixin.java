package uk.co.mysterymayhem.gravitymod.mixin.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import com.mojang.blaze3d.matrix.MatrixStack;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;
import uk.co.mysterymayhem.gravitymod.util.GravityRenderHelper;



































@Pseudo
@Mixin(targets = "net.portalmod.common.sorted.turret.TurretRenderer", remap = false)
public abstract class PortalModTurretRendererMixin {

    
    private static final String RENDER = "render(Lnet/portalmod/common/sorted/turret/TurretEntity;FF"
            + "Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V";

    










    @Unique
    private Quaternion gravitymod$rotation = null;

    









    @Unique
    private EnumGravityDirection gravitymod$direction = null;

    







    @Unique
    private Entity gravitymod$laserTarget = null;

    




    @Dynamic("PortalMod: TurretRenderer#render")
    @Inject(method = RENDER, at = @At("HEAD"))
    private void gravitymod$captureRotation(@Coerce Entity turret, float a, float partialTicks, MatrixStack matrixStack,
                                            IRenderTypeBuffer buffer, int light, CallbackInfo ci) {
        if (GravityRenderHelper.isIdentity(turret, partialTicks)) {
            this.gravitymod$rotation = null;
            this.gravitymod$direction = null;
            return;
        }
        this.gravitymod$rotation = GravityRenderHelper.getRotation(turret, partialTicks);
        this.gravitymod$direction = GravityPhysicsHelper.getDirection(turret);
    }

    








    @Dynamic("PortalMod: TurretRenderer#render")
    @Redirect(
            method = RENDER,
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/vector/Vector3d;multiply(DDD)Lnet/minecraft/util/math/vector/Vector3d;"
            )
    )
    private Vector3d gravitymod$flattenLookInGravityFrame(Vector3d worldLook, double x, double y, double z) {
        EnumGravityDirection direction = this.gravitymod$direction;
        if (direction == null) {
            return worldLook.multiply(x, y, z);
        }
        return direction.inverseAdjustVec(worldLook).multiply(x, y, z);
    }

    
    @Dynamic("PortalMod: TurretRenderer#render")
    @ModifyArgs(method = RENDER, remap = true, at = @At(
            value = "INVOKE", target = "Lcom/mojang/blaze3d/matrix/MatrixStack;translate(DDD)V", ordinal = 0))
    private void gravitymod$reorientTipSwing(Args args) {
        gravitymod$reorientOffset(args);
    }

    
    @Dynamic("PortalMod: TurretRenderer#render")
    @ModifyArgs(method = RENDER, remap = true, at = @At(
            value = "INVOKE", target = "Lcom/mojang/blaze3d/matrix/MatrixStack;translate(DDD)V", ordinal = 1))
    private void gravitymod$reorientTipPivotOut(Args args) {
        gravitymod$reorientOffset(args);
    }

    @Dynamic("PortalMod: TurretRenderer#render")
    @ModifyArgs(method = RENDER, remap = true, at = @At(
            value = "INVOKE", target = "Lcom/mojang/blaze3d/matrix/MatrixStack;translate(DDD)V", ordinal = 2))
    private void gravitymod$reorientTipPivotBack(Args args) {
        gravitymod$reorientOffset(args);
    }

    










    @Dynamic("PortalMod: TurretRenderer#render")
    @Redirect(
            method = RENDER,
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/vector/Vector3f;rotationDegrees(F)Lnet/minecraft/util/math/vector/Quaternion;"
            )
    )
    private Quaternion gravitymod$reorientTipAxis(Vector3f relativeAxis, float degrees) {
        Quaternion rotation = this.gravitymod$rotation;
        if (rotation == null) {
            return relativeAxis.rotationDegrees(degrees);
        }
        Vector3f worldAxis = relativeAxis.copy();
        worldAxis.transform(rotation);
        return worldAxis.rotationDegrees(degrees);
    }

    




    @Dynamic("PortalMod: TurretRenderer#render")
    @ModifyArgs(method = RENDER, at = @At(
            value = "INVOKE", target = "Lnet/minecraft/util/math/vector/Vector3d;<init>(DDD)V", remap = false))
    private void gravitymod$reorientLaserOffsets(Args args) {
        gravitymod$reorientOffset(args);
    }

    








    @Dynamic("PortalMod: TurretRenderer#render")
    @Redirect(
            method = RENDER,
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;getPosition(F)Lnet/minecraft/util/math/vector/Vector3d;"
            )
    )
    private Vector3d gravitymod$captureLaserTarget(LivingEntity target, float partialTicks) {
        this.gravitymod$laserTarget = target;
        return target.getPosition(partialTicks);
    }

    

















    @Dynamic("PortalMod: TurretRenderer#render")
    @Redirect(
            method = RENDER,
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/vector/Vector3d;add(DDD)Lnet/minecraft/util/math/vector/Vector3d;"
            )
    )
    private Vector3d gravitymod$aimAtTargetBodyCentre(Vector3d interpolatedPosition, double x, double y, double z) {
        Entity target = this.gravitymod$laserTarget;
        this.gravitymod$laserTarget = null;
        float partialTicks = Minecraft.getInstance().getFrameTime();
        if (target == null || GravityRenderHelper.isIdentity(target, partialTicks)) {
            return interpolatedPosition.add(x, y, z);
        }
        Vector3f offset = new Vector3f((float) x, (float) y, (float) z);
        offset.transform(GravityRenderHelper.getRotation(target, partialTicks));
        return interpolatedPosition.add(offset.x(), offset.y(), offset.z());
    }

    
    @Unique
    private void gravitymod$reorientOffset(Args args) {
        Quaternion rotation = this.gravitymod$rotation;
        if (rotation == null) {
            return;
        }
        Vector3f offset = new Vector3f(
                ((Double) args.get(0)).floatValue(),
                ((Double) args.get(1)).floatValue(),
                ((Double) args.get(2)).floatValue());
        offset.transform(rotation);
        args.set(0, (double) offset.x());
        args.set(1, (double) offset.y());
        args.set(2, (double) offset.z());
    }
}
