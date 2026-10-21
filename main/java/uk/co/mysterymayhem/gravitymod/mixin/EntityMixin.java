package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.Pose;
import net.minecraft.util.Direction;
import net.minecraft.util.ReuseableStream;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.List;
import java.util.stream.Stream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.CubeModelCollisionHelper;
import uk.co.mysterymayhem.gravitymod.util.CubePhysics;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;
import uk.co.mysterymayhem.gravitymod.util.IGravityBackedOffMoveHolder;
import uk.co.mysterymayhem.gravitymod.util.OrientedBox;
import uk.co.mysterymayhem.gravitymod.util.PortalModCollisionHelper;






@Mixin(Entity.class)
public abstract class EntityMixin implements IGravityBackedOffMoveHolder, uk.co.mysterymayhem.gravitymod.util.ICubeRigidBodyHolder {

    




    @org.spongepowered.asm.mixin.Unique
    private uk.co.mysterymayhem.gravitymod.util.CubeRigidBody gravitymod$cubeBody;

    @Override
    public uk.co.mysterymayhem.gravitymod.util.CubeRigidBody gravitymod$getCubeBody() {
        return this.gravitymod$cubeBody;
    }

    @Override
    public void gravitymod$setCubeBody(uk.co.mysterymayhem.gravitymod.util.CubeRigidBody body) {
        this.gravitymod$cubeBody = body;
    }

    





    @org.spongepowered.asm.mixin.Unique
    private static final double ZERO_GRAVITY_GROUND_CONTACT_PROBE = 1.0E-4D;

    @Shadow public float fallDistance;
    @Shadow public boolean horizontalCollision;
    @Shadow public boolean verticalCollision;
    @Shadow public boolean onGround;
    @Shadow public float maxUpStep;
    @Shadow public boolean noPhysics;
    @Shadow public float pushthrough;

    @Shadow
    public abstract void setBoundingBox(AxisAlignedBB bb);

    @Shadow
    public abstract void setPosRaw(double x, double y, double z);

    @Shadow
    public abstract void setDeltaMovement(Vector3d motion);

    @Shadow
    public abstract Vector3d getDeltaMovement();

    @Shadow
    public abstract AxisAlignedBB getBoundingBox();

    @Shadow
    public abstract boolean isAddedToWorld();

    @Shadow
    public abstract float getEyeHeight();

    @Shadow
    protected abstract BlockPos getOnPos();

    @Shadow
    public World level;

    @Shadow
    protected boolean wasEyeInWater;

    @Shadow
    protected net.minecraft.tags.ITag<net.minecraft.fluid.Fluid> fluidOnEyes;

    @Shadow
    protected abstract void checkFallDamage(double p_225503_1_, boolean p_225503_3_, net.minecraft.block.BlockState p_225503_4_, BlockPos p_225503_5_);

    @org.spongepowered.asm.mixin.Unique
    private Vector3d gravitymod$actualMovement = Vector3d.ZERO;

    




    @org.spongepowered.asm.mixin.Unique
    private Vector3d gravitymod$preLandingMovement = Vector3d.ZERO;

    















    @org.spongepowered.asm.mixin.Unique
    private Vector3d gravitymod$intendedMoveDelta;

    @org.spongepowered.asm.mixin.Unique
    private Vector3d gravitymod$allowedMoveDelta;

    








    @org.spongepowered.asm.mixin.Unique
    private Vector3d gravitymod$backedOffMoveDelta;

    @Override
    public Vector3d gravitymod$getBackedOffMoveDelta() {
        return this.gravitymod$backedOffMoveDelta;
    }

    @Override
    public void gravitymod$setBackedOffMoveDelta(Vector3d delta) {
        this.gravitymod$backedOffMoveDelta = delta;
    }

