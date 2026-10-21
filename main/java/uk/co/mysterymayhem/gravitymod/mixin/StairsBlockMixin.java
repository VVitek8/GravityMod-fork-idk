package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.block.StairsBlock;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.util.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.util.GravityPlacementHelper;














@Mixin(StairsBlock.class)
public abstract class StairsBlockMixin {

    @Redirect(
            method = "getStateForPlacement",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/BlockItemUseContext;getClickedFace()Lnet/minecraft/util/Direction;")
    )
    private Direction gravitymod$relativeStairHalf(BlockItemUseContext context) {
        Boolean worldLowerHalf = GravityPlacementHelper.isWorldLowerHalf(context);
        if (worldLowerHalf == null) {
            return context.getClickedFace();
        }
        return worldLowerHalf ? Direction.UP : Direction.DOWN;
    }
}
