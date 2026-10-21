package uk.co.mysterymayhem.gravitymod.network;

import net.minecraft.entity.Entity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import uk.co.mysterymayhem.gravitymod.GravityMod;

public class PacketHandler {

    
    
    
    
    private static final String PROTOCOL_VERSION = "8";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GravityMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        INSTANCE.registerMessage(packetId++, GravitySyncPacket.class, GravitySyncPacket::encode, GravitySyncPacket::decode, GravitySyncPacket::handle);
        INSTANCE.registerMessage(packetId++, GravityFieldConfigPacket.class, GravityFieldConfigPacket::encode,
                GravityFieldConfigPacket::decode, GravityFieldConfigPacket::handle);
        INSTANCE.registerMessage(packetId++, GravityStrengthPacket.class, GravityStrengthPacket::encode,
                GravityStrengthPacket::decode, GravityStrengthPacket::handle);
        INSTANCE.registerMessage(packetId++, CubeOrientationPacket.class, CubeOrientationPacket::encode,
                CubeOrientationPacket::decode, CubeOrientationPacket::handle);
        INSTANCE.registerMessage(packetId++, GravityGunPacket.class, GravityGunPacket::encode,
                GravityGunPacket::decode, GravityGunPacket::handle);
        INSTANCE.registerMessage(packetId++, GravityGunSnapPacket.class, GravityGunSnapPacket::encode,
                GravityGunSnapPacket::decode, GravityGunSnapPacket::handle);
        INSTANCE.registerMessage(packetId++, CubePhysicsRulePacket.class, CubePhysicsRulePacket::encode,
                CubePhysicsRulePacket::decode, CubePhysicsRulePacket::handle);
    }

    



    public static void sendToTracking(Entity entity, Object message) {
        if (entity.level instanceof ServerWorld) {
            INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
        }
    }
}
