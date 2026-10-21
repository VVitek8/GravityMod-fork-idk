package uk.co.mysterymayhem.gravitymod.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.ModList;
import net.portalmod.common.sorted.portal.PortalEntity;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;

import java.util.stream.Stream;











































public final class PortalModCollisionHelper {

    private PortalModCollisionHelper() {}

    



    private static final boolean PORTAL_MOD_PRESENT = detectPortalMod();

    private static boolean detectPortalMod() {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded("portalmod");
    }

    









    public static Stream<VoxelShape> additionalCollisions(Entity entity, Vector3d motion,
                                                          EnumGravityDirection direction) {
        if (!PORTAL_MOD_PRESENT) {
            return Stream.empty();
        }
        return Stream.concat(Stream.of(Bridge.portalShape(entity)), Bridge.cubesStoodOn(entity, motion, direction));
    }

    



    private static final class Bridge {

        private Bridge() {}

        static VoxelShape portalShape(Entity entity) {
            return PortalEntity.getCollisionShape(entity);
        }

        







        static Stream<VoxelShape> cubesStoodOn(Entity entity, Vector3d motion, EnumGravityDirection direction) {
            return Stream.empty();
        }
    }
}
