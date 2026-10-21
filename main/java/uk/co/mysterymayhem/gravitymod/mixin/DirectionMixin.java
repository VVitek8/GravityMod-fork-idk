package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;

import java.util.Arrays;
















@Mixin(Direction.class)
public abstract class DirectionMixin {

    @Inject(method = "orderedByNearest", at = @At("HEAD"), cancellable = true)
    private static void gravitymod$orderByWorldLook(Entity entity, CallbackInfoReturnable<Direction[]> cir) {
        if (entity == null || GravityPhysicsHelper.getDirection(entity) == EnumGravityDirection.DOWN) {
            return;
        }
        
        Vector3d look = entity.getViewVector(1.0F);
        
        
        
        
        
        
        Direction[] ordered = Direction.values();
        Arrays.sort(ordered, (a, b) -> Double.compare(gravitymod$alignment(b, look), gravitymod$alignment(a, look)));
        cir.setReturnValue(ordered);
    }

    @Unique
    private static double gravitymod$alignment(Direction face, Vector3d look) {
        return face.getStepX() * look.x + face.getStepY() * look.y + face.getStepZ() * look.z;
    }
}
