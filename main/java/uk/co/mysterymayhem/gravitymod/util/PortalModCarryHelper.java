package uk.co.mysterymayhem.gravitymod.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SSetPassengersPacket;
import net.minecraftforge.fml.ModList;
import net.portalmod.common.entities.TestElementEntity;
import uk.co.mysterymayhem.gravitymod.network.GravityGunSnapPacket;
import uk.co.mysterymayhem.gravitymod.network.PacketHandler;

import java.util.List;























public final class PortalModCarryHelper {

    private PortalModCarryHelper() {}

    



    private static final boolean PORTAL_MOD_PRESENT = detectPortalMod();

    private static boolean detectPortalMod() {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded("portalmod");
    }

    
    public static boolean isAvailable() {
        return PORTAL_MOD_PRESENT;
    }

    



    public static boolean canCarry(Entity entity) {
        return PORTAL_MOD_PRESENT
                && entity != null
                && entity.isAlive()
                && entity.getVehicle() == null
                && CubeHelper.isCube(entity)
                && Bridge.isHoldable(entity);
    }

    
    public static Entity carriedBy(PlayerEntity player) {
        if (!PORTAL_MOD_PRESENT || player == null) {
            return null;
        }
        List<Entity> passengers = player.getPassengers();
        for (int i = 0; i < passengers.size(); i++) {
            Entity passenger = passengers.get(i);
            if (CubeHelper.isCube(passenger)) {
                return passenger;
            }
        }
        return null;
    }

    




    public static boolean grab(Entity cube, PlayerEntity player) {
        if (!PORTAL_MOD_PRESENT || !cube.startRiding(player)) {
            return false;
        }
        
        
        
        
        
        
        sendPositionToTrackers(cube);
        sendCarryToHolder(player);
        return true;
    }

    







    private static void sendPositionToTrackers(Entity entity) {
        PacketHandler.sendToTracking(entity, new GravityGunSnapPacket(entity));
    }

    

















    private static void sendCarryToHolder(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity) player).connection.send(new SSetPassengersPacket(player));
        }
    }

    









    public static void release(Entity cube) {
        Entity vehicle = cube.getVehicle();
        cube.removeVehicle();
        
        
        
        if (vehicle instanceof PlayerEntity) {
            sendCarryToHolder((PlayerEntity) vehicle);
        }
    }

    



    private static final class Bridge {

        private Bridge() {}

        static boolean isHoldable(Entity entity) {
            return TestElementEntity.isHoldable(entity);
        }
    }
}
