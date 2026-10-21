package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.client.particle.EmitterParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particles.IParticleData;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;






















@Mixin(EmitterParticle.class)
public abstract class EmitterParticleMixin {

    @Shadow
    @Final
    private Entity entity;

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/world/ClientWorld;addParticle(Lnet/minecraft/particles/IParticleData;ZDDDDDD)V"))
    private void gravitymod$relativeEmitterSpread(
            ClientWorld level, IParticleData type, boolean force,
            double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(this.entity);
        if (direction == EnumGravityDirection.DOWN) {
            level.addParticle(type, force, x, y, z, xSpeed, ySpeed, zSpeed);
            return;
        }

        
        double sampleX = xSpeed;
        double sampleY = ySpeed - 0.2D;
        double sampleZ = zSpeed;

        Vector3d position = GravityPhysicsHelper.getRelativeBodyPoint(
                this.entity, sampleX / 4.0D, 0.5D + sampleY / 4.0D, sampleZ / 4.0D);
        
        Vector3d velocity = direction.adjustVec(new Vector3d(sampleX, sampleY + 0.2D, sampleZ));

        level.addParticle(type, force, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
    }
}
