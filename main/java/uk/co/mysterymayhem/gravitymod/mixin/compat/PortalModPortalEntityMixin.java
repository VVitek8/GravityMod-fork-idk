package uk.co.mysterymayhem.gravitymod.mixin.compat;

import net.minecraft.entity.Entity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3d;
import net.portalmod.common.sorted.portal.PortalEntity;
import net.portalmod.core.math.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;
import uk.co.mysterymayhem.gravitymod.util.IGravityBackedOffMoveHolder;
import uk.co.mysterymayhem.gravitymod.util.PortalModPortalHelper;

import java.util.Optional;









































@Pseudo
@Mixin(targets = "net.portalmod.common.sorted.portal.PortalEntity", remap = false)
public abstract class PortalModPortalEntityMixin {

    





















    @Dynamic("PortalMod: PortalEntity#recursivelyTeleportEntity")
    @Redirect(
            method = "recursivelyTeleportEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/portalmod/common/sorted/portal/PortalEntity;getDirection()Lnet/minecraft/util/Direction;"
            )
    )
    private static Direction gravitymod$relativeFace(
            PortalEntity portal, Entity entity, Vector3d delta, PortalEntity justExited, int depth) {
        return gravitymod$relativeFaceFor(portal, entity);
    }

    @Dynamic("PortalMod: PortalEntity#recursivelyTeleportEntity")
    @Redirect(
            method = "recursivelyTeleportEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/portalmod/common/sorted/portal/PortalEntity;func_174811_aO()Lnet/minecraft/util/Direction;"
            )
    )
    private static Direction gravitymod$relativeFaceSrg(
            PortalEntity portal, Entity entity, Vector3d delta, PortalEntity justExited, int depth) {
        return gravitymod$relativeFaceFor(portal, entity);
    }

    @org.spongepowered.asm.mixin.Unique
    private static final ThreadLocal<Boolean> gravitymod$lastFaceQueryWasWorldUpToWorldUp =
            ThreadLocal.withInitial(() -> false);

    @org.spongepowered.asm.mixin.Unique
    private static Direction gravitymod$relativeFaceFor(PortalEntity portal, Entity entity) {
        boolean worldUpToWorldUp = gravitymod$isWorldUpToWorldUpPair(portal);
        gravitymod$lastFaceQueryWasWorldUpToWorldUp.set(worldUpToWorldUp);
        return gravitymod$teleportRuleFace(portal, entity);
    }

    @org.spongepowered.asm.mixin.Unique
    private static Direction gravitymod$worldFace(PortalEntity portal) {
        return ((PortalModPortalEntityAccessorMixin) portal).gravitymod$getPortalDirection();
    }

    @org.spongepowered.asm.mixin.Unique
    private static Direction gravitymod$otherWorldFace(PortalEntity portal) {
        Optional<PortalEntity> otherPortal = portal.getOtherPortal();
        return otherPortal.isPresent() ? gravitymod$worldFace(otherPortal.get()) : null;
    }

    @org.spongepowered.asm.mixin.Unique
    private static boolean gravitymod$isWorldUpToWorldUpPair(PortalEntity portal) {
        return gravitymod$worldFace(portal) == Direction.UP && gravitymod$otherWorldFace(portal) == Direction.UP;
    }

    @org.spongepowered.asm.mixin.Unique
    private static Direction gravitymod$teleportRuleFace(PortalEntity portal, Entity entity) {
        Direction worldFace = gravitymod$worldFace(portal);
        if (gravitymod$isWorldUpToWorldUpPair(portal)) {
            return worldFace;
        }
        return PortalModPortalHelper.toRelativeDirection(GravityPhysicsHelper.getDirection(entity), worldFace);
    }

    @org.spongepowered.asm.mixin.Unique
    private static boolean gravitymod$shouldApplyExitFloor(PortalEntity portal, Entity entity) {
        Optional<PortalEntity> targetPortal = portal.getOtherPortal();
        if (!targetPortal.isPresent()) {
            return false;
        }
        return gravitymod$teleportRuleFace(targetPortal.get(), entity) == Direction.UP
                && gravitymod$teleportRuleFace(portal, entity) != Direction.DOWN;
    }

    








    @Dynamic("PortalMod: PortalEntity#recursivelyTeleportEntity")
    @Redirect(
            method = "recursivelyTeleportEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/portalmod/common/sorted/portal/PortalEntity;teleportPoint(Lnet/portalmod/core/math/Vec3;)Lnet/portalmod/core/math/Vec3;"
            )
    )
    private static Vec3 gravitymod$teleportPointWithWorldUpPairClearance(
            PortalEntity portal, Vec3 point, Entity entity, Vector3d delta, PortalEntity justExited, int depth) {
        Vec3 teleportedCenter = portal.teleportPoint(point);
        if (GravityPhysicsHelper.getDirection(entity) == EnumGravityDirection.DOWN
                || !gravitymod$shouldApplyExitFloor(portal, entity)) {
            return teleportedCenter;
        }

        PortalEntity targetPortal = portal.getOtherPortal().get();
        gravitymod$moveOutOfExitPlane(teleportedCenter, entity, targetPortal, 1.0E-3D);
        return teleportedCenter;
    }

    @Dynamic("PortalMod: PortalEntity#recursivelyTeleportEntity")
    @Redirect(
            method = "recursivelyTeleportEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/portalmod/common/sorted/portal/PortalEntity;teleportVector(Lnet/portalmod/core/math/Vec3;)Lnet/portalmod/core/math/Vec3;",
                    ordinal = 0
            )
    )
    private static Vec3 gravitymod$teleportDeltaWithExitFloor(
            PortalEntity portal, Vec3 vector, Entity entity, Vector3d delta, PortalEntity justExited, int depth) {
        Vec3 teleported = portal.teleportVector(vector);
        teleported = gravitymod$applyExitFloor(portal, entity, teleported, 0.5D);
        if (GravityPhysicsHelper.getDirection(entity) != EnumGravityDirection.DOWN) {
            ((IGravityBackedOffMoveHolder) entity).gravitymod$setPortalTransformedMoveDelta(teleported.to3d());
        }
        return teleported;
    }

    @Dynamic("PortalMod: PortalEntity#recursivelyTeleportEntity")
    @Redirect(
            method = "recursivelyTeleportEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/portalmod/common/sorted/portal/PortalEntity;teleportVector(Lnet/portalmod/core/math/Vec3;)Lnet/portalmod/core/math/Vec3;",
                    ordinal = 1
            )
    )
    private static Vec3 gravitymod$teleportVelocityWithExitFloor(
            PortalEntity portal, Vec3 vector, Entity entity, Vector3d delta, PortalEntity justExited, int depth) {
        Vec3 teleported = portal.teleportVector(vector);
        return gravitymod$applyExitFloor(portal, entity, teleported, 0.7D);
    }

    @org.spongepowered.asm.mixin.Unique
    private static Vec3 gravitymod$applyExitFloor(PortalEntity portal, Entity entity, Vec3 teleported, double floor) {
        if (GravityPhysicsHelper.getDirection(entity) == EnumGravityDirection.DOWN
                || !gravitymod$shouldApplyExitFloor(portal, entity)) {
            return teleported;
        }

        PortalEntity targetPortal = portal.getOtherPortal().get();
        double outward = gravitymod$componentAlong(teleported, gravitymod$worldFace(targetPortal));
        if (outward < floor) {
            gravitymod$addAlong(teleported, gravitymod$worldFace(targetPortal), floor - outward);
        }
        return teleported;
    }

    @org.spongepowered.asm.mixin.Unique
    private static void gravitymod$moveOutOfExitPlane(
            Vec3 center, Entity entity, PortalEntity targetPortal, double clearance) {
        Direction targetFace = gravitymod$worldFace(targetPortal);
        double outwardDistance = gravitymod$componentAlong(center, targetFace)
                - gravitymod$componentAlong(((Entity) (Object) targetPortal).position(), targetFace);
        double requiredDistance = gravitymod$halfExtentAlong(entity, targetFace) + clearance;
        if (outwardDistance < requiredDistance) {
            gravitymod$addAlong(center, targetFace, requiredDistance - outwardDistance);
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private static double gravitymod$halfExtentAlong(Entity entity, Direction direction) {
        switch (direction.getAxis()) {
            case X:
                return (entity.getBoundingBox().maxX - entity.getBoundingBox().minX) * 0.5D;
            case Y:
                return (entity.getBoundingBox().maxY - entity.getBoundingBox().minY) * 0.5D;
            default:
                return (entity.getBoundingBox().maxZ - entity.getBoundingBox().minZ) * 0.5D;
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private static double gravitymod$componentAlong(Vec3 vector, Direction direction) {
        switch (direction.getAxis()) {
            case X:
                return vector.x * direction.getStepX();
            case Y:
                return vector.y * direction.getStepY();
            default:
                return vector.z * direction.getStepZ();
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private static double gravitymod$componentAlong(Vector3d vector, Direction direction) {
        switch (direction.getAxis()) {
            case X:
                return vector.x * direction.getStepX();
            case Y:
                return vector.y * direction.getStepY();
            default:
                return vector.z * direction.getStepZ();
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private static void gravitymod$addAlong(Vec3 vector, Direction direction, double amount) {
        vector.x += direction.getStepX() * amount;
        vector.y += direction.getStepY() * amount;
        vector.z += direction.getStepZ() * amount;
    }

    




















    @Dynamic("PortalMod: PortalEntity#recursivelyTeleportEntity")
    @ModifyVariable(method = "recursivelyTeleportEntity", at = @At("STORE"), name = "doMathTrickery")
    private static boolean gravitymod$suppressWorldAxisDragCancel(
            boolean original, Entity entity, Vector3d delta, PortalEntity justExited, int depth) {
        return original
                && (GravityPhysicsHelper.getDirection(entity) == EnumGravityDirection.DOWN
                || gravitymod$lastFaceQueryWasWorldUpToWorldUp.get());
    }

    


















    @Dynamic("PortalMod: PortalEntity#recursivelyTeleportEntity")
    @ModifyVariable(method = "recursivelyTeleportEntity", at = @At("STORE"), name = "shouldResetVelocityAtApex")
    private static boolean gravitymod$apexResetOnlyWhenVerticalIsY(
            boolean original, Entity entity, Vector3d delta, PortalEntity justExited, int depth) {
        return original && GravityPhysicsHelper.getDirection(entity) == EnumGravityDirection.DOWN;
    }

    

















    @Dynamic("PortalMod: PortalEntity#recursivelyTeleportEntity")
    @ModifyVariable(method = "recursivelyTeleportEntity", at = @At("STORE"), name = "shouldDisableFlying")
    private static boolean gravitymod$keepFlightUnderRotatedGravity(
            boolean original, Entity entity, Vector3d delta, PortalEntity justExited, int depth) {
        return original && GravityPhysicsHelper.getDirection(entity) == EnumGravityDirection.DOWN;
    }

    


















    @Dynamic("PortalMod: PortalEntity#recursivelyTeleportEntity")
    @Redirect(
            method = "recursivelyTeleportEntity",
            at = @At(value = "FIELD", target = "Lnet/portalmod/core/math/Vec3;y:D", opcode = Opcodes.PUTFIELD)
    )
    private static void gravitymod$recentreOnlyWhenVerticalIsY(
            Vec3 exitCentre, double portalY, Entity entity, Vector3d delta, PortalEntity justExited, int depth) {
        if (GravityPhysicsHelper.getDirection(entity) == EnumGravityDirection.DOWN) {
            exitCentre.y = portalY;
        }
    }
}
