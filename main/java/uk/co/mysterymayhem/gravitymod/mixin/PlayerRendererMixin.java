package uk.co.mysterymayhem.gravitymod.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.util.GravityRenderHelper;















@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    



    @Redirect(
            method = "renderNameTag",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/matrix/MatrixStack;translate(DDD)V")
    )
    private void gravitymod$stackScoreLineAlongRelativeUp(MatrixStack matrixStack, double x, double y, double z,
                                                          AbstractClientPlayerEntity entity, ITextComponent displayName,
                                                          MatrixStack sameMatrixStack, IRenderTypeBuffer buffer,
                                                          int packedLight) {
        
        float partialTicks = Minecraft.getInstance().getFrameTime();
        if (GravityRenderHelper.isIdentity(entity, partialTicks)) {
            matrixStack.translate(x, y, z);
            return;
        }
        
        double[] relativeUp = GravityRenderHelper.getRelativeUp(entity, partialTicks);
        matrixStack.translate(relativeUp[0] * y, relativeUp[1] * y, relativeUp[2] * y);
    }
}
