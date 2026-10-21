package uk.co.mysterymayhem.gravitymod.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.util.GravityRenderHelper;

















@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/matrix/MatrixStack;pushPose()V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void gravitymod$rollItemSprite(ItemEntity entity, float entityYaw, float partialTicks,
                                           MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight,
                                           CallbackInfo ci) {
        if (GravityRenderHelper.isIdentity(entity, partialTicks)) {
            return;
        }
        matrixStack.mulPose(GravityRenderHelper.getRotation(entity, partialTicks));
    }
}
