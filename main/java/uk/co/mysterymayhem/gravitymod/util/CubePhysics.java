package uk.co.mysterymayhem.gravitymod.util;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlimeBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.ModList;
import net.portalmod.common.sorted.cube.Cube;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.network.CubeOrientationPacket;
import uk.co.mysterymayhem.gravitymod.network.PacketHandler;
import uk.co.mysterymayhem.gravitymod.world.ModGameRules;

import java.util.ArrayList;
import java.util.List;

































public final class CubePhysics {

    private CubePhysics() {}

    



    private static final double DEFAULT_FRICTION = 1.2D;

    
    private static final double DEFAULT_RESTITUTION = 0.12D;

    private static final double SLIME_RESTITUTION = 0.8D;

    private static final double BED_RESTITUTION = 0.55D;

    
    private static final double CUBE_FRICTION = 1.3D;

    private static final double CUBE_RESTITUTION = 0.2D;

    

















    private static final double FRICTION_FROM_SLIPPERINESS = 3.0D;

    



    private static final double MIN_FRICTION = 0.02D;

    private static final double MAX_FRICTION = 2.0D;

    
    private static final double REGION_MARGIN = 0.15D;

    
    private static final double STACK_WAKE_DISTANCE = 0.002D;

    private static final double STACK_WAKE_SPIN = 0.01D;

    
    private static final double BRACED_DOT = 0.35D;

    
    private static final double VANILLA_GRAVITY = 0.08D;

    private static final int FACE_COUNT = 6;

    
    private static final Direction[] FACES = {
            Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH};

    private static final Vector3d[] WORLD_AXES = {
            new Vector3d(1.0D, 0.0D, 0.0D), new Vector3d(0.0D, 1.0D, 0.0D), new Vector3d(0.0D, 0.0D, 1.0D)};

    
    private static final double FLUSH_TOLERANCE = 1.0E-9D;

    



    private static final boolean PORTAL_MOD_PRESENT = detectPortalMod();

    private static boolean detectPortalMod() {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded("portalmod");
    }

    
    
    

    





    public static boolean controls(Entity cube) {
        if (!CubeModelCollisionHelper.usesModelCollision(cube)) {
            return false;
        }
        if (cube.level == null || cube.level.isClientSide) {
            return false;
        }
        if (cube.isPassenger() || cube.noPhysics || cube.isSpectator()) {
            return false;
        }
        return !Bridge.isFizzling(cube);
    }

    
    public static CubeRigidBody bodyOf(Entity entity, boolean create) {
        if (!(entity instanceof ICubeRigidBodyHolder)) {
            return null;
        }
        ICubeRigidBodyHolder holder = (ICubeRigidBodyHolder) entity;
        CubeRigidBody body = holder.gravitymod$getCubeBody();
        if (body == null && create) {
            body = new CubeRigidBody();
            holder.gravitymod$setCubeBody(body);
        }
        return body;
    }

    












    public static CubeRigidBody orientationOf(Entity cube) {
        if (!ModGameRules.isCubePhysicsEnabled(cube.level)) {
            return null;
        }
        CubeRigidBody body = bodyOf(cube, false);
        return body != null && body.isControlling() ? body : null;
    }

    







    public static Vector3d centreOf(Entity cube, EnumGravityDirection direction) {
        return CubeModelCollisionHelper.centreOf(cube, direction);
    }

    
    
    

    






    public static Vector3d step(Entity cube, Vector3d movement, EnumGravityDirection direction) {
        CubeRigidBody body = bodyOf(cube, true);
        body.beginControl(cube, direction);
        body.configure(cube);
        Vector3d centre = centreOf(cube, direction);
        List<Obstacle> obstacles = collectObstacles(cube, movement, direction, regionFor(cube, centre, movement, body));
        Vector3d moved = body.step(cube, centre, movement, direction, tickGravityOf(cube), obstacles);
        if (moved.distanceToSqr(centre) > STACK_WAKE_DISTANCE * STACK_WAKE_DISTANCE
                || body.getAngularVelocity().lengthSqr() > STACK_WAKE_SPIN * STACK_WAKE_SPIN) {
            
            
            for (int i = 0; i < obstacles.size(); i++) {
                obstacles.get(i).disturb();
            }
        }
        writeBackFacing(cube, direction, body);
        
        
        syncIfNeeded(cube, body);
        return moved.subtract(centre);
    }

    







    private static double tickGravityOf(Entity cube) {
        return VANILLA_GRAVITY * GravityPhysicsHelper.getStrength(cube);
    }

    private static void syncIfNeeded(Entity cube, CubeRigidBody body) {
        if (body.needsSync()) {
            PacketHandler.sendToTracking(cube, packetFor(cube, body));
            body.markSynced();
        }
    }

    







