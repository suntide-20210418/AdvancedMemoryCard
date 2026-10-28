package com.suntide_20210418.advancedmemorycard.client.gui.widgets;

import com.suntide_20210418.advancedmemorycard.client.gui.P2PTypeNames;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.p2p.ChannelInfo;
import com.suntide_20210418.advancedmemorycard.p2p.P2PChannelUsage;
import com.suntide_20210418.advancedmemorycard.p2p.P2PInfo;
import com.suntide_20210418.advancedmemorycard.utils.ActionStatus;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 配对条：源设备 → 目标频段。
 *
 * <p>这是整个改版的核心。旧版里“把哪台绑到哪个频段”是一件跨视图、跨滚动、只存在于
 * 服务端字段里的事；现在它是屏幕上一条常驻的、可读可改的配对流。
 *
 * <p>目标槽的下拉列表把“选频段”从一个隐藏的两段式操作压成一次点击 —— 这就是方案 A
 * 吸收方案 B 的部分：频段本质上就是一个可以从列表里挑的值。
 */
public class PairBarWidget extends AbstractWidget {

    private static final int SLOT_Y = 2;
    private static final int SLOT_H = 29;
    private static final int SRC_X = 2;
    private static final int SLOT_W = 192;
    private static final int ARROW_X = 196;
    private static final int ARROW_W = 28;
    private static final int TGT_X = 226;
    private static final int STATUS_Y = 36;

    private static final int DROPDOWN_MAX_ROWS = 7;
    private static final int DROPDOWN_ROW_H = 14;

    public interface Callback {
        /** 清空源设备（UI 侧只清显示，服务端仍保留原锚点） */
        void onSourceCleared();

        /** 目标频段被选中（点击下拉项或树里的频段行） */
        void onTargetChanged(String frequency);

        /** 请求为源设备分配一个全新频段 */
        void onNewFrequency();
    }

    private P2PInfo source;
    /** 配对源数量：>1 表示 Ctrl 多选 */
    private int sourceCount = 0;
    private String sourceType;
    private String targetFrequency;
    private String targetAlias;
    /** 目标频段所属的 P2P 类型；换配对源时用它判断要不要清空。 */
    private String targetType;
    private List<ChannelInfo> channels = new ArrayList<>();
    private int statusCode = ActionStatus.NONE;
    private String statusParam = "";

    private boolean dropdownOpen = false;
    private int dropdownScroll = 0;
    private int hoveredSlot = -1;      // 0 = 源槽, 1 = 目标槽
    private int hoveredRow = -1;

    private Callback callback;

    public PairBarWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    // ==================== 数据 ====================

    public void setSource(P2PInfo source) {
        setSources(source == null ? List.of() : List.of(source));
    }

    /** 设置配对源（可能多台）。多选时源槽显示数量与类型。 */
    public void setSources(List<P2PInfo> sources) {
        int count = sources == null ? 0 : sources.size();
        this.sourceCount = count;
        this.source = count == 0 ? null : sources.get(0);
        this.sourceType = this.source == null ? null : this.source.p2pType();
    }

    public int getSourceCount() {
        return sourceCount;
    }

    public P2PInfo getSource() {
        return source;
    }

    public void setChannels(List<ChannelInfo> channels) {
        List<ChannelInfo> sorted = new ArrayList<>(channels == null ? List.of() : channels);
        sorted.sort(Comparator.comparingInt(c -> {
            try {
                return Integer.parseInt(c.frequency(), 16);
            } catch (NumberFormatException e) {
                return Integer.MAX_VALUE;
            }
        }));
        this.channels = sorted;
    }

    public void setStatus(int code, String param) {
        this.statusCode = code;
        this.statusParam = param == null ? "" : param;
    }

    public void setTarget(String frequency, String alias) {
        setTarget(frequency, alias, null);
    }

    public void setTarget(String frequency, String alias, String type) {
        this.targetFrequency = frequency;
        this.targetAlias = alias;
        this.targetType = type;
        this.dropdownOpen = false;
    }

    /** 目标频段所属的 P2P 类型；未选目标时为 null。 */
    public String getTargetType() {
        return targetFrequency == null ? null : targetType;
    }

