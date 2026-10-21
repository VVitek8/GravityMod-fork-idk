package uk.co.mysterymayhem.gravitymod.network;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import uk.co.mysterymayhem.gravitymod.util.CubePhysics;

import java.util.function.Supplier;



















public class CubeOrientationPacket {

    private final int entityId;
    private final float x;
    private final float y;
    private final float z;
    private final float w;
    private final float spinX;
    private final float spinY;
    private final float spinZ;
    private final boolean controlling;

    public CubeOrientationPacket(int entityId, float x, float y, float z, float w,
                                 float spinX, float spinY, float spinZ, boolean controlling) {
        this.entityId = entityId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        this.spinX = spinX;
        this.spinY = spinY;
        this.spinZ = spinZ;
        this.controlling = controlling;
    }

    public static void encode(CubeOrientationPacket msg, PacketBuffer buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeFloat(msg.x);
        buf.writeFloat(msg.y);
        buf.writeFloat(msg.z);
        buf.writeFloat(msg.w);
        buf.writeFloat(msg.spinX);
        buf.writeFloat(msg.spinY);
        buf.writeFloat(msg.spinZ);
        buf.writeBoolean(msg.controlling);
    }

    public static CubeOrientationPacket decode(PacketBuffer buf) {
        return new CubeOrientationPacket(buf.readVarInt(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean());
    }

    public static void handle(CubeOrientationPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> apply(msg)));
        ctx.setPacketHandled(true);
    }

    private static void apply(CubeOrientationPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(msg.entityId);
        if (entity == null) {
            
            
            return;
        }
        CubePhysics.acceptOrientation(entity, msg.x, msg.y, msg.z, msg.w,
                new Vector3d(msg.spinX, msg.spinY, msg.spinZ), msg.controlling);
    }
}
