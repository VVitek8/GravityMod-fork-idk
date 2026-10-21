package uk.co.mysterymayhem.gravitymod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import uk.co.mysterymayhem.gravitymod.GravityMod;
import uk.co.mysterymayhem.gravitymod.item.GravityGunItem;
import uk.co.mysterymayhem.gravitymod.network.GravityGunPacket;
import uk.co.mysterymayhem.gravitymod.network.PacketHandler;





















@Mod.EventBusSubscriber(modid = GravityMod.MODID, value = Dist.CLIENT)
public final class GravityGunClient {

    private GravityGunClient() {}

    
    private static boolean pulling;

    



    private static boolean punting;

    



    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientPlayerEntity player = minecraft.player;

        if (punting && (player == null || !minecraft.options.keyAttack.isDown())) {
            punting = false;
        }
        if (!pulling) {
            return;
        }

        
        
        boolean stillHeld = player != null
                && minecraft.screen == null
                && minecraft.overlay == null
                && GravityGunItem.isHolding(player)
                && minecraft.options.keyUse.isDown();

        if (!stillHeld) {
            pulling = false;
            send(GravityGunPacket.Action.RELEASE_PULL);
        }
    }

    



    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClickInput(InputEvent.ClickInputEvent event) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player == null || !GravityGunItem.isHolding(player)) {
            return;
        }
        if (event.isUseItem()) {
            
            if (!pulling) {
                pulling = true;
                send(GravityGunPacket.Action.PRESS_PULL);
            }
        } else if (event.isAttack() && !punting) {
            punting = true;
            send(GravityGunPacket.Action.PUNT);
        }
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    private static void send(GravityGunPacket.Action action) {
        PacketHandler.INSTANCE.sendToServer(new GravityGunPacket(action));
    }
}
