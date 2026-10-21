package uk.co.mysterymayhem.gravitymod.mixin.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.portalmod.common.sorted.portal.OrthonormalBasis;
import net.portalmod.core.math.Mat4;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.util.PortalModCameraHelper;




























@Pseudo
@Mixin(targets = "net.portalmod.common.sorted.portal.PortalEntityClient", remap = false)
public abstract class PortalModPortalEntityClientMixin {

    














    @Dynamic("PortalMod: PortalEntityClient#teleportCamera / #teleportThirdPersonCamera")
    @Redirect(
            method = {"teleportCamera", "teleportThirdPersonCamera"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/portalmod/common/sorted/portal/OrthonormalBasis;transform"
                            + "(Lnet/portalmod/core/math/Mat4;)Lnet/portalmod/common/sorted/portal/OrthonormalBasis;"
            )
    )
    private static OrthonormalBasis gravitymod$reorientRenderCameraInViewerFrame(OrthonormalBasis basis, Mat4 matrix) {
        Entity viewer = Minecraft.getInstance().player;
        Mat4 adjusted = PortalModCameraHelper.inViewerFrame(matrix, viewer, Minecraft.getInstance().getFrameTime());
        return basis.transform(adjusted);
    }
}
