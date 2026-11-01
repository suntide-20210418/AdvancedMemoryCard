package com.suntide_20210418.advancedmemorycard.client.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

import com.suntide_20210418.advancedmemorycard.client.gui.GuiDraw;
import com.suntide_20210418.advancedmemorycard.client.gui.P2PTypeNames;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.p2p.ChannelInfo;
import com.suntide_20210418.advancedmemorycard.p2p.NodeType;
import com.suntide_20210418.advancedmemorycard.p2p.P2PChannelUsage;
import com.suntide_20210418.advancedmemorycard.p2p.P2PInfo;
import com.suntide_20210418.advancedmemorycard.p2p.P2PTypeInfo;
import com.suntide_20210418.advancedmemorycard.p2p.TreeNode;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;

/**
 * 详情检查器（新设计，1.7.10 自绘实现）。
 *
 * <p>
 * 旧版是等权重 {@code label: value} 八行，最该判断的「状态」排第 7 行，且一进改名态其余行
 * 全消失，玩家失去上下文。现在：状态做成头部 chips 永远第一眼可见；元数据降到 6/5/3 行定宽
 * 属性表；改名是就地编辑，属性表继续显示。
 *
 * <p>
 * 与基准 1.20.1 的差异：基准继承 AE2 的 {@code AbstractWidget} 并用 {@code ScreenStyle} 取色，
 * 这里用自绘 {@link Widget} + {@link GuiDraw} + {@code client.palette.*} 配置色；改名用 vanilla
 * {@link GuiTextField}（基准用 {@code ConfirmableTextField}）。布局偏移与基准像素级一致，
 * 以复用迁移过的美术资产。
 */
public class DetailPanelWidget extends Widget {

    private static final int PAD = 4;
    private static final int CHIP_H = 10;
    private static final int ROW_H = 14;
    private static final int BTN_H = 18;
    private static final int BTN_GAP = 6;
    private static final int LABEL_W = 44;
    private static final String ME_TYPE = P2PChannelUsage.ME_TYPE;

    private final GuiTextField nameField;
    private final FlatButton[] buttons = new FlatButton[5];

    private TreeNode selectedNode;
    private P2PInfo selectedP2PInfo;
    private Map<String, ChannelInfo> channelInfoMap;
    private Map<String, P2PTypeInfo> p2pTypeInfoMap;

    private boolean editing = false;
    private String selectionId;
    private ActionCallback actionCallback;

    public DetailPanelWidget(int x, int y, int width, int height) {
        super(x, y, width, height);

        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        nameField = new GuiTextField(font, 0, 0, 0, 10);
        nameField.setMaxStringLength(64);
        nameField.setVisible(false);

        for (int i = 0; i < buttons.length; i++) {
            final int idx = i;
            buttons[i] = new FlatButton(0, 0, 80, BTN_H, "", () -> onButtonClick(idx));
            buttons[i].visible = false;
        }
        repositionButtons();
    }

    public void setActionCallback(ActionCallback callback) {
        this.actionCallback = callback;
    }

    public void setSelectedNode(TreeNode node, Map<String, ChannelInfo> channelInfoMap,
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

    // ==================== 布局 ====================

    private int chipsY() {
        return y + 3;
    }

    private int nameRowY() {
        return y + 15;
    }

    private int dividerY() {
        return y + 29;
    }

    private int rowsTop() {
        return y + 32;
    }

    private int rowY(int index) {
        return rowsTop() + index * ROW_H;
    }

    private int buttonsTop() {
        int rows = getRowCount();
        int top = rowsTop() + rows * ROW_H + 4;
        if (hasChannelBar()) {
            top += 18;
        }
        return top;
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
            boolean alone = slot == visibleCount - 1 && col == 0;
            button.setX(x + PAD + (alone ? 0 : col * (bw + BTN_GAP)));
            button.setY(top + row * (BTN_H + BTN_GAP));
            button.setWidth(alone ? innerW : bw);
            button.setHeight(BTN_H);
            slot++;
        }
    }

    // ==================== 内容模型 ====================

