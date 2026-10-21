package uk.co.mysterymayhem.gravitymod.client.model;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;















@OnlyIn(Dist.CLIENT)
public final class GravityGunModel {

    




    private static final byte[] CORNERS = {
            1, 1, 0,  1, 0, 0,  0, 0, 0,  0, 1, 0, 
            1, 1, 1,  1, 0, 1,  1, 0, 0,  1, 1, 0, 
            0, 1, 1,  0, 0, 1,  1, 0, 1,  1, 1, 1, 
            0, 1, 0,  0, 0, 0,  0, 0, 1,  0, 1, 1, 
            0, 1, 0,  0, 1, 1,  1, 1, 1,  1, 1, 0, 
            0, 0, 1,  0, 0, 0,  1, 0, 0,  1, 0, 1, 
    };

    
    private static final byte[] FACE_AXIS = {2, 0, 2, 0, 1, 1};

    
    private static final byte[] NORMALS = {
            0, 0, -1, 
            1, 0, 0,  
            0, 0, 1,  
            -1, 0, 0, 
            0, 1, 0,  
            0, -1, 0, 
    };

    
    private static final float[][] CUBES = {
        
        {6F, 0.5F, 2F, 10F, 4.5F, 9F, 8F, 0.5F, 3F, 0F, 0F, 0F, 0.10938F, 0.3125F, 0.17188F, 0.375F, 0F, 0.3125F, 0.10938F, 0.375F, 0.28125F, 0.3125F, 0.34375F, 0.375F, 0.17188F, 0.3125F, 0.28125F, 0.375F, 0.17188F, 0.3125F, 0.10938F, 0.20312F, 0.23438F, 0.20312F, 0.17188F, 0.3125F},
        
        {5.5F, 0F, 9F, 10.5F, 5F, 12F, 8F, 0F, 14F, 0F, 0F, 0F, 0.79688F, 0.32812F, 0.875F, 0.40625F, 0.75F, 0.32812F, 0.79688F, 0.40625F, 0.92188F, 0.32812F, 1F, 0.40625F, 0.875F, 0.32812F, 0.92188F, 0.40625F, 0.875F, 0.32812F, 0.79688F, 0.28125F, 0.95312F, 0.28125F, 0.875F, 0.32812F},
        
        {6.501F, 0.001F, 10.001F, 9.499F, 3.999F, 13.999F, 8F, 0F, 11F, 0F, 0F, 0F, 0.42188F, 0.3125F, 0.46875F, 0.375F, 0.35938F, 0.3125F, 0.42188F, 0.375F, 0.53125F, 0.3125F, 0.57812F, 0.375F, 0.46875F, 0.3125F, 0.53125F, 0.375F, 0.46875F, 0.3125F, 0.42188F, 0.25F, 0.51562F, 0.25F, 0.46875F, 0.3125F},
        
        {4.5F, 0.5F, 10.5F, 11.5F, 3.5F, 13.5F, 8F, 0.5F, 12.5F, 0F, 0F, 0F, 0.32812F, 0.04688F, 0.4375F, 0.09375F, 0.28125F, 0.04688F, 0.32812F, 0.09375F, 0.48438F, 0.04688F, 0.59375F, 0.09375F, 0.4375F, 0.04688F, 0.48438F, 0.09375F, 0.4375F, 0.04688F, 0.32812F, 0F, 0.54688F, 0F, 0.4375F, 0.04688F},
        
        {7F, 0.75F, 14F, 9F, 2.75F, 19F, 8F, 2.75F, 14F, 20F, 0F, 0F, 0.39062F, 0.20312F, 0.42188F, 0.23438F, 0.3125F, 0.20312F, 0.39062F, 0.23438F, 0.5F, 0.20312F, 0.53125F, 0.23438F, 0.42188F, 0.20312F, 0.5F, 0.23438F, 0.42188F, 0.20312F, 0.39062F, 0.125F, 0.45312F, 0.125F, 0.42188F, 0.20312F},
        
        {5.85F, 0.35F, 1.7F, 10.15F, 4.65F, 2.75F, 8F, 0.25F, 2.75F, 0F, 0F, 0F, 0.59375F, 0.39062F, 0.65625F, 0.45312F, 0.57812F, 0.39062F, 0.59375F, 0.45312F, 0.67188F, 0.39062F, 0.73438F, 0.45312F, 0.65625F, 0.39062F, 0.67188F, 0.45312F, 0.65625F, 0.39062F, 0.59375F, 0.375F, 0.71875F, 0.375F, 0.65625F, 0.39062F},
        
        {5.85F, 0.35F, 2.75F, 10.15F, 4.65F, 9F, 8F, 0F, 8F, 0F, 0F, 0F, 0.10938F, 0.10938F, 0.1875F, 0.1875F, 0F, 0.10938F, 0.10938F, 0.1875F, 0.29688F, 0.10938F, 0.375F, 0.1875F, 0.1875F, 0.10938F, 0.29688F, 0.1875F, 0.1875F, 0.10938F, 0.10938F, 0F, 0.26562F, 0F, 0.1875F, 0.10938F},
        
        {4.6F, 1.8F, 13.5F, 6.4F, 3.6F, 14.3F, 6.4F, 1.8F, 13.4F, -22.5F, -5F, 0F, 0.01562F, 0.03125F, 0.04688F, 0.0625F, 0F, 0.03125F, 0.01562F, 0.0625F, 0.0625F, 0.03125F, 0.09375F, 0.0625F, 0.04688F, 0.03125F, 0.0625F, 0.0625F, 0.01562F, 0.01562F, 0.04688F, 0.03125F, 0.04688F, 0.01562F, 0.07812F, 0.03125F},
        
        {7.501F, 4.501F, 2.001F, 8.499F, 5.499F, 3.999F, 8F, 4.5F, 4F, 0F, 0F, 0F, 0.75F, 0.03125F, 0.76562F, 0.04688F, 0.71875F, 0.03125F, 0.75F, 0.04688F, 0.79688F, 0.03125F, 0.8125F, 0.04688F, 0.76562F, 0.03125F, 0.79688F, 0.04688F, 0.76562F, 0.03125F, 0.75F, 0F, 0.78125F, 0F, 0.76562F, 0.03125F},
        
        {5.751F, 1.501F, 2.001F, 6.749F, 2.499F, 3.999F, 6.25F, 1.5F, 2F, 0F, 0F, 120F, 0.64062F, 0.03125F, 0.65625F, 0.04688F, 0.60938F, 0.03125F, 0.64062F, 0.04688F, 0.6875F, 0.03125F, 0.70312F, 0.04688F, 0.65625F, 0.03125F, 0.6875F, 0.04688F, 0.65625F, 0.03125F, 0.64062F, 0F, 0.67188F, 0F, 0.65625F, 0.03125F},
        
        {9.251F, 1.501F, 2.001F, 10.249F, 2.499F, 3.999F, 9.75F, 1.5F, 2F, 0F, 0F, -120F, 0.85938F, 0.03125F, 0.875F, 0.04688F, 0.82812F, 0.03125F, 0.85938F, 0.04688F, 0.90625F, 0.03125F, 0.92188F, 0.04688F, 0.875F, 0.03125F, 0.90625F, 0.04688F, 0.875F, 0.03125F, 0.85938F, 0F, 0.89062F, 0F, 0.875F, 0.03125F},
        
        {8F, 2.5F, -3F, 8F, 6.5F, 2F, 8F, 4.5F, 2F, 35F, 0F, 0F, 0.70312F, 0.07812F, 0.70312F, 0.14062F, 0.71875F, 0.07812F, 0.79688F, 0.14062F, 0.70312F, 0.07812F, 0.70312F, 0.14062F, 0.79688F, 0.07812F, 0.875F, 0.14062F, 0.70312F, 0.14062F, 0.70312F, 0.0625F, 0.70312F, 0.0625F, 0.70312F, 0.14062F},
        
        {6.25F, -0.5F, -3F, 6.25F, 3.5F, 2F, 6.25F, 1.5F, 2F, 35F, 0F, 120F, 0.70312F, 0.17188F, 0.70312F, 0.23438F, 0.71875F, 0.17188F, 0.79688F, 0.23438F, 0.70312F, 0.17188F, 0.70312F, 0.23438F, 0.79688F, 0.17188F, 0.875F, 0.23438F, 0.70312F, 0.23438F, 0.70312F, 0.15625F, 0.70312F, 0.15625F, 0.70312F, 0.23438F},
        
        {9.75F, -0.5F, -3F, 9.75F, 3.5F, 2F, 9.75F, 1.5F, 2F, 35F, 0F, -120F, 0.59375F, 0.25F, 0.59375F, 0.3125F, 0.76562F, 0.25F, 0.6875F, 0.3125F, 0.59375F, 0.25F, 0.59375F, 0.3125F, 0.6875F, 0.25F, 0.60938F, 0.3125F, 0.59375F, 0.3125F, 0.59375F, 0.23438F, 0.59375F, 0.23438F, 0.59375F, 0.3125F},
    };
    

