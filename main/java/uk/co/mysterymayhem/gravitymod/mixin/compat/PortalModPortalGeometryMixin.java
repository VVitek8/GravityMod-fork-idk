package uk.co.mysterymayhem.gravitymod.mixin.compat;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3d;
import net.portalmod.core.math.Mat4;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.mysterymayhem.gravitymod.util.PortalModGeometryHelper;











@Pseudo
@Mixin(targets = "net.portalmod.common.sorted.portal.PortalEntity", remap = false)
public abstract class PortalModPortalGeometryMixin {

    @Dynamic("PortalMod: PortalEntity#setupMatrix(MatrixStack, Direction, Direction, Vector3d)")
    @Inject(
            method = "setupMatrix(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/util/Direction;Lnet/minecraft/util/Direction;Lnet/minecraft/util/math/vector/Vector3d;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void gravitymod$setupMatrixFromFullPortalBasis(
            MatrixStack matrix, Direction direction, Direction upVector, Vector3d center, CallbackInfo ci) {
        PortalModGeometryHelper.setupMatrix(matrix, direction, upVector, center);
        ci.cancel();
    }

    @Dynamic("PortalMod: PortalEntity#setupMatrix(Direction, Direction, Vector3d)")
    @Inject(
            method = "setupMatrix(Lnet/minecraft/util/Direction;Lnet/minecraft/util/Direction;Lnet/minecraft/util/math/vector/Vector3d;)Lnet/portalmod/core/math/Mat4;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void gravitymod$setupMat4FromFullPortalBasis(
            Direction direction, Direction upVector, Vector3d center, CallbackInfoReturnable<Mat4> cir) {
        cir.setReturnValue(PortalModGeometryHelper.setupMatrix(direction, upVector, center));
    }
}
