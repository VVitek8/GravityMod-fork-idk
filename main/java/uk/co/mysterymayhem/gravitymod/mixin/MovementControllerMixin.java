package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.pathfinding.PathType;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
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
















@Mixin(MovementController.class)
public abstract class MovementControllerMixin {

    @Shadow
    @Final
    protected MobEntity mob;

    @Shadow
    protected double wantedX;

    @Shadow
    protected double wantedY;

    @Shadow
    protected double wantedZ;

    @Shadow
    protected double speedModifier;

    @Shadow
    protected float strafeForwards;

    @Shadow
    protected float strafeRight;

    @Shadow
    protected MovementController.Action operation;

    @Shadow
    protected abstract float rotlerp(float from, float to, float maxDelta);

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void gravitymod$tick(CallbackInfo ci) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(this.mob);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        ci.cancel();

        if (this.operation == MovementController.Action.STRAFE) {
            float walkSpeed = (float) this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
            float speed = (float) this.speedModifier * walkSpeed;
            float forwards = this.strafeForwards;
            float sideways = this.strafeRight;
            float magnitude = MathHelper.sqrt(forwards * forwards + sideways * sideways);
            if (magnitude < 1.0F) {
                magnitude = 1.0F;
            }
            magnitude = speed / magnitude;
            forwards = forwards * magnitude;
            sideways = sideways * magnitude;
            float sin = MathHelper.sin(this.mob.yRot * ((float) Math.PI / 180F));
            float cos = MathHelper.cos(this.mob.yRot * ((float) Math.PI / 180F));
            
            
            float relativeX = forwards * cos - sideways * sin;
            float relativeZ = sideways * cos + forwards * sin;
            if (!this.gravitymod$isWalkable(direction, relativeX, relativeZ)) {
                this.strafeForwards = 1.0F;
                this.strafeRight = 0.0F;
            }
            this.mob.setSpeed(speed);
            this.mob.setZza(this.strafeForwards);
            this.mob.setXxa(this.strafeRight);
            this.operation = MovementController.Action.WAIT;
        }
        else if (this.operation == MovementController.Action.MOVE_TO) {
            this.operation = MovementController.Action.WAIT;
            Vector3d offset = direction.inverseAdjustVec(new Vector3d(
                    this.wantedX - this.mob.getX(),
                    this.wantedY - this.mob.getY(),
                    this.wantedZ - this.mob.getZ()));
            double forwardX = offset.x;
            double climb = offset.y;
            double forwardZ = offset.z;
            if (forwardX * forwardX + climb * climb + forwardZ * forwardZ < (double) 2.5000003E-7F) {
                this.mob.setZza(0.0F);
                return;
            }

            float yaw = (float) (MathHelper.atan2(forwardZ, forwardX) * (double) (180F / (float) Math.PI)) - 90.0F;
            this.mob.yRot = this.rotlerp(this.mob.yRot, yaw, 90.0F);
            this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));

            
            
            
            
            
            Vector3d feetRelative = direction.inverseAdjustVec(this.mob.position());
            BlockPos cell = GravityPathHelper.toRelativeCell(direction, this.mob.position());
            BlockPos pos = GravityPathHelper.toWorldBlock(direction, cell.getX(), cell.getY(), cell.getZ());
            BlockState state = this.mob.level.getBlockState(pos);
            Block block = state.getBlock();
            VoxelShape shape = state.getCollisionShape(this.mob.level, pos);

            
            boolean stepTooHigh = climb > (double) this.mob.maxUpStep
                    && forwardX * forwardX + forwardZ * forwardZ < (double) Math.max(1.0F, this.mob.getBbWidth());
            boolean embedded = false;
            if (!shape.isEmpty()) {
                double surface = (double) cell.getY() + GravityPathHelper.relativeUpExtent(direction, shape);
                embedded = feetRelative.y < surface && !block.is(BlockTags.DOORS) && !block.is(BlockTags.FENCES);
            }
            if (stepTooHigh || embedded) {
                this.mob.getJumpControl().jump();
                this.operation = MovementController.Action.JUMPING;
            }
        }
        else if (this.operation == MovementController.Action.JUMPING) {
            this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            
            if (this.mob.isOnGround()) {
                this.operation = MovementController.Action.WAIT;
            }
        }
        else {
            this.mob.setZza(0.0F);
        }
    }

    









    @Unique
    private boolean gravitymod$isWalkable(EnumGravityDirection direction, double relativeX, double relativeZ) {
        Vector3d sample = this.mob.position().add(direction.adjustVec(new Vector3d(relativeX, 0.0D, relativeZ)));
        
        
        BlockPos cell = GravityPathHelper.toRelativeCell(direction, sample);
        BlockPos at = GravityPathHelper.toWorldBlock(direction, cell.getX(), cell.getY(), cell.getZ());
        BlockPos below = GravityPathHelper.toWorldBlock(direction, cell.getX(), cell.getY() - 1, cell.getZ());
        return this.mob.level.getBlockState(at).isPathfindable(this.mob.level, at, PathType.LAND)
                && !this.mob.level.getBlockState(below).isPathfindable(this.mob.level, below, PathType.LAND);
    }
}
