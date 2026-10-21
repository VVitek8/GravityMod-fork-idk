package uk.co.mysterymayhem.gravitymod.mixin.compat;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.portalmod.common.entities.TestElementEntity;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import uk.co.mysterymayhem.gravitymod.util.CubeModelCollisionHelper;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;






















@Pseudo
@Mixin(targets = "net.portalmod.common.entities.TestElementEntityRenderer", remap = false)
public abstract class PortalModTestElementRendererMixin {

    













    @Dynamic("PortalMod: TestElementEntityRenderer#render")
    @Redirect(
            method = {"render", "func_225623_a_"},
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getBrightness(Lnet/minecraft/world/LightType;Lnet/minecraft/util/math/BlockPos;)I"
            )
    )
    private int gravitymod$brightnessAtTheCubesCentre(World level, LightType type, BlockPos ignoredFeetPosition,
                                                      TestElementEntity target, float yaw, float partialTicks,
                                                      MatrixStack matrixStack, IRenderTypeBuffer buffer, int light) {
        Entity entity = (Entity) (Object) target;
        return level.getBrightness(type,
                new BlockPos(CubeModelCollisionHelper.centreOf(entity, GravityPhysicsHelper.getDirection(entity))));
    }
}
