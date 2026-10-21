package uk.co.mysterymayhem.gravitymod.network;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;



















public class GravityGunSnapPacket {

    private final int entityId;
    private final double x;
    private final double y;
    private final double z;

    public GravityGunSnapPacket(Entity entity) {
        this(entity.getId(), entity.getX(), entity.getY(), entity.getZ());
    }

    private GravityGunSnapPacket(int entityId, double x, double y, double z) {
        this.entityId = entityId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void encode(GravityGunSnapPacket packet, PacketBuffer buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
    }

    public static GravityGunSnapPacket decode(PacketBuffer buffer) {
        return new GravityGunSnapPacket(buffer.readVarInt(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble());
    }

    public static void handle(GravityGunSnapPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> packet.apply()));
        ctx.setPacketHandled(true);
    }

    private void apply() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(this.entityId);
        if (entity == null) {
            return;
        }
        
        
        
        entity.lerpTo(this.x, this.y, this.z, entity.yRot, entity.xRot, 0, false);
        entity.moveTo(this.x, this.y, this.z, entity.yRot, entity.xRot);
    }
}
