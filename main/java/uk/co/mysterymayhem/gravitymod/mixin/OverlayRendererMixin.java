package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.OverlayRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;














@Mixin(OverlayRenderer.class)
public abstract class OverlayRendererMixin {

    
    
    @Inject(method = "getOverlayBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private static void gravitymod$fixViewBlockingSampling(PlayerEntity player, CallbackInfoReturnable<Pair<BlockState, BlockPos>> cir) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(player);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        Vector3d eye = GravityPhysicsHelper.getRelativeEyePosition(player);
        double halfWidth = player.getBbWidth() * 0.8D;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int i = 0; i < 8; ++i) {
            double relX = ((double) ((i >> 0) % 2) - 0.5D) * halfWidth;
            double relUp = ((double) ((i >> 1) % 2) - 0.5D) * 0.1D;
            double relZ = ((double) ((i >> 2) % 2) - 0.5D) * halfWidth;
            double[] worldOffset = direction.adjustXYZValues(relX, relUp, relZ);
            mutable.set(eye.x + worldOffset[0], eye.y + worldOffset[1], eye.z + worldOffset[2]);
            BlockState state = player.level.getBlockState(mutable);
            if (state.getRenderShape() != BlockRenderType.INVISIBLE && state.isViewBlocking(player.level, mutable)) {
                cir.setReturnValue(Pair.of(state, mutable.immutable()));
                return;
            }
        }
        cir.setReturnValue(null);
    }
}
