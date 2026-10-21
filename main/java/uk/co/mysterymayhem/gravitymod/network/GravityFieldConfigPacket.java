package uk.co.mysterymayhem.gravitymod.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.block.GravityFieldCreatorTileEntity;

import java.util.function.Supplier;


public final class GravityFieldConfigPacket {

    private final BlockPos blockPos;
    private final double width;
    private final double depth;
    private final double height;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;
    private final EnumGravityDirection direction;
    private final double strength;
    private final boolean fieldVisible;
    private final boolean keepGravity;
    private final boolean affectPlayers;
    private final boolean affectCubes;

    public GravityFieldConfigPacket(BlockPos blockPos, double width, double depth, double height, double offsetX,
                                    double offsetY, double offsetZ, EnumGravityDirection direction, double strength,
                                    boolean fieldVisible, boolean keepGravity, boolean affectPlayers,
                                    boolean affectCubes) {
        this.blockPos = blockPos;
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.direction = direction;
        this.strength = strength;
        this.fieldVisible = fieldVisible;
        this.keepGravity = keepGravity;
        this.affectPlayers = affectPlayers;
        this.affectCubes = affectCubes;
    }

    public static void encode(GravityFieldConfigPacket packet, PacketBuffer buffer) {
        buffer.writeBlockPos(packet.blockPos);
        buffer.writeDouble(packet.width);
        buffer.writeDouble(packet.depth);
        buffer.writeDouble(packet.height);
        buffer.writeDouble(packet.offsetX);
        buffer.writeDouble(packet.offsetY);
        buffer.writeDouble(packet.offsetZ);
        buffer.writeEnum(packet.direction);
        buffer.writeDouble(packet.strength);
        buffer.writeBoolean(packet.fieldVisible);
        buffer.writeBoolean(packet.keepGravity);
        buffer.writeBoolean(packet.affectPlayers);
        buffer.writeBoolean(packet.affectCubes);
    }

    public static GravityFieldConfigPacket decode(PacketBuffer buffer) {
        return new GravityFieldConfigPacket(buffer.readBlockPos(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readEnum(EnumGravityDirection.class), buffer.readDouble(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(GravityFieldConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayerEntity player = context.getSender();
            if (player == null || player.distanceToSqr(packet.blockPos.getX() + 0.5D, packet.blockPos.getY() + 0.5D,
                    packet.blockPos.getZ() + 0.5D) > 64.0D) {
                return;
            }
            TileEntity tileEntity = player.level.getBlockEntity(packet.blockPos);
            if (tileEntity instanceof GravityFieldCreatorTileEntity) {
                ((GravityFieldCreatorTileEntity) tileEntity).setConfiguration(packet.width, packet.depth, packet.height,
                        packet.offsetX, packet.offsetY, packet.offsetZ, packet.direction, packet.strength,
                        packet.fieldVisible, packet.keepGravity, packet.affectPlayers, packet.affectCubes);
            }
        });
        context.setPacketHandled(true);
    }
}
