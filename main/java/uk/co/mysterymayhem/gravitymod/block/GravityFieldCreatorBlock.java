package uk.co.mysterymayhem.gravitymod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nonnull;





public class GravityFieldCreatorBlock extends Block {

    public GravityFieldCreatorBlock() {
        super(Properties.of(Material.METAL).strength(3.0F, 6.0F));
    }

    @Nonnull
    @Override
    public ActionResultType use(@Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos,
                                @Nonnull PlayerEntity player, @Nonnull Hand hand,
                                @Nonnull BlockRayTraceResult hit) {
        if (!world.isClientSide && player instanceof ServerPlayerEntity) {
            TileEntity tileEntity = world.getBlockEntity(pos);
            if (tileEntity instanceof GravityFieldCreatorTileEntity) {
                NetworkHooks.openGui((ServerPlayerEntity) player, (GravityFieldCreatorTileEntity) tileEntity, pos);
            }
        }
        return ActionResultType.sidedSuccess(world.isClientSide);
    }

    @Override
    public boolean hasTileEntity(@Nonnull BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull net.minecraft.world.IBlockReader world) {
        return new GravityFieldCreatorTileEntity();
    }

    @Override
    public void onRemove(@Nonnull BlockState oldState, @Nonnull World world, @Nonnull BlockPos pos,
                         @Nonnull BlockState newState, boolean isMoving) {
        if (oldState.getBlock() != newState.getBlock()) {
            TileEntity tileEntity = world.getBlockEntity(pos);
            if (tileEntity instanceof GravityFieldCreatorTileEntity) {
                ((GravityFieldCreatorTileEntity) tileEntity).restoreAllEntities();
            }
        }
        super.onRemove(oldState, world, pos, newState, isMoving);
    }
}
