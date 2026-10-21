package uk.co.mysterymayhem.gravitymod.block;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.Container;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.capability.GravityAPI;
import uk.co.mysterymayhem.gravitymod.capability.IGravityCapability;
import uk.co.mysterymayhem.gravitymod.container.GravityFieldCreatorContainer;
import uk.co.mysterymayhem.gravitymod.util.CubeHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;






public class GravityFieldCreatorTileEntity extends TileEntity implements ITickableTileEntity, INamedContainerProvider {

    public static final double MIN_SIZE = 0.1D;
    public static final double MAX_SIZE = 128.0D;
    public static final double MAX_OFFSET = 256.0D;
    public static final double DEFAULT_SIZE = 5.0D;

    private double width = DEFAULT_SIZE;
    private double depth = DEFAULT_SIZE;
    private double height = DEFAULT_SIZE;
    private double offsetX;
    private double offsetY;
    private double offsetZ;
    @Nonnull
    private EnumGravityDirection direction = EnumGravityDirection.DOWN;
    private double strength = IGravityCapability.DEFAULT_STRENGTH;
    
    private boolean fieldVisible = true;
    
    private boolean keepGravity = false;
    
    private boolean affectPlayers = true;
    private boolean affectCubes = true;
    
    private boolean active;

    
    private final Map<UUID, SavedGravity> overriddenEntities = new HashMap<>();

    public GravityFieldCreatorTileEntity() {
        super(ModTileEntities.GRAVITY_FIELD_CREATOR.get());
    }

    public double getWidth() {
        return this.width;
    }

    public double getDepth() {
        return this.depth;
    }

    public double getHeight() {
        return this.height;
    }

    public double getOffsetX() {
        return this.offsetX;
    }

    public double getOffsetY() {
        return this.offsetY;
    }

    public double getOffsetZ() {
        return this.offsetZ;
    }

    @Nonnull
    public EnumGravityDirection getDirection() {
        return this.direction;
    }

    public double getStrength() {
        return this.strength;
    }

    public boolean isFieldVisible() {
        return this.fieldVisible;
    }

    public boolean isKeepGravity() {
        return this.keepGravity;
    }

    public boolean isAffectPlayers() {
        return this.affectPlayers;
    }

    public boolean isAffectCubes() {
        return this.affectCubes;
    }

    public boolean isActive() {
        return this.active;
    }

    




    @Nonnull
    public AxisAlignedBB getFieldBounds() {
        double centreX = this.worldPosition.getX() + 0.5D + this.offsetX;
        double centreY = this.worldPosition.getY() + 0.5D + this.offsetY;
        double centreZ = this.worldPosition.getZ() + 0.5D + this.offsetZ;
        return new AxisAlignedBB(
                centreX - this.width / 2.0D, centreY - this.height / 2.0D, centreZ - this.depth / 2.0D,
                centreX + this.width / 2.0D, centreY + this.height / 2.0D, centreZ + this.depth / 2.0D);
    }

    
    public void setConfiguration(double width, double depth, double height, double offsetX, double offsetY,
                                 double offsetZ, @Nonnull EnumGravityDirection direction, double strength,
                                 boolean fieldVisible, boolean keepGravity, boolean affectPlayers,
                                 boolean affectCubes) {
        this.width = clampSize(width);
        this.depth = clampSize(depth);
        this.height = clampSize(height);
        this.offsetX = clampOffset(offsetX);
        this.offsetY = clampOffset(offsetY);
        this.offsetZ = clampOffset(offsetZ);
        this.direction = direction;
        this.strength = clampStrength(strength);
        this.fieldVisible = fieldVisible;
        this.keepGravity = keepGravity;
        this.affectPlayers = affectPlayers;
        this.affectCubes = affectCubes;
        this.setChanged();
        this.syncToClient();
    }

    @Override
    public void tick() {
        if (this.level == null || this.level.isClientSide || !(this.level instanceof ServerWorld)) {
            return;
        }
        ServerWorld world = (ServerWorld) this.level;
        this.setActive(world.hasNeighborSignal(this.worldPosition));
        if (!this.active) {
            
            
            this.restoreAllEntities();
            return;
        }
        Set<UUID> currentlyInside = new HashSet<>();
        
        
        AxisAlignedBB bounds = this.getFieldBounds();
        for (Entity entity : world.getEntities((Entity) null, bounds, entity -> this.isInField(entity, bounds))) {
            UUID uuid = entity.getUUID();
            currentlyInside.add(uuid);
            this.applyFieldGravity(entity, uuid);
        }

        
        
        
        for (UUID uuid : new HashSet<>(this.overriddenEntities.keySet())) {
            if (!currentlyInside.contains(uuid)) {
                Entity entity = world.getEntity(uuid);
                if (entity != null) {
                    this.restoreEntity(entity, uuid);
                }
            }
        }
    }

    







