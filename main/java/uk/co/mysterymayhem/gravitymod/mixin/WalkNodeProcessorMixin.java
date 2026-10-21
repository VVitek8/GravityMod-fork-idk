package uk.co.mysterymayhem.gravitymod.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.MobEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.pathfinding.NodeProcessor;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.pathfinding.PathType;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPathHelper;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;

import java.util.EnumSet;

import javax.annotation.Nullable;























@Mixin(WalkNodeProcessor.class)
public abstract class WalkNodeProcessorMixin extends NodeProcessor {

    




    @Unique
    private static final int GRAVITYMOD$SCAN_LIMIT = 256;

    @Shadow
    @Final
    private Long2ObjectMap<PathNodeType> pathTypesByPosCache;

    @Shadow
    @Final
    private Object2BooleanMap<AxisAlignedBB> collisionCache;

    @Shadow
    protected static PathNodeType getBlockPathTypeRaw(IBlockReader reader, BlockPos pos) {
        throw new AssertionError("@Shadow");
    }

    @Shadow
    protected abstract PathNodeType evaluateBlockPathType(IBlockReader reader, boolean canOpenDoors,
                                                          boolean canPassDoors, BlockPos mobPos, PathNodeType type);

    @Unique
    private EnumGravityDirection gravitymod$direction() {
        return this.mob == null ? EnumGravityDirection.DOWN : GravityPhysicsHelper.getDirection(this.mob);
    }

    
    
    

    








    @Inject(
            method = "getBlockPathType(Lnet/minecraft/world/IBlockReader;III)Lnet/minecraft/pathfinding/PathNodeType;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void gravitymod$blockPathType(IBlockReader reader, int x, int y, int z,
                                          CallbackInfoReturnable<PathNodeType> cir) {
        EnumGravityDirection direction = this.gravitymod$direction();
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        cir.setReturnValue(this.gravitymod$pathTypeAt(reader, direction, x, y, z));
    }

    
    @Unique
    private PathNodeType gravitymod$pathTypeAt(IBlockReader reader, EnumGravityDirection direction, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        PathNodeType type = getBlockPathTypeRaw(reader, pos);
        BlockPos relative = GravityPathHelper.toRelativeBlock(direction, x, y, z);

        if (type == PathNodeType.OPEN) {
            BlockPos below = GravityPathHelper.toWorldBlock(direction, relative.getX(), relative.getY() - 1, relative.getZ());
            PathNodeType belowType = getBlockPathTypeRaw(reader, below);
            type = belowType != PathNodeType.WALKABLE && belowType != PathNodeType.OPEN
                    && belowType != PathNodeType.WATER && belowType != PathNodeType.LAVA
                    ? PathNodeType.WALKABLE : PathNodeType.OPEN;
            if (belowType == PathNodeType.DAMAGE_FIRE) {
                type = PathNodeType.DAMAGE_FIRE;
            }
            if (belowType == PathNodeType.DAMAGE_CACTUS) {
                type = PathNodeType.DAMAGE_CACTUS;
            }
            if (belowType == PathNodeType.DAMAGE_OTHER) {
                type = PathNodeType.DAMAGE_OTHER;
            }
            if (belowType == PathNodeType.STICKY_HONEY) {
                type = PathNodeType.STICKY_HONEY;
            }
        }

        if (type == PathNodeType.WALKABLE) {
            type = this.gravitymod$checkNeighbourBlocks(reader, direction, relative, type);
        }
        return type;
    }

    






