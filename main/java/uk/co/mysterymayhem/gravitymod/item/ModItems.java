package uk.co.mysterymayhem.gravitymod.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BlockItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import uk.co.mysterymayhem.gravitymod.GravityMod;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.block.ModBlocks;
import uk.co.mysterymayhem.gravitymod.client.GravityGunItemRenderer;

import javax.annotation.Nonnull;





public final class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GravityMod.MODID);

    
    public static final ItemGroup GRAVITY_TAB = new ItemGroup("gravitymod") {
        @Nonnull
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(GRAVITY_CHANGER_UP.get());
        }
    };

    public static final RegistryObject<Item> GRAVITY_CHANGER_UP = register("gravity_changer_up", EnumGravityDirection.UP, false);
    public static final RegistryObject<Item> GRAVITY_CHANGER_DOWN = register("gravity_changer_down", EnumGravityDirection.DOWN, false);
    public static final RegistryObject<Item> GRAVITY_CHANGER_NORTH = register("gravity_changer_north", EnumGravityDirection.NORTH, false);
    public static final RegistryObject<Item> GRAVITY_CHANGER_SOUTH = register("gravity_changer_south", EnumGravityDirection.SOUTH, false);
    public static final RegistryObject<Item> GRAVITY_CHANGER_EAST = register("gravity_changer_east", EnumGravityDirection.EAST, false);
    public static final RegistryObject<Item> GRAVITY_CHANGER_WEST = register("gravity_changer_west", EnumGravityDirection.WEST, false);

    public static final RegistryObject<Item> GRAVITY_CHANGER_UP_AOE = register("gravity_changer_up_aoe", EnumGravityDirection.UP, true);
    public static final RegistryObject<Item> GRAVITY_CHANGER_DOWN_AOE = register("gravity_changer_down_aoe", EnumGravityDirection.DOWN, true);
    public static final RegistryObject<Item> GRAVITY_CHANGER_NORTH_AOE = register("gravity_changer_north_aoe", EnumGravityDirection.NORTH, true);
    public static final RegistryObject<Item> GRAVITY_CHANGER_SOUTH_AOE = register("gravity_changer_south_aoe", EnumGravityDirection.SOUTH, true);
    public static final RegistryObject<Item> GRAVITY_CHANGER_EAST_AOE = register("gravity_changer_east_aoe", EnumGravityDirection.EAST, true);
    public static final RegistryObject<Item> GRAVITY_CHANGER_WEST_AOE = register("gravity_changer_west_aoe", EnumGravityDirection.WEST, true);

    
    public static final RegistryObject<Item> GRAVITY_STRENGTH_ADJUSTER = ITEMS.register("gravity_strength_adjuster",
            () -> new GravityStrengthAdjusterItem(new Item.Properties().tab(GRAVITY_TAB).stacksTo(1)));

    
    public static final RegistryObject<Item> GRAVITY_FIELD_CREATOR = ITEMS.register("gravity_field_creator",
            () -> new BlockItem(ModBlocks.GRAVITY_FIELD_CREATOR.get(), new Item.Properties().tab(GRAVITY_TAB)));

    




    public static final RegistryObject<Item> GRAVITY_GUN = ITEMS.register("gravity_gun",
            () -> new GravityGunItem(new Item.Properties().tab(GRAVITY_TAB).stacksTo(1)
                    .setISTER(() -> GravityGunItemRenderer::new)));

    private ModItems() {}

    private static RegistryObject<Item> register(String name, EnumGravityDirection direction, boolean aoe) {
        return ITEMS.register(name,
                () -> new GravityChangerItem(direction, aoe, new Item.Properties().tab(GRAVITY_TAB)));
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