    private boolean isMEP2P() {
        if (selectedP2PInfo != null) return selectedP2PInfo.isMEP2P();
        if (selectedNode != null && selectedNode.type == NodeType.CHANNEL
            && selectedNode.data instanceof ChannelInfo info) {
            return ME_TYPE.equals(info.p2pType());
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

    /** 把当前输出端设备改成本频段的输入端，原输入端自动降级为输出。 */
    private boolean canSwitchToInput(P2PInfo info) {
        if (info == null || !info.isOutput()) {
            return false;
        }
        String freq = info.frequency();
        return freq != null && !"0000000000000000".equals(freq);
    }

    private int getRowCount() {
        if (selectedNode == null) return 0;
        switch (selectedNode.type) {
            case P2P:
                return 6;
            case CHANNEL:
                return 5;
            case P2P_TYPE:
                return 3;
            default:
                return 0;
        }
    }

    /** 当前节点该显示的频道占用；只有 ME 类型才有值。 */
    private P2PChannelUsage currentUsage() {
        if (selectedNode == null) return P2PChannelUsage.NONE;
        if (selectedNode.type == NodeType.P2P) {
            return P2PChannelUsage.ofDevice(selectedP2PInfo);
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
    public void render(double mouseX, double mouseY, float partialTick) {
        if (!visible) return;
        FontRenderer font = GuiDraw.font();

        GuiDraw.fillRect(x, y, width, height, ModConfigs.getClientConfig().paletteSurface);

        if (selectedNode == null) {
            String hint = TranslateHelper.DetailPanel.placeholder();
            int w = GuiDraw.width(hint);
            GuiDraw.drawString(hint, x + (width - w) / 2, y + 40, colorTertiary());
            return;
        }

        renderChips(font);
        renderNameRow(font);
        GuiDraw.fill(x + PAD, dividerY(), x + width - PAD, dividerY() + 1, colorDivider());

        String[][] rows = buildRows();
        for (int i = 0; i < rows.length; i++) {
            int ry = rowY(i);
            GuiDraw.drawString(rows[i][0], x + PAD, ry + 3, colorTertiary());
            int valueColor = rows[i].length > 2 && "warn".equals(rows[i][2]) ? colorWarn() : colorPrimary();
            String value = rows[i][1];
            int maxValW = width - 2 * PAD - LABEL_W;
            if (GuiDraw.width(value) > maxValW) {
                value = GuiDraw.trim(value, maxValW);
            }
            GuiDraw.drawString(value, x + PAD + LABEL_W, ry + 3, valueColor);
        }

        if (hasChannelBar()) {
            renderChannelBar(buttonsTop() - 16);
        }

        for (FlatButton button : buttons) {
            button.draw(font, (int) mouseX, (int) mouseY);
        }
    }

    private String[][] buildRows() {
        if (selectedNode == null) return new String[0][];

        if (selectedNode.type == NodeType.P2P && selectedP2PInfo != null) {
            P2PInfo info = selectedP2PInfo;
            String alias = channelAlias(info.frequency(), info.p2pType());
            String freqText = alias.equals(info.frequency()) ? info.frequency() : alias + " · " + info.frequency();
            String role = TranslateHelper
                .translate(info.isOutput() ? TranslateHelper.Keys.STATUS_OUTPUT : TranslateHelper.Keys.STATUS_INPUT);
            String status = !info.isActive() ? TranslateHelper.Status.notActive()
                : (info.isConnected() ? TranslateHelper.Status.connected() : TranslateHelper.Status.notConnected());
            return new String[][] { { TranslateHelper.DetailPanel.frequencyLabel(), freqText, "" },
                { TranslateHelper.DetailPanel.typeLabel(), P2PTypeNames.string(info.p2pType()), "" },
                { TranslateHelper.DetailPanel.statusLabel(), status + " " + role, info.isConnected() ? "" : "warn" },
                { TranslateHelper.DetailPanel.dimensionLabel(), info.dimension(), "" },
                { TranslateHelper.DetailPanel.positionLabel(), info.position()
                    .getX() + ", "
                    + info.position()
                        .getY()
                    + ", "
                    + info.position()
                        .getZ(),
                    "" },
                { TranslateHelper.DetailPanel.directionLabel(), info.direction()
                    .name(), "" }, };
        }

        if (selectedNode.type == NodeType.CHANNEL) {
            ChannelInfo info = selectedNode.data instanceof ChannelInfo c ? c : null;
            int in = 0, out = 0;
            if (info != null && info.p2pInfoList() != null) {
                for (P2PInfo p : info.p2pInfoList()) {
                    if (p.isOutput()) out++;
                    else in++;
                }
            }
            String alias = info == null ? selectedNode.frequency : info.alias();
            String freqText = alias == null || alias.equals(selectedNode.frequency) ? selectedNode.frequency
                : alias + " · " + selectedNode.frequency;
            String type = info == null ? "unknown" : info.p2pType();
            return new String[][] { { TranslateHelper.DetailPanel.frequencyLabel(), freqText, "" },
                { TranslateHelper.DetailPanel.typeLabel(), P2PTypeNames.string(type), "" },
                { TranslateHelper.DetailPanel.p2pCountLabel(), String.valueOf(info == null ? 0 : info.p2pCount()), "" },
                { TranslateHelper.DetailPanel.inputCountLabel(), String.valueOf(in), "" },
                { TranslateHelper.DetailPanel.outputCountLabel(), String.valueOf(out), "" }, };
        }

        P2PTypeInfo typeInfo = p2pTypeInfoMap == null || selectedNode.typeName == null ? null
            : p2pTypeInfoMap.get(selectedNode.typeName);
        return new String[][] {
            { TranslateHelper.DetailPanel.typeLabel(),
                selectedNode.typeName == null ? "—" : P2PTypeNames.string(selectedNode.typeName), "" },
            { TranslateHelper.DetailPanel.totalCountLabel(), String.valueOf(typeInfo == null ? 0 : typeInfo.p2pCount()),
                "" },
            { TranslateHelper.DetailPanel.channelCountLabel(),
                String.valueOf(typeInfo == null ? 0 : typeInfo.channelCount()), "" }, };
    }

    private void renderChips(FontRenderer font) {
        int cx = x + PAD;
        int cy = chipsY();
        for (Chip chip : buildChips()) {
            int w = GuiDraw.width(chip.text) + 8;
            GuiDraw.fillRect(cx, cy, w, CHIP_H, chip.background);
            GuiDraw.drawString(chip.text, cx + 4, cy + 1, chip.foreground);
            cx += w + 3;
            if (cx > x + width - PAD) break;
        }
    }

    private static final class Chip {

        final String text;
        final int foreground;
        final int background;

        Chip(String text, int foreground, int background) {
            this.text = text;
            this.foreground = foreground;
            this.background = background;
        }
    }

    private List<Chip> buildChips() {
        List<Chip> chips = new ArrayList<>();
        int okBg = ModConfigs.getClientConfig().paletteChipOkBg;
        int accentBg = ModConfigs.getClientConfig().paletteChipAccentBg;
        int warnBg = ModConfigs.getClientConfig().paletteChipWarnBg;
        int okFg = ModConfigs.getClientConfig().paletteStatusOk;
        int accentFg = ModConfigs.getClientConfig().paletteAccent;
        int warnFg = ModConfigs.getClientConfig().paletteStatusWarn;
        int neutralFg = ModConfigs.getClientConfig().paletteTextSecondary;

        if (selectedNode == null) return chips;

        if (selectedNode.type == NodeType.P2P && selectedP2PInfo != null) {
            P2PInfo info = selectedP2PInfo;
            if (!info.isActive()) {
                chips.add(new Chip(TranslateHelper.Status.notActive(), neutralFg, warnBg));
            } else if (info.isConnected()) {
                chips.add(new Chip(TranslateHelper.Status.connected(), okFg, okBg));
            } else {
                chips.add(new Chip(TranslateHelper.Status.notConnected(), warnFg, warnBg));
            }
            chips.add(
                new Chip(
                    TranslateHelper.translate(
                        info.isOutput() ? TranslateHelper.Keys.STATUS_OUTPUT : TranslateHelper.Keys.STATUS_INPUT),
                    accentFg,
                    accentBg));
            P2PChannelUsage usage = P2PChannelUsage.ofDevice(info);
            if (usage.available()) {
                boolean tight = usage.remaining() < ModConfigs.getClientConfig().treeChannelWarningThreshold;
                chips.add(new Chip(usage.used() + "/" + usage.max(), tight ? warnFg : okFg, tight ? warnBg : okBg));
            }
            return chips;
        }

        if (selectedNode.type == NodeType.CHANNEL) {
            ChannelInfo info = selectedNode.data instanceof ChannelInfo c ? c : null;
            chips.add(new Chip((info == null ? 0 : info.p2pCount()) + " 台", accentFg, accentBg));
            P2PChannelUsage usage = P2PChannelUsage.ofChannel(info);
            if (usage.available()) {
                boolean tight = usage.remaining() < ModConfigs.getClientConfig().treeChannelWarningThreshold;
                chips.add(new Chip(usage.used() + "/" + usage.max(), tight ? warnFg : okFg, tight ? warnBg : okBg));
            }
            return chips;
        }

        P2PTypeInfo typeInfo = p2pTypeInfoMap == null ? null : p2pTypeInfoMap.get(selectedNode.typeName);
        chips.add(new Chip((typeInfo == null ? 0 : typeInfo.p2pCount()) + " 台", accentFg, accentBg));
        chips.add(new Chip((typeInfo == null ? 0 : typeInfo.channelCount()) + " 频段", neutralFg, okBg));
        return chips;
    }

    private void renderNameRow(FontRenderer font) {
        int ny = nameRowY();
        if (editing) {
            nameField.drawTextBox();
            return;
        }
        String name = displayName();
        int maxW = width - 2 * PAD;
        if (GuiDraw.width(name) > maxW) {
            name = GuiDraw.trim(name, maxW);
        }
        GuiDraw.drawString(name, x + PAD, ny, colorPrimary());
    }

    private void renderChannelBar(int topY) {
        P2PChannelUsage usage = currentUsage();
        if (!usage.available()) return;

        int barY = topY + 10;
        int barX = x + PAD;
        int barW = width - 2 * PAD - 34;
        boolean tight = usage.remaining() < ModConfigs.getClientConfig().treeChannelWarningThreshold;

        GuiDraw.fillRect(barX, barY, barW, 4, colorDivider());
        int filled = Math.round(barW * Math.min(1f, (float) usage.used() / (float) usage.max()));
        GuiDraw.fillRect(barX, barY, filled, 4, tight ? colorWarn() : colorOk());

        FontRenderer font = GuiDraw.font();
        String text = usage.used() + "/" + usage.max();
        GuiDraw.drawString(text, x + width - PAD - GuiDraw.width(text), topY + 8, colorTertiary());
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
            case P2P: {
                labels[0] = TranslateHelper.Button.highlight();
                labels[1] = editing ? TranslateHelper.Button.cancel() : TranslateHelper.Button.rename();
                labels[2] = TranslateHelper.Button.assignFreq();
                // 「设为输入端」只在设备当前是输出端时出现。反方向（输入端 → 输出端）
                // 不给入口：一条通道只能有一个输入端，把它改成输出就等于让整条通道
                // 没有输入端、彻底不工作，没有任何场景需要这个操作。
                labels[3] = canSwitchToInput(selectedP2PInfo) ? TranslateHelper.Button.switchToInput() : null;
                labels[4] = teleportAllowed && selectedP2PInfo != null ? TranslateHelper.Button.teleport() : null;
                break;
            }
            case CHANNEL: {
                labels[0] = TranslateHelper.Button.highlight();
                labels[1] = editing ? TranslateHelper.Button.cancel() : TranslateHelper.Button.rename();
                labels[2] = TranslateHelper.Button.bind();
                // 频段节点本身没有位置：高亮会把整条频段点亮，但「传送到某个频段」没有意义，
                // 因此这一格留空（不显示按钮），而不是退化成第一台输入端设备。
                labels[3] = null;
                break;
            }
            case P2P_TYPE: {
                // 类型行只是分组标题：高亮 / 传送都得先挑一台具体设备，
                // 用「该类型第一台」代替只是猜，猜错了还得再回树里找，所以这两格直接不给。
                labels[0] = null;
                labels[1] = isMEP2P() ? TranslateHelper.Button.initP2P() : null;
                labels[2] = TranslateHelper.Button.refresh();
                labels[3] = null;
                break;
            }
            default: {}
        }
        for (int i = 0; i < buttons.length; i++) {
            if (labels[i] == null) {
                buttons[i].visible = false;
            } else {
                buttons[i].visible = true;
                buttons[i].setLabel(labels[i]);
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
            case P2P: {
                P2PInfo info = selectedP2PInfo;
                if (info == null) return;
                switch (index) {
                    // 高亮与定位已合并：关界面 → 世界内高亮 → 转头朝向目标
                    case 0:
                        actionCallback.onHighlightP2P(info);
                        break;
                    case 2:
                        actionCallback.onAssignFreq();
                        break;
                    case 3:
                        actionCallback.onSwitchToInput(info);
                        break;
                    case 4:
                        actionCallback.onTeleportP2P(info);
                        break;
                    default:
                }
                break;
            }
            case CHANNEL: {
                String freq = selectedNode.frequency;
                if (freq == null) return;
                switch (index) {
                    case 0:
                        actionCallback.onHighlightChannel(freq);
                        break;
                    case 2:
                        actionCallback.onUseAsTarget(freq);
                        break;
                    default:
                }
                break;
            }
            case P2P_TYPE: {
                switch (index) {
                    case 1:
                        actionCallback.onInitP2P();
                        break;
                    case 2:
                        actionCallback.onRefresh();
                        break;
                    default:
                }
                break;
            }
            default:
        }
    }

    // ==================== 就地改名 ====================

    private void enterEditing() {
        editing = true;
        int fieldY = nameRowY() - 1;
        nameField.xPosition = x + PAD;
        nameField.yPosition = fieldY;
        nameField.width = Math.min(120, width - 2 * PAD);
        nameField.height = 10;
        String current = selectedNode.type == NodeType.P2P ? (selectedP2PInfo == null ? "" : selectedP2PInfo.name())
            : channelAlias(selectedNode.frequency, channelTypeOf(selectedNode));
        nameField.setText(current == null ? "" : current);
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
        String value = nameField.getText()
            .trim();
        if (!value.isEmpty() && actionCallback != null && selectedNode != null) {
            if (selectedNode.type == NodeType.P2P && selectedP2PInfo != null) {
                actionCallback.onRenameP2P(selectedP2PInfo, value);
            } else if (selectedNode.type == NodeType.CHANNEL) {
                actionCallback.onRenameChannel(selectedNode.frequency, value);
            }
        }
        cancelEditing();
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        if (!editing) return false;
        // Esc 取消编辑：以前只能再点一次“改名”，而那时按钮已经变成“取消”，语义绕了一圈
        if (keyCode == 1) {
            cancelEditing();
            return true;
        }
        if (keyCode == 28 || keyCode == 156) { // Enter / 小键盘 Enter
            onNameConfirm();
            return true;
        }
        nameField.textboxKeyTyped(typedChar, keyCode);
        return true;
    }

    public void updateScreen() {
        nameField.updateCursorCounter();
    }

    // ==================== 输入 ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        // 按钮优先（编辑态下「取消」也得能点）。
        for (FlatButton b : buttons) {
            if (b.visible && b.mouseClicked((int) mouseX, (int) mouseY, button)) return true;
        }
        // 再处理改名框：落在框内才把点击交给 GuiTextField 定位光标。
        if (editing && nameField.getVisible()
            && mouseX >= nameField.xPosition
            && mouseX < nameField.xPosition + nameField.width
            && mouseY >= nameField.yPosition
            && mouseY < nameField.yPosition + nameField.height) {
            nameField.mouseClicked((int) mouseX, (int) mouseY, button);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        for (FlatButton b : buttons) {
            if (b.visible && b.mouseReleased((int) mouseX, (int) mouseY, button)) return true;
        }
        return false;
    }

    // ==================== 辅助 ====================

    private String displayName() {
        if (selectedNode == null) return "";
        if (selectedNode.type == NodeType.P2P) {
            if (selectedP2PInfo == null) return "";
            return selectedP2PInfo.name() == null || selectedP2PInfo.name()
                .isEmpty() ? selectedP2PInfo.toShortString() : selectedP2PInfo.name();
        }
        if (selectedNode.type == NodeType.CHANNEL) {
            return channelAlias(selectedNode.frequency, channelTypeOf(selectedNode));
        }
        return selectedNode.typeName == null ? "" : selectedNode.typeName;
    }

    /** 频道节点的父节点是类型节点，频道 map 的键需要它才能命中。 */
    private String channelTypeOf(TreeNode node) {
        return node != null && node.parent != null ? node.parent.typeName : null;
    }

    private String channelAlias(String frequency, String p2pType) {
        ChannelInfo info = ChannelInfo.find(channelInfoMap, frequency, p2pType);
        if (info == null || info.alias() == null
            || info.alias()
                .equals(frequency)) {
            return frequency == null ? "" : frequency;
        }
        return info.alias();
    }

    private int colorPrimary() {
        return ModConfigs.getClientConfig().paletteTextPrimary;
    }

    private int colorTertiary() {
        return ModConfigs.getClientConfig().paletteTextTertiary;
    }

    private int colorDivider() {
        return ModConfigs.getClientConfig().paletteDivider;
    }

    private int colorOk() {
        return ModConfigs.getClientConfig().paletteStatusOk;
    }

    private int colorWarn() {
        return ModConfigs.getClientConfig().paletteStatusWarn;
    }

    public interface ActionCallback {

        /** 高亮：关闭界面 → 世界内高亮 → 转头朝向目标（定位已并入）。 */
        void onHighlightP2P(P2PInfo info);

        void onHighlightChannel(String frequency);

        /** 传送：由服务端执行，受 {@code server.allowTeleport} 约束。频段节点没有自己的位置，不提供传送。 */
        void onTeleportP2P(P2PInfo info);

        /** 把选中的频段设为配对目标（纯客户端状态，真正绑定时随 bind 一起上报）。 */
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
