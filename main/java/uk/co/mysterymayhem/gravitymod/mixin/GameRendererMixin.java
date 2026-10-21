package uk.co.mysterymayhem.gravitymod.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.util.GravityRenderHelper;

















@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow
    @Final
    private ActiveRenderInfo mainCamera;

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/WorldRenderer;renderLevel(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/util/math/vector/Matrix4f;)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void gravitymod$unrollWorldForCamera(float partialTicks, long finishTimeNano, MatrixStack matrixStack, CallbackInfo ci) {
        Entity entity = this.mainCamera.getEntity();
        if (entity == null) {
            return;
        }
        if (GravityRenderHelper.isIdentity(entity, partialTicks)) {
            return;
        }
        matrixStack.mulPose(GravityRenderHelper.getInverseRotation(entity, partialTicks));
    }
}
