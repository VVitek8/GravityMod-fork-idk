package uk.co.mysterymayhem.gravitymod.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.IWorldReader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.CubeModelCollisionHelper;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;
import uk.co.mysterymayhem.gravitymod.util.GravityRenderHelper;
import uk.co.mysterymayhem.gravitymod.util.GravityShadowHelper;






























@Mixin(EntityRendererManager.class)
public abstract class EntityRendererManagerMixin {

    





    @Unique
    private Quaternion gravitymod$hitboxOverlayRoll = null;

    






    @Unique
    private Quaternion gravitymod$flameInverseRoll = null;

    @Unique
    private Entity gravitymod$flameEntity = null;

    @Shadow
    @Final
    private static RenderType SHADOW_RENDER_TYPE;

    @Shadow
    protected abstract void renderBox(MatrixStack matrixStack, IVertexBuilder buffer, Entity entity, float r, float g, float b);

    @Shadow
    protected abstract void renderFlame(MatrixStack matrixStack, IRenderTypeBuffer buffer, Entity entity);

    @Inject(method = "renderHitbox", at = @At("HEAD"))
    private void gravitymod$rollHitboxOverlayStart(MatrixStack matrixStack, IVertexBuilder buffer, Entity entity, float partialTicks, CallbackInfo ci) {
        if (GravityRenderHelper.isIdentity(entity, partialTicks)) {
            this.gravitymod$hitboxOverlayRoll = null;
            return;
        }
        this.gravitymod$hitboxOverlayRoll = GravityRenderHelper.getRotation(entity, partialTicks);
        matrixStack.pushPose();
        matrixStack.mulPose(this.gravitymod$hitboxOverlayRoll);
    }

    @Inject(method = "renderHitbox", at = @At("RETURN"))
    private void gravitymod$rollHitboxOverlayEnd(MatrixStack matrixStack, IVertexBuilder buffer, Entity entity, float partialTicks, CallbackInfo ci) {
        Quaternion roll = this.gravitymod$hitboxOverlayRoll;
        if (roll == null) {
            this.gravitymod$drawCubeModelBox(matrixStack, buffer, entity);
            return;
        }
        
        
        Quaternion inverse = roll.copy();
        inverse.conj();
        matrixStack.pushPose();
        matrixStack.mulPose(inverse);
        this.gravitymod$drawCubeModelBox(matrixStack, buffer, entity);
        matrixStack.popPose();
        matrixStack.popPose();
        this.gravitymod$hitboxOverlayRoll = null;
    }

    











    @Unique
    private void gravitymod$drawCubeModelBox(MatrixStack matrixStack, IVertexBuilder buffer, Entity entity) {
        if (!CubeModelCollisionHelper.usesModelCollision(entity)) {
            return;
        }
        
        
        Vector3d[] corners = CubeModelCollisionHelper.modelBox(entity)
                .move(new Vector3d(-entity.getX(), -entity.getY(), -entity.getZ()))
                .getCorners();
        Matrix4f pose = matrixStack.last().pose();
        for (int i = 0; i < 4; i++) {
            
            
            gravitymod$drawEdge(buffer, pose, corners[i], corners[(i + 1) % 4]);
            gravitymod$drawEdge(buffer, pose, corners[4 + i], corners[4 + (i + 1) % 4]);
            gravitymod$drawEdge(buffer, pose, corners[i], corners[4 + i]);
        }
    }

    @Unique
    private static void gravitymod$drawEdge(IVertexBuilder buffer, Matrix4f pose, Vector3d from, Vector3d to) {
        buffer.vertex(pose, (float) from.x, (float) from.y, (float) from.z).color(0.0F, 1.0F, 1.0F, 1.0F).endVertex();
        buffer.vertex(pose, (float) to.x, (float) to.y, (float) to.z).color(0.0F, 1.0F, 1.0F, 1.0F).endVertex();
    }

