package uk.co.mysterymayhem.gravitymod.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import uk.co.mysterymayhem.gravitymod.block.GravityFieldCreatorTileEntity;

import javax.annotation.Nonnull;


public class GravityFieldCreatorContainer extends Container {

    private final GravityFieldCreatorTileEntity tileEntity;
    private final BlockPos blockPos;

    public GravityFieldCreatorContainer(int id, PlayerInventory inventory, PacketBuffer extraData) {
        this(id, inventory, extraData.readBlockPos());
    }

    public GravityFieldCreatorContainer(int id, PlayerInventory inventory, BlockPos blockPos) {
        super(ModContainers.GRAVITY_FIELD_CREATOR.get(), id);
        this.blockPos = blockPos;
        TileEntity candidate = inventory.player.level.getBlockEntity(blockPos);
        this.tileEntity = candidate instanceof GravityFieldCreatorTileEntity
                ? (GravityFieldCreatorTileEntity) candidate : null;
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public GravityFieldCreatorTileEntity getTileEntity() {
        return this.tileEntity;
    }

    @Override
    public boolean stillValid(@Nonnull PlayerEntity player) {
        return this.tileEntity != null && !this.tileEntity.isRemoved()
                && player.level.getBlockEntity(this.blockPos) == this.tileEntity
                && player.distanceToSqr(this.blockPos.getX() + 0.5D, this.blockPos.getY() + 0.5D,
                this.blockPos.getZ() + 0.5D) <= 64.0D;
    }
}
