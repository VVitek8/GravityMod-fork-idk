package uk.co.mysterymayhem.gravitymod.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.util.math.vector.Quaternion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.util.GravitySunLightHelper;






















@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererSunLightMixin {

    @Inject(method = "setupRotations", at = @At("HEAD"))
    private void gravitymod$shadeFromTheSky(AbstractClientPlayerEntity entity, MatrixStack matrixStack,
                                            float ageInTicks, float rotationYaw, float partialTicks,
                                            CallbackInfo ci) {
        Quaternion keyLight = GravitySunLightHelper.getKeyLightRotation(entity, partialTicks);
        if (keyLight == null) {
            return;
        }
        
        
        matrixStack.last().normal().mul(keyLight);
    }
}
