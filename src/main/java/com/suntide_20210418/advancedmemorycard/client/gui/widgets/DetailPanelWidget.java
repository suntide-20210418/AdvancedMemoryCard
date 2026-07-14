package com.suntide_20210418.advancedmemorycard.client.gui.widgets;

import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ConfirmableTextField;
import com.suntide_20210418.advancedmemorycard.p2p.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Map;

/**
 * P2P 详细信息面板控件<br/>
 * 选中 P2P 节点后显示设备详情，未选中时显示"请选择P2P"提示。
 */
public class DetailPanelWidget extends AbstractWidget {

    // 颜色常量
    private static final int COLOR_BG = 0xFF9A9FB4;
    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_LABEL = 0xFF000000;
    private static final int COLOR_VALUE = 0xFFFFFFFF;
    private static final int COLOR_PLACEHOLDER = 0xFF888888;
    private static final int COLOR_SEPARATOR = 0xFF888888;

    // 按钮区域背景色（略深以区分上方信息区）
    private static final int COLOR_BUTTON_AREA_BG = 0xFF9A9FB4;

    // 布局常量
    private static final int BUTTON_WIDTH = 46;
    private static final int BUTTON_HEIGHT = 14;
    private static final int BUTTON_SPACING = 4;
    private static final int INFO_AREA_HEIGHT_RATIO = 3; // 3/4
    private static final int TOTAL_HEIGHT_RATIO = 4;
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING_LEFT = 4;
    private static final int PADDING_TOP = 4;

    // 按钮数量
    private static final int BUTTON_COUNT_MAX = 5;
    private static final String[] BUTTON_LABELS_ALL = {"改名", "选择", "高亮", "待绑定", "刷新"};

    // 频段模式按钮标签（5个按钮）
    private static final String[] CHANNEL_BUTTON_LABELS = {"改名", "绑定", "高亮", "待绑定", "刷新"};
    // 类型模式按钮标签（3个按钮）
    private static final String[] TYPE_BUTTON_LABELS = {"刷新", "初始化P2P", "待绑定"};
    // 操作按钮（FlatButton：与 P2PTreeWidget 列表项风格一致）
    private final FlatButton[] buttons = new FlatButton[BUTTON_COUNT_MAX];
    private final ScreenStyle style;
    // 当前显示的按钮标签
    private String[] activeButtonLabels = BUTTON_LABELS_ALL;
    private int activeButtonCount = BUTTON_COUNT_MAX;
    // 数据
    private P2PInfo selectedP2PInfo;
    // 当前选中的节点（可能是 P2P_TYPE、CHANNEL 或 P2P）
    private TreeNode selectedNode;
    // 频段信息映射（别名即输入端 P2P 名称），用于显示 "名称（频段hex）"
    private Map<String, ChannelInfo> channelInfoMap;
    // P2P 类型信息映射
    private Map<String, P2PTypeInfo> p2pTypeInfoMap;
    // 待绑定的 P2P（由 ConfigModeScreen.lastNavigatedP2P 维护），供"定位"按钮导航
    private P2PInfo pendingBindP2P;
    // 编辑状态（P2P 模式下仅编辑名称；频道模式下编辑输入端 P2P 名称）
    private boolean isEditing = false;
    private ConfirmableTextField nameField;
    private ConfirmableTextField freqField;
    // 乐观更新缓存：在服务端同步数据到达之前，本地暂存最新的名称/别名
    // 用于 P2P 名称修改后即时刷新 UI（key: P2PInfo.hashCode 组合，value: 新名称）
    private String optimisticP2PName = null;
    private String optimisticP2PNameKey = null;
    // 用于频段输入端名称修改后即时刷新 UI（key: frequency, value: 新名称）
    private String optimisticChannelAlias = null;
    private String optimisticChannelAliasFreq = null;
    private ActionCallback actionCallback;

    // ==================== 回调接口 ====================

    public DetailPanelWidget(ScreenStyle style, int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("Detail Panel"));
        this.style = style;

        // 创建改名用的 ConfirmableTextField（延迟创建，位置在进入编辑模式时更新）
        Font font = Minecraft.getInstance().font;
        nameField = new ConfirmableTextField(style, font, 0, 0, 0, LINE_HEIGHT);
        nameField.setBordered(false);
        nameField.setVisible(false);
        nameField.setOnConfirm(this::onNameConfirm);

