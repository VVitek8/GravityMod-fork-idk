package uk.co.mysterymayhem.gravitymod.network;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.capability.CapabilityGravityDirection;
import uk.co.mysterymayhem.gravitymod.capability.GravityAPI;

import java.util.function.Supplier;





public class GravitySyncPacket {

    private final int entityId;
    private final EnumGravityDirection direction;
    private final double strength;
    private final boolean isChange;

    public GravitySyncPacket(int entityId, EnumGravityDirection direction, double strength, boolean isChange) {
        this.entityId = entityId;
        this.direction = direction;
        this.strength = strength;
        this.isChange = isChange;
    }

    public static void encode(GravitySyncPacket msg, PacketBuffer buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeEnum(msg.direction);
        buf.writeDouble(msg.strength);
        buf.writeBoolean(msg.isChange);
    }

    public static GravitySyncPacket decode(PacketBuffer buf) {
        return new GravitySyncPacket(buf.readVarInt(), buf.readEnum(EnumGravityDirection.class), buf.readDouble(), buf.readBoolean());
    }

    public static void handle(GravitySyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> apply(msg)));
        ctx.setPacketHandled(true);
    }

    private static void apply(GravitySyncPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(msg.entityId);
        if (entity == null) {
            return;
        }
        EnumGravityDirection oldDirection = GravityAPI.getDirection(entity);
        if (oldDirection == msg.direction && Double.compare(GravityAPI.getStrength(entity), msg.strength) == 0) {
            return;
        }
        entity.getCapability(CapabilityGravityDirection.GRAVITY_CAPABILITY).ifPresent(cap -> {
            cap.setDirection(msg.direction, true);
            cap.setStrength(msg.strength);
            
            
            
            
            if (msg.isChange) {
                cap.beginTransition(oldDirection);
            }
        });
        if (msg.isChange) {
            
            
            
            
            GravityAPI.pivotForDirectionChange(entity, oldDirection, msg.direction);
            
            
            
            GravityAPI.applyYawCorrection(entity, oldDirection, msg.direction);
            
            
            
        } else {
            
            
            
            
            
            
            
            
            
            
            
            entity.setPos(entity.getX(), entity.getY(), entity.getZ());
        }
    }
}
