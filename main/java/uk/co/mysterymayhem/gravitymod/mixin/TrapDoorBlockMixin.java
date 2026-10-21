package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.TrapDoorBlock;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.properties.Half;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.mysterymayhem.gravitymod.util.GravityPlacementHelper;







@Mixin(TrapDoorBlock.class)
public abstract class TrapDoorBlockMixin {

    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void gravitymod$relativeTrapDoorHalf(BlockItemUseContext context, CallbackInfoReturnable<BlockState> cir) {
        Boolean worldLowerHalf = GravityPlacementHelper.isWorldLowerHalf(context);
        if (worldLowerHalf == null) {
            return;
        }
        BlockState state = cir.getReturnValue();
        if (state != null) {
            cir.setReturnValue(state.setValue(TrapDoorBlock.HALF, worldLowerHalf ? Half.BOTTOM : Half.TOP));
        }
    }
}
