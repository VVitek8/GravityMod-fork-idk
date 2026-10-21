package uk.co.mysterymayhem.gravitymod.world;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import uk.co.mysterymayhem.gravitymod.network.CubePhysicsRulePacket;
import uk.co.mysterymayhem.gravitymod.network.PacketHandler;










public class GameRuleSyncHandler {

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        sendTo(event.getPlayer());
    }

    




    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sendTo(event.getPlayer());
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sendTo(event.getPlayer());
    }

    private static void sendTo(net.minecraft.entity.player.PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity)) {
            return;
        }
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                new CubePhysicsRulePacket(ModGameRules.isCubePhysicsEnabled(serverPlayer.getServer())));
    }
}
