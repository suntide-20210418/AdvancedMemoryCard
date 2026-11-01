package com.suntide_20210418.advancedmemorycard.client.gui.widgets;

import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ConfirmableTextField;
import com.suntide_20210418.advancedmemorycard.client.gui.P2PTypeNames;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.p2p.*;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Map;

/**
 * 详情检查器。
 *
 * <p>旧版是 8 行等权重的 {@code label: value}：名称 / 频段 / 类型 / 维度 / 坐标 / 朝向 /
 * 状态 / 已用-剩余-总数。最需要判断的「状态」排在第 7 行，前 6 行是构造后不变的元数据；
 * 而且一进改名编辑态，除名称外的所有行都会消失，玩家失去“我在改哪一台”的上下文。
 *
 * <p>现在：
 * <ul>
 *   <li>状态做成头部 chips，永远第一眼可见；</li>
 *   <li>元数据降到 6 行定宽属性表，标签用第三级文字色，值用主文字色；</li>
 *   <li>改名是就地编辑，属性表继续显示，不会丢失上下文。</li>
 * </ul>
 */
public class DetailPanelWidget extends AbstractWidget {

    private static final int PAD = 4;
    private static final int CHIP_H = 10;
    private static final int ROW_H = 14;
    private static final int BTN_H = 18;
    private static final int BTN_GAP = 6;
    private static final int LABEL_W = 44;

    private final ScreenStyle style;
    private final ConfirmableTextField nameField;

    private TreeNode selectedNode;
    private P2PInfo selectedP2PInfo;
    private Map<String, ChannelInfo> channelInfoMap;
    private Map<String, P2PTypeInfo> p2pTypeInfoMap;

    private boolean editing = false;
    private String selectionId;
    private ActionCallback actionCallback;

    private final FlatButton[] buttons = new FlatButton[5];

    public DetailPanelWidget(ScreenStyle style, int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.style = style;

        Font font = Minecraft.getInstance().font;
        nameField = new ConfirmableTextField(style, font, 0, 0, 0, 10);
        nameField.setBordered(false);
        nameField.setVisible(false);
        nameField.setOnConfirm(this::onNameConfirm);

        for (int i = 0; i < buttons.length; i++) {
            final int idx = i;
            buttons[i] = new FlatButton(0, 0, 80, BTN_H, Component.empty(),
                    FlatButton.Variant.SECONDARY, () -> onButtonClick(idx));
            buttons[i].visible = false;
        }
        repositionButtons();
    }

    public void setActionCallback(ActionCallback callback) {
        this.actionCallback = callback;
    }

    public void setSelectedNode(TreeNode node,
                                Map<String, ChannelInfo> channelInfoMap,
                                Map<String, P2PTypeInfo> p2pTypeInfoMap) {
        // 必须按 nodeId 比较，不能比引用：树每 tick 重建，TreeNode 每次都是新对象，
        // 比引用会让 editing 每帧被判定为“换了节点”而强制取消 —— 改名框刚出现就关。
        String newId = node == null ? null : node.getNodeId();
        boolean nodeChanged = !java.util.Objects.equals(newId, this.selectionId);
        this.selectionId = newId;
        this.selectedNode = node;
        this.channelInfoMap = channelInfoMap;
        this.p2pTypeInfoMap = p2pTypeInfoMap;
        this.selectedP2PInfo = node != null && node.type == NodeType.P2P ? node.p2pInfo : null;

        if (nodeChanged && editing) {
            cancelEditing();
        }
        updateButtons();
    }

    public ConfirmableTextField[] getEditFields() {
        return new ConfirmableTextField[]{nameField};
    }

    // ==================== 布局 ====================

    private int chipsY() {
        return getY() + 3;
    }

    private int nameRowY() {
        return getY() + 15;
    }

    private int dividerY() {
        return getY() + 29;
    }

    private int rowsTop() {
        return getY() + 32;
    }

    private int rowY(int index) {
        return rowsTop() + index * ROW_H;
    }

    private int buttonsTop() {
        int rows = getRowCount();
        int y = rowsTop() + rows * ROW_H + 4;
        if (hasChannelBar()) {
            y += 18;
        }
        return y;
    }

