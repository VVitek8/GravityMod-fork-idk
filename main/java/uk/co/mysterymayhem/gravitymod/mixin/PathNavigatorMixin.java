package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.MobEntity;
import net.minecraft.network.DebugPacketSender;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPathHelper;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;

import javax.annotation.Nullable;











@Mixin(PathNavigator.class)
public abstract class PathNavigatorMixin {

    @Shadow
    @Final
    protected MobEntity mob;

    @Shadow
    @Final
    protected World level;

    @Shadow
    @Nullable
    protected Path path;

    @Shadow
    protected int tick;

    @Shadow
    protected double speedModifier;

    @Shadow
    protected float maxDistanceToWaypoint;

    @Shadow
    protected boolean hasDelayedRecomputation;

    @Shadow
    public abstract void recomputePath();

    @Shadow
    public abstract boolean isDone();

    @Shadow
    protected abstract Vector3d getTempMobPos();

    @Shadow
    protected abstract boolean canUpdatePath();

    @Shadow
    protected abstract void followThePath();

    @Shadow
    protected abstract void doStuckDetection(Vector3d pos);

    



    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void gravitymod$tick(CallbackInfo ci) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(this.mob);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        ci.cancel();

        ++this.tick;
        if (this.hasDelayedRecomputation) {
            this.recomputePath();
        }
        if (this.isDone()) {
            return;
        }

        if (this.canUpdatePath()) {
            this.followThePath();
        }
        else if (this.path != null && !this.path.isDone()) {
            Vector3d here = direction.inverseAdjustVec(this.getTempMobPos());
            Vector3d next = direction.inverseAdjustVec(this.path.getNextEntityPos(this.mob));
            
            
            if (here.y > next.y && !this.mob.isOnGround()
                    && MathHelper.floor(here.x) == MathHelper.floor(next.x)
                    && MathHelper.floor(here.z) == MathHelper.floor(next.z)) {
                this.path.advance();
            }
        }

        DebugPacketSender.sendPathFindingPacket(this.level, this.mob, this.path, this.maxDistanceToWaypoint);
        if (this.isDone()) {
            return;
        }

        
        
        Vector3d target = this.path.getNextEntityPos(this.mob);
        Vector3d relativeTarget = direction.inverseAdjustVec(target);
        
        
        
        
        BlockPos cell = GravityPathHelper.toRelativeCell(direction, target);
        BlockPos below = GravityPathHelper.toWorldBlock(direction, cell.getX(), cell.getY() - 1, cell.getZ());
        double relativeY = this.level.getBlockState(below).isAir()
                ? relativeTarget.y
                : GravityPathHelper.relativeFloorLevel(this.level, direction, cell.getX(), cell.getY(), cell.getZ());
        Vector3d wanted = direction.adjustVec(new Vector3d(relativeTarget.x, relativeY, relativeTarget.z));
        this.mob.getMoveControl().setWantedPosition(wanted.x, wanted.y, wanted.z, this.speedModifier);
    }

    



    @Inject(method = "followThePath", at = @At("HEAD"), cancellable = true)
    private void gravitymod$followThePath(CallbackInfo ci) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(this.mob);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        ci.cancel();

        Vector3d tempPos = this.getTempMobPos();
        this.maxDistanceToWaypoint = this.mob.getBbWidth() > 0.75F
                ? this.mob.getBbWidth() / 2.0F
                : 0.75F - this.mob.getBbWidth() / 2.0F;

        BlockPos node = GravityPathHelper.toRelativeBlock(direction, this.path.getNextNodePos());
        Vector3d here = direction.inverseAdjustVec(this.mob.position());
        
        double centring = (this.mob.getBbWidth() + 1) / 2.0D;
        double d0 = Math.abs(here.x - ((double) node.getX() + centring));
        double d1 = Math.abs(here.y - (double) node.getY());
        double d2 = Math.abs(here.z - ((double) node.getZ() + centring));

        boolean arrived = d0 <= (double) this.maxDistanceToWaypoint
                && d2 <= (double) this.maxDistanceToWaypoint
                && d1 < 1.0D;
        if (arrived || (this.mob.canCutCorner(this.path.getNextNode().type)
                && this.gravitymod$shouldTargetNextNodeInDirection(direction, tempPos))) {
            this.path.advance();
        }

        
        this.doStuckDetection(tempPos);
    }

    





    @Unique
    private boolean gravitymod$shouldTargetNextNodeInDirection(EnumGravityDirection direction, Vector3d tempPos) {
        if (this.path.getNextNodeIndex() + 1 >= this.path.getNodeCount()) {
            return false;
        }
        Vector3d current = this.gravitymod$bottomCentreOf(direction, this.path.getNextNodePos());
        if (!tempPos.closerThan(current, 2.0D)) {
            return false;
        }
        Vector3d following = this.gravitymod$bottomCentreOf(direction,
                this.path.getNodePos(this.path.getNextNodeIndex() + 1));
        return following.subtract(current).dot(tempPos.subtract(current)) > 0.0D;
    }

    @Unique
    private Vector3d gravitymod$bottomCentreOf(EnumGravityDirection direction, BlockPos worldNode) {
        BlockPos cell = GravityPathHelper.toRelativeBlock(direction, worldNode);
        return direction.adjustVec(new Vector3d(cell.getX() + 0.5D, cell.getY(), cell.getZ() + 0.5D));
    }
}
