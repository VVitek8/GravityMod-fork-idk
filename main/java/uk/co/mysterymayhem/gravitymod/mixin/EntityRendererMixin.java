package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.mysterymayhem.gravitymod.util.PortalModCameraLightHelper;





















@Mixin(value = EntityRenderer.class, priority = 1500)
public abstract class EntityRendererMixin<T extends Entity> {

    @Shadow
    protected abstract int getBlockLightLevel(T entity, BlockPos pos);

    @Shadow
    protected abstract int getSkyLightLevel(T entity, BlockPos pos);

    @Inject(method = "getPackedLightCoords", at = @At("HEAD"), cancellable = true)
    private void gravitymod$lightBodyFromBody(T entity, float partialTicks, CallbackInfoReturnable<Integer> cir) {
        if (!PortalModCameraLightHelper.lightsCameraEntityFromCamera()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (entity != minecraft.cameraEntity) {
            
            return;
        }
        ActiveRenderInfo camera = minecraft.gameRenderer.getMainCamera();
        if (!camera.isDetached()) {
            return;
        }
        
        
        BlockPos probe = new BlockPos(entity.getLightProbePosition(partialTicks));
        cir.setReturnValue(LightTexture.pack(this.getBlockLightLevel(entity, probe), this.getSkyLightLevel(entity, probe)));
    }
}
