package uk.co.mysterymayhem.gravitymod.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import uk.co.mysterymayhem.gravitymod.GravityMod;
import uk.co.mysterymayhem.gravitymod.block.GravityFieldCreatorTileEntity;
import uk.co.mysterymayhem.gravitymod.container.ModContainers;


@Mod.EventBusSubscriber(modid = GravityMod.MODID, value = Dist.CLIENT)
public final class GravityFieldCreatorClient {

    private GravityFieldCreatorClient() {}

    @Mod.EventBusSubscriber(modid = GravityMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        private ModEvents() {}

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> ScreenManager.register(ModContainers.GRAVITY_FIELD_CREATOR.get(),
                    GravityFieldCreatorScreen::new));
        }
    }

    @SubscribeEvent
    public static void renderFieldBorders(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Vector3d camera = minecraft.gameRenderer.getMainCamera().getPosition();
        MatrixStack matrixStack = event.getMatrixStack();
        matrixStack.pushPose();
        matrixStack.translate(-camera.x, -camera.y, -camera.z);
        restoreWorldDepthForFabulous(minecraft);
        IRenderTypeBuffer.Impl buffers = minecraft.renderBuffers().bufferSource();
        RenderType lines = GravityFieldRenderType.fieldLines();
        IVertexBuilder vertices = buffers.getBuffer(lines);
        for (TileEntity tileEntity : minecraft.level.blockEntityList) {
            if (tileEntity instanceof GravityFieldCreatorTileEntity
                    && ((GravityFieldCreatorTileEntity) tileEntity).isActive()
                    && ((GravityFieldCreatorTileEntity) tileEntity).isFieldVisible()) {
                WorldRenderer.renderLineBox(matrixStack, vertices,
                        ((GravityFieldCreatorTileEntity) tileEntity).getFieldBounds(), 1.0F, 0.92F, 0.0F, 1.0F);
            }
        }
        buffers.endBatch(lines);
        matrixStack.popPose();
    }

    



















    private static void restoreWorldDepthForFabulous(Minecraft minecraft) {
        if (!Minecraft.useShaderTransparency()) {
            return;
        }
        Framebuffer worldDepth = minecraft.levelRenderer.getItemEntityTarget();
        if (worldDepth == null) {
            return;
        }
        Framebuffer main = minecraft.getMainRenderTarget();
        main.copyDepthFrom(worldDepth);
        
        main.bindWrite(false);
    }
}
