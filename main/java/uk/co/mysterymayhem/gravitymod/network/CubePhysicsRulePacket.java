package uk.co.mysterymayhem.gravitymod.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import uk.co.mysterymayhem.gravitymod.world.ModGameRules;

import java.util.function.Supplier;












public class CubePhysicsRulePacket {

    private final boolean enabled;

    public CubePhysicsRulePacket(boolean enabled) {
        this.enabled = enabled;
    }

    public static void encode(CubePhysicsRulePacket msg, PacketBuffer buf) {
        buf.writeBoolean(msg.enabled);
    }

    public static CubePhysicsRulePacket decode(PacketBuffer buf) {
        return new CubePhysicsRulePacket(buf.readBoolean());
    }

    public static void handle(CubePhysicsRulePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ModGameRules.acceptClientValue(msg.enabled)));
        ctx.setPacketHandled(true);
    }
}