    private GravityGunModel() {}

    



    public static void render(MatrixStack matrixStack, IVertexBuilder builder, int light, int overlay) {
        for (float[] cube : CUBES) {
            matrixStack.pushPose();
            matrixStack.translate(cube[6], cube[7], cube[8]);
            
            
            
            if (cube[11] != 0.0F) {
                matrixStack.mulPose(Vector3f.ZP.rotationDegrees(cube[11]));
            }
            if (cube[10] != 0.0F) {
                matrixStack.mulPose(Vector3f.YP.rotationDegrees(cube[10]));
            }
            if (cube[9] != 0.0F) {
                matrixStack.mulPose(Vector3f.XP.rotationDegrees(cube[9]));
            }
            matrixStack.translate(-cube[6], -cube[7], -cube[8]);

            MatrixStack.Entry entry = matrixStack.last();
            for (int face = 0; face < 6; face++) {
                int uv = 12 + face * 4;
                
                if (cube[uv] >= 0.0F && !isEdgeOn(cube, face)) {
                    renderFace(entry, builder, cube, face, uv, light, overlay);
                }
            }
            matrixStack.popPose();
        }
    }

    








    private static boolean isEdgeOn(float[] cube, int face) {
        int normal = FACE_AXIS[face];
        return isFlat(cube, (normal + 1) % 3) || isFlat(cube, (normal + 2) % 3);
    }

    private static boolean isFlat(float[] cube, int axis) {
        return cube[axis] == cube[axis + 3];
    }

    private static void renderFace(MatrixStack.Entry entry, IVertexBuilder builder, float[] cube, int face, int uv,
                                   int light, int overlay) {
        Matrix4f pose = entry.pose();
        Matrix3f normalMatrix = entry.normal();
        int normal = face * 3;
        int corner = face * 12;

        for (int i = 0; i < 4; i++) {
            int c = corner + i * 3;
            
            float x = cube[CORNERS[c] * 3];
            float y = cube[CORNERS[c + 1] * 3 + 1];
            float z = cube[CORNERS[c + 2] * 3 + 2];
            
            float u = (i == 0 || i == 1) ? cube[uv] : cube[uv + 2];
            float v = (i == 0 || i == 3) ? cube[uv + 1] : cube[uv + 3];

            builder.vertex(pose, x, y, z)
                    .color(255, 255, 255, 255)
                    .uv(u, v)
                    .overlayCoords(overlay)
                    .uv2(light)
                    .normal(normalMatrix, NORMALS[normal], NORMALS[normal + 1], NORMALS[normal + 2])
                    .endVertex();
        }
    }
}
