package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.capability.GravityAPI;
import uk.co.mysterymayhem.gravitymod.capability.IGravityCapability;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;























@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {

    @Shadow
    public abstract void shoot(double x, double y, double z, float velocity, float inaccuracy);

    





    @Inject(method = "tick", at = @At("HEAD"))
    private void gravitymod$tickGravityCapability(CallbackInfo ci) {
        GravityAPI.getCapability((Entity) (Object) this).ifPresent(IGravityCapability::tick);
    }

    @Inject(method = "shootFromRotation", at = @At("HEAD"), cancellable = true)
    private void gravitymod$shootRelativeToGravity(Entity shooter, float xRot, float yRot, float roll, float velocity, float inaccuracy, CallbackInfo ci) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(shooter);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        ProjectileEntity self = (ProjectileEntity) (Object) this;
        
        
        
        double[] relativeUp = direction.adjustXYZValues(0.0D, 1.0D, 0.0D);
        Vector3d eye = GravityPhysicsHelper.getRelativeEyePosition(shooter);
        self.setPos(eye.x - relativeUp[0] * 0.1D, eye.y - relativeUp[1] * 0.1D, eye.z - relativeUp[2] * 0.1D);
        
        float f = -MathHelper.sin(yRot * ((float) Math.PI / 180F)) * MathHelper.cos(xRot * ((float) Math.PI / 180F));
        float f1 = -MathHelper.sin((xRot + roll) * ((float) Math.PI / 180F));
        float f2 = MathHelper.cos(yRot * ((float) Math.PI / 180F)) * MathHelper.cos(xRot * ((float) Math.PI / 180F));
        
        
        Vector3d worldDir = direction.adjustVec(new Vector3d(f, f1, f2));
        this.shoot(worldDir.x, worldDir.y, worldDir.z, velocity, inaccuracy);
        
        Vector3d shooterMotion = shooter.getDeltaMovement();
        self.setDeltaMovement(self.getDeltaMovement().add(shooterMotion.x, shooter.isOnGround() ? 0.0D : shooterMotion.y, shooterMotion.z));
        ci.cancel();
    }
}