    private void repositionButtons() {
        int innerW = width - 2 * PAD;
        int bw = (innerW - BTN_GAP) / 2;
        int top = buttonsTop();
        // 按「可见按钮」编号占位，而不是按数组下标：类型行只剩两个按钮时，
        // 按下标摆会留出左上和右下两个空洞，看起来像按钮漂移。
        int visibleCount = 0;
        for (FlatButton button : buttons) {
            if (button.visible) visibleCount++;
        }
        int slot = 0;
        for (FlatButton button : buttons) {
            if (!button.visible) continue;
            int col = slot % 2;
            int row = slot / 2;
            // 奇数个按钮时，最后落单的那个拉通整行：右下角留一个空槽会看起来像「少画了一个」。
            // 拉通之后末尾那个动作也自然成了一条独立的行动条。
            boolean alone = slot == visibleCount - 1 && col == 0;
            button.setX(getX() + PAD + (alone ? 0 : col * (bw + BTN_GAP)));
            button.setY(top + row * (BTN_H + BTN_GAP));
            button.setWidth(alone ? innerW : bw);
            button.setHeight(BTN_H);
            slot++;
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

    // ==================== 内容模型 ====================

    private boolean isMEP2P() {
        if (selectedP2PInfo != null) return selectedP2PInfo.isMEP2P();
        if (selectedNode != null && selectedNode.type == NodeType.CHANNEL
                && selectedNode.data instanceof ChannelInfo info) {
            return "me_p2p_tunnel".equals(info.p2pType());
        }
        if (selectedNode != null && selectedNode.type == NodeType.P2P_TYPE) {
            P2PTypeInfo typeInfo = p2pTypeInfoMap == null ? null : p2pTypeInfoMap.get(selectedNode.typeName);
            if (typeInfo != null && typeInfo.p2pInfoList() != null) {
                for (P2PInfo info : typeInfo.p2pInfoList()) {
                    if (info.isMEP2P()) return true;
                }
            }
        }
        return false;
    }

    /**
     * 这台设备能不能「设为输入端」。
     *
     * <p>两个前置条件，缺一不可：
     * <ul>
     *   <li>它现在是<b>输出端</b> —— 反方向会把这条通道唯一的输入端改掉，通道立即失效；</li>
     *   <li>它已绑定真实频段 —— 0000 是 AE2 的「未绑定」，没有通道可接管，而且
     *       freq=0 的设备会被 P2PService 登记进 inputs[0]，对它们做角色切换只会让
     *       一堆未绑定设备在界面里互相「连通」。</li>
     * </ul>
     */
    private boolean canSwitchToInput(P2PInfo info) {
        if (info == null || !info.isOutput()) {
            return false;
        }
        String freq = info.frequency();
        return freq != null && !"0000".equals(freq);
    }

    private int getRowCount() {
        if (selectedNode == null) return 0;
        return switch (selectedNode.type) {
            case P2P -> 6;
            case CHANNEL -> 5;
            case P2P_TYPE -> 3;
            default -> 0;
        };
    }

    /** 当前节点该显示的频道占用；只有 ME 类型才有值。 */
    private P2PChannelUsage currentUsage() {
        if (selectedNode == null) return P2PChannelUsage.NONE;
        if (selectedNode.type == NodeType.P2P) {
            ChannelInfo channel = selectedP2PInfo == null || channelInfoMap == null
                    ? null : channelInfoMap.get(selectedP2PInfo.frequency());
            return P2PChannelUsage.ofRow(selectedP2PInfo, channel);
        }
        if (selectedNode.type == NodeType.CHANNEL && selectedNode.data instanceof ChannelInfo c) {
            return P2PChannelUsage.ofChannel(c);
        }
        return P2PChannelUsage.NONE;
    }

    private boolean hasChannelBar() {
        return currentUsage().available();
    }

    // ==================== 渲染 ====================

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        Font font = Minecraft.getInstance().font;

        if (selectedNode == null) {
            Component hint = TranslateHelper.DetailPanel.placeholder();
            int w = font.width(hint);
            g.drawString(font, hint, getX() + (width - w) / 2, getY() + 40,
                    colorTertiary(), false);
            return;
        }

        renderChips(g, font);
        renderNameRow(g, font);
        g.fill(getX() + PAD, dividerY(), getX() + width - PAD, dividerY() + 1, colorDivider());

        String[][] rows = buildRows();
        for (int i = 0; i < rows.length; i++) {
            int y = rowY(i);
            g.drawString(font, rows[i][0], getX() + PAD, y + 3, colorTertiary(), false);
            int valueColor = rows[i].length > 2 && "warn".equals(rows[i][2]) ? colorWarn() : colorPrimary();
            g.drawString(font, rows[i][1], getX() + PAD + LABEL_W, y + 3, valueColor, false);
        }

        if (hasChannelBar()) {
            renderChannelBar(g, buttonsTop() - 16);
        }

        for (FlatButton button : buttons) {
            button.render(g, mouseX, mouseY, partialTick);
        }
    }

    private String[][] buildRows() {
        Map<String, ChannelInfo> map = channelInfoMap;
        if (selectedNode == null) return new String[0][];

        if (selectedNode.type == NodeType.P2P && selectedP2PInfo != null) {
            P2PInfo info = selectedP2PInfo;
            String alias = channelAlias(info.frequency());
            String freqText = alias.equals(info.frequency()) ? info.frequency()
                    : alias + " · " + info.frequency();
            String role = TranslateHelper.translate(info.isOutput()
                    ? TranslateHelper.Keys.STATUS_OUTPUT
                    : TranslateHelper.Keys.STATUS_INPUT).getString();
            String status = !info.isActive()
                    ? TranslateHelper.Status.notActive().getString()
                    : (info.isConnected() ? TranslateHelper.Status.connected().getString()
                    : TranslateHelper.Status.notConnected().getString());
            return new String[][]{
                    {TranslateHelper.DetailPanel.frequencyLabel().getString(), freqText, ""},
                    {TranslateHelper.DetailPanel.typeLabel().getString(), P2PTypeNames.string(info.p2pType()), ""},
                    {TranslateHelper.DetailPanel.statusLabel().getString(), status + " " + role,
                            info.isConnected() ? "" : "warn"},
                    {TranslateHelper.DetailPanel.dimensionLabel().getString(),
                            info.dimension().location().getPath(), ""},
                    {TranslateHelper.DetailPanel.positionLabel().getString(),
                            info.position().getX() + ", " + info.position().getY() + ", " + info.position().getZ(), ""},
                    {TranslateHelper.DetailPanel.directionLabel().getString(), info.direction().getName(), ""},
            };
        }

        if (selectedNode.type == NodeType.CHANNEL) {
            ChannelInfo info = selectedNode.data instanceof ChannelInfo c ? c : null;
            int in = 0, out = 0;
            if (info != null && info.p2pInfoList() != null) {
                for (P2PInfo p : info.p2pInfoList()) {
                    if (p.isOutput()) out++; else in++;
                }
            }
            String alias = info == null ? selectedNode.frequency : info.alias();
            String freqText = alias == null || alias.equals(selectedNode.frequency)
                    ? selectedNode.frequency
                    : alias + " · " + selectedNode.frequency;
            String type = info == null ? "unknown" : info.p2pType();
            return new String[][]{
                    {TranslateHelper.DetailPanel.frequencyLabel().getString(), freqText, ""},
                    {TranslateHelper.DetailPanel.typeLabel().getString(), P2PTypeNames.string(type), ""},
                    {TranslateHelper.DetailPanel.p2pCountLabel().getString(),
                            String.valueOf(info == null ? 0 : info.p2pCount()), ""},
                    {TranslateHelper.DetailPanel.inputCountLabel().getString(), String.valueOf(in), ""},
                    {TranslateHelper.DetailPanel.outputCountLabel().getString(), String.valueOf(out), ""},
            };
        }

        P2PTypeInfo typeInfo = p2pTypeInfoMap == null || selectedNode.typeName == null
                ? null : p2pTypeInfoMap.get(selectedNode.typeName);
        return new String[][]{
                {TranslateHelper.DetailPanel.typeLabel().getString(),
                        selectedNode.typeName == null ? "—" : P2PTypeNames.string(selectedNode.typeName), ""},
                {TranslateHelper.DetailPanel.totalCountLabel().getString(),
                        String.valueOf(typeInfo == null ? 0 : typeInfo.p2pCount()), ""},
                {TranslateHelper.DetailPanel.channelCountLabel().getString(),
                        String.valueOf(typeInfo == null ? 0 : typeInfo.channelCount()), ""},
        };
    }

    private void renderChips(GuiGraphics g, Font font) {
        int x = getX() + PAD;
        int y = chipsY();
        for (Chip chip : buildChips()) {
            int w = font.width(chip.text) + 8;
            g.fill(x, y, x + w, y + CHIP_H, chip.background);
            g.drawString(font, chip.text, x + 4, y + 1, chip.foreground, false);
            x += w + 3;
            if (x > getX() + width - PAD) break;
        }
    }

    private record Chip(String text, int foreground, int background) {}

    private java.util.List<Chip> buildChips() {
        java.util.List<Chip> chips = new ArrayList<>();
        int okBg = ModConfigs.getClientConfig().paletteChipOkBg.get();
        int accentBg = ModConfigs.getClientConfig().paletteChipAccentBg.get();
        int warnBg = ModConfigs.getClientConfig().paletteChipWarnBg.get();
        int okFg = ModConfigs.getClientConfig().paletteStatusOk.get();
        int accentFg = ModConfigs.getClientConfig().paletteAccent.get();
        int warnFg = ModConfigs.getClientConfig().paletteStatusWarn.get();
        int neutralFg = ModConfigs.getClientConfig().paletteTextSecondary.get();

        if (selectedNode == null) return chips;

        if (selectedNode.type == NodeType.P2P && selectedP2PInfo != null) {
            P2PInfo info = selectedP2PInfo;
            if (!info.isActive()) {
                chips.add(new Chip(TranslateHelper.Status.notActive().getString(), neutralFg, warnBg));
            } else if (info.isConnected()) {
                chips.add(new Chip(TranslateHelper.Status.connected().getString(), okFg, okBg));
            } else {
                chips.add(new Chip(TranslateHelper.Status.notConnected().getString(), warnFg, warnBg));
            }
            chips.add(new Chip(
                    TranslateHelper.translate(info.isOutput()
                            ? TranslateHelper.Keys.TREE_ROLE_OUTPUT
                            : TranslateHelper.Keys.TREE_ROLE_INPUT).getString(),
                    accentFg, accentBg));
            P2PChannelUsage usage = P2PChannelUsage.ofDevice(info);
            if (usage.available()) {
                boolean tight = usage.remaining()
                        < ModConfigs.getClientConfig().treeChannelWarningThreshold.get();
                chips.add(new Chip(usage.used() + "/" + usage.max(),
                        tight ? warnFg : okFg, tight ? warnBg : okBg));
            }
            return chips;
        }

        if (selectedNode.type == NodeType.CHANNEL) {
            ChannelInfo info = selectedNode.data instanceof ChannelInfo c ? c : null;
            chips.add(new Chip((info == null ? 0 : info.p2pCount()) + " 台", accentFg, accentBg));
            P2PChannelUsage usage = P2PChannelUsage.ofChannel(info);
            if (usage.available()) {
                boolean tight = usage.remaining()
                        < ModConfigs.getClientConfig().treeChannelWarningThreshold.get();
                chips.add(new Chip(usage.used() + "/" + usage.max(),
                        tight ? warnFg : okFg, tight ? warnBg : okBg));
            }
            return chips;
        }

        P2PTypeInfo typeInfo = p2pTypeInfoMap == null ? null : p2pTypeInfoMap.get(selectedNode.typeName);
        chips.add(new Chip((typeInfo == null ? 0 : typeInfo.p2pCount()) + " 台", accentFg, accentBg));
        chips.add(new Chip((typeInfo == null ? 0 : typeInfo.channelCount()) + " 频段", neutralFg, okBg));
        return chips;
    }

    private void renderNameRow(GuiGraphics g, Font font) {
        int y = nameRowY();
        if (editing) {
            nameField.render(g, 0, 0, 0);
            return;
        }
        String name = displayName();
        int color = colorPrimary();
        g.drawString(font, name, getX() + PAD, y, color, false);
    }

    private void renderChannelBar(GuiGraphics g, int y) {
        P2PChannelUsage usage = currentUsage();
        if (!usage.available()) return;

        int barY = y + 10;
        int barX = getX() + PAD;
        int barW = width - 2 * PAD - 34;
        boolean tight = usage.remaining()
                < ModConfigs.getClientConfig().treeChannelWarningThreshold.get();

        g.fill(barX, barY, barX + barW, barY + 4, colorDivider());
        int filled = Math.round(barW * Math.min(1f, usage.used() / (float) usage.max()));
        g.fill(barX, barY, barX + filled, barY + 4, tight ? colorWarn() : colorOk());

        Font font = Minecraft.getInstance().font;
        String text = usage.used() + "/" + usage.max();
        g.drawString(font, text, getX() + width - PAD - font.width(text), y + 8, colorTertiary(), false);
    }

    // ==================== 按钮 ====================

    private void updateButtons() {
        String[] labels = new String[5];
        if (selectedNode == null) {
            for (FlatButton b : buttons) b.visible = false;
            return;
        }
        // 「传送」按钮的存在与否由服务端配置决定：允许传送才占位，否则整格隐藏。
        boolean teleportAllowed = ModConfigs.isTeleportAllowed();
        switch (selectedNode.type) {
            case P2P -> {
                labels[0] = TranslateHelper.Button.highlight().getString();
                labels[1] = editing ? TranslateHelper.Button.cancel().getString()
                        : TranslateHelper.Button.rename().getString();
                labels[2] = TranslateHelper.Button.assignFreq().getString();
                // 「设为输入端」只在设备当前是输出端时出现。反方向（输入端 → 输出端）
                // 不给入口：一条通道只能有一个输入端，把它改成输出就等于让整条通道
                // 没有输入端、彻底不工作，没有任何场景需要这个操作。
                labels[3] = canSwitchToInput(selectedP2PInfo)
                        ? TranslateHelper.Button.switchToInput().getString()
                        : null;
                labels[4] = teleportAllowed && selectedP2PInfo != null
                        ? TranslateHelper.Button.teleport().getString()
                        : null;
            }
            case CHANNEL -> {
                labels[0] = TranslateHelper.Button.highlight().getString();
                labels[1] = editing ? TranslateHelper.Button.cancel().getString()
                        : TranslateHelper.Button.rename().getString();
                labels[2] = TranslateHelper.Button.bind().getString();
                // 频段节点本身没有位置：高亮会把整条频段点亮，但「传送到某个频段」没有意义，
                // 因此这一格留空（不显示按钮），而不是退化成第一台输入端设备。
                labels[3] = null;
            }
            case P2P_TYPE -> {
                // 类型行只是分组标题：高亮 / 传送都得先挑一台具体设备，
                // 用「该类型第一台」代替只是猜，猜错了还得再回树里找，所以这两格直接不给。
                labels[0] = null;
                labels[1] = isMEP2P() ? TranslateHelper.Button.initP2P().getString() : null;
                labels[2] = TranslateHelper.Button.refresh().getString();
                labels[3] = null;
            }
            default -> { }
        }
        for (int i = 0; i < buttons.length; i++) {
            if (labels[i] == null) {
                buttons[i].visible = false;
            } else {
                buttons[i].visible = true;
                buttons[i].setLabel(Component.literal(labels[i]));
                buttons[i].active = true;
            }
        }
        repositionButtons();
    }

    private void onButtonClick(int index) {
        if (actionCallback == null || selectedNode == null) return;

        // 设备行 / 频段行的第 2 格是「改名」，编辑态下变成「取消」。
        if ((selectedNode.type == NodeType.P2P || selectedNode.type == NodeType.CHANNEL) && index == 1) {
            if (editing) {
                cancelEditing();
            } else {
                enterEditing();
            }
            return;
        }

        switch (selectedNode.type) {
            case P2P -> {
                P2PInfo info = selectedP2PInfo;
                if (info == null) return;
                switch (index) {
                    // 高亮与定位已合并：关界面 → 世界内高亮 → 转头朝向目标
                    case 0 -> actionCallback.onHighlightP2P(info);
                    case 2 -> actionCallback.onAssignFreq();
                    case 3 -> actionCallback.onSwitchToInput(info);
                    case 4 -> actionCallback.onTeleportP2P(info);
                    default -> { }
                }
            }
            case CHANNEL -> {
                String freq = selectedNode.frequency;
                if (freq == null) return;
                switch (index) {
                    case 0 -> actionCallback.onHighlightChannel(freq);
                    case 2 -> actionCallback.onUseAsTarget(freq);
                    default -> { }
                }
            }
            case P2P_TYPE -> {
                switch (index) {
                    case 1 -> actionCallback.onInitP2P();
                    case 2 -> actionCallback.onRefresh();
                    default -> { }
                }
            }
            default -> { }
        }
    }

    // ==================== 就地改名 ====================

    private void enterEditing() {
        editing = true;
        int fieldY = nameRowY() - 1;
        nameField.setX(getX() + PAD);
        nameField.setY(fieldY);
        nameField.setWidth(120);
        nameField.setHeight(10);
        String current = selectedNode.type == NodeType.P2P
                ? (selectedP2PInfo == null ? "" : selectedP2PInfo.name())
                : channelAlias(selectedNode.frequency);
        nameField.setValue(current == null ? "" : current);
        nameField.setVisible(true);
        nameField.setFocused(true);
        updateButtons();
    }

    private void cancelEditing() {
        editing = false;
        nameField.setVisible(false);
        nameField.setFocused(false);
        updateButtons();
    }

    private void onNameConfirm() {
        if (!editing) return;
        String value = nameField.getValue().trim();
        if (!value.isEmpty() && actionCallback != null && selectedNode != null) {
            if (selectedNode.type == NodeType.P2P && selectedP2PInfo != null) {
                actionCallback.onRenameP2P(selectedP2PInfo, value);
            } else if (selectedNode.type == NodeType.CHANNEL) {
                actionCallback.onRenameChannel(selectedNode.frequency, value);
            }
        }
        cancelEditing();
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!editing) return false;
        // Esc 取消编辑：以前只能再点一次“改名”，而那时按钮已经变成“取消”，语义绕了一圈
        if (keyCode == 256) {
            cancelEditing();
            return true;
        }
        return nameField.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (!editing) return false;
        return nameField.charTyped(codePoint, modifiers);
    }

