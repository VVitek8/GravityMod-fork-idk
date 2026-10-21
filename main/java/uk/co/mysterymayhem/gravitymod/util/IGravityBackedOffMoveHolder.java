package uk.co.mysterymayhem.gravitymod.util;

import net.minecraft.util.math.vector.Vector3d;

















public interface IGravityBackedOffMoveHolder {

    Vector3d gravitymod$getBackedOffMoveDelta();

    void gravitymod$setBackedOffMoveDelta(Vector3d delta);

    void gravitymod$setPortalTransformedMoveDelta(Vector3d delta);
}
