package uk.co.mysterymayhem.gravitymod.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorldReader;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;


























public final class GravityShadowHelper {

    
    private static final int FULL_BRIGHT = 15728880;

    private GravityShadowHelper() {}

    



    public static void renderShadow(MatrixStack matrixStack, IVertexBuilder builder, Entity entity,
                                    EnumGravityDirection direction, float shadowAlpha, float partialTicks,
                                    IWorldReader world, float shadowRadius) {
        float radius = shadowRadius;
        if (entity instanceof MobEntity && ((MobEntity) entity).isBaby()) {
            radius = shadowRadius * 0.5F;
        }

        
        
        
        Vector3d feet = direction.inverseAdjustVec(new Vector3d(
                MathHelper.lerp((double) partialTicks, entity.xOld, entity.getX()),
                MathHelper.lerp((double) partialTicks, entity.yOld, entity.getY()),
                MathHelper.lerp((double) partialTicks, entity.zOld, entity.getZ())));

        
        
        
        
        
        double[] near = direction.adjustXYZValues(feet.x - radius, feet.y - radius, feet.z - radius);
        double[] far = direction.adjustXYZValues(feet.x + radius, feet.y, feet.z + radius);
        BlockPos min = new BlockPos(
                MathHelper.floor(Math.min(near[0], far[0])),
                MathHelper.floor(Math.min(near[1], far[1])),
                MathHelper.floor(Math.min(near[2], far[2])));
        BlockPos max = new BlockPos(
                MathHelper.floor(Math.max(near[0], far[0])),
                MathHelper.floor(Math.max(near[1], far[1])),
                MathHelper.floor(Math.max(near[2], far[2])));

        
        
        double[] up = direction.adjustXYZValues(0.0D, 1.0D, 0.0D);
        Direction gravity = direction.getFacingEquivalent();
        MatrixStack.Entry entry = matrixStack.last();

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            renderBlockShadow(entry, builder, world, pos, direction, gravity, up, feet, radius, shadowAlpha);
        }
    }

    




    private static void renderBlockShadow(MatrixStack.Entry entry, IVertexBuilder builder, IWorldReader world,
                                          BlockPos pos, EnumGravityDirection direction, Direction gravity,
                                          double[] up, Vector3d feet, float radius, float shadowAlpha) {
        BlockPos ground = pos.relative(gravity);
        BlockState state = world.getBlockState(ground);
        if (state.getRenderShape() == BlockRenderType.INVISIBLE || world.getMaxLocalRawBrightness(pos) <= 3) {
            return;
        }
        if (!state.isCollisionShapeFullBlock(world, ground)) {
            return;
        }
        VoxelShape shape = state.getShape(world, ground);
        if (shape.isEmpty()) {
            return;
        }

        
        
        
        AxisAlignedBB bounds = shape.bounds().move(ground);
        Vector3d cornerA = direction.inverseAdjustVec(new Vector3d(bounds.minX, bounds.minY, bounds.minZ));
        Vector3d cornerB = direction.inverseAdjustVec(new Vector3d(bounds.maxX, bounds.maxY, bounds.maxZ));
        
        double surface = Math.max(cornerA.y, cornerB.y);

        float alpha = (float) ((shadowAlpha - (feet.y - surface) / 2.0D) * 0.5D * world.getBrightness(pos));
        if (alpha < 0.0F) {
            return;
        }
        if (alpha > 1.0F) {
            alpha = 1.0F;
        }

        float x0 = (float) (Math.min(cornerA.x, cornerB.x) - feet.x);
        float x1 = (float) (Math.max(cornerA.x, cornerB.x) - feet.x);
        float y = (float) (surface - feet.y);
        float z0 = (float) (Math.min(cornerA.z, cornerB.z) - feet.z);
        float z1 = (float) (Math.max(cornerA.z, cornerB.z) - feet.z);

        float u0 = -x0 / 2.0F / radius + 0.5F;
        float u1 = -x1 / 2.0F / radius + 0.5F;
        float v0 = -z0 / 2.0F / radius + 0.5F;
        float v1 = -z1 / 2.0F / radius + 0.5F;

        
        shadowVertex(entry, builder, direction, up, alpha, x0, y, z0, u0, v0);
        shadowVertex(entry, builder, direction, up, alpha, x0, y, z1, u0, v1);
        shadowVertex(entry, builder, direction, up, alpha, x1, y, z1, u1, v1);
        shadowVertex(entry, builder, direction, up, alpha, x1, y, z0, u1, v0);
    }

    




    private static void shadowVertex(MatrixStack.Entry entry, IVertexBuilder builder, EnumGravityDirection direction,
                                     double[] up, float alpha, float x, float y, float z, float u, float v) {
        double[] world = direction.adjustXYZValues(x, y, z);
        builder.vertex(entry.pose(), (float) world[0], (float) world[1], (float) world[2])
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(entry.normal(), (float) up[0], (float) up[1], (float) up[2])
                .endVertex();
    }
}
