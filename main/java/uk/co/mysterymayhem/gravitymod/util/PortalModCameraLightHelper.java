package uk.co.mysterymayhem.gravitymod.util;

import net.minecraftforge.fml.ModList;

























public final class PortalModCameraLightHelper {

    private PortalModCameraLightHelper() {}

    



    private static final boolean PORTAL_MOD_PRESENT = detectPortalMod();

    private static boolean detectPortalMod() {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded("portalmod");
    }

    



    public static boolean lightsCameraEntityFromCamera() {
        return PORTAL_MOD_PRESENT;
    }
}
