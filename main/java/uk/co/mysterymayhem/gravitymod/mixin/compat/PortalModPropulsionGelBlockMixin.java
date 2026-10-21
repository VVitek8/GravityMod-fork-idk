package uk.co.mysterymayhem.gravitymod.mixin.compat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.portalmod.common.sorted.gel.IGelAffected;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;
import uk.co.mysterymayhem.gravitymod.util.PortalModGelHelper;


























@Pseudo
@Mixin(targets = "net.portalmod.common.sorted.gel.PropulsionGelBlock", remap = false)
public abstract class PortalModPropulsionGelBlockMixin {

    



    @Shadow
    public static void removeBounceSpeedBoost(LivingEntity entity) {
        throw new AssertionError("Shadow method bodies are discarded at mixin application time");
    }

    



















    @Dynamic("PortalMod: PropulsionGelBlock#onPreTick")
    @Inject(method = "onPreTick", at = @At("TAIL"))
    private static void gravitymod$clearStrandedBounceBoost(LivingEntity entity, CallbackInfo ci) {
        if (GravityPhysicsHelper.getDirection(entity) == EnumGravityDirection.DOWN) {
            return;
        }
        IGelAffected gelAffected = (IGelAffected) entity;
        
        
        if (gelAffected.getPropulsionTicks() <= 0 || !gelAffected.getBounced()) {
            removeBounceSpeedBoost(entity);
        }
    }

    










    @Dynamic("PortalMod: PropulsionGelBlock#onPreTick")
    @Redirect(
            method = "onPreTick",
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;blockPosition()Lnet/minecraft/util/math/BlockPos;"
            )
    )
    private static BlockPos gravitymod$gelBlockUnderfoot(LivingEntity entity) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        if (direction == EnumGravityDirection.DOWN) {
            return entity.blockPosition();
        }
        return PortalModGelHelper.blockAtContactSurface(direction, entity);
    }

    










    @Dynamic("PortalMod: PropulsionGelBlock#onPreTick")
    @Redirect(
            method = "onPreTick",
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/vector/Vector3d;multiply(DDD)Lnet/minecraft/util/math/vector/Vector3d;"
            )
    )
    private static Vector3d gravitymod$flattenOntoRelativeHorizontal(Vector3d velocity, double x, double y, double z,
                                                                     LivingEntity entity) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        boolean isVerticalFlatten = x == 1.0D && y == 0.0D && z == 1.0D;
        if (direction == EnumGravityDirection.DOWN || !isVerticalFlatten) {
            return velocity.multiply(x, y, z);
        }
        return PortalModGelHelper.flattenToRelativeHorizontal(direction, velocity);
    }
}
