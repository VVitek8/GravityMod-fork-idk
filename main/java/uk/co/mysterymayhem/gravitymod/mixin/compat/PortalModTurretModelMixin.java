package uk.co.mysterymayhem.gravitymod.mixin.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.util.GravityRenderHelper;

































@Pseudo
@Mixin(targets = "net.portalmod.common.sorted.turret.TurretModel", remap = false)
public abstract class PortalModTurretModelMixin {

    
    private static final String SETUP_ANIM =
            "setupAnim(Lnet/portalmod/common/sorted/turret/TurretEntity;FFFFF)V";

    




    @Dynamic("PortalMod: TurretModel#setupAnim")
    @Redirect(
            method = SETUP_ANIM,
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/vector/Vector3d;normalize()Lnet/minecraft/util/math/vector/Vector3d;"
            )
    )
    private Vector3d gravitymod$aimWingsInModelFrame(Vector3d worldAim, @Coerce Entity turret, float limbSwing,
                                                     float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                                     float headPitch) {
        float partialTicks = Minecraft.getInstance().getFrameTime();
        if (GravityRenderHelper.isIdentity(turret, partialTicks)) {
            
            return worldAim.normalize();
        }
        return GravityRenderHelper.inverseRotate(turret, worldAim, partialTicks).normalize();
    }
}
