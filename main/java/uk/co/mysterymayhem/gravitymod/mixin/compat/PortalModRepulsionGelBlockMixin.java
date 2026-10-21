package uk.co.mysterymayhem.gravitymod.mixin.compat;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.Property;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
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
@Mixin(targets = "net.portalmod.common.sorted.gel.RepulsionGelBlock", remap = false)
public abstract class PortalModRepulsionGelBlockMixin {

    
    private static final double GRAVITYMOD$HORIZONTAL_BOUNCE = 0.7D;
    private static final double GRAVITYMOD$VERTICAL_BOUNCE = 0.25D;
    private static final double GRAVITYMOD$SPEED_BONUS = 1.4D;
    private static final double GRAVITYMOD$MAX_SPEED_BOOST = 2.0D;
    private static final double GRAVITYMOD$MIN_APPROACH_SPEED = 0.1D;

    



    @Shadow
    public static void playBounceSound(Entity entity) {
        throw new AssertionError("Shadow method bodies are discarded at mixin application time");
    }

    












    @Dynamic("PortalMod: RepulsionGelBlock#verticalBounce")
    @Redirect(
            method = "verticalBounce",
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;setDeltaMovement(DDD)V"
            )
    )
    private static void gravitymod$launchAlongRelativeUp(Entity entity, double x, double velocity, double z) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        if (direction == EnumGravityDirection.DOWN) {
            entity.setDeltaMovement(x, velocity, z);
            return;
        }
        Vector3d relative = direction.inverseAdjustVec(entity.getDeltaMovement());
        entity.setDeltaMovement(direction.adjustVec(new Vector3d(relative.x, velocity, relative.z)));
    }

    














    @Dynamic("PortalMod: RepulsionGelBlock#calculateBounce")
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(
            method = "calculateBounce",
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/BlockState;getValue(Lnet/minecraft/state/Property;)Ljava/lang/Comparable;"
            )
    )
    private static Comparable gravitymod$readRoleFace(BlockState state, Property property,
                                                      World level, BlockState state2, BlockPos pos, Entity entity) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        if (direction == EnumGravityDirection.DOWN) {
            return state.getValue(property);
        }
        BooleanProperty remapped = PortalModGelHelper.remapFaceProperty(direction, property);
        
        return remapped == null ? state.getValue(property) : state.getValue(remapped);
    }

    









    @Dynamic("PortalMod: RepulsionGelBlock#calculateBounce")
    @Redirect(
            method = "calculateBounce",
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/BlockPos;getY()I"
            )
    )
    private static int gravitymod$gelFloorAlongGravityAxis(BlockPos gelPos,
                                                           World level, BlockState state, BlockPos pos, Entity entity) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        if (direction == EnumGravityDirection.DOWN) {
            return gelPos.getY();
        }
        return (int) PortalModGelHelper.blockRelativeFloor(direction, gelPos);
    }

    








    @Dynamic("PortalMod: RepulsionGelBlock#calculateBounce")
    @Redirect(
            method = "calculateBounce",
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;getDeltaMovement()Lnet/minecraft/util/math/vector/Vector3d;"
            )
    )
    private static Vector3d gravitymod$relativeMotionForBounceTests(Entity entity) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        if (direction == EnumGravityDirection.DOWN) {
            return entity.getDeltaMovement();
        }
        return direction.inverseAdjustVec(entity.getDeltaMovement());
    }

    












    @Dynamic("PortalMod: RepulsionGelBlock#calculateBounce")
    @Redirect(
            method = "calculateBounce",
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;position()Lnet/minecraft/util/math/vector/Vector3d;"
            )
    )
    private static Vector3d gravitymod$heightWithinBlockProbe(Entity entity) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        if (direction == EnumGravityDirection.DOWN) {
            return entity.position();
        }
        return new Vector3d(0.0D, PortalModGelHelper.heightWithinBlock(direction, entity), 0.0D);
    }

    





















    @Dynamic("PortalMod: RepulsionGelBlock#horizontalBounce")
    @Inject(method = "horizontalBounce", at = @At("HEAD"), cancellable = true)
    private static void gravitymod$relativeHorizontalBounce(BlockPos pos, Entity entity, Direction roleFace,
                                                            CallbackInfo ci) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        ci.cancel();

        IGelAffected gelAffected = (IGelAffected) entity;
        if (entity.isSpectator() || gelAffected.getHorizontalBounced()) {
            return;
        }

        
        
        Direction face = PortalModGelHelper.remapFace(direction, roleFace);
        Vector3d intoWall = Vector3d.atLowerCornerOf(face.getNormal());
        Vector3d outOfWall = intoWall.reverse();
        Vector3d relativeUp = PortalModGelHelper.relativeUp(direction);

        
        Vector3d surface = Vector3d.atCenterOf(pos).add(intoWall.scale(0.5D));
        Vector3d center = PortalModGelHelper.boundingBoxCenter(direction, entity);
        double gap = Math.abs(center.subtract(surface).dot(intoWall));
        if (gap > PortalModGelHelper.extentAlong(direction, entity, face.getAxis()) / 1.5D) {
            return;
        }

        
        Vector3d lastDelta = gelAffected.getLastDeltaMovement();
        if (lastDelta.dot(intoWall) <= GRAVITYMOD$MIN_APPROACH_SPEED) {
            return;
        }

        Vector3d alongWall = relativeUp.cross(intoWall);
        double slide = lastDelta.dot(alongWall);
        double bonus = Math.abs(slide) < GRAVITYMOD$MAX_SPEED_BOOST ? GRAVITYMOD$SPEED_BONUS : 1.0D;
        double lift = Math.max(GRAVITYMOD$VERTICAL_BOUNCE, direction.inverseAdjustVec(entity.getDeltaMovement()).y);

        entity.setDeltaMovement(outOfWall.scale(GRAVITYMOD$HORIZONTAL_BOUNCE)
                .add(relativeUp.scale(lift))
                .add(alongWall.scale(slide * bonus)));

        playBounceSound(entity);
        gelAffected.setHorizontalBounced(true);
    }

    






    @Dynamic("PortalMod: RepulsionGelBlock#onPreTick")
    @Redirect(
            method = "onPreTick",
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;getDeltaMovement()Lnet/minecraft/util/math/vector/Vector3d;"
            )
    )
    private static Vector3d gravitymod$relativeFallCheck(LivingEntity entity) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        if (direction == EnumGravityDirection.DOWN) {
            return entity.getDeltaMovement();
        }
        return direction.inverseAdjustVec(entity.getDeltaMovement());
    }

    




    @Dynamic("PortalMod: RepulsionGelBlock#onPreTick")
    @Redirect(
            method = "onPreTick",
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;getY()D"
            )
    )
    private static double gravitymod$recordNeutralRelativeHeight(LivingEntity entity) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        if (direction == EnumGravityDirection.DOWN) {
            return entity.getY();
        }
        return PortalModGelHelper.relativeHeight(direction, entity.position());
    }

    







    @Dynamic("PortalMod: RepulsionGelBlock#onPostTick")
    @Redirect(
            method = "onPostTick",
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/BlockPos;above()Lnet/minecraft/util/math/BlockPos;"
            )
    )
    private static BlockPos gravitymod$relativeAbove(BlockPos pos, LivingEntity entity) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        if (direction == EnumGravityDirection.DOWN) {
            return pos.above();
        }
        return pos.relative(direction.getRelativeUp().getFacingEquivalent());
    }
}
