package uk.co.mysterymayhem.gravitymod.capability;

import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import uk.co.mysterymayhem.gravitymod.GravityMod;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.network.GravitySyncPacket;
import uk.co.mysterymayhem.gravitymod.network.PacketHandler;

public class GravityCapabilityEventHandler {

    public static final ResourceLocation CAPABILITY_ID = new ResourceLocation(GravityMod.MODID, "gravity_direction");

    












    @SubscribeEvent
    public void attachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof LivingEntity || event.getObject() instanceof ItemEntity
                || event.getObject() instanceof ProjectileEntity) {
            event.addCapability(CAPABILITY_ID, new GravityCapabilityProvider());
        }
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingUpdateEvent event) {
        event.getEntityLiving().getCapability(CapabilityGravityDirection.GRAVITY_CAPABILITY)
                .ifPresent(IGravityCapability::tick);
    }

    
    
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            EnumGravityDirection direction = GravityAPI.getDirection(player);
            if (direction != EnumGravityDirection.DOWN || GravityAPI.getStrength(player) != IGravityCapability.DEFAULT_STRENGTH) {
                GravityAPI.syncTo(player, direction);
            }
        }
    }

    





    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) {
            return;
        }
        Entity target = event.getTarget();
        ServerPlayerEntity tracker = (ServerPlayerEntity) event.getPlayer();
        EnumGravityDirection direction = GravityAPI.getDirection(target);
        if (direction != EnumGravityDirection.DOWN || GravityAPI.getStrength(target) != IGravityCapability.DEFAULT_STRENGTH) {
            PacketHandler.INSTANCE.send(
                    net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> tracker),
                    new GravitySyncPacket(target.getId(), direction, GravityAPI.getStrength(target), false));
        }
        
        
        Object cubeOrientation = uk.co.mysterymayhem.gravitymod.util.CubePhysics.trackingPacket(target);
        if (cubeOrientation != null) {
            PacketHandler.INSTANCE.send(
                    net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> tracker), cubeOrientation);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        PlayerEntity original = event.getOriginal();
        PlayerEntity clone = event.getPlayer();
        EnumGravityDirection direction = GravityAPI.getDirection(original);
        double strength = GravityAPI.getStrength(original);
        clone.getCapability(CapabilityGravityDirection.GRAVITY_CAPABILITY)
                .ifPresent(cap -> {
                    cap.setDirection(direction, true);
                    cap.setStrength(strength);
                });
    }

    








    @SubscribeEvent
    public void onBabySpawn(BabyEntitySpawnEvent event) {
        AgeableEntity child = event.getChild();
        if (child == null) {
            return;
        }
        EnumGravityDirection parentA = GravityAPI.getDirection(event.getParentA());
        EnumGravityDirection parentB = GravityAPI.getDirection(event.getParentB());
        boolean inheritA = parentA == parentB || child.getRandom().nextBoolean();
        EnumGravityDirection inherited = inheritA ? parentA : parentB;
        double inheritedStrength = GravityAPI.getStrength(inheritA ? event.getParentA() : event.getParentB());
        if (inherited != EnumGravityDirection.DOWN || inheritedStrength != IGravityCapability.DEFAULT_STRENGTH) {
            child.getCapability(CapabilityGravityDirection.GRAVITY_CAPABILITY)
                    .ifPresent(cap -> {
                        cap.setDirection(inherited, true);
                        cap.setStrength(inheritedStrength);
                    });
        }
    }

    



















    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntityLiving();
        EnumGravityDirection direction = GravityAPI.getDirection(dead);
        double strength = GravityAPI.getStrength(dead);
        if (direction == EnumGravityDirection.DOWN && strength == IGravityCapability.DEFAULT_STRENGTH) {
            return;
        }
        Vector3d origin = dead.position();
        for (ItemEntity drop : event.getDrops()) {
            drop.getCapability(CapabilityGravityDirection.GRAVITY_CAPABILITY)
                    .ifPresent(cap -> {
                        cap.setDirection(direction, true);
                        cap.setStrength(strength);
                    });
            if (direction != EnumGravityDirection.DOWN) {
                Vector3d spawnOffset = direction.adjustVec(drop.position().subtract(origin));
                drop.setPos(origin.x + spawnOffset.x, origin.y + spawnOffset.y, origin.z + spawnOffset.z);
                drop.setDeltaMovement(direction.adjustVec(drop.getDeltaMovement()));
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity && !event.getWorld().isClientSide) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
            EnumGravityDirection direction = GravityAPI.getDirection(player);
            if (direction != EnumGravityDirection.DOWN || GravityAPI.getStrength(player) != IGravityCapability.DEFAULT_STRENGTH) {
                PacketHandler.INSTANCE.send(net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> player),
                        new GravitySyncPacket(player.getId(), direction, GravityAPI.getStrength(player), false));
            }
        }
    }
}