    @Override
    public void gravitymod$setPortalTransformedMoveDelta(Vector3d delta) {
        if (this.gravitymod$intendedMoveDelta != null) {
            this.gravitymod$intendedMoveDelta = delta;
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private Vector3d gravitymod$effectiveMoveDelta(Vector3d fallback) {
        if (this.gravitymod$backedOffMoveDelta != null) {
            return this.gravitymod$backedOffMoveDelta;
        }
        return this.gravitymod$intendedMoveDelta != null ? this.gravitymod$intendedMoveDelta : fallback;
    }

    



    @Inject(method = "setPos", at = @At("HEAD"), cancellable = true)
    private void gravitymod$reshapeBoundingBoxOnSetPos(double x, double y, double z, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        this.setPosRaw(x, y, z);
        this.setBoundingBox(direction.getGravityAdjustedAABB(self.getBbWidth(), self.getBbHeight()).move(x, y, z));
        ci.cancel();
    }

    







    @Inject(method = "setLocationFromBoundingbox", at = @At("HEAD"), cancellable = true)
    private void gravitymod$fixLocationFromBoundingBox(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        Vector3d anchor = direction.getAnchorPosition(this.getBoundingBox());
        this.setPosRaw(anchor.x, anchor.y, anchor.z);
        if (this.isAddedToWorld() && !this.level.isClientSide && this.level instanceof ServerWorld) {
            ((ServerWorld) this.level).updateChunkPos(self);
        }
        ci.cancel();
    }

    







    @Inject(method = "refreshDimensions", at = @At("RETURN"))
    private void gravitymod$reshapeBoundingBoxAfterRefreshDimensions(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        this.setBoundingBox(direction.getGravityAdjustedAABB(self.getBbWidth(), self.getBbHeight())
                .move(self.getX(), self.getY(), self.getZ()));
    }

    













    @Inject(method = "getBoundingBoxForPose", at = @At("RETURN"), cancellable = true)
    private void gravitymod$reshapePoseBoundingBox(Pose pose, CallbackInfoReturnable<AxisAlignedBB> cir) {
        Entity self = (Entity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        AxisAlignedBB vanilla = cir.getReturnValue();
        float width = (float) (vanilla.maxX - vanilla.minX);
        float height = (float) (vanilla.maxY - vanilla.minY);
        cir.setReturnValue(direction.getGravityAdjustedAABB(width, height)
                .move(self.getX(), self.getY(), self.getZ()));
    }

    









    @Inject(method = "move", at = @At("HEAD"))
    private void gravitymod$resyncBoundingBoxShape(MoverType moverType, Vector3d pPos, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            this.gravitymod$intendedMoveDelta = null;
            this.gravitymod$backedOffMoveDelta = null;
            return;
        }
        this.setBoundingBox(direction.getGravityAdjustedAABB(self.getBbWidth(), self.getBbHeight())
                .move(self.getX(), self.getY(), self.getZ()));
        
        
        
        this.gravitymod$intendedMoveDelta = pPos;
        this.gravitymod$allowedMoveDelta = null;
        
        
        this.gravitymod$backedOffMoveDelta = null;
    }

    






    @Inject(method = "move", at = @At("RETURN"))
    private void gravitymod$restoreMovementAfterMove(MoverType moverType, Vector3d pPos, CallbackInfo ci) {
        
        
        
        
        CubePhysics.afterMove((Entity) (Object) this);

        Vector3d intended = this.gravitymod$effectiveMoveDelta(pPos);
        Vector3d allowed = this.gravitymod$allowedMoveDelta;
        this.gravitymod$intendedMoveDelta = null;
        this.gravitymod$allowedMoveDelta = null;
        if (allowed == null) {
            return;
        }
        
        
        
        
        
        Vector3d velocity = this.getDeltaMovement();
        if (intended.y > 0.0D && Math.abs(velocity.y) < 1.0E-9D && allowed.y > 1.0E-7D) {
            this.setDeltaMovement(new Vector3d(velocity.x, allowed.y, velocity.z));
        }
    }

    






    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void gravitymod$saveCubePhysics(net.minecraft.nbt.CompoundNBT tag, CallbackInfoReturnable<net.minecraft.nbt.CompoundNBT> cir) {
        net.minecraft.nbt.CompoundNBT saved = cir.getReturnValue();
        if (saved != null) {
            CubePhysics.save((Entity) (Object) this, saved);
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void gravitymod$loadCubePhysics(net.minecraft.nbt.CompoundNBT tag, CallbackInfo ci) {
        CubePhysics.load((Entity) (Object) this, tag);
    }

    













    @Inject(method = "getOnPos", at = @At("HEAD"), cancellable = true)
    private void gravitymod$relativeOnPos(CallbackInfoReturnable<BlockPos> cir) {
        Entity self = (Entity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        Vector3d sample = self.position().add(direction.adjustVec(new Vector3d(0.0D, -0.2D, 0.0D)));
        BlockPos pos = new BlockPos(MathHelper.floor(sample.x), MathHelper.floor(sample.y), MathHelper.floor(sample.z));
        if (this.level.isEmptyBlock(pos)) {
            BlockPos further = pos.relative(direction.getFacingEquivalent());
            net.minecraft.block.BlockState state = this.level.getBlockState(further);
            if (state.collisionExtendsVertically(this.level, further, self)) {
                cir.setReturnValue(further);
                return;
            }
        }
        cir.setReturnValue(pos);
    }

    










    @Redirect(
            method = "move",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;updateEntityAfterFallOn(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/entity/Entity;)V")
    )
    private void gravitymod$suppressWorldAxisFallOn(net.minecraft.block.Block block, net.minecraft.world.IBlockReader reader, Entity entity) {
        if (!GravityPhysicsHelper.isAffected(entity)) {
            block.updateEntityAfterFallOn(reader, entity);
        }
    }

    












    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isMovementNoisy()Z", ordinal = 0))
    private void gravitymod$relativeUpdateEntityAfterFallOn(MoverType moverType, Vector3d pPos, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        double relativeIntendedY = direction.inverseAdjustVec(this.gravitymod$effectiveMoveDelta(pPos)).y;
        double relativeAllowedY = direction.inverseAdjustVec(this.gravitymod$actualMovement).y;
        if (relativeIntendedY == relativeAllowedY) {
            return;
        }

        
        
        
        
        
        
        
        
        
        
        
        
        
        Vector3d stopped = direction.inverseAdjustVec(self.getDeltaMovement());
        double impactY = direction.inverseAdjustVec(this.gravitymod$preLandingMovement).y;
        self.setDeltaMovement(direction.adjustVec(new Vector3d(stopped.x, impactY, stopped.z)));

        BlockPos pos = this.getOnPos();
        this.level.getBlockState(pos).getBlock().updateEntityAfterFallOn(this.level, self);
    }

    
















    @Redirect(
            method = "move",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;getHorizontalDistanceSqr(Lnet/minecraft/util/math/vector/Vector3d;)D"
            )
    )
    private double gravitymod$relativeWalkDistance(Vector3d allowedMovement) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection((Entity) (Object) this);
        if (direction == EnumGravityDirection.DOWN) {
            return allowedMovement.x * allowedMovement.x + allowedMovement.z * allowedMovement.z;
        }
        return gravitymod$relHorizontalDistanceSqr(direction, allowedMovement);
    }

    

































    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private void gravitymod$relativeCollide(Vector3d pVec, CallbackInfoReturnable<Vector3d> cir) {
        Entity self = (Entity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);

        
        
        
        
        
        
        
        
        
        Vector3d moveVec = this.gravitymod$effectiveMoveDelta(pVec);

        
        
        
        
        
        if (CubeModelCollisionHelper.usesModelCollision(self)) {
            Vector3d modelResult;
            if (CubePhysics.controls(self)) {
                
                
                
                modelResult = CubePhysics.step(self, moveVec, direction);
            }
            else {
                
                
                
                CubeModelCollisionHelper.clampFacing(self);
                modelResult = CubeModelCollisionHelper.collide(self, moveVec, direction);
            }
            if (direction != EnumGravityDirection.DOWN) {
                this.gravitymod$allowedMoveDelta = modelResult;
            }
            cir.setReturnValue(modelResult);
            return;
        }

        
        
        
        
        List<OrientedBox> cubes = CubeModelCollisionHelper.solidCubes(self, moveVec, direction);

        
        
        
        
        
        if (direction == EnumGravityDirection.DOWN && cubes.isEmpty()) {
            return;
        }

        AxisAlignedBB box = this.getBoundingBox();
        ISelectionContext context = ISelectionContext.of(self);
        VoxelShape borderShape = this.level.getWorldBorder().getCollisionShape();
        Stream<VoxelShape> borderStream = VoxelShapes.joinIsNotEmpty(borderShape, VoxelShapes.create(box.deflate(1.0E-7D)), IBooleanFunction.AND)
                ? Stream.empty() : Stream.of(borderShape);
        
        
        
        Vector3d escape = CubeModelCollisionHelper.escapeFromCubes(cubes, box);
        
        
        
        
        Stream<VoxelShape> entityStream = this.level.getEntityCollisions(self,
                box.expandTowards(moveVec).expandTowards(escape.x, escape.y, escape.z),
                (entity) -> !CubeModelCollisionHelper.usesModelCollision(entity));
        
        
        
        
        Stream<VoxelShape> portalModStream = PortalModCollisionHelper.additionalCollisions(self, moveVec, direction);
        ReuseableStream<VoxelShape> potentialHits =
                new ReuseableStream<>(Stream.concat(Stream.concat(entityStream, borderStream), portalModStream));

        
        
        
        
        
        Vector3d ejection = Vector3d.ZERO;
        if (escape.lengthSqr() > 0.0D) {
            ejection = CubeModelCollisionHelper.collideAgainstCubes(self, escape, box, context, potentialHits, cubes);
            box = box.move(ejection);
        }

        Vector3d base = moveVec.lengthSqr() == 0.0D ? moveVec
                : CubeModelCollisionHelper.collideAgainstCubes(self, moveVec, box, context, potentialHits, cubes);

        Vector3d relIntended = direction.inverseAdjustVec(moveVec);
        Vector3d relBase = direction.inverseAdjustVec(base);
        boolean relFlagHorizontal = relIntended.x != relBase.x || relIntended.z != relBase.z;
        boolean relFlagVertical = relIntended.y != relBase.y;
        boolean groundedOrFalling = this.onGround || (relFlagVertical && relIntended.y < 0.0D);

        Vector3d result = base;
        if (this.maxUpStep > 0.0F && groundedOrFalling && relFlagHorizontal) {
            double step = this.maxUpStep;
            
            
            Vector3d relHorizontalMove = direction.adjustVec(new Vector3d(relIntended.x, 0.0D, relIntended.z));
            Vector3d relUpStep = direction.adjustVec(new Vector3d(0.0D, step, 0.0D));

            
            
            
            
            
            
            Vector3d plainLift = CubeModelCollisionHelper.collideAgainstCubes(self, relUpStep, box, context, potentialHits, cubes);
            Vector3d stepped = CubeModelCollisionHelper.collideAgainstCubes(self, relHorizontalMove, box.move(plainLift), context, potentialHits, cubes).add(plainLift);

            Vector3d liftOnly = CubeModelCollisionHelper.collideAgainstCubes(self, relUpStep,
                    box.expandTowards(relHorizontalMove.x, relHorizontalMove.y, relHorizontalMove.z), context, potentialHits, cubes);
            if (direction.inverseAdjustVec(liftOnly).y < step) {
                Vector3d partialStep = CubeModelCollisionHelper.collideAgainstCubes(self, relHorizontalMove, box.move(liftOnly), context, potentialHits, cubes).add(liftOnly);
                if (gravitymod$relHorizontalDistanceSqr(direction, partialStep) > gravitymod$relHorizontalDistanceSqr(direction, stepped)) {
                    stepped = partialStep;
                }
            }

            if (gravitymod$relHorizontalDistanceSqr(direction, stepped) > gravitymod$relHorizontalDistanceSqr(direction, base)) {
                
                double settle = -direction.inverseAdjustVec(stepped).y + relIntended.y;
                Vector3d settleMove = direction.adjustVec(new Vector3d(0.0D, settle, 0.0D));
                result = stepped.add(CubeModelCollisionHelper.collideAgainstCubes(self, settleMove, box.move(stepped), context, potentialHits, cubes));
            }
        }

        
        
        
        
        result = ejection.add(result);

        
        
        
        
        if (direction != EnumGravityDirection.DOWN) {
            this.gravitymod$allowedMoveDelta = result;
        }
        cir.setReturnValue(result);
    }

    @org.spongepowered.asm.mixin.Unique
    private static double gravitymod$relHorizontalDistanceSqr(EnumGravityDirection direction, Vector3d worldVec) {
        Vector3d relative = direction.inverseAdjustVec(worldVec);
        return relative.x * relative.x + relative.z * relative.z;
    }

    














    @Inject(
            method = "move",
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;onGround:Z", opcode = org.objectweb.asm.Opcodes.PUTFIELD, ordinal = 0, shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void gravitymod$fixOnGround(MoverType moverType, Vector3d pPos, CallbackInfo ci, Vector3d actualMovement) {
        this.gravitymod$actualMovement = actualMovement;
        
        
        this.gravitymod$preLandingMovement = this.getDeltaMovement();
        Entity self = (Entity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction != EnumGravityDirection.DOWN) {
            Vector3d relativeIntended = direction.inverseAdjustVec(this.gravitymod$effectiveMoveDelta(pPos));
            Vector3d relativeAllowed = direction.inverseAdjustVec(actualMovement);
            boolean relativeVerticalCollision = relativeIntended.y != relativeAllowed.y;
            self.setOnGround(relativeVerticalCollision && relativeIntended.y < 0.0D);
            
            
            
            
            
            
            
            this.horizontalCollision = !MathHelper.equal(relativeIntended.x, relativeAllowed.x)
                    || !MathHelper.equal(relativeIntended.z, relativeAllowed.z);
        }

        
        
        if (!this.onGround && this.gravitymod$isRestingOnRelativeGround(self, direction)) {
            self.setOnGround(true);
        }
    }

    

























    @org.spongepowered.asm.mixin.Unique
    private boolean gravitymod$isRestingOnRelativeGround(Entity self, EnumGravityDirection direction) {
        if (GravityPhysicsHelper.getStrength(self) != 0.0D) {
            return false;
        }
        if (self instanceof net.minecraft.entity.player.PlayerEntity
                && ((net.minecraft.entity.player.PlayerEntity) self).abilities.flying) {
            return false;
        }
        Vector3d probe = direction.adjustVec(new Vector3d(0.0D, -ZERO_GRAVITY_GROUND_CONTACT_PROBE, 0.0D));
        
        
        return !CubeModelCollisionHelper.noCollision(self, this.getBoundingBox().move(probe));
    }

    




    @Redirect(
            method = "move",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;checkFallDamage(DZLnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)V")
    )
    private void gravitymod$fixFallDamageAxis(Entity self, double yMovement, boolean onGround, net.minecraft.block.BlockState state, BlockPos pos) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            this.checkFallDamage(yMovement, onGround, state, pos);
        }
        else {
            double relativeY = direction.inverseAdjustVec(this.gravitymod$actualMovement).y;
            this.checkFallDamage(relativeY, onGround, state, pos);
        }
    }

    






































    @Inject(method = "push(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void gravitymod$push(Entity other, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!gravitymod$shoveIsRealContact(self, other)) {
            ci.cancel();
            return;
        }
        EnumGravityDirection selfDirection = GravityPhysicsHelper.getDirection(self);
        EnumGravityDirection otherDirection = GravityPhysicsHelper.getDirection(other);
        if (selfDirection == EnumGravityDirection.DOWN && otherDirection == EnumGravityDirection.DOWN) {
            return;
        }
        ci.cancel();
        if (self.isPassengerOfSameVehicle(other) || other.noPhysics || this.noPhysics) {
            return;
        }

        Vector3d separation = other.getBoundingBox().getCenter().subtract(self.getBoundingBox().getCenter());
        
        if (!self.isVehicle()) {
            Vector3d push = gravitymod$collisionSeparation(separation, selfDirection, this.pushthrough);
            if (push != null) {
                self.push(-push.x, -push.y, -push.z);
            }
        }
        if (!other.isVehicle()) {
            Vector3d push = gravitymod$collisionSeparation(separation, otherDirection, this.pushthrough);
            if (push != null) {
                other.push(push.x, push.y, push.z);
            }
        }
    }

    