    public static void afterMove(Entity cube) {
        CubeRigidBody body = bodyOf(cube, false);
        if (body == null) {
            return;
        }
        Vector3d velocity = body.takeSolvedVelocity();
        if (velocity == null) {
            return;
        }
        cube.setDeltaMovement(velocity);
        cube.setOnGround(body.isGrounded());
        if (body.isGrounded()) {
            cube.fallDistance = 0.0F;
        }
    }

    












    public static void onCubeTick(Entity cube) {
        if (cube.level == null) {
            return;
        }
        CubeRigidBody body = bodyOf(cube, false);
        if (body == null) {
            return;
        }
        if (cube.level.isClientSide) {
            body.advanceClientTick();
            return;
        }
        if (!controls(cube)) {
            
            
            body.releaseControl(cube, GravityPhysicsHelper.getDirection(cube));
        }
        syncIfNeeded(cube, body);
    }

    
    public static Object trackingPacket(Entity cube) {
        CubeRigidBody body = bodyOf(cube, false);
        return body == null ? null : packetFor(cube, body);
    }

    private static CubeOrientationPacket packetFor(Entity cube, CubeRigidBody body) {
        Vector3d spin = body.getAngularVelocity();
        return new CubeOrientationPacket(cube.getId(),
                body.orientationX(), body.orientationY(), body.orientationZ(), body.orientationW(),
                (float) spin.x, (float) spin.y, (float) spin.z, body.isControlling());
    }

    
    public static void acceptOrientation(Entity cube, float x, float y, float z, float w, Vector3d spin,
                                         boolean controlling) {
        CubeRigidBody body = bodyOf(cube, true);
        body.acceptFromNetwork(x, y, z, w, spin, controlling);
    }

    



    public static Quaternion renderOrientation(Entity cube, float partialTicks) {
        if (!ModGameRules.isCubePhysicsEnabled(cube.level)) {
            return null;
        }
        CubeRigidBody body = bodyOf(cube, false);
        if (body == null || !body.isControlling()) {
            return null;
        }
        return body.renderOrientation(partialTicks);
    }

    









    private static void writeBackFacing(Entity cube, EnumGravityDirection direction, CubeRigidBody body) {
        Vector3d front = direction.inverseAdjustVec(body.axisZ());
        Vector3d side = direction.inverseAdjustVec(body.axisX());
        float yaw;
        if (front.x * front.x + front.z * front.z >= side.x * side.x + side.z * side.z) {
            if (front.x * front.x + front.z * front.z < 1.0E-6D) {
                return;
            }
            yaw = (float) Math.toDegrees(Math.atan2(-front.x, front.z));
        }
        else {
            if (side.x * side.x + side.z * side.z < 1.0E-6D) {
                return;
            }
            yaw = (float) Math.toDegrees(Math.atan2(side.z, side.x));
        }
        yaw = MathHelper.wrapDegrees(yaw);
        cube.yRot = yaw;
        if (cube instanceof LivingEntity) {
            ((LivingEntity) cube).yBodyRot = yaw;
            ((LivingEntity) cube).yHeadRot = yaw;
        }
    }

    
    
    

    private static final String NBT_KEY = "gravitymod:CubePhysics";

    
    public static void save(Entity entity, CompoundNBT tag) {
        CubeRigidBody body = bodyOf(entity, false);
        if (body == null || !body.isControlling()) {
            return;
        }
        CompoundNBT physics = new CompoundNBT();
        body.save(physics);
        tag.put(NBT_KEY, physics);
    }

    public static void load(Entity entity, CompoundNBT tag) {
        if (!tag.contains(NBT_KEY)) {
            return;
        }
        CubeRigidBody body = bodyOf(entity, true);
        if (body != null) {
            body.load(tag.getCompound(NBT_KEY));
        }
    }

    
    
    

    
    private static AxisAlignedBB regionFor(Entity cube, Vector3d centre, Vector3d movement, CubeRigidBody body) {
        double halfSide = CubeModelCollisionHelper.modelHalfSide(cube);
        double halfHeight = CubeModelCollisionHelper.modelHalfHeight(cube);
        double radius = Math.sqrt(2.0D * halfSide * halfSide + halfHeight * halfHeight);
        AxisAlignedBB own = new AxisAlignedBB(
                centre.x - radius, centre.y - radius, centre.z - radius,
                centre.x + radius, centre.y + radius, centre.z + radius);
        double turn = body.getAngularVelocity().length() * radius;
        return own.expandTowards(movement.x, movement.y, movement.z).inflate(REGION_MARGIN + turn);
    }

    






