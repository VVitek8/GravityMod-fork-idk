package uk.co.mysterymayhem.gravitymod.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.CubeModelCollisionHelper;
import uk.co.mysterymayhem.gravitymod.util.CubePhysics;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;
import uk.co.mysterymayhem.gravitymod.util.GravityRenderHelper;














@Mixin(LivingRenderer.class)
public abstract class LivingRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Inject(method = "setupRotations", at = @At("HEAD"), cancellable = true)
    private void gravitymod$rotateModel(T entity, MatrixStack matrixStack, float ageInTicks, float rotationYaw, float partialTicks, CallbackInfo ci) {
        if (gravitymod$rotateCube(entity, matrixStack, partialTicks)) {
            ci.cancel();
            return;
        }
        if (GravityRenderHelper.isIdentity(entity, partialTicks)) {
            return;
        }
        matrixStack.mulPose(GravityRenderHelper.getRotation(entity, partialTicks));
    }

    















    private static boolean gravitymod$rotateCube(LivingEntity entity, MatrixStack matrixStack, float partialTicks) {
        Quaternion orientation = CubePhysics.renderOrientation(entity, partialTicks);
        if (orientation == null) {
            return false;
        }
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        double halfHeight = CubeModelCollisionHelper.modelHalfHeight(entity);
        Vector3d centre = direction.adjustVec(new Vector3d(0.0D, halfHeight, 0.0D));
        matrixStack.translate(centre.x, centre.y, centre.z);
        orientation.mul(Vector3f.YP.rotationDegrees(180.0F));
        matrixStack.mulPose(orientation);
        matrixStack.translate(0.0D, -halfHeight, 0.0D);
        return true;
    }
}
