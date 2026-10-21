package uk.co.mysterymayhem.gravitymod.container;

import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import uk.co.mysterymayhem.gravitymod.GravityMod;


public final class ModContainers {

    public static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(
            ForgeRegistries.CONTAINERS, GravityMod.MODID);

    public static final RegistryObject<ContainerType<GravityFieldCreatorContainer>> GRAVITY_FIELD_CREATOR =
            CONTAINERS.register("gravity_field_creator", () -> IForgeContainerType.create(GravityFieldCreatorContainer::new));

    private ModContainers() {}

    public static void register(IEventBus modEventBus) {
        CONTAINERS.register(modEventBus);
    }
}