    private static List<Obstacle> collectObstacles(Entity cube, Vector3d movement, EnumGravityDirection direction,
                                                   AxisAlignedBB region) {
        World level = cube.level;
        List<Obstacle> obstacles = new ArrayList<>();
        ISelectionContext context = ISelectionContext.of(cube);

        int minX = MathHelper.floor(region.minX);
        int minY = MathHelper.floor(region.minY);
        int minZ = MathHelper.floor(region.minZ);
        int maxX = MathHelper.floor(region.maxX);
        int maxY = MathHelper.floor(region.maxY);
        int maxZ = MathHelper.floor(region.maxZ);
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    if (!level.hasChunkAt(pos)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    VoxelShape shape = state.getCollisionShape(level, pos, context);
                    if (shape.isEmpty()) {
                        continue;
                    }
                    double friction = frictionOf(state, level, pos, cube);
                    double restitution = restitutionOf(state);
                    for (AxisAlignedBB box : shape.toAabbs()) {
                        AxisAlignedBB world = box.move(x, y, z);
                        if (!world.intersects(region)) {
                            continue;
                        }
                        obstacles.add(new Obstacle(world, friction, restitution, blockedFaces(level, pos, world)));
                    }
                }
            }
        }

        level.getEntityCollisions(cube, region, other -> true)
                .forEach(shape -> addShape(obstacles, region, shape, DEFAULT_FRICTION, DEFAULT_RESTITUTION));
        
        
        PortalModCollisionHelper.additionalCollisions(cube, movement, direction)
                .forEach(shape -> addShape(obstacles, region, shape, DEFAULT_FRICTION, DEFAULT_RESTITUTION));
        collectCubes(cube, direction, region, obstacles);

        
        
        
        VoxelShape border = level.getWorldBorder().getCollisionShape();
        AxisAlignedBB own = region.deflate(REGION_MARGIN);
        if (!VoxelShapes.joinIsNotEmpty(border, VoxelShapes.create(own), IBooleanFunction.AND)) {
            addShape(obstacles, region, border, DEFAULT_FRICTION, 0.0D);
        }
        return obstacles;
    }

    
    private static void collectCubes(Entity cube, EnumGravityDirection direction, AxisAlignedBB region,
                                     List<Obstacle> obstacles) {
        if (!PORTAL_MOD_PRESENT) {
            return;
        }
        List<Entity> others = cube.level.getEntities(cube,
                region.inflate(CubeModelCollisionHelper.MODEL_OVERHANG), Bridge::isCube);
        for (int i = 0; i < others.size(); i++) {
            Entity other = others.get(i);
            if (!CubeModelCollisionHelper.usesModelCollision(other)) {
                AxisAlignedBB plain = other.getBoundingBox();
                if (plain.intersects(region)) {
                    obstacles.add(new Obstacle(plain, CUBE_FRICTION, CUBE_RESTITUTION, 0));
                }
                continue;
            }
            
            
            
            
            
            
            OrientedBox model = CubeModelCollisionHelper.modelBox(other);
            if (!model.getBounds().intersects(region)) {
                continue;
            }
            CubeRigidBody body = orientationOf(other);
            EnumGravityDirection otherDirection = GravityPhysicsHelper.getDirection(other);
            if (body != null) {
                body.configure(other);
            }
            obstacles.add(new Obstacle(model.getBounds(), model.getCenter(),
                    new Vector3d[]{model.getAxisX(), model.getAxisY(), model.getAxisZ()},
                    new double[]{model.getHalfX(), model.getHalfY(), model.getHalfZ()},
                    CUBE_FRICTION, CUBE_RESTITUTION, 0, body,
                    otherDirection.adjustVec(new Vector3d(0.0D, 1.0D, 0.0D)),
                    body == null || body.isBraced()));
        }
    }

    private static void addShape(List<Obstacle> obstacles, AxisAlignedBB region, VoxelShape shape,
                                 double friction, double restitution) {
        for (AxisAlignedBB box : shape.toAabbs()) {
            double minX = Math.max(box.minX, region.minX);
            double minY = Math.max(box.minY, region.minY);
            double minZ = Math.max(box.minZ, region.minZ);
            double maxX = Math.min(box.maxX, region.maxX);
            double maxY = Math.min(box.maxY, region.maxY);
            double maxZ = Math.min(box.maxZ, region.maxZ);
            if (minX < maxX && minY < maxY && minZ < maxZ) {
                obstacles.add(new Obstacle(new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ),
                        friction, restitution, 0));
            }
        }
    }

    







    private static int blockedFaces(IBlockReader level, BlockPos pos, AxisAlignedBB world) {
        int mask = 0;
        for (int face = 0; face < FACE_COUNT; face++) {
            Direction direction = FACES[face];
            if (!isFlush(world, pos, direction)) {
                continue;
            }
            BlockPos neighbour = pos.relative(direction);
            BlockState state = level.getBlockState(neighbour);
            if (state.isFaceSturdy(level, neighbour, direction.getOpposite())) {
                mask |= 1 << face;
            }
        }
        return mask;
    }

    
    private static boolean isFlush(AxisAlignedBB box, BlockPos pos, Direction direction) {
        switch (direction) {
            case WEST:
                return Math.abs(box.minX - pos.getX()) < FLUSH_TOLERANCE;
            case EAST:
                return Math.abs(box.maxX - (pos.getX() + 1)) < FLUSH_TOLERANCE;
            case DOWN:
                return Math.abs(box.minY - pos.getY()) < FLUSH_TOLERANCE;
            case UP:
                return Math.abs(box.maxY - (pos.getY() + 1)) < FLUSH_TOLERANCE;
            case NORTH:
                return Math.abs(box.minZ - pos.getZ()) < FLUSH_TOLERANCE;
            default:
                return Math.abs(box.maxZ - (pos.getZ() + 1)) < FLUSH_TOLERANCE;
        }
    }

    private static double frictionOf(BlockState state, World level, BlockPos pos, Entity cube) {
        double slipperiness = state.getSlipperiness(level, pos, cube);
        return MathHelper.clamp((1.0D - slipperiness) * FRICTION_FROM_SLIPPERINESS, MIN_FRICTION, MAX_FRICTION);
    }

    private static double restitutionOf(BlockState state) {
        if (state.getBlock() instanceof SlimeBlock) {
            return SLIME_RESTITUTION;
        }
        if (state.getBlock() instanceof BedBlock) {
            return BED_RESTITUTION;
        }
        return DEFAULT_RESTITUTION;
    }

    
    
    

    







    public static final class Obstacle {

        
        public final AxisAlignedBB box;

        
        public final Vector3d centre;
        public final Vector3d[] axes;
        public final double[] half;

        public final double friction;
        public final double restitution;

        
        private final int blockedFaces;

        private final CubeRigidBody body;

        
        private final Vector3d supportUp;
        private final boolean braced;

        
        Obstacle(AxisAlignedBB box, double friction, double restitution, int blockedFaces) {
            this(box, box.getCenter(), WORLD_AXES,
                    new double[]{box.getXsize() * 0.5D, box.getYsize() * 0.5D, box.getZsize() * 0.5D},
                    friction, restitution, blockedFaces, null, WORLD_AXES[1], false);
        }

        Obstacle(AxisAlignedBB box, Vector3d centre, Vector3d[] axes, double[] half,
                 double friction, double restitution, int blockedFaces, CubeRigidBody body,
                 Vector3d supportUp, boolean braced) {
            this.box = box;
            this.centre = centre;
            this.axes = axes;
            this.half = half;
            this.friction = friction;
            this.restitution = restitution;
            this.blockedFaces = blockedFaces;
            this.body = body;
            this.supportUp = supportUp;
            this.braced = braced;
        }

        







        public boolean isBracedAgainst(Vector3d normal) {
            return this.body == null || (this.braced && normal.dot(this.supportUp) > BRACED_DOT);
        }

        
        public boolean isAsleep() {
            return this.body == null || this.body.isResting();
        }

        
        public Vector3d faceNormal(int face) {
            return (face & 1) == 0 ? this.axes[face >> 1].scale(-1.0D) : this.axes[face >> 1];
        }

        public boolean isFaceBlocked(int face) {
            return face >= 0 && (this.blockedFaces & (1 << face)) != 0;
        }

        
        public double effectiveMass(Vector3d point, Vector3d direction) {
            if (this.body == null) {
                return 0.0D;
            }
            return this.body.effectiveMassAt(point.subtract(this.centre), direction);
        }

        public Vector3d pointVelocity(Vector3d point) {
            if (this.body == null) {
                return Vector3d.ZERO;
            }
            return this.body.getLinearVelocity()
                    .add(this.body.getAngularVelocity().cross(point.subtract(this.centre)));
        }

        



        public void receiveImpulse(Vector3d point, Vector3d impulse) {
            if (this.body != null) {
                this.body.applyExternalImpulse(this.centre, point, impulse);
            }
        }

        
        public void disturb() {
            if (this.body != null) {
                this.body.disturb();
            }
        }
    }

    



    private static final class Bridge {

        private Bridge() {}

        static boolean isCube(Entity entity) {
            return Cube.class.isInstance(entity);
        }

        



        static boolean isFizzling(Entity entity) {
            return ((Cube) (Object) entity).isFizzling();
        }
    }
}