        freqField = new ConfirmableTextField(style, font, 0, 0, 0, LINE_HEIGHT);
        freqField.setBordered(false);
        freqField.setVisible(false);
        freqField.setOnConfirm(this::onChannelAliasConfirm);

        // 创建扁平风格按钮（最大数量）
        for (int i = 0; i < BUTTON_COUNT_MAX; i++) {
            final int idx = i;
            buttons[i] = new FlatButton(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT,
                    Component.literal(BUTTON_LABELS_ALL[i]), () -> onButtonClick(idx));
        }
        repositionButtons();
    }

    public void setActionCallback(ActionCallback callback) {
        this.actionCallback = callback;
    }

    /**
     * 设置待绑定的 P2P，供"定位"按钮跳转使用。
     * 由 ConfigModeScreen 在 containerTick 中同步 lastNavigatedP2P。
     */
    public void setPendingBindP2P(P2PInfo pendingBindP2P) {
        this.pendingBindP2P = pendingBindP2P;
    }

    /**
     * 按钮点击处理
     */
    private void onButtonClick(int index) {
        if (actionCallback == null) {
            return;
        }

        if (selectedNode == null) {
            return;
        }

        // 根据节点类型分发
        switch (selectedNode.type) {
            case P2P -> handleP2PButtonClick(index);
            case CHANNEL -> handleChannelButtonClick(index);
            case P2P_TYPE -> handleTypeButtonClick(index);
        }
    }

    private void handleP2PButtonClick(int index) {
        if (selectedP2PInfo == null) return;
        switch (index) {
            case 0: // 改名 / 取消
                if (isEditing) {
                    cancelEditing();
                } else {
                    enterEditing();
                }
                break;
            case 1: // 设置
                actionCallback.onSelect(selectedP2PInfo);
                break;
            case 2: // 高亮
                Minecraft.getInstance().setScreen(null);
                actionCallback.onHighlight(selectedP2PInfo);
                break;
            case 3: // 定位到待绑定的p2p
                actionCallback.onLocate(pendingBindP2P);
                break;
            case 4: // 刷新
                actionCallback.onRefresh();
                break;
        }
    }

    private void handleChannelButtonClick(int index) {
        String frequency = selectedNode.frequency;
        if (frequency == null) return;
        switch (index) {
            case 0: // 改名
                if (isEditing) {
                    cancelEditing();
                } else {
                    enterChannelEditing();
                }
                break;
            case 1: // 绑定
                actionCallback.onChannelBind(frequency);
                break;
            case 2: // 高亮
                Minecraft.getInstance().setScreen(null);
                actionCallback.onChannelHighlight(frequency);
                break;
            case 3:
                actionCallback.onLocate(pendingBindP2P);
                break;
            case 4: // 刷新
                actionCallback.onRefresh();
                break;
        }
    }

    private void handleTypeButtonClick(int index) {
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
    }

    /**
     * 进入 P2P 改名编辑模式：仅显示名称输入框，按钮切换为"取消"
     */
    private void enterEditing() {
        isEditing = true;

        int x = getX() + PADDING_LEFT;
        int labelWidth = Minecraft.getInstance().font.width("名称: ");
        int nameY = getY() + PADDING_TOP;

        String currentName = selectedP2PInfo.name();
        String initialName = (currentName != null && !currentName.isEmpty()) ? currentName : "";
        nameField.setValue(initialName);
        nameField.setX(x + labelWidth);
        nameField.setY(nameY);
        nameField.setWidth(width - labelWidth - PADDING_LEFT - 2);
        nameField.setVisible(true);

        // 按钮文字切换为"取消"
        buttons[0].setLabel(Component.literal("取消"));
    }

    /**
     * 进入频段输入端名称编辑模式（修改该频段中输入端 P2P 的名称）
     */
    private void enterChannelEditing() {
        isEditing = true;

        int x = getX() + PADDING_LEFT;
        int labelWidth = Minecraft.getInstance().font.width("输入端名称: ");

        // 频段行 Y（信息区第一行）
        int freqY = getY() + PADDING_TOP;

        // 设置频段输入框：显示现有别名（即输入端 P2P 名称）
        String currentFreq = selectedNode.frequency;
        String alias = getFrequencyAlias(currentFreq);
        freqField.setValue(alias);
        freqField.setX(x + labelWidth);
        freqField.setY(freqY);
        freqField.setWidth(width - labelWidth - PADDING_LEFT - 2);
        freqField.setVisible(true);

        // 隐藏名称输入框（频道模式下不需要）
        nameField.setVisible(false);

        // 按钮文字切换为"取消"
        buttons[0].setLabel(Component.literal("取消"));
    }

    /**
     * 取消编辑模式：隐藏所有 text field，恢复按钮文字
     */
    private void cancelEditing() {
        isEditing = false;

        nameField.setVisible(false);
        freqField.setVisible(false);

        buttons[0].setLabel(Component.literal("改名"));
    }

    /**
     * P2P 名称输入框确认回调（Enter 键触发）。
     * 仅更新 P2P 设备名。
     */
    private void onNameConfirm() {
        if (!isEditing) return;
        if (selectedP2PInfo != null) {
            String newName = nameField.getValue().trim();
            if (!newName.isEmpty() && actionCallback != null) {
                // 乐观更新：本地缓存新名称，使 UI 立即刷新
                optimisticP2PName = newName;
                optimisticP2PNameKey = selectedP2PInfo.frequency() + ":" + selectedP2PInfo.toShortString();
                actionCallback.onRename(selectedP2PInfo, newName);
            }
        }
        finishEditing();
    }

    /**
     * 频段输入端名称输入框确认回调（Enter 键触发）。
     * 将频段中输入端 P2P 的名称设置为用户输入的值。
     */
    private void onChannelAliasConfirm() {
        if (!isEditing) return;
        String newAlias = freqField.getValue().trim();
        if (!newAlias.isEmpty() && actionCallback != null
                && selectedNode != null && selectedNode.type == NodeType.CHANNEL) {
            // 乐观更新：本地缓存新别名，使 UI 立即刷新
            optimisticChannelAlias = newAlias;
            optimisticChannelAliasFreq = selectedNode.frequency;
            actionCallback.onChannelRename(selectedNode.frequency, newAlias);
        }
        finishEditing();
    }

    /**
     * 退出编辑模式，隐藏所有输入框并恢复按钮文字
     */
    private void finishEditing() {
        isEditing = false;

        nameField.setVisible(false);
        freqField.setVisible(false);

        buttons[0].setLabel(Component.literal("改名"));
    }

    /**
     * 根据当前控件位置和大小重新计算各按钮坐标。
     * 当 AE2 布局系统通过 setX/setY/setWidth/setHeight 更新控件后自动调用。
     */
    private void repositionButtons() {
        int infoHeight = height * INFO_AREA_HEIGHT_RATIO / TOTAL_HEIGHT_RATIO;
        int areaY = getY() + infoHeight + 1;
        int buttonAreaHeight = height - infoHeight - 1;

        int count = activeButtonCount;
        if (count == 0) {
            return;
        }
        int cols = Math.min(count, 3);
        int rows = (count + cols - 1) / cols;

        int totalButtonsWidth = cols * BUTTON_WIDTH + (cols - 1) * BUTTON_SPACING;
        int startX = getX() + (width - totalButtonsWidth) / 2;
        int totalRowsHeight = rows * BUTTON_HEIGHT + (rows - 1) * BUTTON_SPACING;
        int startY = areaY + (buttonAreaHeight - totalRowsHeight) / 2;

        for (int i = 0; i < BUTTON_COUNT_MAX; i++) {
            int row = i / cols;
            int col = i % cols;
            int bx = startX + col * (BUTTON_WIDTH + BUTTON_SPACING);
            int by = startY + row * (BUTTON_HEIGHT + BUTTON_SPACING);
            buttons[i].setX(bx);
            buttons[i].setY(by);
        }
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        repositionButtons();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        repositionButtons();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        repositionButtons();
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        repositionButtons();
    }

    /**
     * 设置当前选中的节点（P2P、CHANNEL 或 P2P_TYPE）和映射数据。
     * 传入 null 表示未选中任何节点。
     */
    public void setSelectedNode(TreeNode node,
                                 Map<String, ChannelInfo> channelInfoMap,
                                 Map<String, P2PTypeInfo> p2pTypeInfoMap) {
        this.selectedNode = node;
        this.channelInfoMap = channelInfoMap;
        this.p2pTypeInfoMap = p2pTypeInfoMap;

        if (node != null && node.type == NodeType.P2P) {
            this.selectedP2PInfo = node.p2pInfo;
            // 检查乐观缓存是否已过期：如果 P2PInfo 中的名称已与乐观值一致，清除缓存
            if (optimisticP2PName != null && selectedP2PInfo != null) {
                if (optimisticP2PName.equals(selectedP2PInfo.name())) {
                    optimisticP2PName = null;
                    optimisticP2PNameKey = null;
                }
            }
        } else {
            this.selectedP2PInfo = null;
            // 切换节点时清除 P2P 名称乐观缓存
            optimisticP2PName = null;
            optimisticP2PNameKey = null;
        }

        // 检查频道别名乐观缓存是否已过期：如果 channelInfoMap 中别名已更新，清除缓存
        if (optimisticChannelAlias != null && optimisticChannelAliasFreq != null) {
            ChannelInfo ci = channelInfoMap != null ? channelInfoMap.get(optimisticChannelAliasFreq) : null;
            if (ci != null && optimisticChannelAlias.equals(ci.alias())) {
                optimisticChannelAlias = null;
                optimisticChannelAliasFreq = null;
            }
        }

        // 根据节点类型更新按钮标签和可见性
        updateButtonMode();
    }

    /**
     * 根据当前选中节点类型更新按钮显示
     */
    private void updateButtonMode() {
        if (selectedNode == null) {
            activeButtonLabels = new String[0];
            activeButtonCount = 0;
            return;
        }

        switch (selectedNode.type) {
            case P2P -> {
                activeButtonLabels = BUTTON_LABELS_ALL;
                activeButtonCount = BUTTON_COUNT_MAX;
            }
            case CHANNEL -> {
                activeButtonLabels = CHANNEL_BUTTON_LABELS;
                activeButtonCount = CHANNEL_BUTTON_LABELS.length;
            }
            case P2P_TYPE -> {
                activeButtonLabels = TYPE_BUTTON_LABELS;
                activeButtonCount = TYPE_BUTTON_LABELS.length;
            }
            default -> {
                activeButtonLabels = new String[0];
                activeButtonCount = 0;
            }
        }

        // 更新按钮标签和可见性
        for (int i = 0; i < BUTTON_COUNT_MAX; i++) {
            if (i < activeButtonCount) {
                buttons[i].setLabel(Component.literal(activeButtonLabels[i]));
                buttons[i].visible = true;
                buttons[i].active = true;
            } else {
                buttons[i].visible = false;
                buttons[i].active = false;
            }
        }

        repositionButtons();
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 绘制背景
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, COLOR_BG);

        if (selectedNode == null) {
            renderPlaceholder(guiGraphics);
        } else {
            // 信息区高度（3/4）
            int infoHeight = height * INFO_AREA_HEIGHT_RATIO / TOTAL_HEIGHT_RATIO;

            // 绘制信息区
            renderInfoArea(guiGraphics, infoHeight, mouseX, mouseY, partialTick);

            // 绘制分隔线
            int sepY = getY() + infoHeight;
            guiGraphics.fill(getX() + 2, sepY, getX() + width - 2, sepY + 1, COLOR_SEPARATOR);

            // 绘制操作区（1/4）
            renderButtonArea(guiGraphics, sepY + 1, mouseX, mouseY, partialTick);
        }
    }

    /**
     * 绘制"请选择P2P"占位提示
     */
    private void renderPlaceholder(GuiGraphics guiGraphics) {
        Font font = Minecraft.getInstance().font;
        Component placeholder = Component.literal("请选择P2P");
        int textWidth = font.width(placeholder);
        guiGraphics.drawString(font, placeholder,
                getX() + (width - textWidth) / 2,
                getY() + height / 2 - 4,
                COLOR_PLACEHOLDER, false);
    }

    /**
     * 绘制信息区
     */
    private void renderInfoArea(GuiGraphics guiGraphics, int infoHeight,
                                 int mouseX, int mouseY, float partialTick) {
        if (selectedNode == null) return;

        switch (selectedNode.type) {
            case P2P -> renderP2PInfoArea(guiGraphics, infoHeight, mouseX, mouseY, partialTick);
            case CHANNEL -> renderChannelInfoArea(guiGraphics, infoHeight, mouseX, mouseY, partialTick);
            case P2P_TYPE -> renderTypeInfoArea(guiGraphics, infoHeight, mouseX, mouseY, partialTick);
        }
    }

    /**
     * 绘制 P2P 节点信息区
     */
    private void renderP2PInfoArea(GuiGraphics guiGraphics, int infoHeight,
                                    int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int x = getX() + PADDING_LEFT;
        int y = getY() + PADDING_TOP;
        int lineSpacing = LINE_HEIGHT + 2;

        if (isEditing) {
            // 编辑模式：仅显示名称输入框
            drawLabelOnly(guiGraphics, font, x, y, "名称: ");
            nameField.render(guiGraphics, mouseX, mouseY, partialTick);
            y += lineSpacing;
        } else {
            // 优先使用乐观更新的名称，其次使用 P2PInfo 中的名称
            String deviceName = getEffectiveP2PName();
            if (selectedP2PInfo.isPendingBind()) {
                deviceName += "（当前选中）";
            }
            drawLabelValue(guiGraphics, font, x, y, "名称: ", deviceName, COLOR_TITLE);
            y += lineSpacing;
        }

        // 频段（始终显示，非编辑模式也显示），优先使用乐观更新的别名
        String frequency = selectedP2PInfo.frequency();
        String alias = getEffectiveChannelAlias(frequency);
        String freqDisplay = frequency;
        if (!alias.equals(frequency)) {
            freqDisplay = alias + " (" + frequency + ")";
        }
        drawLabelValue(guiGraphics, font, x, y, "频段: ", freqDisplay, COLOR_VALUE);
        y += lineSpacing;

        String p2pType = selectedP2PInfo.p2pType();
        drawLabelValue(guiGraphics, font, x, y, "类型: ", p2pType, COLOR_VALUE);
        y += lineSpacing;

        String dimension = selectedP2PInfo.dimension().location().getPath();
        drawLabelValue(guiGraphics, font, x, y, "维度: ", dimension, COLOR_VALUE);
        y += lineSpacing;

        String position = "[" + selectedP2PInfo.position().getX() + ", "
                + selectedP2PInfo.position().getY() + ", "
                + selectedP2PInfo.position().getZ() + "]";
        drawLabelValue(guiGraphics, font, x, y, "位置: ", position, COLOR_VALUE);
        y += lineSpacing;

        String direction = selectedP2PInfo.direction().getName();
        drawLabelValue(guiGraphics, font, x, y, "方向: ", direction, COLOR_VALUE);
        y += lineSpacing;

        String status;
        int statusColor;
        if (!selectedP2PInfo.isActive()) {
            status = "⚫ 未激活";
            statusColor = 0xFF000000;
        } else if (!selectedP2PInfo.isConnected()) {
            status = "🔴 未连接";
            statusColor = 0xFFFC5454;
        } else {
            status = "🟢 已连接";
            statusColor = 0xFF54FC54;
            if (selectedP2PInfo.isOutput()) {
                status += "（输出端）";
            } else {
                status += "（输入端）";
            }
        }
        drawLabelValue(guiGraphics, font, x, y, "状态: ", status, statusColor);
        y += lineSpacing;

        int used = 0;
        int maxCh = 0;
        int remaining = 0;
        if (selectedP2PInfo.isMEP2P() && selectedP2PInfo.isActive() && selectedP2PInfo.isConnected()) {
            used = selectedP2PInfo.channel();
            maxCh = selectedP2PInfo.maxChannel();
            remaining = channelInfoMap.get(frequency).channelRemaining();
        }
        String channelInfo = used + "/" + remaining + "/" + maxCh;
        drawLabelValue(guiGraphics, font, x, y, "已用/剩余/总数: ", channelInfo, COLOR_VALUE);
    }

    /**
     * 绘制频段节点信息区
     */
    private void renderChannelInfoArea(GuiGraphics guiGraphics, int infoHeight,
                                        int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int x = getX() + PADDING_LEFT;
        int y = getY() + PADDING_TOP;
        int lineSpacing = LINE_HEIGHT + 2;

        String frequency = selectedNode.frequency;
        ChannelInfo channelInfo = channelInfoMap != null ? channelInfoMap.get(frequency) : null;

        if (isEditing) {
            // 编辑模式下：输入端名称输入（频段别名指向输入端 P2P 名称）
            drawLabelOnly(guiGraphics, font, x, y, "输入端名称: ");
            freqField.render(guiGraphics, mouseX, mouseY, partialTick);
            y += lineSpacing;
        } else {
            // 频段（别名+hex），优先使用乐观更新的别名
            String alias = getEffectiveChannelAlias(frequency);
            String freqDisplay = frequency;
            if (!alias.equals(frequency)) {
                freqDisplay = alias + " (" + frequency + ")";
            }
            drawLabelValue(guiGraphics, font, x, y, "频段: ", freqDisplay, COLOR_TITLE);
            y += lineSpacing;
        }

        // P2P 数量
        int p2pCount = channelInfo != null ? channelInfo.p2pCount() : 0;
        drawLabelValue(guiGraphics, font, x, y, "P2P数量: ", String.valueOf(p2pCount), COLOR_VALUE);
        y += lineSpacing;

        // 频道使用情况
        int remaining = channelInfo != null ? channelInfo.channelRemaining() : 0;
        int usedCh = 0;
        int maxCh = 0;
        if (channelInfo != null) {
            ArrayList<P2PInfo> p2ps = channelInfo.p2pInfoList();
            if (p2ps != null) {
                for (P2PInfo info : p2ps) {
                    if (info.isMEP2P() && info.isActive() && info.isConnected()) {
                        usedCh = info.channel();
                        maxCh = info.maxChannel();
                        break;
                    }
                }
            }
        }
        String channelInfoStr = usedCh + "/" + remaining + "/" + maxCh;
        drawLabelValue(guiGraphics, font, x, y, "已用/剩余/总数: ", channelInfoStr, COLOR_VALUE);
        y += lineSpacing;

        // P2P 类型
        String p2pType = channelInfo != null ? channelInfo.p2pType() : "unknown";
        drawLabelValue(guiGraphics, font, x, y, "类型: ", p2pType, COLOR_VALUE);
        y += lineSpacing;

        // 输入/输出端统计
        int inputCount = 0;
        int outputCount = 0;
        if (channelInfo != null) {
            ArrayList<P2PInfo> p2ps = channelInfo.p2pInfoList();
            if (p2ps != null) {
                for (P2PInfo info : p2ps) {
                    if (info.isOutput()) {
                        outputCount++;
                    } else {
                        inputCount++;
                    }
                }
            }
        }
        drawLabelValue(guiGraphics, font, x, y, "输入端: ", String.valueOf(inputCount), COLOR_VALUE);
        y += lineSpacing;
        drawLabelValue(guiGraphics, font, x, y, "输出端: ", String.valueOf(outputCount), COLOR_VALUE);
    }

    /**
     * 绘制 P2P 类型节点信息区
     */
    private void renderTypeInfoArea(GuiGraphics guiGraphics, int infoHeight,
                                     int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int x = getX() + PADDING_LEFT;
        int y = getY() + PADDING_TOP;
        int lineSpacing = LINE_HEIGHT + 2;

        String typeName = selectedNode.typeName;
        P2PTypeInfo typeInfo = p2pTypeInfoMap != null ? p2pTypeInfoMap.get(typeName) : null;

        // 类型名称
        drawLabelValue(guiGraphics, font, x, y, "类型: ", typeName != null ? typeName : "unknown", COLOR_TITLE);
        y += lineSpacing;

        // P2P 总数
        int p2pCount = typeInfo != null ? typeInfo.p2pCount() : 0;
        drawLabelValue(guiGraphics, font, x, y, "P2P总数: ", String.valueOf(p2pCount), COLOR_VALUE);
        y += lineSpacing;

        // 频段数量
        int channelCount = typeInfo != null ? typeInfo.channelCount() : 0;
        drawLabelValue(guiGraphics, font, x, y, "频段数: ", String.valueOf(channelCount), COLOR_VALUE);
    }

    /**
     * 仅绘制标签文本（不含值），用于编辑模式下配合 text field 渲染
     */
    private void drawLabelOnly(GuiGraphics guiGraphics, Font font,
                                int x, int y, String label) {
        guiGraphics.drawString(font, label, x, y, COLOR_LABEL, false);
    }

    /**
     * 获取频段别名（即输入端 P2P 名称），没有别名则返回频段 hex。
     * 优先使用乐观更新的别名，否则从 channelInfoMap 获取。
     */
    private String getEffectiveChannelAlias(String frequency) {
        // 乐观更新优先
        if (optimisticChannelAlias != null && frequency.equals(optimisticChannelAliasFreq)) {
            return optimisticChannelAlias;
        }
        return getFrequencyAlias(frequency);
    }

    /**
     * 获取频段别名（即输入端 P2P 名称），没有别名则返回频段 hex
     */
    private String getFrequencyAlias(String frequency) {
        if (channelInfoMap != null) {
            ChannelInfo channelInfo = channelInfoMap.get(frequency);
            if (channelInfo != null && channelInfo.alias() != null
                    && !channelInfo.alias().equals("frequency " + frequency)) {
                return channelInfo.alias();
            }
        }
        return frequency;
    }

    /**
     * 获取 P2P 设备显示名称。
     * 优先使用乐观更新的名称，其次使用 P2PInfo 中的名称。
     */
    private String getEffectiveP2PName() {
        if (optimisticP2PName != null && selectedP2PInfo != null) {
            String currentKey = selectedP2PInfo.frequency() + ":" + selectedP2PInfo.toShortString();
            if (currentKey.equals(optimisticP2PNameKey)) {
                return optimisticP2PName;
            }
        }
        if (selectedP2PInfo.name().isEmpty()) {
            return selectedP2PInfo.toShortString();
        }
        return selectedP2PInfo.name();
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        for (FlatButton btn : buttons) {
            btn.mouseMoved(mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active || selectedNode == null) {
            return false;
        }
        for (FlatButton btn : buttons) {
            if (btn.visible && btn.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 绘制操作区按钮（委托给 FlatButton 控件渲染）
     */
    private void renderButtonArea(GuiGraphics guiGraphics, int areaY, int mouseX, int mouseY, float partialTick) {
        // 按钮区域的整体背景（略微区别于上方信息区）
        int buttonAreaBottom = getY() + height;
        guiGraphics.fill(getX(), areaY, getX() + width, buttonAreaBottom, COLOR_BUTTON_AREA_BG);

        // 渲染各 FlatButton
        for (FlatButton btn : buttons) {
            btn.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    /**
     * 绘制标签+值的组合文本
     */
    private void drawLabelValue(GuiGraphics guiGraphics, Font font,
                                int x, int y, String label, String value, int valueColor) {
        guiGraphics.drawString(font, label, x, y, COLOR_LABEL, false);
        int labelWidth = font.width(label);
        guiGraphics.drawString(font, value, x + labelWidth, y, valueColor, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active) {
            return false;
        }
        if (selectedNode == null) {
            return false;
        }
        // 编辑模式下：如果点击在 text field 上，不消费事件，让 MC 继续遍历 children
        if (isEditing) {
            if (nameField.isVisible() && nameField.isMouseOver(mouseX, mouseY)) {
                return false;
            }
            if (freqField.isVisible() && freqField.isMouseOver(mouseX, mouseY)) {
                return false;
            }
        }
        // 委托点击到各 FlatButton（仅可见按钮）
        for (FlatButton btn : buttons) {
            if (btn.visible && btn.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationOutput) {
        this.defaultButtonNarrationText(narrationOutput);
    }

    /**
     * 获取编辑用的 text field 列表，供 Screen 注册为 children 以确保焦点和键盘事件正常
     */
    public ConfirmableTextField[] getEditFields() {
        return new ConfirmableTextField[]{nameField, freqField};
    }

    /**
     * 转发按键事件到编辑中的 text field，返回 true 表示已消费
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isEditing) return false;
        if (nameField.isVisible() && nameField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (freqField.isVisible() && freqField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return false;
    }

    // ==================== 键盘事件转发（供 Screen 调用） ====================

    /**
     * 转发字符输入事件到编辑中的 text field，返回 true 表示已消费
     */
    public boolean charTyped(char codePoint, int modifiers) {
        if (!isEditing) return false;
        if (nameField.isVisible() && nameField.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (freqField.isVisible() && freqField.charTyped(codePoint, modifiers)) {
            return true;
        }
        return false;
    }

    /**
     * 按钮操作回调接口，由外部（ConfigModeScreen）实现。
     */
    public interface ActionCallback {
        // P2P 模式回调
        void onRename(P2PInfo p2pInfo, String newName);
        void onBind(P2PInfo p2pInfo);
        void onSelect(P2PInfo p2pInfo);
        void onHighlight(P2PInfo p2pInfo);
        void onLocate(P2PInfo p2pInfo);
        void onAutoAssign();

        // 频段模式回调
        void onChannelRename(String frequency, String newAlias);
        void onChannelBind(String frequency);
        void onChannelHighlight(String frequency);

        // 通用刷新回调
        void onRefresh();
    }
}
