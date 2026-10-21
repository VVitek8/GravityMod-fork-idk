package uk.co.mysterymayhem.gravitymod.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.lwjgl.glfw.GLFW;
import uk.co.mysterymayhem.gravitymod.api.EnumGravityDirection;
import uk.co.mysterymayhem.gravitymod.block.GravityFieldCreatorTileEntity;
import uk.co.mysterymayhem.gravitymod.capability.IGravityCapability;
import uk.co.mysterymayhem.gravitymod.container.GravityFieldCreatorContainer;
import uk.co.mysterymayhem.gravitymod.network.GravityFieldConfigPacket;
import uk.co.mysterymayhem.gravitymod.network.PacketHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;









public final class GravityFieldCreatorScreen extends ContainerScreen<GravityFieldCreatorContainer> {

    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 231;

    private static final int FIELD_WIDTH = 52;
    private static final int FIELD_HEIGHT = 18;

    
    private static final int LEFT_LABEL_RIGHT = 60;
    private static final int LEFT_FIELD_X = 64;
    private static final int RIGHT_LABEL_RIGHT = 172;
    private static final int RIGHT_FIELD_X = 176;
    private static final int LEFT_COLUMN_X = 12;
    private static final int LEFT_COLUMN_RIGHT = LEFT_FIELD_X + FIELD_WIDTH;
    private static final int RIGHT_COLUMN_X = 124;
    private static final int RIGHT_COLUMN_RIGHT = RIGHT_FIELD_X + FIELD_WIDTH;

    private static final int TITLE_Y = 7;
    private static final int SIZE_HEADER_Y = 27;
    private static final int FIRST_ROW_Y = 41;
    private static final int ROW_SPACING = 22;
    private static final int GRAVITY_HEADER_Y = 111;
    private static final int GRAVITY_ROW_Y = 125;
    private static final int VISIBLE_ROW_Y = GRAVITY_ROW_Y + ROW_SPACING;
    private static final int TARGET_ROW_Y = VISIBLE_ROW_Y + ROW_SPACING;
    private static final int HINT_Y = 192;
    private static final int BUTTON_Y = 205;
    private static final int BUTTON_WIDTH = 88;

    
    private static final double COARSE_STEP = 1.0D;
    private static final double FINE_STEP = 0.1D;

    private static final int COLOUR_SHADOW = 0x60000000;
    private static final int COLOUR_BORDER = 0xFF5A5A5A;
    private static final int COLOUR_BACKGROUND = 0xF2141414;
    private static final int COLOUR_TITLE_BAR = 0xFF262626;
    private static final int COLOUR_ACCENT = 0xFFFFE77B;
    private static final int COLOUR_DIVIDER = 0x40FFE77B;
    private static final int COLOUR_LABEL = 0xFFC6C6C6;
    private static final int COLOUR_HINT = 0xFF7F7F7F;
    private static final int COLOUR_TEXT_OK = 0xE0E0E0;
    private static final int COLOUR_TEXT_BAD = 0xFF5555;

    private final List<ValueField> valueFields = new ArrayList<>();

    private ValueField widthField;
    private ValueField depthField;
    private ValueField heightField;
    private ValueField offsetXField;
    private ValueField offsetYField;
    private ValueField offsetZField;
    private ValueField strengthField;

    private Button directionButton;
    private EnumGravityDirection direction;
    
    private Boolean fieldVisible;
    private Boolean keepGravity;
    private Boolean affectPlayers;
    private Boolean affectCubes;
    private boolean directionMenuOpen;
    private int directionButtonX;
    private int directionButtonY;

