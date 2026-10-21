package uk.co.mysterymayhem.gravitymod;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.co.mysterymayhem.gravitymod.capability.CapabilityGravityDirection;
import uk.co.mysterymayhem.gravitymod.capability.GravityCapabilityEventHandler;
import uk.co.mysterymayhem.gravitymod.capability.IGravityCapability;
import uk.co.mysterymayhem.gravitymod.command.GravityCommand;
import uk.co.mysterymayhem.gravitymod.block.ModBlocks;
import uk.co.mysterymayhem.gravitymod.block.ModTileEntities;
import uk.co.mysterymayhem.gravitymod.container.ModContainers;
import uk.co.mysterymayhem.gravitymod.item.ModItems;
import uk.co.mysterymayhem.gravitymod.network.PacketHandler;
import uk.co.mysterymayhem.gravitymod.world.GameRuleSyncHandler;
import uk.co.mysterymayhem.gravitymod.world.ModGameRules;

@Mod(GravityMod.MODID)
public class GravityMod {

    public static final String MODID = "gravitymod";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public GravityMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        ModBlocks.register(modEventBus);
        ModTileEntities.register(modEventBus);
        ModContainers.register(modEventBus);
        ModItems.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(new GravityCapabilityEventHandler());
        MinecraftForge.EVENT_BUS.register(new GameRuleSyncHandler());
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        
        
        
        CapabilityManager.INSTANCE.register(IGravityCapability.class, new CapabilityGravityDirection.Storage(), CapabilityGravityDirection.DefaultImpl::new);
        PacketHandler.register();
        
        
        event.enqueueWork(ModGameRules::register);
    }

    private void registerCommands(final RegisterCommandsEvent event) {
        GravityCommand.register(event.getDispatcher());
    }
}
