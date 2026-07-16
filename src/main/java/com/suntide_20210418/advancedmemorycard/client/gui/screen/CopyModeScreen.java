package com.suntide_20210418.advancedmemorycard.client.gui.screen;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import com.suntide_20210418.advancedmemorycard.client.gui.menu.CopyModeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 采用ae2界面，样式文件定义于ae2/guis/advanced_memory_card.json
 *
 */

public class CopyModeScreen extends AEBaseScreen<CopyModeMenu> {

    // 标识符，用于.setValue时不被监听器捕捉，重新定义数据
    private boolean suppressListener = true;
    private IconButton clearButton;

    private BlockPos startPos;
    private BlockPos endPos;

    private EditBox sX;
    private EditBox sY;
    private EditBox sZ;
    private EditBox eX;
    private EditBox eY;
    private EditBox eZ;


    public CopyModeScreen(CopyModeMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        menu.sendUpdateItemInf();

        sX = registerFields("sX");
        sY = registerFields("sY");
        sZ = registerFields("sZ");
        eX = registerFields("eX");
        eY = registerFields("eY");
        eZ = registerFields("eZ");

        clearButton = new IconButton(button -> {
            menu.sendClearPos();
            menu.sendUpdateItemInf();
        })

        {
            @Override
            protected Icon getIcon() {
                return Icon.CONDENSER_OUTPUT_TRASH;
            }
        };

        clearButton.setTooltip(Tooltip.create(Component.translatable(com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.Keys.COPY_MODE_SCREEN_CLEAR_TOOLTIP)));
        this.addToLeftToolbar(clearButton);
        updateFieldsFromHeldItem();
    }

    private EditBox registerFields(String fieldName){
        EditBox field = new EditBox(this.font, 0, 0, 0, 0, Component.translatable(fieldName));
        field.setBordered(true);
        field.setFilter(this::isDigitsWithOptionalSign);
        field.setResponder(this::onInputChange);
        this.widgets.add(fieldName, field);
        return field;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        getPos();
        updateFieldsFromHeldItem();

        setTextContent("start_pos", Component.translatable(com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.Keys.COPY_MODE_SCREEN_START_POS));
        setTextContent("end_pos", Component.translatable(com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.Keys.COPY_MODE_SCREEN_END_POS));

    }

    //从服务端拿到数据并处理，获取的数据必须有@GuiSync注解
    private void getPos(){
        startPos = parseBlockPos(menu.getStartPos());
        endPos = parseBlockPos(menu.getEndPos());
    }

    //更新输入框数据
    private void updateFieldsFromHeldItem(){
        suppressListener = false;
        if (startPos != null){
            if (isNullOrEmpty(sX.getValue())) sX.setValue(String.valueOf(startPos.getX()));
            if (isNullOrEmpty(sY.getValue())) sY.setValue(String.valueOf(startPos.getY()));
            if (isNullOrEmpty(sZ.getValue())) sZ.setValue(String.valueOf(startPos.getZ()));
        } else {
            sX.setValue("");
            sY.setValue("");
            sZ.setValue("");
        }
        if (endPos != null){
            if (isNullOrEmpty(eX.getValue())) eX.setValue(String.valueOf(endPos.getX()));
            if (isNullOrEmpty(eY.getValue())) eY.setValue(String.valueOf(endPos.getY()));
            if (isNullOrEmpty(eZ.getValue())) eZ.setValue(String.valueOf(endPos.getZ()));
        } else {
            eX.setValue("");
            eY.setValue("");
            eZ.setValue("");
        }
        suppressListener = true;
    }

    private boolean isNullOrEmpty(String s){
        return s == null || s.trim().isEmpty();
    }

    private boolean isDigitsWithOptionalSign(String input) {
        if (input == null || input.isEmpty()) {
            return true; // 允许空输入
        }
        return input.matches("-?\\d*"); // 匹配可选负号和数字
    }

    public BlockPos parseBlockPos(String input) {
        if (input.isEmpty()){
            return null;
        }
        // 使用正则表达式提取数字
        String regex = "BlockPos\\{x=\\s*(-?\\d+),\\s*y=\\s*(-?\\d+),\\s*z=\\s*(-?\\d+)}";
        Pattern pattern = java.util.regex.Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            // 提取 x, y, z 的值
            String x = matcher.group(1);
            String y = matcher.group(2);
            String z = matcher.group(3);

            return new BlockPos(Integer.parseInt(x), Integer.parseInt(y), Integer.parseInt(z));
            // 返回目标格式

        } else {
            throw new IllegalArgumentException("输入字符串格式不正确: " + input);
        }
    }
    private void onInputChange(String ignored) {
        if (suppressListener) {
            if (!isNullOrEmpty(sX.getValue()) && !isNullOrEmpty(sY.getValue()) && !isNullOrEmpty(sZ.getValue())) {
                try {
                    int sx = Integer.parseInt(sX.getValue().trim());
                    int sy = Integer.parseInt(sY.getValue().trim());
                    int sz = Integer.parseInt(sZ.getValue().trim());
                    this.getMenu().sendReviseStartPos(new BlockPos(sx, sy, sz));
                } catch (NumberFormatException ignoredEx) {
                }
            }

            if (!isNullOrEmpty(eX.getValue()) && !isNullOrEmpty(eY.getValue()) && !isNullOrEmpty(eZ.getValue())) {
                try {
                    int ex = Integer.parseInt(eX.getValue().trim());
                    int ey = Integer.parseInt(eY.getValue().trim());
                    int ez = Integer.parseInt(eZ.getValue().trim());
                    this.getMenu().sendReviseEndPos(new BlockPos(ex, ey, ez));
                } catch (NumberFormatException ignoredEx) {
                }
            }
        }
    }
}