    @org.spongepowered.asm.mixin.Unique
    private static boolean gravitymod$shoveIsRealContact(Entity self, Entity other) {
        boolean selfIsCube = CubeModelCollisionHelper.usesModelCollision(self);
        boolean otherIsCube = CubeModelCollisionHelper.usesModelCollision(other);
        if (!selfIsCube && !otherIsCube) {
            return true;
        }
        if (selfIsCube && otherIsCube) {
            return false;
        }
        Entity cube = selfIsCube ? self : other;
        Entity shoved = selfIsCube ? other : self;
        return CubeModelCollisionHelper.modelBox(cube).intersects(shoved.getBoundingBox());
    }

    





    @org.spongepowered.asm.mixin.Unique
    private static Vector3d gravitymod$collisionSeparation(Vector3d worldSeparation, EnumGravityDirection direction,
                                                           float pushthrough) {
        Vector3d relative = direction.inverseAdjustVec(worldSeparation);
        double d0 = relative.x;
        double d1 = relative.z;
        double d2 = MathHelper.absMax(d0, d1);
        if (d2 < (double) 0.01F) {
            return null;
        }
        d2 = (double) MathHelper.sqrt(d2);
        d0 = d0 / d2;
        d1 = d1 / d2;
        double d3 = 1.0D / d2;
        if (d3 > 1.0D) {
            d3 = 1.0D;
        }
        d0 = d0 * d3 * (double) 0.05F * (double) (1.0F - pushthrough);
        d1 = d1 * d3 * (double) 0.05F * (double) (1.0F - pushthrough);
        return direction.adjustVec(new Vector3d(d0, 0.0D, d1));
    }

    