    public void clearTarget() {
        this.targetFrequency = null;
        this.targetAlias = null;
        this.targetType = null;
    }

    public String getTargetFrequency() {
        return targetFrequency;
    }

    public boolean isReady() {
        return source != null && targetFrequency != null && !"0000".equals(targetFrequency);
    }

    public boolean isDropdownOpen() {
        return dropdownOpen;
    }

    public void closeDropdown() {
        dropdownOpen = false;
        hoveredRow = -1;
    }

    /** 树里点中频段行时，直接把它填进目标槽 —— 这是 A 的主路径 */
    public void pickChannel(ChannelInfo info) {
        pickChannel(info, info == null ? null : info.p2pType());
    }

    /**
     * @param groupType 这个频段在树里挂在哪个类型分组下。
     *                  不能直接用 {@code info.p2pType()}：它取的是该频段里“第一台设备”的类型，
     *                  而 AE2 允许同一频段混装不同类型的隧道，于是界面上明明在「ME P2P 隧道」
     *                  分组里点的频段，内部类型却可能被判成物品隧道，接着点同组设备就被误清空。
     */
    public void pickChannel(ChannelInfo info, String groupType) {
        if (info == null) return;
        setTarget(info.frequency(), info.alias(), groupType != null ? groupType : info.p2pType());
    }

    // ==================== 渲染 ====================

    private int srcX() {
        return getX() + SRC_X;
    }

    private int tgtX() {
        return getX() + TGT_X;
    }