    public GravityFieldCreatorScreen(GravityFieldCreatorContainer container, PlayerInventory inventory,
                                     ITextComponent title) {
        super(container, inventory, title);
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        
        GravityFieldCreatorTileEntity tile = this.menu.getTileEntity();
        String width = this.keepOr(this.widthField, () -> formatNumber(this.tileValue(GravityFieldCreatorTileEntity::getWidth, GravityFieldCreatorTileEntity.DEFAULT_SIZE)));
        String depth = this.keepOr(this.depthField, () -> formatNumber(this.tileValue(GravityFieldCreatorTileEntity::getDepth, GravityFieldCreatorTileEntity.DEFAULT_SIZE)));
        String height = this.keepOr(this.heightField, () -> formatNumber(this.tileValue(GravityFieldCreatorTileEntity::getHeight, GravityFieldCreatorTileEntity.DEFAULT_SIZE)));
        String offsetX = this.keepOr(this.offsetXField, () -> formatNumber(this.tileValue(GravityFieldCreatorTileEntity::getOffsetX, 0.0D)));
        String offsetY = this.keepOr(this.offsetYField, () -> formatNumber(this.tileValue(GravityFieldCreatorTileEntity::getOffsetY, 0.0D)));
        String offsetZ = this.keepOr(this.offsetZField, () -> formatNumber(this.tileValue(GravityFieldCreatorTileEntity::getOffsetZ, 0.0D)));
        String strength = this.keepOr(this.strengthField,
                () -> formatNumber(tile == null ? IGravityCapability.DEFAULT_STRENGTH : tile.getStrength()));
        if (this.direction == null) {
            this.direction = tile == null ? EnumGravityDirection.DOWN : tile.getDirection();
        }
        if (this.fieldVisible == null) {
            this.fieldVisible = tile == null || tile.isFieldVisible();
        }
        if (this.keepGravity == null) {
            this.keepGravity = tile != null && tile.isKeepGravity();
        }
        if (this.affectPlayers == null) {
            this.affectPlayers = tile == null || tile.isAffectPlayers();
        }
        if (this.affectCubes == null) {
            this.affectCubes = tile == null || tile.isAffectCubes();
        }

        super.init();
        this.valueFields.clear();

        double sizeMin = GravityFieldCreatorTileEntity.MIN_SIZE;
        double sizeMax = GravityFieldCreatorTileEntity.MAX_SIZE;
        double offsetMax = GravityFieldCreatorTileEntity.MAX_OFFSET;
        ITextComponent sizeTooltip = new TranslationTextComponent("gui.gravitymod.gravity_field_creator.size_tooltip",
                formatNumber(sizeMin), formatNumber(sizeMax));
        ITextComponent offsetTooltip = new TranslationTextComponent("gui.gravitymod.gravity_field_creator.offset_tooltip",
                formatNumber(-offsetMax), formatNumber(offsetMax));

        this.widthField = this.addValueField(LEFT_FIELD_X, this.rowY(0), width, sizeMin, sizeMax, sizeTooltip);
        this.heightField = this.addValueField(LEFT_FIELD_X, this.rowY(1), height, sizeMin, sizeMax, sizeTooltip);
        this.depthField = this.addValueField(LEFT_FIELD_X, this.rowY(2), depth, sizeMin, sizeMax, sizeTooltip);
        this.offsetXField = this.addValueField(RIGHT_FIELD_X, this.rowY(0), offsetX, -offsetMax, offsetMax, offsetTooltip);
        this.offsetYField = this.addValueField(RIGHT_FIELD_X, this.rowY(1), offsetY, -offsetMax, offsetMax, offsetTooltip);
        this.offsetZField = this.addValueField(RIGHT_FIELD_X, this.rowY(2), offsetZ, -offsetMax, offsetMax, offsetTooltip);
        this.strengthField = this.addValueField(RIGHT_FIELD_X, GRAVITY_ROW_Y, strength,
                IGravityCapability.MIN_STRENGTH, IGravityCapability.MAX_STRENGTH,
                new TranslationTextComponent("gui.gravitymod.gravity_field_creator.strength_tooltip",
                        formatNumber(IGravityCapability.MIN_STRENGTH), formatNumber(IGravityCapability.MAX_STRENGTH)));

        this.directionButtonX = this.leftPos + LEFT_FIELD_X;
        this.directionButtonY = this.topPos + GRAVITY_ROW_Y;
        this.directionButton = this.addButton(new FlatButton(this.directionButtonX, this.directionButtonY, FIELD_WIDTH,
                FIELD_HEIGHT, directionName(this.direction), button -> this.directionMenuOpen = !this.directionMenuOpen));

        this.addToggle(LEFT_FIELD_X, VISIBLE_ROW_Y, () -> this.fieldVisible, value -> this.fieldVisible = value);
        this.addToggle(RIGHT_FIELD_X, VISIBLE_ROW_Y, () -> this.keepGravity, value -> this.keepGravity = value);
        this.addToggle(LEFT_FIELD_X, TARGET_ROW_Y, () -> this.affectPlayers, value -> this.affectPlayers = value);
        this.addToggle(RIGHT_FIELD_X, TARGET_ROW_Y, () -> this.affectCubes, value -> this.affectCubes = value);

        this.addButton(new FlatButton(this.leftPos + 24, this.topPos + BUTTON_Y, BUTTON_WIDTH, FIELD_HEIGHT,
                new TranslationTextComponent("gui.gravitymod.gravity_field_creator.done"), button -> this.saveAndClose()));
        this.addButton(new FlatButton(this.leftPos + PANEL_WIDTH - 24 - BUTTON_WIDTH, this.topPos + BUTTON_Y, BUTTON_WIDTH,
                FIELD_HEIGHT, new TranslationTextComponent("gui.gravitymod.gravity_field_creator.cancel"),
                button -> this.onClose()));

        this.setFocused(this.widthField.widget);
        this.widthField.widget.setFocus(true);
    }

