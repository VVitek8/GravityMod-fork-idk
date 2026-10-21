package uk.co.mysterymayhem.gravitymod.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import uk.co.mysterymayhem.gravitymod.GravityMod;
import uk.co.mysterymayhem.gravitymod.client.model.GravityGunModel;






@OnlyIn(Dist.CLIENT)
public class GravityGunItemRenderer extends ItemStackTileEntityRenderer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(GravityMod.MODID, "textures/item/gravity_gun.png");

    @Override
    public void renderByItem(ItemStack stack, ItemCameraTransforms.TransformType transformType,
                             MatrixStack matrixStack, IRenderTypeBuffer buffer, int light, int overlay) {
        matrixStack.pushPose();
        
        
        matrixStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);

        
        
        
        
        IVertexBuilder builder = ItemRenderer.getFoilBuffer(
                buffer, RenderType.entityCutout(TEXTURE), true, stack.hasFoil());
        GravityGunModel.render(matrixStack, builder, light, overlay);

        matrixStack.popPose();
    }
}
