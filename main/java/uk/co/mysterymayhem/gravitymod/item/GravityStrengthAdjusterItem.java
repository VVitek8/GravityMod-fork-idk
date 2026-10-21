package uk.co.mysterymayhem.gravitymod.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nonnull;









public class GravityStrengthAdjusterItem extends Item {

    public GravityStrengthAdjusterItem(Properties properties) {
        super(properties);
    }

    @Nonnull
    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, @Nonnull Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (world.isClientSide) {
            
            
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> uk.co.mysterymayhem.gravitymod.client.GravityStrengthAdjusterScreen.open());
        }
        return ActionResult.sidedSuccess(stack, world.isClientSide);
    }
}
