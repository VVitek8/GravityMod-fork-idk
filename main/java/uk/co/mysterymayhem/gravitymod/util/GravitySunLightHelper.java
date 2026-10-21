package uk.co.mysterymayhem.gravitymod.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.LightType;
import net.minecraft.world.World;



































public final class GravitySunLightHelper {

    








    private static final float DAY_LEAN = 1.0F;

    



    private static final float NIGHT_LEAN = 0.35F;

    
    private static final float MIN_LEAN = 1.0E-3F;

    
    private static final float MAX_SKY_LIGHT = 15.0F;

    private GravitySunLightHelper() {}

    










    public static Quaternion getKeyLightRotation(Entity entity, float partialTicks) {
        World world = entity.level;
        if (world == null || !world.dimensionType().hasSkyLight()) {
            
            
            
            return null;
        }

        
        
        
        
        
        float angle = world.getSunAngle(partialTicks);
        float skyX = -MathHelper.sin(angle);
        float skyY = MathHelper.cos(angle);

        
        
        
        boolean daytime = skyY > 0.0F;
        if (!daytime) {
            skyX = -skyX;
            skyY = -skyY;
        }

        float lean = leanTowardsSky(entity, world, daytime, skyY, partialTicks);
        if (lean < MIN_LEAN) {
            return null;
        }

        
        
        
        
        float keyX = skyX * lean;
        float keyY = 1.0F - lean + skyY * lean;

        
        
        
        return new Quaternion(Vector3f.ZP, (float) Math.atan2(keyX, keyY), false);
    }

    



    private static float leanTowardsSky(Entity entity, World world, boolean daytime, float elevation, float partialTicks) {
        float lean = daytime ? DAY_LEAN : NIGHT_LEAN;

        
        
        
        
        
        
        lean *= elevation;

        
        
        
        BlockPos probe = new BlockPos(entity.getLightProbePosition(partialTicks));
        lean *= world.getBrightness(LightType.SKY, probe) / MAX_SKY_LIGHT;

        
        
        lean *= 1.0F - world.getRainLevel(partialTicks);

        return lean;
    }
}
