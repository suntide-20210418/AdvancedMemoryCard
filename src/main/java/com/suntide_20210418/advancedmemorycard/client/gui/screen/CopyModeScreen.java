package com.suntide_20210418.advancedmemorycard.client.gui.screen;

import com.suntide_20210418.advancedmemorycard.client.gui.menu.CopyModeMenu;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import com.suntide_20210418.advancedmemorycard.utils.BlockPos;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SideOnly(Side.CLIENT)
public class CopyModeScreen extends GuiContainer {

    private final CopyModeMenu menu;
    private final EntityPlayer player;

    private GuiTextField sX, sY, sZ, eX, eY, eZ;
    private final GuiTextField[] fields = new GuiTextField[6];
    private final String[] lastFieldText = new String[6];

    private GuiButton clearButton;

    private BlockPos startPos;
    private BlockPos endPos;

    private static final Pattern BLOCKPOS_PATTERN =
            Pattern.compile("BlockPos\\{x=\\s*(-?\\d+),\\s*y=\\s*(-?\\d+),\\s*z=\\s*(-?\\d+)}");

    public CopyModeScreen(CopyModeMenu menu, EntityPlayer player) {
        super(menu);
        this.menu = menu;
        this.player = player;
        this.xSize = 220;
        this.ySize = 160;
    }

    @Override
    public void initGui() {
        this.xSize = 220;
        this.ySize = 160;
        super.initGui();

        int baseX = guiLeft + 60;
        int y = guiTop + 30;
        int w = 40, h = 14;

        sX = new GuiTextField(fontRendererObj, baseX, y, w, h);
        sY = new GuiTextField(fontRendererObj, baseX + 45, y, w, h);
        sZ = new GuiTextField(fontRendererObj, baseX + 90, y, w, h);
        y += 25;
        eX = new GuiTextField(fontRendererObj, baseX, y, w, h);
        eY = new GuiTextField(fontRendererObj, baseX + 45, y, w, h);
        eZ = new GuiTextField(fontRendererObj, baseX + 90, y, w, h);

        fields[0] = sX; fields[1] = sY; fields[2] = sZ;
        fields[3] = eX; fields[4] = eY; fields[5] = eZ;
        for (GuiTextField f : fields) {
            f.setMaxStringLength(12);
            f.setText("");
        }

        clearButton = new GuiButton(0, guiLeft + 10, guiTop + ySize - 30, 80, 20,
                TranslateHelper.translate(TranslateHelper.Keys.COPY_MODE_SCREEN_CLEAR_TOOLTIP));
        this.buttonList.add(clearButton);

        updateFieldsFromHeldItem();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == clearButton) {
            menu.dispatchClientAction("clear_pos");
            menu.dispatchClientAction("update_item_inf");
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        getPos();
        updateFieldsFromHeldItem();
    }

    private void getPos() {
        startPos = parseBlockPos(menu.getStartPos());
        endPos = parseBlockPos(menu.getEndPos());
    }

    private void updateFieldsFromHeldItem() {
        for (int i = 0; i < 6; i++) {
            GuiTextField f = fields[i];
            if (!f.isFocused()) {
                String newVal = coordValue(i);
                if (!f.getText().equals(newVal)) f.setText(newVal);
                lastFieldText[i] = f.getText();
            } else {
                if (!f.getText().equals(lastFieldText[i])) {
                    handleFieldChange(i);
                    lastFieldText[i] = f.getText();
                }
            }
        }
    }

    private String coordValue(int i) {
        BlockPos p = (i < 3) ? startPos : endPos;
        if (p == null) return "";
        switch (i % 3) {
            case 0: return String.valueOf(p.getX());
            case 1: return String.valueOf(p.getY());
            default: return String.valueOf(p.getZ());
        }
    }

    private void handleFieldChange(int changedIndex) {
        if (changedIndex < 3) {
            if (!isEmpty(sX) && !isEmpty(sY) && !isEmpty(sZ)) {
                try {
                    BlockPos sp = new BlockPos(Integer.parseInt(sX.getText().trim()),
                            Integer.parseInt(sY.getText().trim()), Integer.parseInt(sZ.getText().trim()));
                    menu.dispatchClientAction("revise_start_pos", sp);
                } catch (NumberFormatException ignored) { }
            }
        } else {
            if (!isEmpty(eX) && !isEmpty(eY) && !isEmpty(eZ)) {
                try {
                    BlockPos ep = new BlockPos(Integer.parseInt(eX.getText().trim()),
                            Integer.parseInt(eY.getText().trim()), Integer.parseInt(eZ.getText().trim()));
                    menu.dispatchClientAction("revise_end_pos", ep);
                } catch (NumberFormatException ignored) { }
            }
        }
    }

    private boolean isEmpty(GuiTextField f) {
        return f.getText() == null || f.getText().trim().isEmpty();
    }

    public BlockPos parseBlockPos(String input) {
        if (input == null || input.isEmpty()) return null;
        Matcher matcher = BLOCKPOS_PATTERN.matcher(input);
        if (matcher.matches()) {
            return new BlockPos(Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
        }
        return null;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 先让 GuiContainer 绘制默认背景与按钮（clearButton 由 GuiScreen 负责绘制）
        super.drawScreen(mouseX, mouseY, partialTicks);

        // 再在之上绘制面板，覆盖默认背景遮罩
        Gui.drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF2B2B2B);
        Gui.drawRect(guiLeft + 2, guiTop + 2, guiLeft + xSize - 2, guiTop + ySize - 2, 0xFF3B3B3B);

        fontRendererObj.drawString(TranslateHelper.translate(TranslateHelper.Keys.COPY_MODE_SCREEN_TITLE),
                guiLeft + 4, guiTop + 5, 0xFFFFFFFF);
        fontRendererObj.drawString(TranslateHelper.translate(TranslateHelper.Keys.COPY_MODE_SCREEN_START_POS),
                guiLeft + 4, guiTop + 32, 0xFFCCCCCC);
        fontRendererObj.drawString(TranslateHelper.translate(TranslateHelper.Keys.COPY_MODE_SCREEN_END_POS),
                guiLeft + 4, guiTop + 57, 0xFFCCCCCC);

        for (GuiTextField f : fields) f.drawTextBox();

        // 重新绘制清除按钮，确保其显示在面板之上
        if (clearButton != null) clearButton.drawButton(mc, mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for (GuiTextField f : fields) {
            f.mouseClicked(mouseX, mouseY, mouseButton);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        for (GuiTextField f : fields) {
            if (f.isFocused()) {
                f.textboxKeyTyped(typedChar, keyCode);
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