    private int rowY(int row) {
        return FIRST_ROW_Y + row * ROW_SPACING;
    }

    




    private void addToggle(int x, int y, BooleanSupplier get, Consumer<Boolean> set) {
        this.addButton(new FlatButton(this.leftPos + x, this.topPos + y, FIELD_WIDTH, FIELD_HEIGHT,
                toggleLabel(get.getAsBoolean()), button -> {
            set.accept(!get.getAsBoolean());
            button.setMessage(toggleLabel(get.getAsBoolean()));
        }));
    }

    private double tileValue(ToDoubleFunction<GravityFieldCreatorTileEntity> getter, double fallback) {
        GravityFieldCreatorTileEntity tile = this.menu.getTileEntity();
        return tile == null ? fallback : getter.applyAsDouble(tile);
    }

    private String keepOr(@Nullable ValueField existing, Supplier<String> fromBlock) {
        return existing == null ? fromBlock.get() : existing.widget.getValue();
    }

    



    private ValueField addValueField(int x, int y, String initialValue, double min, double max, ITextComponent tooltip) {
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + y;
        TextFieldWidget widget = new TextFieldWidget(this.font, absoluteX, absoluteY, FIELD_WIDTH, FIELD_HEIGHT,
                StringTextComponent.EMPTY);
        
        
        widget.setMaxLength(min < 0.0D ? 8 : 7);
        widget.setFilter(value -> value.matches(min < 0.0D ? "-?\\d*(?:\\.\\d*)?" : "\\d*(?:\\.\\d*)?"));
        widget.setValue(initialValue);
        ValueField field = new ValueField(widget, absoluteX, absoluteY, min, max, tooltip);
        widget.setResponder(value -> field.refreshTextColour());
        field.refreshTextColour();
        this.addButton(widget);
        this.valueFields.add(field);
        return field;
    }

