package uk.co.mysterymayhem.gravitymod.mixin.compat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;






































@Pseudo
@Mixin(targets = "net.portalmod.common.particles.PortalGunSparkParticle", remap = false)
public abstract class PortalModPortalGunSparkParticleMixin {

    






    @Dynamic("PortalMod: PortalGunSparkParticle#createParticles")
    @Redirect(
            method = "createParticles",
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/vector/Vector3d;directionFromRotation(FF)Lnet/minecraft/util/math/vector/Vector3d;"
            )
    )
    private static Vector3d gravitymod$reorientScreenRight(float pitch, float yaw,
                                                           World world, PlayerEntity player, boolean offhand) {
        Vector3d relative = Vector3d.directionFromRotation(pitch, yaw);
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(player);
        if (direction == EnumGravityDirection.DOWN) {
            return relative;
        }
        return direction.adjustVec(relative);
    }
}
