package uk.co.mysterymayhem.gravitymod.mixin.compat;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.vector.Quaternion;
import net.portalmod.common.sorted.portal.PortalEntity;
import net.portalmod.core.math.Mat4;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.util.PortalModCameraHelper;


























@Pseudo
@Mixin(targets = "net.portalmod.common.sorted.portal.PortalRenderer", remap = false)
public abstract class PortalModPortalRendererMixin {

    










    @Dynamic("PortalMod: PortalRenderer#setupCamera")
    @Redirect(
            method = "setupCamera",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/portalmod/common/sorted/portal/PortalEntity;getPortalToPortalRotationMatrix"
                            + "(Lnet/portalmod/common/sorted/portal/PortalEntity;Lnet/portalmod/common/sorted/portal/PortalEntity;)"
                            + "Lnet/portalmod/core/math/Mat4;"
            )
    )
    private Mat4 gravitymod$portalRotationInViewerFrame(
            PortalEntity portal, PortalEntity otherPortal, ActiveRenderInfo camera, PortalEntity portalArg, float partialTicks) {
        return PortalModCameraHelper.inViewerFrame(
                PortalEntity.getPortalToPortalRotationMatrix(portal, otherPortal), camera.getEntity(), partialTicks);
    }

    











    @Dynamic("PortalMod: PortalRenderer#setupMatrixStack")
    @Redirect(
            method = "setupMatrixStack",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/matrix/MatrixStack;mulPose(Lnet/minecraft/util/math/vector/Quaternion;)V",
                    ordinal = 2
            ),
            remap = true
    )
    private void gravitymod$unrollNestedView(MatrixStack stack, Quaternion yaw, MatrixStack matrixStack, ActiveRenderInfo camera) {
        stack.mulPose(yaw);
        gravitymod$appendUnroll(stack, camera);
    }

    








    @Dynamic("PortalMod: PortalRenderer#getViewMatrix")
    @Redirect(
            method = "getViewMatrix",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/matrix/MatrixStack;mulPose(Lnet/minecraft/util/math/vector/Quaternion;)V",
                    ordinal = 2
            ),
            remap = true
    )
    private void gravitymod$unrollCullingView(MatrixStack stack, Quaternion yaw, ActiveRenderInfo camera) {
        stack.mulPose(yaw);
        gravitymod$appendUnroll(stack, camera);
    }

    





    private static void gravitymod$appendUnroll(MatrixStack stack, ActiveRenderInfo camera) {
        Quaternion unroll = PortalModCameraHelper.unroll(camera, Minecraft.getInstance().getFrameTime());
        if (unroll != null) {
            stack.mulPose(unroll);
        }
    }
}
