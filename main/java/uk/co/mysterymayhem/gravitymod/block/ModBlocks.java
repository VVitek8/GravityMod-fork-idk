package uk.co.mysterymayhem.gravitymod.block;

import net.minecraft.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import uk.co.mysterymayhem.gravitymod.GravityMod;


public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GravityMod.MODID);

    public static final RegistryObject<Block> GRAVITY_FIELD_CREATOR = BLOCKS.register(
            "gravity_field_creator", GravityFieldCreatorBlock::new);

    private ModBlocks() {}

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
