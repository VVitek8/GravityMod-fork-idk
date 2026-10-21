package uk.co.mysterymayhem.gravitymod.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.ModList;
import net.portalmod.common.sorted.cube.Cube;
import uk.co.mysterymayhem.gravitymod.GravityMod;















public final class CubeHelper {

    private CubeHelper() {}

    



    private static final Tags.IOptionalNamedTag<EntityType<?>> CUBES =
            EntityTypeTags.createOptional(new ResourceLocation(GravityMod.MODID, "cubes"));

    



    private static final boolean PORTAL_MOD_PRESENT = detectPortalMod();

    private static boolean detectPortalMod() {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded("portalmod");
    }

    public static boolean isCube(Entity entity) {
        return CUBES.contains(entity.getType()) || (PORTAL_MOD_PRESENT && Bridge.isCube(entity));
    }

    



    private static final class Bridge {

        private Bridge() {}

        static boolean isCube(Entity entity) {
            
            
            
            return Cube.class.isInstance(entity);
        }
    }
}
