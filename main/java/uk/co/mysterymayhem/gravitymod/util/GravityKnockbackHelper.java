package uk.co.mysterymayhem.gravitymod.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;






public final class GravityKnockbackHelper {

    private GravityKnockbackHelper() {}

    










    public static void fromAttackerFacing(LivingEntity attacker, LivingEntity victim,
                                          float strength, double ratioX, double ratioZ) {
        EnumGravityDirection attackerDirection = GravityPhysicsHelper.getDirection(attacker);
        if (attackerDirection == EnumGravityDirection.DOWN) {
            victim.knockback(strength, ratioX, ratioZ);
            return;
        }
        Vector3d world = attackerDirection.adjustVec(new Vector3d(ratioX, 0.0D, ratioZ));
        withWorldDirection(victim, world, strength, world.x, world.z);
    }

    










    public static void fromWorldDisplacement(LivingEntity victim, Vector3d worldDisplacement,
                                             float strength, double ratioX, double ratioZ) {
        withWorldDirection(victim, worldDisplacement, strength, ratioX, ratioZ);
    }

    private static void withWorldDirection(LivingEntity victim, Vector3d worldDirection,
                                           float strength, double ratioX, double ratioZ) {
        
        
        boolean stashed = victim instanceof IGravityKnockbackHolder
                && GravityPhysicsHelper.getDirection(victim) != EnumGravityDirection.DOWN;
        if (stashed) {
            ((IGravityKnockbackHolder) victim).gravitymod$setKnockbackDirection(worldDirection);
        }
        try {
            victim.knockback(strength, ratioX, ratioZ);
        }
        finally {
            
            
            if (stashed) {
                ((IGravityKnockbackHolder) victim).gravitymod$setKnockbackDirection(null);
            }
        }
    }
}
