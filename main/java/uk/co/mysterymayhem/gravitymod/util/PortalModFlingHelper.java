package uk.co.mysterymayhem.gravitymod.util;

import net.minecraft.entity.Entity;
import net.minecraftforge.fml.ModList;
import net.portalmod.common.sorted.faithplate.Flingable;

























public final class PortalModFlingHelper {

    private PortalModFlingHelper() {}

    



    private static final boolean PORTAL_MOD_PRESENT = detectPortalMod();

    private static boolean detectPortalMod() {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded("portalmod");
    }

    



    public static boolean isFlinging(Entity entity) {
        return PORTAL_MOD_PRESENT && Bridge.isFlinging(entity);
    }

    



    private static final class Bridge {

        private Bridge() {}

        static boolean isFlinging(Entity entity) {
            return entity instanceof Flingable && ((Flingable) entity).isFlinging();
        }
    }
}
