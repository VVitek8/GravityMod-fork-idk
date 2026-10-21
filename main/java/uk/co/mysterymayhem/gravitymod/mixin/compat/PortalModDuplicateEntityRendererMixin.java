package uk.co.mysterymayhem.gravitymod.mixin.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import uk.co.mysterymayhem.gravitymod.mixin.MinecraftAccessor;
import uk.co.mysterymayhem.gravitymod.util.GravityRenderHelper;
































@Pseudo
@Mixin(targets = "net.portalmod.common.sorted.portal.DuplicateEntityRenderer", remap = false)
public abstract class PortalModDuplicateEntityRendererMixin {

    
    @Dynamic("PortalMod: DuplicateEntityRenderer#shouldRender")
    @ModifyArgs(
            method = "shouldRender",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/portalmod/core/math/Vec3;add(DDD)Lnet/portalmod/core/math/Vec3;"
            )
    )
    private static void gravitymod$reorientEyeOffset(Args args) {
        gravitymod$reorientOntoRelativeUp(args);
    }

    





    @Dynamic("PortalMod: DuplicateEntityRenderer#shouldRender")
    @ModifyArgs(
            method = "shouldRender",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/portalmod/core/math/Vec3;sub(DDD)Lnet/portalmod/core/math/Vec3;"
            )
    )
    private static void gravitymod$reorientBaseProbeOffset(Args args) {
        gravitymod$reorientOntoRelativeUp(args);
    }

    







    private static void gravitymod$reorientOntoRelativeUp(Args args) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity cameraEntity = minecraft.cameraEntity;
        if (cameraEntity == null) {
            return;
        }
        
        
        
        float partialTicks = minecraft.isPaused()
                ? ((MinecraftAccessor) minecraft).gravitymod$getPausePartialTick()
                : minecraft.getFrameTime();
        if (GravityRenderHelper.isIdentity(cameraEntity, partialTicks)) {
            
            
            return;
        }
        double eyeOffset = (Double) args.get(1);
        
        
        double[] relativeUp = GravityRenderHelper.getRelativeUp(cameraEntity, partialTicks);
        args.set(0, relativeUp[0] * eyeOffset);
        args.set(1, relativeUp[1] * eyeOffset);
        args.set(2, relativeUp[2] * eyeOffset);
    }
}