    @Override
    public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        drawDownArrow(matrixStack, this.directionButtonX + FIELD_WIDTH - 10, this.directionButtonY + 8, 0xFFFFE77B);
        if (this.directionMenuOpen) {
            this.renderDirectionMenu(matrixStack, mouseX, mouseY);
            return;
        }
        for (ValueField field : this.valueFields) {
            if (field.isMouseOver(mouseX, mouseY)) {
                this.renderTooltip(matrixStack, field.tooltip, mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    protected void renderBg(@Nonnull MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
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

        this.drawSectionHeader(matrixStack, "gui.gravitymod.gravity_field_creator.size", LEFT_COLUMN_X,
                SIZE_HEADER_Y, LEFT_COLUMN_RIGHT);
        this.drawSectionHeader(matrixStack, "gui.gravitymod.gravity_field_creator.offset", RIGHT_COLUMN_X,
                SIZE_HEADER_Y, RIGHT_COLUMN_RIGHT);
        this.drawFieldLabel(matrixStack, "gui.gravitymod.gravity_field_creator.width", LEFT_LABEL_RIGHT, this.rowY(0));
        this.drawFieldLabel(matrixStack, "gui.gravitymod.gravity_field_creator.height", LEFT_LABEL_RIGHT, this.rowY(1));
        this.drawFieldLabel(matrixStack, "gui.gravitymod.gravity_field_creator.depth", LEFT_LABEL_RIGHT, this.rowY(2));
        this.drawFieldLabel(matrixStack, "gui.gravitymod.gravity_field_creator.offset_x", RIGHT_LABEL_RIGHT, this.rowY(0));
        this.drawFieldLabel(matrixStack, "gui.gravitymod.gravity_field_creator.offset_y", RIGHT_LABEL_RIGHT, this.rowY(1));
        this.drawFieldLabel(matrixStack, "gui.gravitymod.gravity_field_creator.offset_z", RIGHT_LABEL_RIGHT, this.rowY(2));

        this.drawSectionHeader(matrixStack, "gui.gravitymod.gravity_field_creator.gravity", LEFT_COLUMN_X,
                GRAVITY_HEADER_Y, RIGHT_COLUMN_RIGHT);
        this.drawFieldLabel(matrixStack, "gui.gravitymod.gravity_field_creator.direction", LEFT_LABEL_RIGHT, GRAVITY_ROW_Y);
        this.drawFieldLabel(matrixStack, "gui.gravitymod.gravity_field_creator.strength", RIGHT_LABEL_RIGHT, GRAVITY_ROW_Y);
        this.drawFieldLabel(matrixStack, "gui.gravitymod.gravity_field_creator.visible", LEFT_LABEL_RIGHT, VISIBLE_ROW_Y);
        this.drawFieldLabel(matrixStack, "gui.gravitymod.gravity_field_creator.keep_gravity", RIGHT_LABEL_RIGHT, VISIBLE_ROW_Y);
        this.drawFieldLabel(matrixStack, "gui.gravitymod.gravity_field_creator.affect_players", LEFT_LABEL_RIGHT, TARGET_ROW_Y);
        this.drawFieldLabel(matrixStack, "gui.gravitymod.gravity_field_creator.affect_cubes", RIGHT_LABEL_RIGHT, TARGET_ROW_Y);

        drawCenteredString(matrixStack, this.font,
                new TranslationTextComponent("gui.gravitymod.gravity_field_creator.strength_hint"),
                left + PANEL_WIDTH / 2, top + HINT_Y, COLOUR_HINT);
    }

    
    private void drawSectionHeader(MatrixStack matrixStack, String translationKey, int x, int y, int columnRight) {
        ITextComponent text = new TranslationTextComponent(translationKey);
        this.font.draw(matrixStack, text, this.leftPos + x, this.topPos + y, COLOUR_ACCENT);
        int ruleLeft = this.leftPos + x + this.font.width(text) + 5;
        int ruleRight = this.leftPos + columnRight;
        if (ruleRight > ruleLeft) {
            fill(matrixStack, ruleLeft, this.topPos + y + 3, ruleRight, this.topPos + y + 4, COLOUR_DIVIDER);
        }
    }

    
    private void drawFieldLabel(MatrixStack matrixStack, String translationKey, int labelRight, int rowY) {
        ITextComponent text = new TranslationTextComponent(translationKey);
        this.font.draw(matrixStack, text, this.leftPos + labelRight - this.font.width(text),
                this.topPos + rowY + (FIELD_HEIGHT - 8) / 2.0F, COLOUR_LABEL);
    }

    @Override
    protected void renderLabels(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY) {
        
        
    }

    private void renderDirectionMenu(MatrixStack matrixStack, int mouseX, int mouseY) {
        EnumGravityDirection[] directions = EnumGravityDirection.values();
        int popupTop = this.directionMenuTop();
        int popupBottom = popupTop + directions.length * FIELD_HEIGHT;
        fill(matrixStack, this.directionButtonX - 1, popupTop - 1, this.directionButtonX + FIELD_WIDTH + 1,
                popupBottom + 1, 0xFFFFFFFF);
        fill(matrixStack, this.directionButtonX, popupTop, this.directionButtonX + FIELD_WIDTH, popupBottom, 0xFF000000);
        for (int index = 0; index < directions.length; index++) {
            int y = popupTop + index * FIELD_HEIGHT;
            boolean hovered = this.isInsideDirectionMenu(mouseX, mouseY) && mouseY >= y && mouseY < y + FIELD_HEIGHT;
            if (hovered) {
                fill(matrixStack, this.directionButtonX, y, this.directionButtonX + FIELD_WIDTH, y + FIELD_HEIGHT,
                        0xFF3C3C3C);
            }
            boolean selected = directions[index] == this.direction;
            drawCenteredString(matrixStack, this.font, directionName(directions[index]),
                    this.directionButtonX + FIELD_WIDTH / 2, y + (FIELD_HEIGHT - 8) / 2,
                    selected ? COLOUR_ACCENT : 0xFFFFFFFF);
        }
    }

    
    private int directionMenuTop() {
        int popupHeight = EnumGravityDirection.values().length * FIELD_HEIGHT;
        int below = this.directionButtonY + FIELD_HEIGHT;
        return below + popupHeight <= this.height ? below : this.directionButtonY - popupHeight;
    }

    private boolean isInsideDirectionMenu(double mouseX, double mouseY) {
        int popupTop = this.directionMenuTop();
        return mouseX >= this.directionButtonX && mouseX < this.directionButtonX + FIELD_WIDTH
                && mouseY >= popupTop && mouseY < popupTop + EnumGravityDirection.values().length * FIELD_HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.directionMenuOpen) {
            if (this.isInsideDirectionMenu(mouseX, mouseY)) {
                int selected = (int) ((mouseY - this.directionMenuTop()) / FIELD_HEIGHT);
                this.direction = EnumGravityDirection.getSafeDirectionFromOrdinal(selected);
                this.directionButton.setMessage(directionName(this.direction));
            }
            
            this.directionMenuOpen = false;
            return true;
        }
        
        
        for (ValueField field : this.valueFields) {
            if (!field.isMouseOver(mouseX, mouseY)) {
                field.widget.setFocus(false);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    



    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!this.directionMenuOpen && delta != 0.0D) {
            double step = hasShiftDown() ? FINE_STEP : COARSE_STEP;
            for (ValueField field : this.valueFields) {
                if (field.isMouseOver(mouseX, mouseY)) {
                    field.nudge(delta > 0.0D ? step : -step);
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (this.directionMenuOpen) {
                this.directionMenuOpen = false;
                return true;
            }
            
            this.saveAndClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.saveAndClose();
            return true;
        }
        
        TextFieldWidget focused = this.focusedField();
        if (focused != null) {
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                this.cycleFieldFocus(focused, !hasShiftDown());
                return true;
            }
            return focused.keyPressed(keyCode, scanCode, modifiers) || focused.canConsumeInput();
        }
        
        if (this.minecraft != null
                && this.minecraft.options.keyInventory.isActiveAndMatches(InputMappings.getKey(keyCode, scanCode))) {
            this.saveAndClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void cycleFieldFocus(TextFieldWidget current, boolean forwards) {
        int index = 0;
        for (int candidate = 0; candidate < this.valueFields.size(); candidate++) {
            if (this.valueFields.get(candidate).widget == current) {
                index = candidate;
                break;
            }
        }
        int count = this.valueFields.size();
        TextFieldWidget next = this.valueFields.get((index + (forwards ? 1 : count - 1)) % count).widget;
        current.setFocus(false);
        next.setFocus(true);
        this.setFocused(next);
    }

    @Nullable
    private TextFieldWidget focusedField() {
        for (ValueField field : this.valueFields) {
            if (field.widget.isFocused()) {
                return field.widget;
            }
        }
        return null;
    }

    private void saveAndClose() {
        double defaultSize = GravityFieldCreatorTileEntity.DEFAULT_SIZE;
        PacketHandler.INSTANCE.sendToServer(new GravityFieldConfigPacket(this.menu.getBlockPos(),
                this.widthField.value(defaultSize), this.depthField.value(defaultSize), this.heightField.value(defaultSize),
                this.offsetXField.value(0.0D), this.offsetYField.value(0.0D), this.offsetZField.value(0.0D),
                this.direction, this.strengthField.value(IGravityCapability.DEFAULT_STRENGTH), this.fieldVisible,
                this.keepGravity, this.affectPlayers, this.affectCubes));
        this.onClose();
    }

    private static ITextComponent directionName(EnumGravityDirection direction) {
        return new TranslationTextComponent("gui.gravitymod.direction." + direction.getSerializedName());
    }

    private static ITextComponent toggleLabel(boolean on) {
        return new TranslationTextComponent(on
                ? "gui.gravitymod.gravity_field_creator.visible_yes"
                : "gui.gravitymod.gravity_field_creator.visible_no")
                .withStyle(on ? TextFormatting.GREEN : TextFormatting.RED);
    }

    



    private static String formatNumber(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        if (text.indexOf('.') >= 0) {
            text = text.replaceAll("0+$", "");
            if (text.endsWith(".")) {
                text = text.substring(0, text.length() - 1);
            }
        }
        return text;
    }

    private static void drawDownArrow(MatrixStack matrixStack, int x, int y, int colour) {
        for (int row = 0; row < 3; row++) {
            fill(matrixStack, x + row, y + row, x + 5 - row, y + row + 1, colour);
        }
    }

    
    private static final class ValueField {

        private final TextFieldWidget widget;
        private final int x;
        private final int y;
        private final double min;
        private final double max;
        private final ITextComponent tooltip;

        private ValueField(TextFieldWidget widget, int x, int y, double min, double max, ITextComponent tooltip) {
            this.widget = widget;
            this.x = x;
            this.y = y;
            this.min = min;
            this.max = max;
            this.tooltip = tooltip;
        }

        private boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + FIELD_WIDTH && mouseY >= this.y && mouseY < this.y + FIELD_HEIGHT;
        }

        
        private double value(double fallback) {
            try {
                double parsed = Double.parseDouble(this.widget.getValue());
                return Double.isFinite(parsed) ? this.clamp(parsed) : fallback;
            }
            catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        
        private void nudge(double amount) {
            this.widget.setValue(formatNumber(this.clamp(this.value(this.clamp(0.0D)) + amount)));
        }

        private double clamp(double value) {
            return Math.max(this.min, Math.min(this.max, value));
        }

        
        private void refreshTextColour() {
            boolean valid;
            try {
                double parsed = Double.parseDouble(this.widget.getValue());
                valid = Double.isFinite(parsed) && parsed >= this.min && parsed <= this.max;
            }
            catch (NumberFormatException ignored) {
                valid = false;
            }
            this.widget.setTextColor(valid ? COLOUR_TEXT_OK : COLOUR_TEXT_BAD);
        }
    }
}
