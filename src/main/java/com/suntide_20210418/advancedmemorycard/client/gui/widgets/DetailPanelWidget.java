package com.suntide_20210418.advancedmemorycard.client.gui.widgets;

import java.util.ArrayList;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.p2p.ChannelInfo;
import com.suntide_20210418.advancedmemorycard.p2p.NodeType;
import com.suntide_20210418.advancedmemorycard.p2p.P2PInfo;
import com.suntide_20210418.advancedmemorycard.p2p.P2PTypeInfo;
import com.suntide_20210418.advancedmemorycard.p2p.TreeNode;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;

/**
 * P2P 详细信息面板控件（1.7.10 vanilla 实现）。<br/>
 * 选中 P2P 节点后显示设备详情，未选中时显示"请选择P2P"提示。
 */
public class DetailPanelWidget {

    private int x;
    private int y;
    private int width;
    private int height;

    private static final int BUTTON_SPACING = 4;

    private final GuiTextField nameField;
    private final FlatButton[] buttons;

    private String[] activeButtonLabelKeys = new String[] { TranslateHelper.Keys.BUTTON_RENAME,
        TranslateHelper.Keys.BUTTON_SELECT, TranslateHelper.Keys.BUTTON_HIGHLIGHT,
        TranslateHelper.Keys.BUTTON_PENDING_BIND, TranslateHelper.Keys.BUTTON_REFRESH,
        TranslateHelper.Keys.BUTTON_ASSIGN_FREQ };
    private int activeButtonCount = 6;

    private P2PInfo selectedP2PInfo;
    private TreeNode selectedNode;
    private Map<String, ChannelInfo> channelInfoMap;
    private Map<String, P2PTypeInfo> p2pTypeInfoMap;
    private P2PInfo pendingBindP2P;
    private boolean isEditing = false;

    private String optimisticP2PName = null;
    private String optimisticP2PNameKey = null;
    private String optimisticChannelAlias = null;
    private String optimisticChannelAliasFreq = null;

    private ActionCallback actionCallback;

    private int getColorBg() {
        return ModConfigs.getClientConfig().panelColorBg;
    }

    private int getColorTitle() {
        return ModConfigs.getClientConfig().panelColorTitle;
    }

    private int getColorLabel() {
        return ModConfigs.getClientConfig().panelColorLabel;
    }

    private int getColorValue() {
        return ModConfigs.getClientConfig().panelColorValue;
    }

    private int getColorPlaceholder() {
        return ModConfigs.getClientConfig().panelColorPlaceholder;
    }

    private int getColorSeparator() {
        return ModConfigs.getClientConfig().panelColorSeparator;
    }

    private int getColorButtonAreaBg() {
        return ModConfigs.getClientConfig().panelColorButtonAreaBg;
    }

    private int getStatusNotActive() {
        return ModConfigs.getClientConfig().panelStatusNotActive;
    }

    private int getStatusNotConnected() {
        return ModConfigs.getClientConfig().panelStatusNotConnected;
    }

    private int getStatusConnected() {
        return ModConfigs.getClientConfig().panelStatusConnected;
    }

    private int getButtonWidth() {
        return ModConfigs.getClientConfig().panelButtonWidth;
    }

    private int getButtonHeight() {
        return ModConfigs.getClientConfig().panelButtonHeight;
    }

    private int getInfoAreaHeightRatio() {
        return ModConfigs.getClientConfig().panelInfoAreaHeightRatio;
    }

    private int getTotalHeightRatio() {
        return ModConfigs.getClientConfig().panelTotalHeightRatio;
    }

    private int getLineHeight() {
        return ModConfigs.getClientConfig().panelLineHeight;
    }

    private int getPaddingLeft() {
        return ModConfigs.getClientConfig().panelPaddingLeft;
    }

    private int getPaddingTop() {
        return ModConfigs.getClientConfig().panelPaddingTop;
    }

    private int getButtonCountMax() {
        return ModConfigs.getClientConfig().panelButtonCountMax;
    }

    public DetailPanelWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        nameField = new GuiTextField(font, 0, 0, 0, getLineHeight());
        nameField.setMaxStringLength(64);
        nameField.setVisible(false);

