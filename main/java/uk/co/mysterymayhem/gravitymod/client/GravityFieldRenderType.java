package uk.co.mysterymayhem.gravitymod.client;

import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import java.util.OptionalDouble;


























public abstract class GravityFieldRenderType extends RenderState {

    private GravityFieldRenderType() {
        super("", () -> {}, () -> {});
    }

    
    private static final int GL_LINES = 1;

    private static final RenderType FIELD_LINES = RenderType.create(
            "gravitymod:field_lines", DefaultVertexFormats.POSITION_COLOR, GL_LINES, 256,
            RenderType.State.builder()
                    
                    .setLineState(new RenderState.LineState(OptionalDouble.empty()))
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(MAIN_TARGET)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .createCompositeState(false));

    public static RenderType fieldLines() {
        return FIELD_LINES;
    }
}
