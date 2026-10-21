package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;
























@Mixin({ThrowableEntity.class, AbstractArrowEntity.class})
public abstract class ProjectileSpawnMixin {

    @Inject(
            method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/World;)V",
            at = @At("RETURN")
    )
    private void gravitymod$spawnAtRelativeEye(EntityType<?> type, LivingEntity shooter, World world, CallbackInfo ci) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(shooter);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        
        Vector3d spawn = GravityPhysicsHelper.getRelativeEyePosition(shooter)
                .subtract(direction.adjustVec(new Vector3d(0.0D, 0.1D, 0.0D)));
        ((Entity) (Object) this).setPos(spawn.x, spawn.y, spawn.z);
    }
}