    private int slotY() {
        return getY() + SLOT_Y;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        Font font = Minecraft.getInstance().font;
        int surface = ModConfigs.getClientConfig().paletteSurface.get();
        int surfaceHover = ModConfigs.getClientConfig().paletteSurfaceHover.get();
        int primary = ModConfigs.getClientConfig().paletteTextPrimary.get();
        int secondary = ModConfigs.getClientConfig().paletteTextSecondary.get();
        int tertiary = ModConfigs.getClientConfig().paletteTextTertiary.get();
        int accent = ModConfigs.getClientConfig().paletteAccent.get();
        int divider = ModConfigs.getClientConfig().paletteDivider.get();

        hoveredSlot = -1;
        if (!dropdownOpen) {
            if (isOver(mouseX, mouseY, srcX(), slotY(), SLOT_W, SLOT_H)) hoveredSlot = 0;
            else if (isOver(mouseX, mouseY, tgtX(), slotY(), SLOT_W, SLOT_H)) hoveredSlot = 1;
        }

        // ---- 源槽 ----
        g.fill(srcX(), slotY(), srcX() + SLOT_W, slotY() + SLOT_H,
                hoveredSlot == 0 ? surfaceHover : surface);
        g.drawString(font, TranslateHelper.Pair.source().getString(), srcX() + 6, slotY() + 4, secondary, false);

        if (source != null) {
            String meta;
            String name;
            if (sourceCount > 1) {
                // 多选：显示台数 + 类型译名
                meta = P2PTypeNames.string(sourceType);
                name = TranslateHelper.Pair.sourceMulti(String.valueOf(sourceCount)).getString();
            } else {
                String freq = source.frequency();
                String role = TranslateHelper.translate(source.isOutput()
                        ? TranslateHelper.Keys.STATUS_OUTPUT
                        : TranslateHelper.Keys.STATUS_INPUT).getString();
                meta = freq + " " + role;
                name = source.name() == null || source.name().isEmpty()
                        ? source.toShortString()
                        : source.name();
            }
            g.drawString(font, meta, srcX() + SLOT_W - 6 - font.width(meta), slotY() + 4, secondary, false);
            name = fit(font, name, SLOT_W - 12);
            g.drawString(font, name, srcX() + 6, slotY() + 16, primary, false);

            // 清除按钮
            int cx = srcX() + SLOT_W - 10;
            int cy = slotY() + SLOT_H - 11;
            boolean overX = isOver(mouseX, mouseY, cx - 2, cy - 2, 10, 10);
            drawCross(g, cx, cy, overX ? accent : tertiary);
        } else {
            g.drawString(font, TranslateHelper.Pair.sourcePlaceholder().getString(),
                    srcX() + 6, slotY() + 16, tertiary, false);
        }

        // ---- 箭头 ----
        int ax = getX() + ARROW_X;
        int ay = slotY() + SLOT_H / 2;
        boolean ready = isReady();
        int arrowColor = ready ? accent : divider;
        g.fill(ax + 4, ay - 1, ax + 20, ay, arrowColor);
        g.fill(ax + 16, ay - 3, ax + 17, ay + 2, arrowColor);
        g.fill(ax + 17, ay - 2, ax + 18, ay + 1, arrowColor);
        g.fill(ax + 18, ay - 1, ax + 19, ay, arrowColor);

        // ---- 目标槽 ----
        g.fill(tgtX(), slotY(), tgtX() + SLOT_W, slotY() + SLOT_H,
                hoveredSlot == 1 || dropdownOpen ? surfaceHover : surface);
        g.drawString(font, TranslateHelper.Pair.target().getString(), tgtX() + 6, slotY() + 4, secondary, false);

        if (targetFrequency != null) {
            String meta = targetAlias != null && !targetAlias.equals(targetFrequency)
                    ? targetAlias
                    : "";
            meta = (meta.isEmpty() ? "" : meta + " · ") + targetFrequency;
            meta = fit(font, meta, SLOT_W - 24);
            g.drawString(font, meta, tgtX() + 6, slotY() + 16, primary, false);
            drawCross(g, tgtX() + SLOT_W - 10, slotY() + SLOT_H - 11, tertiary);
        } else {
            g.drawString(font, TranslateHelper.Pair.targetPlaceholder().getString(),
                    tgtX() + 6, slotY() + 16, tertiary, false);
        }
        // 下拉箭头
        int chevX = tgtX() + SLOT_W - 14;
        int chevY = slotY() + 6;
        g.fill(chevX, chevY, chevX + 5, chevY + 1, secondary);
        g.fill(chevX + 1, chevY + 1, chevX + 4, chevY + 2, secondary);
        g.fill(chevX + 2, chevY + 2, chevX + 3, chevY + 3, secondary);

        // ---- 状态行 ----
        int sev = ActionStatus.severity(statusCode);
        int statusColor = switch (sev) {
            case ActionStatus.SEV_OK -> ModConfigs.getClientConfig().paletteStatusOk.get();
            case ActionStatus.SEV_WARN -> ModConfigs.getClientConfig().paletteStatusWarn.get();
            case ActionStatus.SEV_ERROR -> ModConfigs.getClientConfig().paletteStatusDanger.get();
            default -> secondary;
        };
        int dotX = getX() + 2;
        int dotY = getY() + STATUS_Y + 2;
        g.fill(dotX, dotY, dotX + 5, dotY + 5, statusColor);
        Component message = ActionStatus.message(statusCode, statusParam);
        g.drawString(font, message, getX() + 11, getY() + STATUS_Y, statusColor, false);
    }

    /** 浮层必须在所有 widget 之后画，否则会被树/面板盖住。由 Screen 在 super.render() 后调用。 */
    public void renderOverlay(GuiGraphics g, int mouseX, int mouseY) {
        if (!dropdownOpen) return;

        Font font = Minecraft.getInstance().font;
        int x = tgtX();
        int y = slotY() + SLOT_H + 1;
        int w = SLOT_W;
        List<Row> rows = buildRows();
        int visibleRows = Math.min(rows.size(), DROPDOWN_MAX_ROWS);
        int h = visibleRows * DROPDOWN_ROW_H + 4;

        int shadow = ModConfigs.getClientConfig().paletteOverlayShadow.get();
        int bg = ModConfigs.getClientConfig().paletteOverlayBg.get();
        int border = ModConfigs.getClientConfig().paletteOverlayBorder.get();
        int primary = ModConfigs.getClientConfig().paletteTextPrimary.get();
        int secondary = ModConfigs.getClientConfig().paletteTextSecondary.get();
        int accent = ModConfigs.getClientConfig().paletteAccent.get();

        g.fill(x + 2, y + 2, x + w + 2, y + h + 2, shadow);
        g.fill(x, y, x + w, y + h, bg);
        // 1px 描边
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);

