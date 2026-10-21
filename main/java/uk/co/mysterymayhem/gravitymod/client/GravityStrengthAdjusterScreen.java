package uk.co.mysterymayhem.gravitymod.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.lwjgl.glfw.GLFW;
import uk.co.mysterymayhem.gravitymod.capability.GravityAPI;
import uk.co.mysterymayhem.gravitymod.capability.IGravityCapability;
import uk.co.mysterymayhem.gravitymod.network.GravityStrengthPacket;
import uk.co.mysterymayhem.gravitymod.network.PacketHandler;

import javax.annotation.Nonnull;
import java.util.Locale;














public final class GravityStrengthAdjusterScreen extends Screen {

    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 136;

    private static final int TITLE_Y = 7;
    private static final int READOUT_Y = 32;
    private static final int SLIDER_X = 20;
    private static final int SLIDER_Y = 58;
    private static final int SLIDER_WIDTH = PANEL_WIDTH - 2 * SLIDER_X;
    private static final int SLIDER_HEIGHT = 16;
    private static final int TICK_Y = SLIDER_Y + SLIDER_HEIGHT + 2;
    private static final int TICK_HEIGHT = 3;
    private static final int TICK_LABEL_Y = TICK_Y + TICK_HEIGHT + 2;
    private static final int HINT_Y = 97;
    private static final int BUTTON_Y = 110;
    private static final int BUTTON_WIDTH = 70;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_MARGIN = 16;

    
    private static final int HANDLE_WIDTH = 8;

    
    private static final double[] TICKS = {0.0D, 0.5D, 1.0D, 5.0D, 10.0D};

    
    private static final double COARSE_STEP = 0.1D;
    private static final double FINE_STEP = 0.01D;

    private static final int COLOUR_SHADOW = 0x60000000;
    private static final int COLOUR_BORDER = 0xFF5A5A5A;
    private static final int COLOUR_BACKGROUND = 0xF2141414;
    private static final int COLOUR_TITLE_BAR = 0xFF262626;
    private static final int COLOUR_ACCENT = 0xFFFFE77B;
    private static final int COLOUR_DIVIDER = 0x40FFE77B;
    private static final int COLOUR_LABEL = 0xFFC6C6C6;
    private static final int COLOUR_HINT = 0xFF7F7F7F;
    private static final int COLOUR_TRACK = 0xFF3C3C3C;
    private static final int COLOUR_TRACK_FILLED = 0xFF8C7F44;

    private int leftPos;
    private int topPos;

    private double strength;
    
    private double sentStrength;

    private StrengthSlider slider;

