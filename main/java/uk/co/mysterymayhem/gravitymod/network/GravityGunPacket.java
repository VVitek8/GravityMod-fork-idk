package uk.co.mysterymayhem.gravitymod.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import uk.co.mysterymayhem.gravitymod.item.GravityGunItem;

import java.util.function.Supplier;








public class GravityGunPacket {

    public enum Action {
        
        PRESS_PULL,
        
        RELEASE_PULL,
        
        PUNT
    }

    private static final Action[] ACTIONS = Action.values();

    private final Action action;

    public GravityGunPacket(Action action) {
        this.action = action;
    }

    public static void encode(GravityGunPacket packet, PacketBuffer buffer) {
        buffer.writeByte(packet.action.ordinal());
    }

    public static GravityGunPacket decode(PacketBuffer buffer) {
        int ordinal = buffer.readByte();
        
        return new GravityGunPacket(ordinal >= 0 && ordinal < ACTIONS.length ? ACTIONS[ordinal] : Action.RELEASE_PULL);
    }

    public static void handle(GravityGunPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayerEntity player = ctx.getSender();
            if (player == null) {
                return;
            }
            
            
            if (packet.action == GravityGunPacket.Action.RELEASE_PULL) {
                GravityGunItem.onReleasePull(player);
                return;
            }
            if (!GravityGunItem.isHolding(player)) {
                return;
            }
            switch (packet.action) {
                case PRESS_PULL:
                    GravityGunItem.onPressPull(player);
                    break;
                case PUNT:
                    GravityGunItem.onPunt(player);
                    break;
                default:
                    break;
            }
        });
        ctx.setPacketHandled(true);
    }
}