        int maxScroll = Math.max(0, rows.size() - DROPDOWN_MAX_ROWS);
        dropdownScroll = Math.max(0, Math.min(dropdownScroll, maxScroll));

        hoveredRow = -1;
        for (int i = 0; i < visibleRows; i++) {
            int idx = dropdownScroll + i;
            Row row = rows.get(idx);
            int rowY = y + 2 + i * DROPDOWN_ROW_H;
            boolean over = mouseX >= x && mouseX < x + w && mouseY >= rowY && mouseY < rowY + DROPDOWN_ROW_H;
            if (over) hoveredRow = idx;

            if (row.isNewFrequency) {
                g.fill(x + 1, rowY - 1, x + w - 1, rowY, border);
            }
            if (over) {
                g.fill(x + 1, rowY, x + w - 1, rowY + DROPDOWN_ROW_H, accent);
            }
            int color = over ? 0xFFFFFFFF : (row.current ? accent : primary);
            String label = row.label;
            if (!row.isNewFrequency && row.frequency != null) {
                String freqText = row.frequency;
                int freqW = font.width(freqText);
                g.drawString(font, freqText, x + w - 6 - freqW, rowY + 3,
                        over ? 0xFFD7DEEC : secondary, false);
                label = fit(font, label, w - 12 - freqW - 6);
            }
            g.drawString(font, label, x + 6, rowY + 3, color, false);
        }

