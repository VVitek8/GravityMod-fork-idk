package uk.co.mysterymayhem.gravitymod.mixin.compat;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;





























@Pseudo
@Mixin(targets = "net.portalmod.common.sorted.portal.CameraAnimator", remap = false)
public abstract class PortalModCameraAnimatorMixin {

    















    @Dynamic("PortalMod: CameraAnimator#start{Pitch,Yaw,Roll}Animation")
    @Redirect(
            method = {"startPitchAnimation", "startYawAnimation", "startRollAnimation"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/portalmod/common/sorted/portal/CameraAnimator;normalizeAngle(F)F",
                    ordinal = 0
            )
    )
    private float gravitymod$shortestArcStart(float value, float start, float end) {
        float normalisedStart = gravitymod$normalize(value);
        float normalisedEnd = gravitymod$normalize(end);
        float delta = normalisedStart - normalisedEnd;
        if (delta > 180.0F) {
            delta -= 360.0F;
        } else if (delta < -180.0F) {
            delta += 360.0F;
        }
        return normalisedEnd + delta;
    }

    




    private static float gravitymod$normalize(float angle) {
        angle %= 360.0F;
        return angle < 0.0F ? angle + 360.0F : angle;
    }
}