    private GravityStrengthAdjusterScreen(double strength) {
        super(new TranslationTextComponent("gui.gravitymod.gravity_strength_adjuster.title"));
        this.strength = clamp(strength);
        this.sentStrength = this.strength;
    }

    
    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.setScreen(new GravityStrengthAdjusterScreen(GravityAPI.getStrength(minecraft.player)));
        }
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - PANEL_WIDTH) / 2;
        this.topPos = (this.height - PANEL_HEIGHT) / 2;

        this.slider = this.addButton(new StrengthSlider(this.leftPos + SLIDER_X, this.topPos + SLIDER_Y));
        this.addButton(new FlatButton(this.leftPos + BUTTON_MARGIN, this.topPos + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT,
                new TranslationTextComponent("gui.gravitymod.gravity_strength_adjuster.reset"), button -> {
            this.setStrength(IGravityCapability.DEFAULT_STRENGTH);
            this.sendIfChanged();
        }));
        this.addButton(new FlatButton(this.leftPos + PANEL_WIDTH - BUTTON_MARGIN - BUTTON_WIDTH, this.topPos + BUTTON_Y,
                BUTTON_WIDTH, BUTTON_HEIGHT, new TranslationTextComponent("gui.gravitymod.gravity_strength_adjuster.done"),
                button -> this.onClose()));
    }

    



    @Override
    public void tick() {
        this.sendIfChanged();
    }

    @Override
    public boolean isPauseScreen() {
        
        return false;
    }

    @Override
    public void onClose() {
        this.sendIfChanged();
        super.onClose();
    }

    private void sendIfChanged() {
        if (Double.compare(this.strength, this.sentStrength) != 0) {
            this.sentStrength = this.strength;
            PacketHandler.INSTANCE.sendToServer(new GravityStrengthPacket(this.strength));
        }
    }

    private void setStrength(double value) {
        this.strength = clamp(value);
        if (this.slider != null) {
            
            this.slider.setMessage(this.valueText());
        }
    }

    @Override
    public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        this.renderPanel(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private void renderPanel(MatrixStack matrixStack) {
        int left = this.leftPos;
        int top = this.topPos;
        int right = left + PANEL_WIDTH;
        int bottom = top + PANEL_HEIGHT;

        fill(matrixStack, left + 3, top + 3, right + 3, bottom + 3, COLOUR_SHADOW);
        fill(matrixStack, left, top, right, bottom, COLOUR_BORDER);
        fill(matrixStack, left + 1, top + 1, right - 1, bottom - 1, COLOUR_BACKGROUND);
        fill(matrixStack, left + 1, top + 1, right - 1, top + 20, COLOUR_TITLE_BAR);
        fill(matrixStack, left + 1, top + 20, right - 1, top + 21, COLOUR_DIVIDER);
        drawCenteredString(matrixStack, this.font, this.title, left + PANEL_WIDTH / 2, top + TITLE_Y, 0xFFFFFFFF);

        this.drawReadout(matrixStack);
        this.drawTicks(matrixStack);

        drawCenteredString(matrixStack, this.font,
                new TranslationTextComponent("gui.gravitymod.gravity_strength_adjuster.hint"),
                left + PANEL_WIDTH / 2, top + HINT_Y, COLOUR_HINT);
    }

    
    private void drawReadout(MatrixStack matrixStack) {
        ITextComponent text = this.valueText();
        float scale = 2.0F;
        matrixStack.pushPose();
        matrixStack.scale(scale, scale, 1.0F);
        this.font.draw(matrixStack, text,
                (this.leftPos + PANEL_WIDTH / 2.0F - this.font.width(text) * scale / 2.0F) / scale,
                (this.topPos + READOUT_Y) / scale, COLOUR_ACCENT);
        matrixStack.popPose();
    }

    



    private void drawTicks(MatrixStack matrixStack) {
        for (double tick : TICKS) {
            int x = this.leftPos + SLIDER_X + HANDLE_WIDTH / 2
                    + (int) Math.round(strengthToFraction(tick) * (SLIDER_WIDTH - HANDLE_WIDTH));
            fill(matrixStack, x, this.topPos + TICK_Y, x + 1, this.topPos + TICK_Y + TICK_HEIGHT, COLOUR_LABEL);
            ITextComponent label = new StringTextComponent(formatNumber(tick));
            this.font.draw(matrixStack, label, x - this.font.width(label) / 2.0F, this.topPos + TICK_LABEL_Y, COLOUR_HINT);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.onClose();
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT) {
            double step = hasShiftDown() ? FINE_STEP : COARSE_STEP;
            this.nudge(keyCode == GLFW.GLFW_KEY_RIGHT ? step : -step);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    



    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0.0D) {
            double step = hasShiftDown() ? FINE_STEP : COARSE_STEP;
            this.nudge(delta > 0.0D ? step : -step);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    
    private void nudge(double amount) {
        this.setStrength(roundToStep(this.strength + amount));
    }

    private ITextComponent valueText() {
        return new TranslationTextComponent("gui.gravitymod.gravity_strength_adjuster.value",
                String.format(Locale.ROOT, "%.2f", this.strength));
    }

    



    private static double fractionToStrength(double fraction) {
        double clamped = Math.max(0.0D, Math.min(1.0D, fraction));
        if (clamped <= 0.5D) {
            return IGravityCapability.MIN_STRENGTH
                    + (clamped / 0.5D) * (IGravityCapability.DEFAULT_STRENGTH - IGravityCapability.MIN_STRENGTH);
        }
        return IGravityCapability.DEFAULT_STRENGTH
                + ((clamped - 0.5D) / 0.5D) * (IGravityCapability.MAX_STRENGTH - IGravityCapability.DEFAULT_STRENGTH);
    }

    
    private static double strengthToFraction(double strength) {
        double clamped = clamp(strength);
        if (clamped <= IGravityCapability.DEFAULT_STRENGTH) {
            return 0.5D * (clamped - IGravityCapability.MIN_STRENGTH)
                    / (IGravityCapability.DEFAULT_STRENGTH - IGravityCapability.MIN_STRENGTH);
        }
        return 0.5D + 0.5D * (clamped - IGravityCapability.DEFAULT_STRENGTH)
                / (IGravityCapability.MAX_STRENGTH - IGravityCapability.DEFAULT_STRENGTH);
    }

    private static double clamp(double strength) {
        if (!Double.isFinite(strength)) {
            return IGravityCapability.DEFAULT_STRENGTH;
        }
        return Math.max(IGravityCapability.MIN_STRENGTH, Math.min(IGravityCapability.MAX_STRENGTH, strength));
    }

    



    private static double roundToStep(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    
    private static String formatNumber(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        text = text.replaceAll("0+$", "");
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }

    



    private final class StrengthSlider extends Widget {

        private StrengthSlider(int x, int y) {
            super(x, y, SLIDER_WIDTH, SLIDER_HEIGHT, StringTextComponent.EMPTY);
            this.setMessage(GravityStrengthAdjusterScreen.this.valueText());
        }

        @Override
        public void renderButton(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            int trackTop = this.y + this.height / 2 - 2;
            int trackBottom = trackTop + 4;
            int handleLeft = this.handleLeft();
            fill(matrixStack, this.x, trackTop, this.x + this.width, trackBottom, COLOUR_TRACK);
            fill(matrixStack, this.x, trackTop, handleLeft + HANDLE_WIDTH / 2, trackBottom, COLOUR_TRACK_FILLED);
            boolean highlighted = this.isHovered() || this.isFocused();
            fill(matrixStack, handleLeft - 1, this.y - 1, handleLeft + HANDLE_WIDTH + 1, this.y + this.height + 1,
                    highlighted ? 0xFFFFFFFF : 0xFF9E9E9E);
            fill(matrixStack, handleLeft, this.y, handleLeft + HANDLE_WIDTH, this.y + this.height,
                    highlighted ? 0xFF2A2A2A : 0xFF000000);
        }

        private int handleLeft() {
            double fraction = strengthToFraction(GravityStrengthAdjusterScreen.this.strength);
            return this.x + (int) Math.round(fraction * (this.width - HANDLE_WIDTH));
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            this.setFromMouse(mouseX);
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            this.setFromMouse(mouseX);
        }

        
        private void setFromMouse(double mouseX) {
            double travel = this.width - HANDLE_WIDTH;
            double fraction = (mouseX - this.x - HANDLE_WIDTH / 2.0D) / travel;
            GravityStrengthAdjusterScreen.this.setStrength(roundToStep(fractionToStrength(fraction)));
        }

        @Override
        public void playDownSound(@Nonnull SoundHandler soundHandler) {
            
            
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            Minecraft.getInstance().getSoundManager().play(SimpleSound.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            
            GravityStrengthAdjusterScreen.this.sendIfChanged();
        }
    }
}
