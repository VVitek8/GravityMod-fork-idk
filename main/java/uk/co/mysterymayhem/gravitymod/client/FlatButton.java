package uk.co.mysterymayhem.gravitymod.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;





public final class FlatButton extends Button {

    private static final int COLOUR_ACCENT = 0xFFFFE77B;

    public FlatButton(int x, int y, int width, int height, ITextComponent message, IPressable onPress) {
        super(x, y, width, height, message, onPress);
    }

    @Override
    public void renderButton(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        boolean hovered = mouseX >= this.x && mouseX < this.x + this.width
                && mouseY >= this.y && mouseY < this.y + this.height;
        fill(matrixStack, this.x - 1, this.y - 1, this.x + this.width + 1, this.y + this.height + 1,
                hovered ? 0xFFFFFFFF : 0xFF9E9E9E);
        fill(matrixStack, this.x, this.y, this.x + this.width, this.y + this.height,
                hovered ? 0xFF2A2A2A : 0xFF000000);
        drawCenteredString(matrixStack, Minecraft.getInstance().font, this.getMessage(),
                this.x + this.width / 2, this.y + (this.height - 8) / 2, hovered ? COLOUR_ACCENT : 0xFFFFFFFF);
    }
}
