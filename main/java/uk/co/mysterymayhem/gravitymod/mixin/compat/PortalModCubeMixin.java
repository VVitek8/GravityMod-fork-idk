package uk.co.mysterymayhem.gravitymod.mixin.compat;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.CubeModelCollisionHelper;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;
import uk.co.mysterymayhem.gravitymod.util.OrientedBox;
import uk.co.mysterymayhem.gravitymod.util.PortalModGelHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;











































@Pseudo
@Mixin(targets = "net.portalmod.common.sorted.cube.Cube", remap = false)
public abstract class PortalModCubeMixin {

    



    private static final double GRAVITYMOD$CROWD_REACH = 0.05D;

    




    private static final double GRAVITYMOD$STANDING_DEAD_BAND = -0.01D;

    





























    @Dynamic("PortalMod: Cube#canBeCollidedWith")
    @org.spongepowered.asm.mixin.injection.Inject(
            method = "func_241845_aY", at = @At("HEAD"), cancellable = true)
    private void gravitymod$hideHitboxBehindModel(
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (CubeModelCollisionHelper.usesModelCollision((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    









    @Dynamic("PortalMod: Cube#tick")
    @org.spongepowered.asm.mixin.injection.Inject(method = {"tick", "func_70071_h_"}, remap = true, at = @At("TAIL"))
    private void gravitymod$syncCubePhysics(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        uk.co.mysterymayhem.gravitymod.util.CubePhysics.onCubeTick((Entity) (Object) this);
    }

    






























    @Dynamic("PortalMod: Cube#tick, development mappings")
    @Redirect(
            method = {"tick", "func_70071_h_"}, remap = true,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/portalmod/common/sorted/cube/Cube;"
                            + "getDeltaMovement()Lnet/minecraft/util/math/vector/Vector3d;"))
    private Vector3d gravitymod$fallSpeedInCubeFrameDev(Entity self) {
        return gravitymod$fallSpeedInCubeFrame(self);
    }

    
    @Dynamic("PortalMod: Cube#tick, production mappings")
    @Redirect(
            method = {"tick", "func_70071_h_"}, remap = true,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/portalmod/common/sorted/cube/Cube;"
                            + "func_213322_ci()Lnet/minecraft/util/math/vector/Vector3d;"))
    private Vector3d gravitymod$fallSpeedInCubeFrameProd(Entity self) {
        return gravitymod$fallSpeedInCubeFrame(self);
    }

    private static Vector3d gravitymod$fallSpeedInCubeFrame(Entity self) {
        return GravityPhysicsHelper.getDirection(self).inverseAdjustVec(self.getDeltaMovement());
    }

    





























    @Dynamic("PortalMod: Cube#tick")
    @Redirect(
            method = {"tick", "func_70071_h_"},
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/function/Predicate;)Ljava/util/List;"
            )
    )
    private List<Entity> gravitymod$findCrowdingEntitiesInTheirOwnFrame(
            World level, Entity cube, AxisAlignedBB pushBox, Predicate<? super Entity> predicate) {
        AxisAlignedBB cubeBox = cube.getBoundingBox();
        
        
        
        OrientedBox model = CubeModelCollisionHelper.usesModelCollision(cube)
                ? CubeModelCollisionHelper.modelBox(cube) : null;
        AxisAlignedBB searchBox = model == null
                ? cubeBox.inflate(GRAVITYMOD$CROWD_REACH) : model.getBounds();
        List<Entity> candidates = level.getEntities(cube, searchBox, predicate);
        List<Entity> crowding = new ArrayList<>(candidates.size());
        for (Entity candidate : candidates) {
            boolean inside;
            if (model == null) {
                EnumGravityDirection direction = GravityPhysicsHelper.getDirection(candidate);
                inside = gravitymod$crowdingBox(candidate.getBoundingBox(), direction).intersects(cubeBox);
            }
            else {
                inside = model.intersects(candidate.getBoundingBox());
            }
            if (inside) {
                crowding.add(candidate);
            }
        }
        return crowding;
    }

    










    private static AxisAlignedBB gravitymod$crowdingBox(AxisAlignedBB candidateBox, EnumGravityDirection direction) {
        Vector3d up = PortalModGelHelper.relativeUp(direction);
        if (up.y != 0.0D) {
            return candidateBox.inflate(GRAVITYMOD$CROWD_REACH, GRAVITYMOD$STANDING_DEAD_BAND, GRAVITYMOD$CROWD_REACH);
        }
        if (up.x != 0.0D) {
            return candidateBox.inflate(GRAVITYMOD$STANDING_DEAD_BAND, GRAVITYMOD$CROWD_REACH, GRAVITYMOD$CROWD_REACH);
        }
        return candidateBox.inflate(GRAVITYMOD$CROWD_REACH, GRAVITYMOD$CROWD_REACH, GRAVITYMOD$STANDING_DEAD_BAND);
    }
}