    @Redirect(
            method = "renderHitbox",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRendererManager;renderBox(Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;Lnet/minecraft/entity/Entity;FFF)V"
            )
    )
    private void gravitymod$renderBoxWithoutOuterRoll(EntityRendererManager self, MatrixStack matrixStack, IVertexBuilder buffer, Entity entity, float r, float g, float b) {
        Quaternion roll = this.gravitymod$hitboxOverlayRoll;
        if (roll == null) {
            this.renderBox(matrixStack, buffer, entity, r, g, b);
            return;
        }
        
        
        
        Quaternion inverse = roll.copy();
        inverse.conj();
        matrixStack.pushPose();
        matrixStack.mulPose(inverse);
        this.renderBox(matrixStack, buffer, entity, r, g, b);
        matrixStack.popPose();
    }

    





    @Redirect(
            method = "renderHitbox",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;getViewVector(F)Lnet/minecraft/util/math/vector/Vector3d;"
            )
    )
    private Vector3d gravitymod$unrollViewVectorForHitbox(Entity entity, float partialTicks) {
        Vector3d worldView = entity.getViewVector(partialTicks);
        Quaternion roll = this.gravitymod$hitboxOverlayRoll;
        if (roll == null) {
            return worldView;
        }
        
        
        Quaternion inverse = roll.copy();
        inverse.conj();
        Vector3f view = new Vector3f((float) worldView.x, (float) worldView.y, (float) worldView.z);
        view.transform(inverse);
        return new Vector3d(view.x(), view.y(), view.z());
    }

    





    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;getRenderOffset(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/util/math/vector/Vector3d;"
            )
    )
    private Vector3d gravitymod$rotateRenderOffset(EntityRenderer<Entity> renderer, Entity entity, float partialTicks) {
        Vector3d offset = renderer.getRenderOffset(entity, partialTicks);
        if (GravityRenderHelper.isIdentity(entity, partialTicks)) {
            return offset;
        }
        
        
        
        Vector3f rotated = new Vector3f((float) offset.x, (float) offset.y, (float) offset.z);
        rotated.transform(GravityRenderHelper.getRotation(entity, partialTicks));
        return new Vector3d(rotated.x(), rotated.y(), rotated.z());
    }

    












    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void gravitymod$shadowOnRelativeFloor(MatrixStack matrixStack, IRenderTypeBuffer buffer, Entity entity,
                                                         float shadowAlpha, float partialTicks, IWorldReader world,
                                                         float shadowRadius, CallbackInfo ci) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(entity);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }
        GravityShadowHelper.renderShadow(matrixStack, buffer.getBuffer(SHADOW_RENDER_TYPE), entity, direction,
                shadowAlpha, partialTicks, world, shadowRadius);
        ci.cancel();
    }

    











    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRendererManager;renderFlame(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;Lnet/minecraft/entity/Entity;)V"
            )
    )
    private void gravitymod$rollFlame(EntityRendererManager self, MatrixStack matrixStack, IRenderTypeBuffer buffer, Entity entity) {
        
        
        float partialTicks = Minecraft.getInstance().getFrameTime();
        if (GravityRenderHelper.isIdentity(entity, partialTicks)) {
            this.gravitymod$flameInverseRoll = null;
            this.renderFlame(matrixStack, buffer, entity);
            return;
        }
        Quaternion roll = GravityRenderHelper.getRotation(entity, partialTicks);
        
        
        
        Quaternion inverse = roll.copy();
        inverse.conj();
        this.gravitymod$flameInverseRoll = inverse;
        this.gravitymod$flameEntity = entity;
        matrixStack.pushPose();
        matrixStack.mulPose(roll);
        this.renderFlame(matrixStack, buffer, entity);
        matrixStack.popPose();
        this.gravitymod$flameInverseRoll = null;
        this.gravitymod$flameEntity = null;
    }

    











    @Redirect(
            method = "renderFlame",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;getYRot()F")
    )
    private float gravitymod$flameBillboardYaw(ActiveRenderInfo camera) {
        Quaternion inverseRoll = this.gravitymod$flameInverseRoll;
        if (inverseRoll == null || this.gravitymod$flameEntity == null) {
            return camera.getYRot();
        }
        Vector3d offset = camera.getPosition().subtract(this.gravitymod$flameEntity.position());
        Vector3f toCamera = new Vector3f((float) offset.x, (float) offset.y, (float) offset.z);
        toCamera.transform(inverseRoll);
        
        return (float) (MathHelper.atan2(-toCamera.x(), toCamera.z()) * (180.0D / Math.PI));
    }
}
