package uk.co.mysterymayhem.gravitymod.mixin.compat;

import net.minecraft.entity.Entity;
import net.portalmod.common.sorted.portal.OrthonormalBasis;
import net.portalmod.common.sorted.portal.PortalEntity;
import net.portalmod.core.math.Mat4;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.util.PortalModCameraHelper;




























@Pseudo
@Mixin(targets = "net.portalmod.common.sorted.portal.CameraRotator", remap = false)
public abstract class PortalModCameraRotatorMixin {

    








    @Dynamic("PortalMod: CameraRotator#rotate")
    @Redirect(
            method = "rotate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/portalmod/common/sorted/portal/OrthonormalBasis;getChangeOfBasisMatrix"
                            + "(Lnet/portalmod/common/sorted/portal/OrthonormalBasis;)Lnet/portalmod/core/math/Mat4;"
            )
    )
    private static Mat4 gravitymod$turnInTravellerFrame(
            OrthonormalBasis source, OrthonormalBasis destination,
            Entity entity, PortalEntity portal, PortalEntity targetPortal) {
        return PortalModCameraHelper.inViewerFrame(source.getChangeOfBasisMatrix(destination), entity, 1.0F);
    }
}
