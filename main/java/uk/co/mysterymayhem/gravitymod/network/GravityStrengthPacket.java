package uk.co.mysterymayhem.gravitymod.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraftforge.fml.network.NetworkEvent;
import uk.co.mysterymayhem.gravitymod.capability.GravityAPI;
import uk.co.mysterymayhem.gravitymod.item.ModItems;

import java.util.function.Supplier;






public final class GravityStrengthPacket {

    private final double strength;

    public GravityStrengthPacket(double strength) {
        this.strength = strength;
    }

    public static void encode(GravityStrengthPacket packet, PacketBuffer buffer) {
        buffer.writeDouble(packet.strength);
    }

    public static GravityStrengthPacket decode(PacketBuffer buffer) {
        return new GravityStrengthPacket(buffer.readDouble());
    }

    public static void handle(GravityStrengthPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayerEntity player = context.getSender();
            
            
            if (player == null || !isHoldingAdjuster(player)) {
                return;
            }
            GravityAPI.setStrength(player, packet.strength);
        });
        context.setPacketHandled(true);
    }

    private static boolean isHoldingAdjuster(ServerPlayerEntity player) {
        for (Hand hand : Hand.values()) {
            if (player.getItemInHand(hand).getItem() == ModItems.GRAVITY_STRENGTH_ADJUSTER.get()) {
                return true;
            }
        }
        return false;
    }
}
