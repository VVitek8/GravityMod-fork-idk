package uk.co.mysterymayhem.gravitymod.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.util.GravityRenderHelper;



























@Mixin(EntityRenderer.class)
public abstract class EntityRendererNameTagMixin<T extends Entity> {

    




    @Redirect(
            method = "renderNameTag",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/matrix/MatrixStack;translate(DDD)V")
    )
    private void gravitymod$liftNameTagAlongRelativeUp(MatrixStack matrixStack, double x, double y, double z,
                                                       T entity, ITextComponent displayName, MatrixStack sameMatrixStack,
                                                       IRenderTypeBuffer buffer, int packedLight) {
        
        
        float partialTicks = Minecraft.getInstance().getFrameTime();
        if (GravityRenderHelper.isIdentity(entity, partialTicks)) {
            matrixStack.translate(x, y, z);
            return;
        }
        
        
        double[] relativeUp = GravityRenderHelper.getRelativeUp(entity, partialTicks);
        matrixStack.translate(relativeUp[0] * y, relativeUp[1] * y, relativeUp[2] * y);
    }

    









    @Redirect(
            method = "renderNameTag",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/matrix/MatrixStack;mulPose(Lnet/minecraft/util/math/vector/Quaternion;)V")
    )
    private void gravitymod$rollNameTagWithBody(MatrixStack matrixStack, Quaternion cameraOrientation,
                                                T entity, ITextComponent displayName, MatrixStack sameMatrixStack,
                                                IRenderTypeBuffer buffer, int packedLight) {
        Minecraft minecraft = Minecraft.getInstance();
        float partialTicks = minecraft.getFrameTime();
        Entity cameraEntity = minecraft.gameRenderer.getMainCamera().getEntity();
        boolean viewerRolled = cameraEntity != null && !GravityRenderHelper.isIdentity(cameraEntity, partialTicks);
        if (!viewerRolled && GravityRenderHelper.isIdentity(entity, partialTicks)) {
            matrixStack.mulPose(cameraOrientation);
            return;
        }
        
        
        double[] worldUp = GravityRenderHelper.getRelativeUp(entity, partialTicks);
        Vector3f up = new Vector3f((float) worldUp[0], (float) worldUp[1], (float) worldUp[2]);
        Quaternion inverseCamera = cameraOrientation.copy();
        inverseCamera.conj();
        up.transform(inverseCamera);
        
        
        
        
        float roll = (float) (MathHelper.atan2(-up.x(), up.y()) * (180.0D / Math.PI));
        
        
        
        Quaternion billboard = cameraOrientation.copy();
        billboard.mul(Vector3f.ZP.rotationDegrees(roll));
        matrixStack.mulPose(billboard);
    }
}