    @Inject(method = "moveRelative", at = @At("HEAD"), cancellable = true)
    private void gravitymod$moveRelative(float amount, Vector3d relative, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        double lengthSqr = relative.x * relative.x + relative.y * relative.y + relative.z * relative.z;
        if (lengthSqr < 1.0E-7D) {
            ci.cancel();
            return;
        }
        Vector3d scaled = (lengthSqr > 1.0D ? relative.normalize() : relative).scale((double) amount);
        float yawRad = self.yRot * ((float) Math.PI / 180F);
        float sin = net.minecraft.util.math.MathHelper.sin(yawRad);
        float cos = net.minecraft.util.math.MathHelper.cos(yawRad);
        Vector3d relativeSpace = new Vector3d(scaled.x * cos - scaled.z * sin, scaled.y, scaled.z * cos + scaled.x * sin);
        Vector3d worldSpace = direction.adjustVec(relativeSpace);
        this.setDeltaMovement(this.getDeltaMovement().add(worldSpace));
        ci.cancel();
    }

    




    @Inject(method = "calculateViewVector", at = @At("RETURN"), cancellable = true)
    private void gravitymod$adjustViewVector(float xRot, float yRot, CallbackInfoReturnable<Vector3d> cir) {
        Entity self = (Entity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        cir.setReturnValue(direction.adjustVec(cir.getReturnValue()));
    }

    




















    @Inject(method = "getDirection", at = @At("HEAD"), cancellable = true)
    private void gravitymod$adjustHorizontalFacing(CallbackInfoReturnable<Direction> cir) {
        Entity self = (Entity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        cir.setReturnValue(gravitymod$worldHorizontalFacing(self, direction));
    }

    














    @org.spongepowered.asm.mixin.Unique
    private static Direction gravitymod$worldHorizontalFacing(Entity self, EnumGravityDirection direction) {
        
        Direction fromLook = gravitymod$nearestHorizontal(self.getViewVector(1.0F));
        if (fromLook != null) {
            return fromLook;
        }
        float yawRad = self.yRot * ((float) Math.PI / 180F);
        Vector3d relativeForward = new Vector3d(-MathHelper.sin(yawRad), 0.0D, MathHelper.cos(yawRad));
        Direction fromYaw = gravitymod$nearestHorizontal(direction.adjustVec(relativeForward));
        if (fromYaw != null) {
            return fromYaw;
        }
        return direction.getRelativeUp().getFacingEquivalent();
    }

    




    @org.spongepowered.asm.mixin.Unique
    private static Direction gravitymod$nearestHorizontal(Vector3d vec) {
        if (Math.abs(vec.x) < 1.0E-4D && Math.abs(vec.z) < 1.0E-4D) {
            return null;
        }
        if (Math.abs(vec.x) > Math.abs(vec.z)) {
            return vec.x > 0.0D ? Direction.EAST : Direction.WEST;
        }
        return vec.z > 0.0D ? Direction.SOUTH : Direction.NORTH;
    }

    







    @Inject(method = "getEyePosition", at = @At("HEAD"), cancellable = true)
    private void gravitymod$fixEyePosition(float partialTicks, CallbackInfoReturnable<Vector3d> cir) {
        Entity self = (Entity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        double x = net.minecraft.util.math.MathHelper.lerp((double) partialTicks, self.xo, self.getX());
        double y = net.minecraft.util.math.MathHelper.lerp((double) partialTicks, self.yo, self.getY());
        double z = net.minecraft.util.math.MathHelper.lerp((double) partialTicks, self.zo, self.getZ());
        double[] relativeUp = direction.adjustXYZValues(0.0, 1.0, 0.0);
        double eyeOffset = this.getEyeHeight();
        cir.setReturnValue(new Vector3d(
                x + relativeUp[0] * eyeOffset,
                y + relativeUp[1] * eyeOffset,
                z + relativeUp[2] * eyeOffset
        ));
    }

    













    @Inject(method = "updateFluidOnEyes", at = @At("HEAD"), cancellable = true)
    private void gravitymod$fixFluidOnEyes(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        this.wasEyeInWater = self.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
        this.fluidOnEyes = null;
        ci.cancel();

        Vector3d sample = GravityPhysicsHelper.getRelativeEyePosition(self)
                .subtract(direction.adjustVec(new Vector3d(0.0D, 0.11111111D, 0.0D)));

        Entity vehicle = self.getVehicle();
        if (vehicle instanceof net.minecraft.entity.item.BoatEntity) {
            net.minecraft.entity.item.BoatEntity boat = (net.minecraft.entity.item.BoatEntity) vehicle;
            if (!boat.isUnderWater() && boat.getBoundingBox().maxY >= sample.y && boat.getBoundingBox().minY <= sample.y) {
                return;
            }
        }

        BlockPos samplePos = new BlockPos(sample);
        net.minecraft.fluid.FluidState fluidState = this.level.getFluidState(samplePos);
        for (net.minecraft.tags.ITag.INamedTag<net.minecraft.fluid.Fluid> tag : net.minecraft.tags.FluidTags.getWrappers()) {
            if (fluidState.is(tag)) {
                double surfaceY = (double) ((float) samplePos.getY() + fluidState.getHeight(this.level, samplePos));
                if (surfaceY > sample.y) {
                    this.fluidOnEyes = tag;
                }
                return;
            }
        }
    }

    









    @Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
    private void gravitymod$fixSuffocationCheck(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        if (this.noPhysics) {
            cir.setReturnValue(false);
            return;
        }
        double halfWidth = self.getBbWidth() * 0.8D / 2.0D;
        double halfThickness = 0.1D / 2.0D;
        double[] cornerA = direction.adjustXYZValues(-halfWidth, -halfThickness, -halfWidth);
        double[] cornerB = direction.adjustXYZValues(halfWidth, halfThickness, halfWidth);
        Vector3d eye = GravityPhysicsHelper.getRelativeEyePosition(self);
        AxisAlignedBB box = new AxisAlignedBB(cornerA[0], cornerA[1], cornerA[2], cornerB[0], cornerB[1], cornerB[2])
                .move(eye.x, eye.y, eye.z);
        boolean suffocating = this.level.getBlockCollisions(self, box,
                (state, pos) -> state.isSuffocating(this.level, pos)).findAny().isPresent();
        cir.setReturnValue(suffocating);
    }
}