    @Unique
    private PathNodeType gravitymod$checkNeighbourBlocks(IBlockReader reader, EnumGravityDirection direction,
                                                         BlockPos relative, PathNodeType fallback) {
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos pos = GravityPathHelper.toWorldBlock(direction,
                            relative.getX() + dx, relative.getY() + dy, relative.getZ() + dz);
                    BlockState state = reader.getBlockState(pos);
                    if (state.is(Blocks.CACTUS)) {
                        return PathNodeType.DANGER_CACTUS;
                    }
                    if (state.is(Blocks.SWEET_BERRY_BUSH)) {
                        return PathNodeType.DANGER_OTHER;
                    }
                    if (state.is(BlockTags.FIRE) || state.is(Blocks.LAVA) || state.is(Blocks.MAGMA_BLOCK)
                            || CampfireBlock.isLitCampfire(state)) {
                        return PathNodeType.DANGER_FIRE;
                    }
                    if (reader.getFluidState(pos).is(FluidTags.WATER)) {
                        return PathNodeType.WATER_BORDER;
                    }
                }
            }
        }
        return fallback;
    }

    






    @Inject(
            method = "getBlockPathType(Lnet/minecraft/world/IBlockReader;IIILnet/minecraft/entity/MobEntity;IIIZZ)Lnet/minecraft/pathfinding/PathNodeType;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void gravitymod$sizedBlockPathType(IBlockReader reader, int x, int y, int z, MobEntity mob,
                                               int width, int height, int depth, boolean canOpenDoors,
                                               boolean canPassDoors, CallbackInfoReturnable<PathNodeType> cir) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(mob);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }

        EnumSet<PathNodeType> found = EnumSet.noneOf(PathNodeType.class);
        PathNodeType centre = PathNodeType.BLOCKED;
        BlockPos mobPos = mob.blockPosition();
        BlockPos base = GravityPathHelper.toRelativeBlock(direction, x, y, z);

        for (int i = 0; i < width; ++i) {
            for (int j = 0; j < height; ++j) {
                for (int k = 0; k < depth; ++k) {
                    BlockPos world = GravityPathHelper.toWorldBlock(direction,
                            base.getX() + i, base.getY() + j, base.getZ() + k);
                    
                    
                    
                    
                    PathNodeType type = this.getBlockPathType(reader, world.getX(), world.getY(), world.getZ());
                    type = this.evaluateBlockPathType(reader, canOpenDoors, canPassDoors, mobPos, type);
                    if (i == 0 && j == 0 && k == 0) {
                        centre = type;
                    }
                    found.add(type);
                }
            }
        }

        
        if (found.contains(PathNodeType.FENCE)) {
            cir.setReturnValue(PathNodeType.FENCE);
            return;
        }
        if (found.contains(PathNodeType.UNPASSABLE_RAIL)) {
            cir.setReturnValue(PathNodeType.UNPASSABLE_RAIL);
            return;
        }
        PathNodeType best = PathNodeType.BLOCKED;
        for (PathNodeType type : found) {
            if (mob.getPathfindingMalus(type) < 0.0F) {
                cir.setReturnValue(type);
                return;
            }
            if (mob.getPathfindingMalus(type) >= mob.getPathfindingMalus(best)) {
                best = type;
            }
        }
        cir.setReturnValue(centre == PathNodeType.OPEN && mob.getPathfindingMalus(best) == 0.0F && width <= 1
                ? PathNodeType.OPEN : best);
    }

    
    
    

    



    @Unique
    private PathNodeType gravitymod$cachedType(EnumGravityDirection direction, int relX, int relY, int relZ) {
        BlockPos world = GravityPathHelper.toWorldBlock(direction, relX, relY, relZ);
        return this.pathTypesByPosCache.computeIfAbsent(world.asLong(), (key) ->
                this.getBlockPathType(this.level, world.getX(), world.getY(), world.getZ(), this.mob,
                        this.entityWidth, this.entityHeight, this.entityDepth,
                        this.canOpenDoors(), this.canPassDoors()));
    }

    @Unique
    private PathPoint gravitymod$node(EnumGravityDirection direction, int relX, int relY, int relZ) {
        BlockPos world = GravityPathHelper.toWorldBlock(direction, relX, relY, relZ);
        return this.getNode(world.getX(), world.getY(), world.getZ());
    }

    @Unique
    private int gravitymod$relativeY(EnumGravityDirection direction, PathPoint node) {
        return GravityPathHelper.toRelativeBlock(direction, node.x, node.y, node.z).getY();
    }

    @Unique
    private boolean gravitymod$outOfWorld(EnumGravityDirection direction, int relX, int relY, int relZ) {
        BlockPos world = GravityPathHelper.toWorldBlock(direction, relX, relY, relZ);
        return world.getY() < 0 || world.getY() >= this.mob.level.getMaxBuildHeight();
    }

    @Unique
    private boolean gravitymod$hasCollisions(AxisAlignedBB box) {
        return this.collisionCache.computeIfAbsent(box, (key) -> !this.level.noCollision(this.mob, box));
    }

    
    
    

    




    @Inject(method = "getStart", at = @At("HEAD"), cancellable = true)
    private void gravitymod$getStart(CallbackInfoReturnable<PathPoint> cir) {
        EnumGravityDirection direction = this.gravitymod$direction();
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }

        
        
        Vector3d relativePos = direction.inverseAdjustVec(this.mob.position());
        int relX = MathHelper.floor(relativePos.x);
        int relZ = MathHelper.floor(relativePos.z);
        int relY = MathHelper.floor(relativePos.y);

        BlockState state = this.level.getBlockState(GravityPathHelper.toWorldBlock(direction, relX, relY, relZ));
        if (!this.mob.canStandOnFluid(state.getFluidState().getType())) {
            if (this.canFloat() && this.mob.isInWater()) {
                int guard = 0;
                while (guard++ < GRAVITYMOD$SCAN_LIMIT) {
                    if (state.getBlock() != Blocks.WATER && state.getFluidState() != Fluids.WATER.getSource(false)) {
                        --relY;
                        break;
                    }
                    ++relY;
                    state = this.level.getBlockState(GravityPathHelper.toWorldBlock(direction, relX, relY, relZ));
                }
            }
            else if (this.mob.isOnGround()) {
                relY = MathHelper.floor(relativePos.y + 0.5D);
            }
            else {
                int scan = relY;
                int guard = 0;
                while (guard++ < GRAVITYMOD$SCAN_LIMIT && !this.gravitymod$outOfWorld(direction, relX, scan, relZ)) {
                    BlockPos world = GravityPathHelper.toWorldBlock(direction, relX, scan, relZ);
                    BlockState scanned = this.level.getBlockState(world);
                    if (!scanned.isAir() && !scanned.isPathfindable(this.level, world, PathType.LAND)) {
                        break;
                    }
                    --scan;
                }
                relY = scan + 1;
            }
        }
        else {
            int guard = 0;
            while (this.mob.canStandOnFluid(state.getFluidState().getType()) && guard++ < GRAVITYMOD$SCAN_LIMIT) {
                ++relY;
                state = this.level.getBlockState(GravityPathHelper.toWorldBlock(direction, relX, relY, relZ));
            }
            --relY;
        }

        PathNodeType type = this.gravitymod$cachedType(direction, relX, relY, relZ);
        if (this.mob.getPathfindingMalus(type) < 0.0F) {
            
            
            
            
            double halfWidth = this.mob.getBbWidth() / 2.0D;
            int minX = MathHelper.floor(relativePos.x - halfWidth);
            int maxX = MathHelper.floor(relativePos.x + halfWidth);
            int minZ = MathHelper.floor(relativePos.z - halfWidth);
            int maxZ = MathHelper.floor(relativePos.z + halfWidth);
            int[][] corners = {{minX, minZ}, {minX, maxZ}, {maxX, minZ}, {maxX, maxZ}};
            int[] last = null;
            boolean survivable = false;
            for (int[] corner : corners) {
                
                
                last = corner;
                if (this.mob.getPathfindingMalus(this.gravitymod$cachedType(direction, corner[0], relY, corner[1])) >= 0.0F) {
                    survivable = true;
                    break;
                }
            }
            if (survivable) {
                PathPoint corner = this.gravitymod$node(direction, last[0], relY, last[1]);
                corner.type = this.gravitymod$cachedType(direction, last[0], relY, last[1]);
                corner.costMalus = this.mob.getPathfindingMalus(corner.type);
                cir.setReturnValue(corner);
                return;
            }
        }

        PathPoint start = this.gravitymod$node(direction, relX, relY, relZ);
        start.type = this.gravitymod$cachedType(direction, relX, relY, relZ);
        start.costMalus = this.mob.getPathfindingMalus(start.type);
        cir.setReturnValue(start);
    }

    
    
    

    



    @Inject(method = "getNeighbors", at = @At("HEAD"), cancellable = true)
    private void gravitymod$getNeighbors(PathPoint[] out, PathPoint node, CallbackInfoReturnable<Integer> cir) {
        EnumGravityDirection direction = this.gravitymod$direction();
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }

        BlockPos relative = GravityPathHelper.toRelativeBlock(direction, node.x, node.y, node.z);
        int relX = relative.getX();
        int relY = relative.getY();
        int relZ = relative.getZ();

        int count = 0;
        int stepUp = 0;
        PathNodeType above = this.gravitymod$cachedType(direction, relX, relY + 1, relZ);
        PathNodeType here = this.gravitymod$cachedType(direction, relX, relY, relZ);
        if (this.mob.getPathfindingMalus(above) >= 0.0F && here != PathNodeType.STICKY_HONEY) {
            stepUp = MathHelper.floor(Math.max(1.0F, this.mob.maxUpStep));
        }

        double floor = GravityPathHelper.relativeFloorLevel(this.level, direction, relX, relY, relZ);

        PathPoint south = this.gravitymod$landNode(direction, relX, relY, relZ + 1, stepUp, floor, 0, 1, here);
        if (this.gravitymod$isNeighborValid(south, node)) {
            out[count++] = south;
        }
        PathPoint west = this.gravitymod$landNode(direction, relX - 1, relY, relZ, stepUp, floor, -1, 0, here);
        if (this.gravitymod$isNeighborValid(west, node)) {
            out[count++] = west;
        }
        PathPoint east = this.gravitymod$landNode(direction, relX + 1, relY, relZ, stepUp, floor, 1, 0, here);
        if (this.gravitymod$isNeighborValid(east, node)) {
            out[count++] = east;
        }
        PathPoint north = this.gravitymod$landNode(direction, relX, relY, relZ - 1, stepUp, floor, 0, -1, here);
        if (this.gravitymod$isNeighborValid(north, node)) {
            out[count++] = north;
        }

        PathPoint northWest = this.gravitymod$landNode(direction, relX - 1, relY, relZ - 1, stepUp, floor, 0, -1, here);
        if (this.gravitymod$isDiagonalValid(direction, node, west, north, northWest)) {
            out[count++] = northWest;
        }
        PathPoint northEast = this.gravitymod$landNode(direction, relX + 1, relY, relZ - 1, stepUp, floor, 0, -1, here);
        if (this.gravitymod$isDiagonalValid(direction, node, east, north, northEast)) {
            out[count++] = northEast;
        }
        PathPoint southWest = this.gravitymod$landNode(direction, relX - 1, relY, relZ + 1, stepUp, floor, 0, 1, here);
        if (this.gravitymod$isDiagonalValid(direction, node, west, south, southWest)) {
            out[count++] = southWest;
        }
        PathPoint southEast = this.gravitymod$landNode(direction, relX + 1, relY, relZ + 1, stepUp, floor, 0, 1, here);
        if (this.gravitymod$isDiagonalValid(direction, node, east, south, southEast)) {
            out[count++] = southEast;
        }

        cir.setReturnValue(count);
    }

    @Unique
    private boolean gravitymod$isNeighborValid(@Nullable PathPoint candidate, PathPoint from) {
        return candidate != null && !candidate.closed && (candidate.costMalus >= 0.0F || from.costMalus < 0.0F);
    }

    




    @Unique
    private boolean gravitymod$isDiagonalValid(EnumGravityDirection direction, PathPoint centre,
                                               @Nullable PathPoint sideA, @Nullable PathPoint sideB,
                                               @Nullable PathPoint diagonal) {
        if (diagonal == null || sideB == null || sideA == null || diagonal.closed) {
            return false;
        }
        int centreY = this.gravitymod$relativeY(direction, centre);
        int aY = this.gravitymod$relativeY(direction, sideA);
        int bY = this.gravitymod$relativeY(direction, sideB);
        if (bY > centreY || aY > centreY) {
            return false;
        }
        if (sideA.type == PathNodeType.WALKABLE_DOOR || sideB.type == PathNodeType.WALKABLE_DOOR
                || diagonal.type == PathNodeType.WALKABLE_DOOR) {
            return false;
        }
        boolean narrowThroughFences = sideB.type == PathNodeType.FENCE && sideA.type == PathNodeType.FENCE
                && (double) this.mob.getBbWidth() < 0.5D;
        return diagonal.costMalus >= 0.0F
                && (bY < centreY || sideB.costMalus >= 0.0F || narrowThroughFences)
                && (aY < centreY || sideA.costMalus >= 0.0F || narrowThroughFences);
    }

    







    @Unique
    @Nullable
    private PathPoint gravitymod$landNode(EnumGravityDirection direction, int relX, int relY, int relZ,
                                          int stepUpBudget, double floorFrom,
                                          int stepBackRelX, int stepBackRelZ, PathNodeType fromType) {
        PathPoint node = null;
        double floorHere = GravityPathHelper.relativeFloorLevel(this.level, direction, relX, relY, relZ);
        if (floorHere - floorFrom > 1.125D) {
            return null;
        }

        PathNodeType type = this.gravitymod$cachedType(direction, relX, relY, relZ);
        float malus = this.mob.getPathfindingMalus(type);
        double halfWidth = (double) this.mob.getBbWidth() / 2.0D;
        if (malus >= 0.0F) {
            node = this.gravitymod$node(direction, relX, relY, relZ);
            node.type = type;
            node.costMalus = Math.max(node.costMalus, malus);
        }

        if (fromType == PathNodeType.FENCE && node != null && node.costMalus >= 0.0F
                && !this.gravitymod$canReachWithoutCollision(node)) {
            node = null;
        }

        if (type == PathNodeType.WALKABLE) {
            return node;
        }

        if ((node == null || node.costMalus < 0.0F) && stepUpBudget > 0 && type != PathNodeType.FENCE
                && type != PathNodeType.UNPASSABLE_RAIL && type != PathNodeType.TRAPDOOR) {
            node = this.gravitymod$landNode(direction, relX, relY + 1, relZ, stepUpBudget - 1, floorFrom,
                    stepBackRelX, stepBackRelZ, fromType);
            if (node != null && (node.type == PathNodeType.OPEN || node.type == PathNodeType.WALKABLE)
                    && this.mob.getBbWidth() < 1.0F) {
                
                
                
                
                double centreX = (double) (relX - stepBackRelX) + 0.5D;
                double centreZ = (double) (relZ - stepBackRelZ) + 0.5D;
                BlockPos landed = GravityPathHelper.toRelativeBlock(direction, node.x, node.y, node.z);
                double bottom = GravityPathHelper.relativeFloorLevel(this.level, direction,
                        MathHelper.floor(centreX), relY + 1, MathHelper.floor(centreZ)) + 0.001D;
                double top = (double) this.mob.getBbHeight() + GravityPathHelper.relativeFloorLevel(this.level,
                        direction, landed.getX(), landed.getY(), landed.getZ()) - 0.002D;
                AxisAlignedBB relativeBox = new AxisAlignedBB(
                        centreX - halfWidth, bottom, centreZ - halfWidth,
                        centreX + halfWidth, top, centreZ + halfWidth);
                if (this.gravitymod$hasCollisions(GravityPathHelper.toWorldBox(direction, relativeBox))) {
                    node = null;
                }
            }
        }

        if (type == PathNodeType.WATER && !this.canFloat()) {
            if (this.gravitymod$cachedType(direction, relX, relY - 1, relZ) != PathNodeType.WATER) {
                return node;
            }
            int guard = 0;
            while (guard++ < GRAVITYMOD$SCAN_LIMIT && !this.gravitymod$outOfWorld(direction, relX, relY - 1, relZ)) {
                --relY;
                type = this.gravitymod$cachedType(direction, relX, relY, relZ);
                if (type != PathNodeType.WATER) {
                    return node;
                }
                node = this.gravitymod$node(direction, relX, relY, relZ);
                node.type = type;
                node.costMalus = Math.max(node.costMalus, this.mob.getPathfindingMalus(type));
            }
        }

        if (type == PathNodeType.OPEN) {
            int fallen = 0;
            int startRelY = relY;
            while (type == PathNodeType.OPEN) {
                --relY;
                if (this.gravitymod$outOfWorld(direction, relX, relY, relZ)) {
                    PathPoint blocked = this.gravitymod$node(direction, relX, startRelY, relZ);
                    blocked.type = PathNodeType.BLOCKED;
                    blocked.costMalus = -1.0F;
                    return blocked;
                }
                if (fallen++ >= this.mob.getMaxFallDistance()) {
                    PathPoint blocked = this.gravitymod$node(direction, relX, relY, relZ);
                    blocked.type = PathNodeType.BLOCKED;
                    blocked.costMalus = -1.0F;
                    return blocked;
                }
                type = this.gravitymod$cachedType(direction, relX, relY, relZ);
                malus = this.mob.getPathfindingMalus(type);
                if (type != PathNodeType.OPEN && malus >= 0.0F) {
                    node = this.gravitymod$node(direction, relX, relY, relZ);
                    node.type = type;
                    node.costMalus = Math.max(node.costMalus, malus);
                    break;
                }
                if (malus < 0.0F) {
                    PathPoint blocked = this.gravitymod$node(direction, relX, relY, relZ);
                    blocked.type = PathNodeType.BLOCKED;
                    blocked.costMalus = -1.0F;
                    return blocked;
                }
            }
        }

        if (type == PathNodeType.FENCE) {
            node = this.gravitymod$node(direction, relX, relY, relZ);
            node.closed = true;
            node.type = type;
            node.costMalus = type.getMalus();
        }

        return node;
    }

    





    @Unique
    private boolean gravitymod$canReachWithoutCollision(PathPoint node) {
        Vector3d offset = new Vector3d((double) node.x - this.mob.getX(),
                (double) node.y - this.mob.getY(), (double) node.z - this.mob.getZ());
        AxisAlignedBB box = this.mob.getBoundingBox();
        int steps = MathHelper.ceil(offset.length() / box.getSize());
        offset = offset.scale(1.0F / (float) steps);
        for (int i = 1; i <= steps; ++i) {
            box = box.move(offset);
            if (this.gravitymod$hasCollisions(box)) {
                return false;
            }
        }
        return true;
    }
}
