package uk.co.mysterymayhem.gravitymod.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import uk.co.mysterymayhem.gravitymod.GravityMod;
import uk.co.mysterymayhem.gravitymod.util.GravityGunHelper;
import uk.co.mysterymayhem.gravitymod.util.PortalModCarryHelper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;











@Mod.EventBusSubscriber(modid = GravityMod.MODID)
public class GravityGunItem extends Item {

    











    private static final Map<UUID, Integer> PULLING = new ConcurrentHashMap<>();

    private static final int NO_TARGET = -1;

    public GravityGunItem(Properties properties) {
        super(properties);
    }

    
    
    

    
    
    

    





    public static void onPressPull(PlayerEntity player) {
        Entity carried = PortalModCarryHelper.carriedBy(player);
        if (carried != null) {
            GravityGunHelper.drop(player, carried);
            playSound(player, 0.7F);
            return;
        }
        PULLING.put(player.getUUID(), NO_TARGET);
    }

    
    public static void onReleasePull(PlayerEntity player) {
        PULLING.remove(player.getUUID());
    }

    





    public static void onPunt(PlayerEntity player) {
        Entity target = PortalModCarryHelper.carriedBy(player);
        if (target == null) {
            target = GravityGunHelper.findTarget(player, GravityGunHelper.PUNT_REACH);
        }
        if (target != null) {
            GravityGunHelper.punt(player, target);
            playSound(player, 1.2F);
        }
    }

    



    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PULLING.remove(event.getPlayer().getUUID());
    }

    
    
    

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level.isClientSide) {
            return;
        }
        PlayerEntity player = event.player;
        Integer locked = PULLING.get(player.getUUID());
        if (locked == null) {
            return;
        }
        
        
        if (!isHolding(player) || PortalModCarryHelper.carriedBy(player) != null) {
            PULLING.remove(player.getUUID());
            return;
        }

        Entity target = resolveTarget(player, locked);
        if (target == null) {
            
            
            PULLING.put(player.getUUID(), NO_TARGET);
            return;
        }
        PULLING.put(player.getUUID(), target.getId());

        if (GravityGunHelper.pull(player, target) && GravityGunHelper.grab(player, target)) {
            PULLING.remove(player.getUUID());
            playSound(player, 1.0F);
        }
    }

    



    private static Entity resolveTarget(PlayerEntity player, int lockedId) {
        if (lockedId != NO_TARGET) {
            Entity locked = player.level.getEntity(lockedId);
            if (locked != null && GravityGunHelper.canKeepPulling(player, locked)) {
                return locked;
            }
        }
        return GravityGunHelper.findTarget(player);
    }

    public static boolean isHolding(PlayerEntity player) {
        return player.getMainHandItem().getItem() instanceof GravityGunItem;
    }

    private static void playSound(PlayerEntity player, float pitch) {
        player.level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_PICKUP, SoundCategory.PLAYERS, 0.6F, pitch);
    }
}
