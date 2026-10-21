package uk.co.mysterymayhem.gravitymod.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.capability.GravityAPI;

import javax.annotation.Nonnull;
import java.util.List;












public class GravityChangerItem extends Item {

    
    private static final double AOE_RADIUS = 2.5D;

    private final EnumGravityDirection direction;
    private final boolean aoe;

    public GravityChangerItem(EnumGravityDirection direction, boolean aoe, Properties properties) {
        super(properties);
        this.direction = direction;
        this.aoe = aoe;
    }

    @Nonnull
    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, @Nonnull Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            if (this.aoe) {
                applyAoe(world, player);
            }
            else {
                GravityAPI.setDirection(player, this.direction, true);
            }
        }
        return ActionResult.sidedSuccess(stack, world.isClientSide);
    }

    @Nonnull
    @Override
    public ActionResultType interactLivingEntity(@Nonnull ItemStack stack, PlayerEntity player,
                                                 @Nonnull LivingEntity target, @Nonnull Hand hand) {
        if (this.aoe) {
            
            
            if (!player.level.isClientSide) {
                applyAoe(player.level, player);
            }
            return ActionResultType.sidedSuccess(player.level.isClientSide);
        }
        
        
        if (!player.isShiftKeyDown()) {
            return ActionResultType.PASS;
        }
        if (!player.level.isClientSide) {
            GravityAPI.setDirection(target, this.direction, true);
        }
        return ActionResultType.sidedSuccess(player.level.isClientSide);
    }

    private void applyAoe(World world, PlayerEntity player) {
        AxisAlignedBB area = new AxisAlignedBB(
                player.getX() - AOE_RADIUS, player.getY() - AOE_RADIUS, player.getZ() - AOE_RADIUS,
                player.getX() + AOE_RADIUS, player.getY() + AOE_RADIUS, player.getZ() + AOE_RADIUS);
        
        
        
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, area);
        for (LivingEntity target : targets) {
            GravityAPI.setDirection(target, this.direction, true);
        }
    }
}
