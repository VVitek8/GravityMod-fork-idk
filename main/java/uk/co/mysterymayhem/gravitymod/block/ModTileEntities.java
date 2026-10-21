package uk.co.mysterymayhem.gravitymod.block;

import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import uk.co.mysterymayhem.gravitymod.GravityMod;


public final class ModTileEntities {

    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister.create(
            ForgeRegistries.TILE_ENTITIES, GravityMod.MODID);

    public static final RegistryObject<TileEntityType<GravityFieldCreatorTileEntity>> GRAVITY_FIELD_CREATOR =
            TILE_ENTITIES.register("gravity_field_creator", () -> TileEntityType.Builder
                    .of(GravityFieldCreatorTileEntity::new, ModBlocks.GRAVITY_FIELD_CREATOR.get())
                    .build(null));

    private ModTileEntities() {}

    public static void register(IEventBus modEventBus) {
        TILE_ENTITIES.register(modEventBus);
    }
}