        int max = getButtonCountMax();
        buttons = new FlatButton[max];
        for (int i = 0; i < max; i++) {
            final int idx = i;
            buttons[i] = new FlatButton(
                0,
                0,
                getButtonWidth(),
                getButtonHeight(),
                TranslateHelper.Keys.BUTTON_RENAME,
                () -> onButtonClick(idx));
        }
        repositionButtons();
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        repositionButtons();
    }

    public void setActionCallback(ActionCallback callback) {
        this.actionCallback = callback;
    }

    public void setPendingBindP2P(P2PInfo pendingBindP2P) {
        this.pendingBindP2P = pendingBindP2P;
    }

    private void onButtonClick(int index) {
        if (actionCallback == null || selectedNode == null) return;
        switch (selectedNode.type) {
            case P2P:
                handleP2PButtonClick(index);
                break;
            case CHANNEL:
                handleChannelButtonClick(index);
                break;
            case P2P_TYPE:
                handleTypeButtonClick(index);
                break;
        }
    }

    private void handleP2PButtonClick(int index) {
        if (selectedP2PInfo == null) return;
        if (!selectedP2PInfo.isMEP2P()) {
            switch (index) {
                case 0:
                    if (isEditing) cancelEditing();
                    else enterEditing();
                    break;
                case 1:
                    actionCallback.onSelect(selectedP2PInfo);
                    break;
                case 2:
                    Minecraft.getMinecraft()
                        .displayGuiScreen(null);
                    actionCallback.onHighlight(selectedP2PInfo);
                    break;
                case 3:
                    actionCallback.onAssignFreq(selectedP2PInfo);
                    break;
                case 4:
                    actionCallback.onLocate(pendingBindP2P);
                    break;
                case 5:
                    actionCallback.onRefresh();
                    break;
            }
            return;
        }
        switch (index) {
            case 0:
                if (isEditing) cancelEditing();
                else enterEditing();
                break;
            case 1:
                actionCallback.onSelect(selectedP2PInfo);
                break;
            case 2:
                Minecraft.getMinecraft()
                    .displayGuiScreen(null);
                actionCallback.onHighlight(selectedP2PInfo);
                break;
            case 3:
                actionCallback.onLocate(pendingBindP2P);
                break;
            case 4:
                actionCallback.onRefresh();
                break;
        }
    }

    private void handleChannelButtonClick(int index) {
        String frequency = selectedNode.frequency;
        if (frequency == null) return;
        switch (index) {
            case 0:
                if (isEditing) cancelEditing();
                else enterChannelEditing();
                break;
            case 1:
                actionCallback.onChannelBind(frequency);
                break;
            case 2:
                Minecraft.getMinecraft()
                    .displayGuiScreen(null);
                actionCallback.onChannelHighlight(frequency);
                break;
            case 3:
                actionCallback.onLocate(pendingBindP2P);
                break;
            case 4:
                actionCallback.onRefresh();
                break;
        }
    }

    private void handleTypeButtonClick(int index) {
        if (isMEP2PType()) {
            switch (index) {
                case 0:
                    actionCallback.onRefresh();
                    break;
                case 1:
                    actionCallback.onAutoAssign();
                    break;
                case 2:
                    actionCallback.onLocate(pendingBindP2P);
                    break;
            }
        } else {
            switch (index) {
                case 0:
                    actionCallback.onRefresh();
                    break;
                case 1:
                    actionCallback.onLocate(pendingBindP2P);
                    break;
            }
        }
    }

    private void enterEditing() {
        isEditing = true;
        int labelX = x + getPaddingLeft();
        int labelWidth = Minecraft.getMinecraft().fontRenderer.getStringWidth(TranslateHelper.DetailPanel.nameLabel());
        String currentName = selectedP2PInfo.name();
        nameField.setText((currentName != null && !currentName.isEmpty()) ? currentName : "");
        nameField.xPosition = labelX + labelWidth;
        nameField.yPosition = y + getPaddingTop();
        nameField.width = width - labelWidth - getPaddingLeft() - 2;
        nameField.setVisible(true);
        nameField.setFocused(true);
        buttons[0].setLabel(TranslateHelper.Button.cancel());
    }

    private void enterChannelEditing() {
        isEditing = true;
        int labelX = x + getPaddingLeft();
        int labelWidth = Minecraft.getMinecraft().fontRenderer
            .getStringWidth(TranslateHelper.DetailPanel.inputNameLabel());
        String freq = selectedNode.frequency;
        String alias = getFrequencyAlias(freq);
        nameField.setText(alias);
        nameField.xPosition = labelX + labelWidth;
        nameField.yPosition = y + getPaddingTop();
        nameField.width = width - labelWidth - getPaddingLeft() - 2;
        nameField.setVisible(true);
        nameField.setFocused(true);
        buttons[0].setLabel(TranslateHelper.Button.cancel());
    }

    private void cancelEditing() {
        finishEditing();
    }

    private void onNameConfirm() {
        if (!isEditing) return;
        if (selectedP2PInfo != null) {
            String newName = nameField.getText()
                .trim();
            if (!newName.isEmpty() && actionCallback != null) {
                optimisticP2PName = newName;
                optimisticP2PNameKey = selectedP2PInfo.frequency() + ":" + selectedP2PInfo.toShortString();
                actionCallback.onRename(selectedP2PInfo, newName);
            }
        }
        finishEditing();
    }

    private void onChannelAliasConfirm() {
        if (!isEditing) return;
        String newAlias = nameField.getText()
            .trim();
        if (!newAlias.isEmpty() && actionCallback != null
            && selectedNode != null
            && selectedNode.type == NodeType.CHANNEL) {
            optimisticChannelAlias = newAlias;
            optimisticChannelAliasFreq = selectedNode.frequency;
            actionCallback.onChannelRename(selectedNode.frequency, newAlias);
        }
        finishEditing();
    }

    private void finishEditing() {
        isEditing = false;
        nameField.setVisible(false);
        nameField.setFocused(false);
        buttons[0].setLabel(TranslateHelper.Button.rename());
    }

    private void repositionButtons() {
        int infoHeight = height * getInfoAreaHeightRatio() / getTotalHeightRatio();
        int areaY = y + infoHeight + 1;
        int buttonAreaHeight = height - infoHeight - 1;

        int count = activeButtonCount;
        if (count == 0) return;
        int cols = Math.min(count, 3);
        int rows = (count + cols - 1) / cols;

        int bw = getButtonWidth();
        int bh = getButtonHeight();
        int totalButtonsWidth = cols * bw + (cols - 1) * BUTTON_SPACING;
        int startX = x + (width - totalButtonsWidth) / 2;
        int totalRowsHeight = rows * bh + (rows - 1) * BUTTON_SPACING;
        int startY = areaY + (buttonAreaHeight - totalRowsHeight) / 2;

        for (int i = 0; i < getButtonCountMax(); i++) {
            int row = i / cols;
            int col = i % cols;
            buttons[i].setX(startX + col * (bw + BUTTON_SPACING));
            buttons[i].setY(startY + row * (bh + BUTTON_SPACING));
        }
    }

    public void setSelectedNode(TreeNode node, Map<String, ChannelInfo> channelInfoMap,
        Map<String, P2PTypeInfo> p2pTypeInfoMap) {
        this.selectedNode = node;
        this.channelInfoMap = channelInfoMap;
        this.p2pTypeInfoMap = p2pTypeInfoMap;

        if (node != null && node.type == NodeType.P2P) {
            this.selectedP2PInfo = node.p2pInfo;
            if (optimisticP2PName != null && selectedP2PInfo != null
                && optimisticP2PName.equals(selectedP2PInfo.name())) {
                optimisticP2PName = null;
                optimisticP2PNameKey = null;
            }
        } else {
            this.selectedP2PInfo = null;
            optimisticP2PName = null;
            optimisticP2PNameKey = null;
        }

        if (optimisticChannelAlias != null && optimisticChannelAliasFreq != null) {
            ChannelInfo ci = null;
            if (channelInfoMap != null) {
                for (Map.Entry<String, ChannelInfo> e : channelInfoMap.entrySet()) {
                    if (e.getKey()
                        .startsWith(optimisticChannelAliasFreq + "|")) {
                        ci = e.getValue();
                        break;
                    }
                }
            }
            if (ci != null && optimisticChannelAlias.equals(ci.alias())) {
                optimisticChannelAlias = null;
                optimisticChannelAliasFreq = null;
            }
        }

        updateButtonMode();
    }

    private void updateButtonMode() {
        if (selectedNode == null) {
            activeButtonCount = 0;
        } else {
            switch (selectedNode.type) {
                case P2P:
                    if (selectedP2PInfo != null && !selectedP2PInfo.isMEP2P()) {
                        activeButtonLabelKeys = new String[] { TranslateHelper.Keys.BUTTON_RENAME,
                            TranslateHelper.Keys.BUTTON_SELECT, TranslateHelper.Keys.BUTTON_HIGHLIGHT,
                            TranslateHelper.Keys.BUTTON_ASSIGN_FREQ, TranslateHelper.Keys.BUTTON_PENDING_BIND,
                            TranslateHelper.Keys.BUTTON_REFRESH };
                        activeButtonCount = 6;
                    } else {
                        activeButtonLabelKeys = new String[] { TranslateHelper.Keys.BUTTON_RENAME,
                            TranslateHelper.Keys.BUTTON_SELECT, TranslateHelper.Keys.BUTTON_HIGHLIGHT,
                            TranslateHelper.Keys.BUTTON_PENDING_BIND, TranslateHelper.Keys.BUTTON_REFRESH };
                        activeButtonCount = 5;
                    }
                    break;
                case CHANNEL:
                    activeButtonLabelKeys = new String[] { TranslateHelper.Keys.BUTTON_RENAME,
                        TranslateHelper.Keys.BUTTON_BIND, TranslateHelper.Keys.BUTTON_HIGHLIGHT,
                        TranslateHelper.Keys.BUTTON_PENDING_BIND, TranslateHelper.Keys.BUTTON_REFRESH };
                    activeButtonCount = 5;
                    break;
                case P2P_TYPE:
                    if (isMEP2PType()) {
                        activeButtonLabelKeys = new String[] { TranslateHelper.Keys.BUTTON_REFRESH,
                            TranslateHelper.Keys.BUTTON_INIT_P2P, TranslateHelper.Keys.BUTTON_PENDING_BIND };
                    } else {
                        activeButtonLabelKeys = new String[] { TranslateHelper.Keys.BUTTON_REFRESH,
                            TranslateHelper.Keys.BUTTON_PENDING_BIND };
                    }
                    activeButtonCount = activeButtonLabelKeys.length;
                    break;
                default:
                    activeButtonCount = 0;
            }
        }

        for (int i = 0; i < getButtonCountMax(); i++) {
            if (i < activeButtonCount) {
                buttons[i].setLabel(TranslateHelper.translate(activeButtonLabelKeys[i]));
                buttons[i].visible = true;
                buttons[i].active = true;
            } else {
                buttons[i].visible = false;
                buttons[i].active = false;
            }
        }
        repositionButtons();
    }

    public void draw(FontRenderer font, int mouseX, int mouseY) {
        net.minecraft.client.gui.Gui.drawRect(x, y, x + width, y + height, getColorBg());

        if (selectedNode == null) {
            renderPlaceholder(font);
        } else {
            int infoHeight = height * getInfoAreaHeightRatio() / getTotalHeightRatio();
            renderInfoArea(font, infoHeight);
            int sepY = y + infoHeight;
            net.minecraft.client.gui.Gui.drawRect(x + 2, sepY, x + width - 2, sepY + 1, getColorSeparator());
            renderButtonArea(font, mouseX, mouseY);
        }

        if (isEditing && nameField.getVisible()) {
            nameField.drawTextBox();
        }
    }

    private void renderPlaceholder(FontRenderer font) {
        String placeholder = TranslateHelper.DetailPanel.placeholder();
        int textWidth = font.getStringWidth(placeholder);
        font.drawString(placeholder, x + (width - textWidth) / 2, y + height / 2 - 4, getColorPlaceholder());
    }

    private void renderInfoArea(FontRenderer font, int infoHeight) {
        if (selectedNode == null) return;
        switch (selectedNode.type) {
            case P2P:
                renderP2PInfoArea(font);
                break;
            case CHANNEL:
                renderChannelInfoArea(font);
                break;
            case P2P_TYPE:
                renderTypeInfoArea(font);
                break;
        }
    }

    private void renderP2PInfoArea(FontRenderer font) {
        int lx = x + getPaddingLeft();
        int ly = y + getPaddingTop();
        int lineSpacing = getLineHeight() + 2;

        if (isEditing) {
            drawLabelValue(font, lx, ly, TranslateHelper.DetailPanel.nameLabel(), "", getColorLabel());
        } else {
            String deviceName = getEffectiveP2PName();
            if (selectedP2PInfo.isPendingBind()) deviceName += TranslateHelper.Status.currentSelected();
            drawLabelValue(font, lx, ly, TranslateHelper.DetailPanel.nameLabel(), deviceName, getColorTitle());
        }
        ly += lineSpacing;

        String frequency = selectedP2PInfo.frequency();
        String alias = getEffectiveChannelAlias(frequency);
        String freqDisplay = frequency;
        if (!alias.equals(frequency)) freqDisplay = alias + " (" + frequency + ")";
        drawLabelValue(font, lx, ly, TranslateHelper.DetailPanel.frequencyLabel(), freqDisplay, getColorValue());
        ly += lineSpacing;

        drawLabelValue(
            font,
            lx,
            ly,
            TranslateHelper.DetailPanel.typeLabel(),
            selectedP2PInfo.p2pType(),
            getColorValue());
        ly += lineSpacing;

        drawLabelValue(
            font,
            lx,
            ly,
            TranslateHelper.DetailPanel.dimensionLabel(),
            selectedP2PInfo.dimension(),
            getColorValue());
        ly += lineSpacing;

        String position = "[" + selectedP2PInfo.position()
            .getX()
            + ", "
            + selectedP2PInfo.position()
                .getY()
            + ", "
            + selectedP2PInfo.position()
                .getZ()
            + "]";
        drawLabelValue(font, lx, ly, TranslateHelper.DetailPanel.positionLabel(), position, getColorValue());
        ly += lineSpacing;

        drawLabelValue(
            font,
            lx,
            ly,
            TranslateHelper.DetailPanel.directionLabel(),
            selectedP2PInfo.direction()
                .name(),
            getColorValue());
        ly += lineSpacing;

        String status;
        int statusColor;
        if (!selectedP2PInfo.isActive()) {
            status = TranslateHelper.Status.notActive();
            statusColor = getStatusNotActive();
        } else if (!selectedP2PInfo.isConnected()) {
            status = TranslateHelper.Status.notConnected();
            statusColor = getStatusNotConnected();
        } else {
            status = TranslateHelper.Status.connected();
            statusColor = getStatusConnected();
            if (selectedP2PInfo.isOutput()) status += TranslateHelper.Status.output();
            else status += TranslateHelper.Status.input();
        }
        drawLabelValue(font, lx, ly, TranslateHelper.DetailPanel.statusLabel(), status, statusColor);
        ly += lineSpacing;

        if (selectedP2PInfo.isMEP2P()) {
            int used = 0, maxCh = 0, remaining = 0;
            if (selectedP2PInfo.isActive() && selectedP2PInfo.isConnected()) {
                used = selectedP2PInfo.channel();
                maxCh = selectedP2PInfo.maxChannel();
                ChannelInfo meChannel = channelInfoMap != null
                    ? channelInfoMap.get(frequency + "|" + selectedP2PInfo.p2pType())
                    : null;
                remaining = meChannel != null ? meChannel.channelRemaining() : 0;
            }
            drawLabelValue(
                font,
                lx,
                ly,
                TranslateHelper.DetailPanel.channelInfoLabel(),
                used + "/" + remaining + "/" + maxCh,
                getColorValue());
        }
    }

    private void renderChannelInfoArea(FontRenderer font) {
        int lx = x + getPaddingLeft();
        int ly = y + getPaddingTop();
        int lineSpacing = getLineHeight() + 2;

        String frequency = selectedNode.frequency;
        ChannelInfo channelInfo = null;
        if (channelInfoMap != null) {
            if (selectedNode.parent != null && selectedNode.parent.typeName != null) {
                channelInfo = channelInfoMap.get(frequency + "|" + selectedNode.parent.typeName);
            }
            if (channelInfo == null) {
                for (Map.Entry<String, ChannelInfo> e : channelInfoMap.entrySet()) {
                    if (e.getKey()
                        .startsWith(frequency + "|")) {
                        channelInfo = e.getValue();
                        break;
                    }
                }
            }
        }

        if (isEditing) {
            drawLabelValue(font, lx, ly, TranslateHelper.DetailPanel.inputNameLabel(), "", getColorLabel());
        } else {
            String alias = getEffectiveChannelAlias(frequency);
            String freqDisplay = frequency;
            if (!alias.equals(frequency)) freqDisplay = alias + " (" + frequency + ")";
            drawLabelValue(font, lx, ly, TranslateHelper.DetailPanel.frequencyLabel(), freqDisplay, getColorTitle());
        }
        ly += lineSpacing;

        int p2pCount = channelInfo != null ? channelInfo.p2pCount() : 0;
        drawLabelValue(
            font,
            lx,
            ly,
            TranslateHelper.DetailPanel.p2pCountLabel(),
            String.valueOf(p2pCount),
            getColorValue());
        ly += lineSpacing;

        String p2pType = channelInfo != null ? channelInfo.p2pType() : "unknown";
        drawLabelValue(font, lx, ly, TranslateHelper.DetailPanel.typeLabel(), p2pType, getColorValue());
        ly += lineSpacing;

        int inputCount = 0, outputCount = 0;
        if (channelInfo != null) {
            ArrayList<P2PInfo> p2ps = channelInfo.p2pInfoList();
            if (p2ps != null) {
                for (P2PInfo info : p2ps) {
                    if (info.isOutput()) outputCount++;
                    else inputCount++;
                }
            }
        }
        drawLabelValue(
            font,
            lx,
            ly,
            TranslateHelper.DetailPanel.inputCountLabel(),
            String.valueOf(inputCount),
            getColorValue());
        ly += lineSpacing;
        drawLabelValue(
            font,
            lx,
            ly,
            TranslateHelper.DetailPanel.outputCountLabel(),
            String.valueOf(outputCount),
            getColorValue());
        ly += lineSpacing;

        if (isChannelMEP2P(channelInfo)) {
            int remaining = channelInfo.channelRemaining();
            int maxCh = channelInfo.maxChannel();
            int usedCh = maxCh - remaining;
            drawLabelValue(
                font,
                lx,
                ly,
                TranslateHelper.DetailPanel.channelInfoLabel(),
                usedCh + "/" + remaining + "/" + maxCh,
                getColorValue());
        }
    }

    private void renderTypeInfoArea(FontRenderer font) {
        int lx = x + getPaddingLeft();
        int ly = y + getPaddingTop();
        int lineSpacing = getLineHeight() + 2;

        String typeName = selectedNode.typeName;
        P2PTypeInfo typeInfo = p2pTypeInfoMap != null ? p2pTypeInfoMap.get(typeName) : null;

        drawLabelValue(
            font,
            lx,
            ly,
            TranslateHelper.DetailPanel.typeLabel(),
            typeName != null ? typeName : "unknown",
            getColorTitle());
        ly += lineSpacing;

        int p2pCount = typeInfo != null ? typeInfo.p2pCount() : 0;
        drawLabelValue(
            font,
            lx,
            ly,
            TranslateHelper.DetailPanel.totalCountLabel(),
            String.valueOf(p2pCount),
            getColorValue());
        ly += lineSpacing;

        int channelCount = typeInfo != null ? typeInfo.channelCount() : 0;
        drawLabelValue(
            font,
            lx,
            ly,
            TranslateHelper.DetailPanel.channelCountLabel(),
            String.valueOf(channelCount),
            getColorValue());

        if (isMEP2PType()) {
            ly += lineSpacing;
            int totalRemaining = 0, totalMax = 0;
            if (typeInfo != null) {
                ArrayList<ChannelInfo> channelInfoList = typeInfo.channelInfoList();
                if (channelInfoList != null) {
                    for (ChannelInfo info : channelInfoList) {
                        totalRemaining += info.channelRemaining();
                        totalMax += info.maxChannel();
                    }
                }
            }
            int totalUsed = totalMax - totalRemaining;
            drawLabelValue(
                font,
                lx,
                ly,
                TranslateHelper.DetailPanel.channelInfoLabel(),
                totalUsed + "/" + totalRemaining + "/" + totalMax,
                getColorValue());
        }
    }

    private void renderButtonArea(FontRenderer font, int mouseX, int mouseY) {
        int infoHeight = height * getInfoAreaHeightRatio() / getTotalHeightRatio();
        int areaY = y + infoHeight + 1;
        int buttonAreaBottom = y + height;
        net.minecraft.client.gui.Gui.drawRect(x, areaY, x + width, buttonAreaBottom, getColorButtonAreaBg());
        for (FlatButton btn : buttons) {
            btn.draw(font, mouseX, mouseY);
        }
    }

    private void drawLabelValue(FontRenderer font, int lx, int ly, String label, String value, int valueColor) {
        font.drawString(label, lx, ly, getColorLabel());
        int labelWidth = font.getStringWidth(label);
        font.drawString(value, lx + labelWidth, ly, valueColor);
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!nameField.getVisible()) {
            for (FlatButton btn : buttons) {
                if (btn.visible && btn.mouseClicked(mouseX, mouseY, button)) return true;
            }
        } else {
            nameField.mouseClicked(mouseX, mouseY, button);
        }
        return false;
    }

    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        for (FlatButton btn : buttons) {
            if (btn.visible && btn.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        if (!isEditing) return false;
        if (keyCode == 28 || keyCode == 156) { // Enter / keypad Enter
            onNameConfirm();
            return true;
        }
        if (keyCode == 1) { // Escape
            cancelEditing();
            return true;
        }
        nameField.textboxKeyTyped(typedChar, keyCode);
        return true;
    }

    public void updateScreen() {
        nameField.updateCursorCounter();
    }

    private boolean isMEP2PType() {
        if (selectedNode == null || selectedNode.type != NodeType.P2P_TYPE) return false;
        P2PTypeInfo typeInfo = p2pTypeInfoMap != null ? p2pTypeInfoMap.get(selectedNode.typeName) : null;
        if (typeInfo != null) {
            ArrayList<P2PInfo> p2pList = typeInfo.p2pInfoList();
            if (p2pList != null) {
                for (P2PInfo info : p2pList) {
                    if (info.isMEP2P()) return true;
                }
            }
        }
        return false;
    }

    private boolean isChannelMEP2P(ChannelInfo channelInfo) {
        if (channelInfo == null) return false;
        ArrayList<P2PInfo> p2ps = channelInfo.p2pInfoList();
        if (p2ps != null) {
            for (P2PInfo info : p2ps) {
                if (info.isMEP2P()) return true;
            }
        }
        return false;
    }

    private String getEffectiveChannelAlias(String frequency) {
        if (optimisticChannelAlias != null && frequency.equals(optimisticChannelAliasFreq))
            return optimisticChannelAlias;
        return getFrequencyAlias(frequency);
    }

    private String getFrequencyAlias(String frequency) {
        if (channelInfoMap != null) {
            for (Map.Entry<String, ChannelInfo> e : channelInfoMap.entrySet()) {
                if (e.getKey()
                    .startsWith(frequency + "|")) {
                    ChannelInfo channelInfo = e.getValue();
                    if (channelInfo != null && channelInfo.alias() != null
                        && !channelInfo.alias()
                            .equals("frequency " + frequency)) {
                        return channelInfo.alias();
                    }
                    break;
                }
            }
        }
        return frequency;
    }

    private String getEffectiveP2PName() {
        if (optimisticP2PName != null && selectedP2PInfo != null) {
            String currentKey = selectedP2PInfo.frequency() + ":" + selectedP2PInfo.toShortString();
            if (currentKey.equals(optimisticP2PNameKey)) return optimisticP2PName;
        }
        if (selectedP2PInfo.name()
            .isEmpty()) return selectedP2PInfo.toShortString();
        return selectedP2PInfo.name();
    }

    public interface ActionCallback {

        void onRename(P2PInfo p2pInfo, String newName);

        void onBind(P2PInfo p2pInfo);

        void onSelect(P2PInfo p2pInfo);

        void onHighlight(P2PInfo p2pInfo);

        void onLocate(P2PInfo p2pInfo);

        void onAutoAssign();

        void onAssignFreq(P2PInfo p2pInfo);

        void onChannelRename(String frequency, String newAlias);

        void onChannelBind(String frequency);

        void onChannelHighlight(String frequency);

        void onRefresh();
    }
}