    private boolean isInField(Entity entity, AxisAlignedBB bounds) {
        if (!entity.isAlive() || !GravityAPI.getCapability(entity).isPresent() || !this.affects(entity)) {
            return false;
        }
        
        
        if (entity instanceof PlayerEntity && ((PlayerEntity) entity).isSpectator()) {
            return false;
        }
        return this.overriddenEntities.containsKey(entity.getUUID())
                ? bounds.intersects(entity.getBoundingBox())
                : bounds.contains(entity.getBoundingBox().getCenter());
    }

    




    private boolean affects(Entity entity) {
        if (entity instanceof PlayerEntity) {
            return this.affectPlayers;
        }
        if (CubeHelper.isCube(entity)) {
            return this.affectCubes;
        }
        return true;
    }

    private void applyFieldGravity(Entity entity, UUID uuid) {
        GravityAPI.getCapability(entity).ifPresent(capability -> {
            if (!this.overriddenEntities.containsKey(uuid)) {
                this.overriddenEntities.put(uuid, new SavedGravity(capability.getDirection(), capability.getStrength()));
                this.setChanged();
            }
            if (GravityAPI.getDirection(entity) != this.direction) {
                GravityAPI.setDirection(entity, this.direction, true);
            }
            if (Double.compare(GravityAPI.getStrength(entity), this.strength) != 0) {
                GravityAPI.setStrength(entity, this.strength);
            }
        });
    }

    private void restoreEntity(Entity entity, UUID uuid) {
        SavedGravity original = this.overriddenEntities.remove(uuid);
        if (original == null || !GravityAPI.getCapability(entity).isPresent()) {
            return;
        }
        
        
        if (this.keepGravity) {
            this.setChanged();
            return;
        }
        if (GravityAPI.getDirection(entity) != original.direction) {
            GravityAPI.setDirection(entity, original.direction, true);
        }
        if (Double.compare(GravityAPI.getStrength(entity), original.strength) != 0) {
            GravityAPI.setStrength(entity, original.strength);
        }
        this.setChanged();
    }

    
    public void restoreAllEntities() {
        if (!(this.level instanceof ServerWorld)) {
            return;
        }
        ServerWorld world = (ServerWorld) this.level;
        for (UUID uuid : new HashSet<>(this.overriddenEntities.keySet())) {
            Entity entity = world.getEntity(uuid);
            if (entity != null) {
                this.restoreEntity(entity, uuid);
            }
        }
    }

    
    private void setActive(boolean active) {
        if (this.active == active) {
            return;
        }
        this.active = active;
        this.setChanged();
        this.syncToClient();
    }

    
    @Override
    public void onChunkUnloaded() {
        this.restoreAllEntities();
        super.onChunkUnloaded();
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.gravitymod.gravity_field_creator");
    }

    @Nullable
    @Override
    public Container createMenu(int id, @Nonnull PlayerInventory inventory, @Nonnull PlayerEntity player) {
        return new GravityFieldCreatorContainer(id, inventory, this.worldPosition);
    }