    public boolean isEditing() {
        return editing;
    }

    // ==================== 输入 ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !active) return false;
        if (editing && nameField.isVisible() && nameField.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        for (FlatButton b : buttons) {
            if (b.visible && b.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible || !active) return false;
        for (FlatButton b : buttons) {
            if (b.visible && b.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        for (FlatButton b : buttons) {
            b.mouseMoved(mouseX, mouseY);
        }
    }

    // ==================== 辅助 ====================

    private String displayName() {
        if (selectedNode == null) return "";
        if (selectedNode.type == NodeType.P2P) {
            if (selectedP2PInfo == null) return "";
            return selectedP2PInfo.name() == null || selectedP2PInfo.name().isEmpty()
                    ? selectedP2PInfo.toShortString()
                    : selectedP2PInfo.name();
        }
        if (selectedNode.type == NodeType.CHANNEL) {
            return channelAlias(selectedNode.frequency);
        }
        return selectedNode.typeName == null ? "" : selectedNode.typeName;
    }

    private String channelAlias(String frequency) {
        ChannelInfo info = channelInfoMap == null || frequency == null ? null : channelInfoMap.get(frequency);
        if (info == null || info.alias() == null || info.alias().equals(frequency)) {
            return frequency == null ? "" : frequency;
        }
        return info.alias();
    }

    private int colorPrimary() {
        return ModConfigs.getClientConfig().paletteTextPrimary.get();
    }

    private int colorTertiary() {
        return ModConfigs.getClientConfig().paletteTextTertiary.get();
    }

    private int colorDivider() {
        return ModConfigs.getClientConfig().paletteDivider.get();
    }

    private int colorOk() {
        return ModConfigs.getClientConfig().paletteStatusOk.get();
    }

    private int colorWarn() {
        return ModConfigs.getClientConfig().paletteStatusWarn.get();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationOutput) {
        this.defaultButtonNarrationText(narrationOutput);
    }

    public interface ActionCallback {
        /** 高亮：关闭界面 → 世界内高亮 → 转头朝向目标（定位已并入）。 */
        void onHighlightP2P(P2PInfo info);

        void onHighlightChannel(String frequency);

        /** 传送：由服务端执行，受 {@code server.allowTeleport} 约束。频段节点没有自己的位置，不提供传送。 */
        void onTeleportP2P(P2PInfo info);

        void onUseAsTarget(String frequency);

        void onAssignFreq();

        /** 把当前输出端设备改成本频段的输入端，原输入端自动降级为输出。 */
        void onSwitchToInput(P2PInfo info);

        void onInitP2P();

        void onRenameP2P(P2PInfo info, String name);

        void onRenameChannel(String frequency, String alias);

        void onRefresh();
    }
}
