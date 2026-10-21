package uk.co.mysterymayhem.gravitymod.mixin;

import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.util.GravityPhysicsHelper;






























@Mixin(ServerPlayNetHandler.class)
public abstract class ServerPlayNetHandlerMixin {

    @Shadow
    public ServerPlayerEntity player;

    @Shadow
    private double lastGoodX;

    @Shadow
    private double lastGoodY;

    @Shadow
    private double lastGoodZ;

    



    @Unique
    private Vector3d gravitymod$positionBeforeMove;

    @Inject(
            method = "handleMovePlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/ServerPlayerEntity;move(Lnet/minecraft/entity/MoverType;Lnet/minecraft/util/math/vector/Vector3d;)V"))
    private void gravitymod$snapshotPositionBeforeMove(CPlayerPacket packet, CallbackInfo ci) {
        this.gravitymod$positionBeforeMove = this.player.position();
    }

    





    @Redirect(
            method = "handleMovePlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/ServerPlayerEntity;doCheckFallDamage(DZ)V"))
    private void gravitymod$relativeFallDistance(ServerPlayerEntity player, double worldYDelta, boolean onGround) {
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(player);
        if (direction == EnumGravityDirection.DOWN || this.gravitymod$positionBeforeMove == null) {
            player.doCheckFallDamage(worldYDelta, onGround);
            return;
        }
        Vector3d displacement = player.position().subtract(this.gravitymod$positionBeforeMove);
        player.doCheckFallDamage(direction.inverseAdjustVec(displacement).y, onGround);
    }

    



    @Redirect(
            method = "handleMovePlayer",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/entity/player/ServerPlayerEntity;fallDistance:F",
                    opcode = org.objectweb.asm.Opcodes.PUTFIELD))
    private void gravitymod$relativeFallDistanceReset(ServerPlayerEntity player, float value, CPlayerPacket packet) {
        if (this.gravitymod$roseRelativeToGravity(player, packet)) {
            player.fallDistance = value;
        }
    }

    





    @Redirect(
            method = "handleMovePlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/ServerPlayerEntity;jumpFromGround()V"))
    private void gravitymod$relativeJumpTrigger(ServerPlayerEntity player, CPlayerPacket packet) {
        if (this.gravitymod$roseRelativeToGravity(player, packet)) {
            player.jumpFromGround();
        }
    }

    










    @Unique
    private boolean gravitymod$roseRelativeToGravity(ServerPlayerEntity player, CPlayerPacket packet) {
        Vector3d reported = new Vector3d(
                packet.getX(player.getX()), packet.getY(player.getY()), packet.getZ(player.getZ()));
        Vector3d rise = reported.subtract(new Vector3d(this.lastGoodX, this.lastGoodY, this.lastGoodZ));
        EnumGravityDirection direction = GravityPhysicsHelper.getDirection(player);
        if (direction == EnumGravityDirection.DOWN) {
            return rise.y > 0.0D;
        }
        return direction.inverseAdjustVec(rise).y > 0.0D;
    }
}