    private void syncToClient() {
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    @Nonnull
    @Override
    public CompoundNBT save(@Nonnull CompoundNBT tag) {
        super.save(tag);
        this.writeConfiguration(tag);
        ListNBT snapshots = new ListNBT();
        for (Map.Entry<UUID, SavedGravity> entry : this.overriddenEntities.entrySet()) {
            CompoundNBT snapshot = new CompoundNBT();
            snapshot.putUUID("Entity", entry.getKey());
            snapshot.putString("Direction", entry.getValue().direction.getSerializedName());
            snapshot.putDouble("Strength", entry.getValue().strength);
            snapshots.add(snapshot);
        }
        tag.put("OverriddenGravity", snapshots);
        return tag;
    }

    @Override
    public void load(@Nonnull BlockState state, @Nonnull CompoundNBT tag) {
        super.load(state, tag);
        this.readConfiguration(tag);
        this.overriddenEntities.clear();
        ListNBT snapshots = tag.getList("OverriddenGravity", Constants.NBT.TAG_COMPOUND);
        for (int index = 0; index < snapshots.size(); index++) {
            CompoundNBT snapshot = snapshots.getCompound(index);
            if (!snapshot.hasUUID("Entity")) {
                continue;
            }
            this.overriddenEntities.put(snapshot.getUUID("Entity"), new SavedGravity(
                    readDirection(snapshot.getString("Direction")), clampStrength(snapshot.getDouble("Strength"))));
        }
    }

    





    @Nonnull
    @Override
    public CompoundNBT getUpdateTag() {
        CompoundNBT tag = super.save(new CompoundNBT());
        this.writeConfiguration(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(@Nonnull BlockState state, @Nonnull CompoundNBT tag) {
        this.readConfiguration(tag);
    }

    @Nullable
    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(this.worldPosition, 0, this.getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager networkManager, SUpdateTileEntityPacket packet) {
        this.handleUpdateTag(this.getBlockState(), packet.getTag());
    }

    private void writeConfiguration(CompoundNBT tag) {
        tag.putDouble("Width", this.width);
        tag.putDouble("Depth", this.depth);
        tag.putDouble("Height", this.height);
        tag.putDouble("OffsetX", this.offsetX);
        tag.putDouble("OffsetY", this.offsetY);
        tag.putDouble("OffsetZ", this.offsetZ);
        tag.putString("Direction", this.direction.getSerializedName());
        tag.putDouble("Strength", this.strength);
        tag.putBoolean("FieldVisible", this.fieldVisible);
        tag.putBoolean("KeepGravity", this.keepGravity);
        tag.putBoolean("AffectPlayers", this.affectPlayers);
        tag.putBoolean("AffectCubes", this.affectCubes);
        tag.putBoolean("Active", this.active);
    }

    
    
    private void readConfiguration(CompoundNBT tag) {
        this.width = clampSize(tag.contains("Width") ? tag.getDouble("Width") : this.width);
        this.depth = clampSize(tag.contains("Depth") ? tag.getDouble("Depth") : this.depth);
        this.height = clampSize(tag.contains("Height") ? tag.getDouble("Height") : this.height);
        this.offsetX = clampOffset(tag.contains("OffsetX") ? tag.getDouble("OffsetX") : this.offsetX);
        this.offsetY = clampOffset(tag.contains("OffsetY") ? tag.getDouble("OffsetY") : this.offsetY);
        this.offsetZ = clampOffset(tag.contains("OffsetZ") ? tag.getDouble("OffsetZ") : this.offsetZ);
        this.direction = readDirection(tag.getString("Direction"));
        this.strength = clampStrength(tag.contains("Strength") ? tag.getDouble("Strength") : IGravityCapability.DEFAULT_STRENGTH);
        
        this.fieldVisible = !tag.contains("FieldVisible") || tag.getBoolean("FieldVisible");
        
        this.keepGravity = tag.getBoolean("KeepGravity");
        
        this.affectPlayers = !tag.contains("AffectPlayers") || tag.getBoolean("AffectPlayers");
        this.affectCubes = !tag.contains("AffectCubes") || tag.getBoolean("AffectCubes");
        
        
        this.active = tag.getBoolean("Active");
    }

    private static double clampSize(double value) {
        if (!Double.isFinite(value)) {
            return DEFAULT_SIZE;
        }
        return Math.max(MIN_SIZE, Math.min(MAX_SIZE, value));
    }

    private static double clampOffset(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, value));
    }

    private static double clampStrength(double value) {
        if (!Double.isFinite(value)) {
            return IGravityCapability.DEFAULT_STRENGTH;
        }
        return Math.max(IGravityCapability.MIN_STRENGTH, Math.min(IGravityCapability.MAX_STRENGTH, value));
    }

    @Nonnull
    private static EnumGravityDirection readDirection(String name) {
        for (EnumGravityDirection value : EnumGravityDirection.values()) {
            if (value.getSerializedName().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return EnumGravityDirection.DOWN;
    }

    private static final class SavedGravity {
        @Nonnull
        private final EnumGravityDirection direction;
        private final double strength;

        private SavedGravity(@Nonnull EnumGravityDirection direction, double strength) {
            this.direction = direction;
            this.strength = strength;
        }
    }
}
