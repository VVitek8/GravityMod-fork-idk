package uk.co.mysterymayhem.gravitymod.mixin.compat;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.vector.Quaternion;
import net.portalmod.common.sorted.portal.PortalEntity;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.util.PortalModCameraHelper;
















@Pseudo
@Mixin(targets = "net.portalmod.core.util.RenderUtil", remap = false)
public abstract class PortalModRenderUtilMixin {

    







    @Dynamic("PortalMod: RenderUtil#setupClipPlane")
    @Redirect(
            method = "setupClipPlane",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/matrix/MatrixStack;mulPose(Lnet/minecraft/util/math/vector/Quaternion;)V",
                    ordinal = 2
            ),
            remap = true
    )
    private static void gravitymod$unrollClipPlane(
            MatrixStack stack, Quaternion yaw,
            MatrixStack clipMatrix, PortalEntity portal, ActiveRenderInfo camera, float offset, boolean reversed) {
        stack.mulPose(yaw);
        Quaternion unroll = PortalModCameraHelper.unroll(camera, Minecraft.getInstance().getFrameTime());
        if (unroll != null) {
            stack.mulPose(unroll);
        }
    }
}