        // 滚动提示
        if (rows.size() > DROPDOWN_MAX_ROWS) {
            int trackX = x + w - 3;
            g.fill(trackX, y + 2, trackX + 1, y + h - 2, border);
            int thumbH = Math.max(6, (h - 4) * DROPDOWN_MAX_ROWS / rows.size());
            int thumbY = y + 2 + (h - 4 - thumbH) * dropdownScroll / Math.max(1, maxScroll);
            g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, accent);
        }
    }

    // ==================== 交互 ====================

    /** @return true 表示浮层吃掉了这次点击 */
    public boolean handleOverlayClick(double mouseX, double mouseY, int button) {
        if (!dropdownOpen) return false;
        if (button != 0) {
            dropdownOpen = false;
            return false;
        }

        int x = tgtX();
        int y = slotY() + SLOT_H + 1;
        int w = SLOT_W;
        List<Row> rows = buildRows();
        int visibleRows = Math.min(rows.size(), DROPDOWN_MAX_ROWS);
        int h = visibleRows * DROPDOWN_ROW_H + 4;

        // 点在浮层外面：不在这里收。收起交给 widget 自己的 mouseClicked 处理，
        // 否则“先关再开”会把目标槽点成永远展开（第一次点关、第二次点又被打开）。
        if (mouseX < x || mouseX >= x + w || mouseY < y || mouseY >= y + h) {
            return false;
        }

        int rowIndex = (int) ((mouseY - y - 2) / DROPDOWN_ROW_H);
        int idx = dropdownScroll + rowIndex;
        if (idx >= 0 && idx < rows.size()) {
            Row row = rows.get(idx);
            if (row.isNewFrequency) {
                if (callback != null) callback.onNewFrequency();
            } else if (row.frequency != null) {
                setTarget(row.frequency, row.alias, row.type);
                if (callback != null) callback.onTargetChanged(row.frequency);
            }
        }
        dropdownOpen = false;
        return true;
    }

    public boolean handleOverlayScroll(double delta) {
        if (!dropdownOpen) return false;
        dropdownScroll -= (int) Math.signum(delta);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !active) return false;

        // 源槽的 ×
        if (source != null
                && isOver(mouseX, mouseY, srcX() + SLOT_W - 12, slotY() + SLOT_H - 13, 12, 12)) {
            if (callback != null) callback.onSourceCleared();
            return true;
        }

        // 目标槽的 ×
        if (targetFrequency != null
                && isOver(mouseX, mouseY, tgtX() + SLOT_W - 12, slotY() + SLOT_H - 13, 12, 12)) {
            clearTarget();
            return true;
        }

        // 目标槽：再点一次收起频段列表
        if (isOver(mouseX, mouseY, tgtX(), slotY(), SLOT_W, SLOT_H)) {
            dropdownOpen = !dropdownOpen;
            dropdownScroll = 0;
            return true;
        }

        // 点别处：收起频段列表，但不吃掉这次点击，树和面板照常响应
        if (dropdownOpen) {
            dropdownOpen = false;
        }

        if (isOver(mouseX, mouseY, srcX(), slotY(), SLOT_W, SLOT_H)) {
            // 点源槽本身不做事，但吃掉点击避免穿透到下层
            return true;
        }
        return false;
    }

    // ==================== 内部 ====================

    /** 没有别名时用「台数 + 剩余」当主文本；非 ME 频段没有频道概念，只报台数。 */
    private static String channelSummary(ChannelInfo info) {
        P2PChannelUsage usage = P2PChannelUsage.ofChannel(info);
        return usage.available()
                ? TranslateHelper.translate(TranslateHelper.Keys.TREE_SUMMARY_CHANNEL,
                        info.p2pCount(), usage.remaining()).getString()
                : TranslateHelper.translate(TranslateHelper.Keys.TREE_SUMMARY_CHANNEL_PLAIN,
                        info.p2pCount()).getString();
    }

    private static boolean containsType(ChannelInfo info, String type) {
        if (type == null) return true;
        if (info == null || info.p2pInfoList() == null) return false;
        for (P2PInfo p : info.p2pInfoList()) {
            if (p != null && type.equals(p.p2pType())) return true;
        }
        return false;
    }

    private static final class Row {
        String frequency;
        String alias;
        String type;
        String label;
        boolean current;
        boolean isNewFrequency;
    }

    private List<Row> buildRows() {
        List<Row> rows = new ArrayList<>();
        for (ChannelInfo info : channels) {
            if ("0000".equals(info.frequency())) continue;
            // 只在同类型里挑：把 item P2P 绑到 ME 通道上没有意义。
            // 判定看“这个频段里有没有同类型设备”，不看 ChannelInfo 的“第一台设备类型”，
            // 否则混装频段会被整条漏掉或多算。
            if (source != null && !containsType(info, source.p2pType())) {
                continue;
            }
            Row row = new Row();
            row.frequency = info.frequency();
            row.type = source != null ? source.p2pType() : info.p2pType();
            row.alias = info.alias();
            String alias = info.alias();
            row.label = alias == null || alias.equals(info.frequency())
                    ? info.frequency()
                    : alias;
            // 别名本身已经占了主文本，右侧再显示 hex 会重复
            if (alias != null && !alias.equals(info.frequency())) {
                row.label = alias;
            } else {
                row.label = channelSummary(info);
            }
            row.current = info.frequency().equals(targetFrequency);
            rows.add(row);
        }

        // 多选时“新建频段”没有意义：服务端只对当前那一个源设备生效
        if (sourceCount <= 1) {
            Row newRow = new Row();
            newRow.isNewFrequency = true;
            newRow.label = TranslateHelper.Pair.newFrequency().getString();
            rows.add(newRow);
        }

        if (rows.size() == 1) {
            Row empty = new Row();
            empty.label = TranslateHelper.Pair.noChannel().getString();
            rows.add(0, empty);
        }
        return rows;
    }

    private void drawCross(GuiGraphics g, int x, int y, int color) {
        for (int i = 0; i < 5; i++) {
            g.fill(x + i, y + i, x + i + 1, y + i + 1, color);
            g.fill(x + 4 - i, y + i, x + 5 - i, y + i + 1, color);
        }
    }

    private boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String fit(Font font, String text, int maxWidth) {
        if (text == null) return "";
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "…";
        int budget = maxWidth - font.width(ellipsis);
        if (budget <= 0) return "";
        StringBuilder sb = new StringBuilder();
        int acc = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            int cw = font.width(ch);
            if (acc + cw > budget) break;
            sb.append(ch);
            acc += cw;
            i += Character.charCount(cp);
        }
        return sb + ellipsis;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationOutput) {
        this.defaultButtonNarrationText(narrationOutput);
    }
}
