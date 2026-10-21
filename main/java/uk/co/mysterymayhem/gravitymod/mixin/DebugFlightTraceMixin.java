package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;











@Mixin(PlayerEntity.class)
public abstract class DebugFlightTraceMixin {

    @Unique
    private static final boolean GRAVITYMOD$TRACE =
            !"false".equalsIgnoreCase(System.getProperty("upanddown.tracevelocity", "true"));

    
    @Unique
    private int gravitymod$traceTicksLeft = 0;

    @Unique
    private boolean gravitymod$traceWasFlying = false;

    @Unique
    private double gravitymod$tracePreviousRelHorizontal = 0.0D;

    @Inject(method = "tick", at = @At("RETURN"))
    private void gravitymod$traceFlightMomentum(CallbackInfo ci) {
        if (!GRAVITYMOD$TRACE) {
            return;
        }
        PlayerEntity self = (PlayerEntity) (Object) this;
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(self);
        if (direction == EnumGravityDirection.DOWN) {
            return;
        }

        boolean flying = self.abilities.flying;
        String event = "";
        if (flying) {
            this.gravitymod$traceTicksLeft = 60;
            if (!this.gravitymod$traceWasFlying) {
                event = " <<< FLIGHT STARTED";
            }
        }
        else if (this.gravitymod$traceWasFlying) {
            event = " <<< FLIGHT ENDED";
        }
        this.gravitymod$traceWasFlying = flying;

        if (!flying) {
            if (this.gravitymod$traceTicksLeft <= 0) {
                return;
            }
            this.gravitymod$traceTicksLeft--;
        }

        Vector3d world = self.getDeltaMovement();
        Vector3d relative = direction.inverseAdjustVec(world);
        double relHorizontal = Math.sqrt(relative.x * relative.x + relative.z * relative.z);
        
        
        double ratio = this.gravitymod$tracePreviousRelHorizontal > 1.0E-5D
                ? relHorizontal / this.gravitymod$tracePreviousRelHorizontal : Double.NaN;
        this.gravitymod$tracePreviousRelHorizontal = relHorizontal;

        uk.co.mysterymayhem.gravitymod.GravityMod.LOGGER.info(String.format(
                "[UpAndDown flight trace] %s grav=%s flying=%-5s onGround=%-5s hColl=%-5s vColl=%-5s"
                        + " world=(%+.4f,%+.4f,%+.4f) rel=(%+.4f,%+.4f,%+.4f) relHoriz=%.5f ratio=%.4f%s",
                self.level.isClientSide ? "CLIENT" : "SERVER",
                direction, flying, self.isOnGround(), self.horizontalCollision, self.verticalCollision,
                world.x, world.y, world.z, relative.x, relative.y, relative.z, relHorizontal, ratio, event));
    }
}
