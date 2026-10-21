package uk.co.mysterymayhem.gravitymod.mixin.compat;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3d;
import net.portalmod.core.math.Vec3;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;
import uk.co.mysterymayhem.gravitymod.util.PortalModGelHelper;


























@Pseudo
@Mixin(targets = "net.portalmod.common.sorted.faithplate.FaithPlateTileEntity", remap = false)
public abstract class PortalModFaithPlateTileEntityMixin {

    @Dynamic("PortalMod: FaithPlateTileEntity#getTargetPos")
    @Shadow
    public abstract Vector3d getTargetPos();

    @Dynamic("PortalMod: FaithPlateTileEntity#getTargetFace")
    @Shadow
    public abstract Direction getTargetFace();

    
































    @Dynamic("PortalMod: FaithPlateTileEntity#launchEntity")
    @Redirect(
            method = "launchEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/portalmod/core/math/Vec3;to3d()Lnet/minecraft/util/math/vector/Vector3d;"
            )
    )
    private Vector3d gravitymod$aimInEntityFrame(Vec3 aim, Entity entity) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        if (direction == EnumGravityDirection.DOWN) {
            return aim.to3d();
        }

        Vector3d relativeUp = PortalModGelHelper.relativeUp(direction);
        Vector3d plate = Vector3d.atCenterOf(((TileEntity) (Object) this).getBlockPos());
        Vector3d target = this.getTargetPos();
        Vector3d worldAim;

        if (gravitymod$isAlongGravityAxis(direction, target)) {
            
            
            worldAim = plate.add(relativeUp.scale(0.5D)).subtract(entity.position());
        }
        else {
            Direction targetFace = this.getTargetFace();
            double offset = targetFace == direction.getRelativeUp().getFacingEquivalent()
                    ? 0.0D
                    : -0.25D - 0.5D * entity.getBbHeight();
            
            Vector3d faceCentre = target.add(Vector3d.atLowerCornerOf(targetFace.getNormal()).scale(0.5D));
            worldAim = plate.add(faceCentre).subtract(entity.position()).add(relativeUp.scale(offset));
        }

        return direction.inverseAdjustVec(worldAim);
    }

    







    private static boolean gravitymod$isAlongGravityAxis(EnumGravityDirection direction, Vector3d target) {
        Vector3d relative = direction.inverseAdjustVec(target);
        return relative.x == 0.0D && relative.z == 0.0D;
    }

    








    @Dynamic("PortalMod: FaithPlateTileEntity#launchEntity")
    @Redirect(
            method = "launchEntity",
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;setDeltaMovement(Lnet/minecraft/util/math/vector/Vector3d;)V"
            )
    )
    private void gravitymod$launchInWorldFrame(Entity entity, Vector3d velocity) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        if (direction == EnumGravityDirection.DOWN) {
            entity.setDeltaMovement(velocity);
            return;
        }
        entity.setDeltaMovement(direction.adjustVec(velocity));
    }
}
