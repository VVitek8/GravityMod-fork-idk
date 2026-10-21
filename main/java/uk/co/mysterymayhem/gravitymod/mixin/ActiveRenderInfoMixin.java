package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityRenderHelper;







@Mixin(ActiveRenderInfo.class)
public abstract class ActiveRenderInfoMixin {

    





    @Unique
    private static final double GRAVITYMOD$SURFACE_CLEARANCE = 0.05D;

    @Shadow
    private float eyeHeight;

    @Shadow
    private float eyeHeightOld;

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    










    @Inject(
            method = "setup",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;setPosition(DDD)V",
                    shift = At.Shift.AFTER
            )
    )
    private void gravitymod$fixEyeOffset(IBlockReader level, Entity entity, boolean thirdPerson, boolean thirdPersonReverse, float partialTicks, CallbackInfo ci) {
        if (GravityRenderHelper.isIdentity(entity, partialTicks)) {
            return;
        }
        double baseX = MathHelper.lerp((double) partialTicks, entity.xo, entity.getX());
        double baseY = MathHelper.lerp((double) partialTicks, entity.yo, entity.getY());
        double baseZ = MathHelper.lerp((double) partialTicks, entity.zo, entity.getZ());
        double eyeOffset = MathHelper.lerp(partialTicks, this.eyeHeightOld, this.eyeHeight);
        
        
        double[] relativeUp = GravityRenderHelper.getRelativeUp(entity, partialTicks);
        this.setPosition(
                baseX + relativeUp[0] * eyeOffset,
                baseY + relativeUp[1] * eyeOffset,
                baseZ + relativeUp[2] * eyeOffset
        );
    }

    
























    @Inject(method = "getMaxZoom", at = @At("RETURN"), cancellable = true)
    private void gravitymod$clearCollidedSurface(double startingDistance, CallbackInfoReturnable<Double> cir) {
        double zoom = cir.getReturnValue();
        if (zoom >= startingDistance) {
            return;
        }
        cir.setReturnValue(Math.max(0.0D, zoom - GRAVITYMOD$SURFACE_CLEARANCE));
    }

    @Inject(
            method = "setRotation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/vector/Quaternion;mul(Lnet/minecraft/util/math/vector/Quaternion;)V",
                    ordinal = 1,
                    shift = At.Shift.AFTER
            )
    )
    private void gravitymod$rollCamera(float pitch, float yaw, CallbackInfo ci) {
        ActiveRenderInfo self = (ActiveRenderInfo) (Object) this;
        Entity entity = self.getEntity();
        if (entity == null) {
            return;
        }
        
        
        float partialTicks = Minecraft.getInstance().getFrameTime();
        if (GravityRenderHelper.isIdentity(entity, partialTicks)) {
            return;
        }
        
        
        
        
        
        
        
        
        
        
        
        Quaternion roll = GravityRenderHelper.getRotation(entity, partialTicks);
        Quaternion rotation = self.rotation();
        roll.mul(rotation.copy());
        rotation.set(roll.i(), roll.j(), roll.k(), roll.r());
    }
}
