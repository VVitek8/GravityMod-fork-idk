package uk.co.mysterymayhem.gravitymod.mixin.compat;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.portalmod.common.sorted.portal.PortalEnd;
import net.portalmod.core.math.Vec3;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import uk.co.mysterymayhem.gravitymod.util.PortalModPortalHelper;

import java.util.UUID;
import java.util.function.Consumer;























@Pseudo
@Mixin(targets = "net.portalmod.common.sorted.portalgun.PortalGun", remap = false)
public abstract class PortalModPortalGunMixin {

    @Dynamic("PortalMod: PortalGun#placePortal")
    @ModifyArg(
            method = "placePortal",
            at = @At(value = "INVOKE", target = "Lnet/portalmod/common/sorted/portal/PortalPlacer;placePortal"
                    + "(Lnet/minecraft/world/World;Lnet/portalmod/common/sorted/portal/PortalEnd;Ljava/lang/String;"
                    + "Ljava/util/UUID;Lnet/portalmod/core/math/Vec3;Lnet/minecraft/util/Direction;"
                    + "Lnet/minecraft/util/Direction;ZZ[Lnet/minecraft/util/Direction;"
                    + "Lnet/minecraft/entity/player/ServerPlayerEntity;)Lnet/portalmod/common/sorted/portal/PortalEntity;"),
            index = 6
    )
    private static Direction gravitymod$orientImmediatePlacement(
            World level, PortalEnd end, String hue, UUID gunUuid, Vec3 position, Direction face, Direction original,
            boolean override, boolean overwriteForeignPortals, Direction[] lookingDirections, ServerPlayerEntity player) {
        return PortalModPortalHelper.portalUpDirection(player, face);
    }

    



    @Dynamic("PortalMod: PortalGun#placePortal")
    @ModifyArg(
            method = "placePortal",
            at = @At(value = "INVOKE", target = "Lnet/portalmod/common/sorted/portal/PortalManager;schedulePlacement"
                    + "(Lnet/minecraft/world/World;Lnet/portalmod/common/sorted/portal/PortalEnd;Ljava/lang/String;"
                    + "Ljava/util/UUID;Lnet/portalmod/core/math/Vec3;Lnet/minecraft/util/Direction;"
                    + "Lnet/minecraft/util/Direction;ZZ[Lnet/minecraft/util/Direction;"
                    + "Lnet/minecraft/entity/player/ServerPlayerEntity;JLjava/util/function/Consumer;)V"),
            index = 6
    )
    private static Direction gravitymod$orientDelayedPlacement(
            World level, PortalEnd end, String hue, UUID gunUuid, Vec3 position, Direction face, Direction original,
            boolean override, boolean overwriteForeignPortals, Direction[] lookingDirections, ServerPlayerEntity player,
            long ticks, Consumer<?> onPlace) {
        return PortalModPortalHelper.portalUpDirection(player, face);
    }
}
