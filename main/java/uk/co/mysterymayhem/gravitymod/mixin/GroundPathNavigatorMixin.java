package uk.co.mysterymayhem.gravitymod.mixin;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Blocks;
import net.minecraft.entity.MobEntity;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPathHelper;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;









@Mixin(GroundPathNavigator.class)
public abstract class GroundPathNavigatorMixin extends PathNavigator {

    
    @Unique
    private static final int GRAVITYMOD$SCAN_LIMIT = 256;

    






    public GroundPathNavigatorMixin(MobEntity mob, World level) {
        super(mob, level);
    }

    




    @Inject(method = "getTempMobPos", at = @At("HEAD"), cancellable = true)
    private void gravitymod$getTempMobPos(CallbackInfoReturnable<Vector3d> cir) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(this.mob);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        Vector3d relative = direction.inverseAdjustVec(this.mob.position());
        int surface;
        if (this.mob.isInWater() && this.canFloat()) {
            
            surface = MathHelper.floor(relative.y);
            int relX = MathHelper.floor(relative.x);
            int relZ = MathHelper.floor(relative.z);
            int steps = 0;
            while (this.level.getBlockState(GravityPathHelper.toWorldBlock(direction, relX, surface, relZ))
                    .getBlock() == Blocks.WATER) {
                ++surface;
                if (++steps > 16) {
                    surface = MathHelper.floor(relative.y);
                    break;
                }
            }
        }
        else {
            surface = MathHelper.floor(relative.y + 0.5D);
        }
        cir.setReturnValue(direction.adjustVec(new Vector3d(relative.x, surface, relative.z)));
    }

    







    @Inject(
            method = "createPath(Lnet/minecraft/util/math/BlockPos;I)Lnet/minecraft/pathfinding/Path;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void gravitymod$createPath(BlockPos target, int reachRange, CallbackInfoReturnable<Path> cir) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(this.mob);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        BlockPos adjusted = this.gravitymod$snapToGround(direction, target);
        cir.setReturnValue(this.createPath(ImmutableSet.of(adjusted), 8, false, reachRange));
    }

    @Unique
    private BlockPos gravitymod$snapToGround(EnumGravityDirection direction, BlockPos target) {
        BlockPos cell = GravityPathHelper.toRelativeBlock(direction, target);
        int relX = cell.getX();
        int relZ = cell.getZ();

        if (this.level.getBlockState(target).isAir()) {
            int relY = cell.getY() - 1;
            boolean foundGround = false;
            for (int guard = 0; guard < GRAVITYMOD$SCAN_LIMIT; ++guard) {
                if (this.gravitymod$outOfWorld(direction, relX, relY, relZ)) {
                    break;
                }
                if (!this.level.getBlockState(GravityPathHelper.toWorldBlock(direction, relX, relY, relZ)).isAir()) {
                    foundGround = true;
                    break;
                }
                --relY;
            }
            if (foundGround) {
                return GravityPathHelper.toWorldBlock(direction, relX, relY + 1, relZ);
            }
            
            relY = cell.getY();
            for (int guard = 0; guard < GRAVITYMOD$SCAN_LIMIT; ++guard) {
                if (this.gravitymod$outOfWorld(direction, relX, relY, relZ)
                        || !this.level.getBlockState(GravityPathHelper.toWorldBlock(direction, relX, relY, relZ)).isAir()) {
                    break;
                }
                ++relY;
            }
            return GravityPathHelper.toWorldBlock(direction, relX, relY, relZ);
        }

        if (!this.level.getBlockState(target).getMaterial().isSolid()) {
            return target;
        }

        int relY = cell.getY() + 1;
        for (int guard = 0; guard < GRAVITYMOD$SCAN_LIMIT; ++guard) {
            if (this.gravitymod$outOfWorld(direction, relX, relY, relZ)
                    || !this.level.getBlockState(GravityPathHelper.toWorldBlock(direction, relX, relY, relZ))
                            .getMaterial().isSolid()) {
                break;
            }
            ++relY;
        }
        return GravityPathHelper.toWorldBlock(direction, relX, relY, relZ);
    }

    @Unique
    private boolean gravitymod$outOfWorld(EnumGravityDirection direction, int relX, int relY, int relZ) {
        BlockPos world = GravityPathHelper.toWorldBlock(direction, relX, relY, relZ);
        return world.getY() < 0 || world.getY() >= this.level.getMaxBuildHeight();
    }
}
