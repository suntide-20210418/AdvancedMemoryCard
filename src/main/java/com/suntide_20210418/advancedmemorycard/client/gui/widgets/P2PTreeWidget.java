package com.suntide_20210418.advancedmemorycard.client.gui.widgets;

import com.suntide_20210418.advancedmemorycard.p2p.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.*;
import java.util.function.Consumer;

/**
 * P2P树形控件
 * 显示 P2P类型 -> 频段 -> P2P 的树形结构
 */
public class P2PTreeWidget extends AbstractWidget {

    // 颜色常量
    private static final int COLOR_BG = 0xFFADB0C4;
    private static final int COLOR_SELECTED = 0xFF4A6EA9;
    private static final int COLOR_HOVER = 0xFF3A5A8A;
    private static final int COLOR_TEXT = 0xFFFFFFFF;

    // 缩进像素
    private static final int INDENT_WIDTH = 2;
    // 行高
    private static final int ROW_HEIGHT = 12;
    // 图标和文本间距
    private static final int ICON_TEXT_SPACING = 4;
    // 展开/收起图标宽度
    private static final int EXPAND_ICON_WIDTH = 2;

    // 数据源
    private Map<String, P2PTypeInfo> p2pTypeInfoMap;
    private Map<Short, ChannelInfo> channelInfoMap;
    private Map<P2PPosition, P2PInfo> p2pInfoMap;

    // 树节点列表（平铺展示）
    private List<TreeNode> flatNodes = new ArrayList<>();

    private int currentScrollOffset = 0;
    private int totalContentHeight = 0;

    // 选中项
    private TreeNode selectedNode = null;
    private P2PInfo selectedP2PInfo = null;

    // 选择监听器
    private Consumer<P2PInfo> selectionListener;

    // 悬停项
    private TreeNode hoveredNode = null;


    public P2PTreeWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("P2P Tree"));
    }

    public static int getRowHeight() {
        return ROW_HEIGHT;
    }

    /**
     * 更新数据并重建树
     */
    public void updateData(Map<String, P2PTypeInfo> p2pTypeInfoMap,
                           Map<Short, ChannelInfo> channelInfoMap,
                           Map<P2PPosition, P2PInfo> p2pInfoMap) {
        this.p2pTypeInfoMap = p2pTypeInfoMap != null ? p2pTypeInfoMap : new HashMap<>();
        this.channelInfoMap = channelInfoMap != null ? channelInfoMap : new HashMap<>();
        this.p2pInfoMap = p2pInfoMap != null ? p2pInfoMap : new HashMap<>();

        rebuildTree();
    }

    /**
     * 设置选择监听器
     */
    public void setSelectionListener(Consumer<P2PInfo> listener) {
        this.selectionListener = listener;
    }

    /**
     * 重建树结构（保留展开和选中状态）
     */
    private void rebuildTree() {
        // 保存当前展开状态
        Set<String> expandedTypeIds = new HashSet<>();
        saveExpandedState(expandedTypeIds);

        // 保存当前选中的节点ID
        String selectedNodeId = getSelectedNodeId();

        // 重建树
        flatNodes.clear();

        // 按 P2P 类型排序
        List<String> sortedTypes = new ArrayList<>(p2pTypeInfoMap.keySet());
        sortedTypes.sort(String::compareTo);

        for (String typeName : sortedTypes) {
            P2PTypeInfo typeInfo = p2pTypeInfoMap.get(typeName);
            if (typeInfo == null) continue;

            TreeNode typeNode = new TreeNode();
            typeNode.type = NodeType.P2P_TYPE;
            typeNode.typeName = typeName;
            typeNode.displayComponent = formatTypeDisplayName(typeName, typeInfo);  // 存储 Component
            typeNode.expanded = expandedTypeIds.contains(typeNode.getNodeId());
            typeNode.data = typeInfo;

            flatNodes.add(typeNode);

            if (typeNode.expanded) {
                // 获取该类型下的所有频段
                List<ChannelInfo> channels = typeInfo.channelInfoList();
                if (channels != null) {
                    channels.sort(Comparator.comparingInt(ChannelInfo::frequency));

                    for (ChannelInfo channelInfo : channels) {
                        TreeNode channelNode = new TreeNode();
                        channelNode.type = NodeType.CHANNEL;
                        channelNode.frequency = channelInfo.frequency();
                        channelNode.displayComponent = formatChannelDisplayName(channelInfo);
                        // 恢复展开状态
                        channelNode.expanded = expandedTypeIds.contains(channelNode.getNodeId());
                        channelNode.data = channelInfo;
                        channelNode.parent = typeNode;

                        flatNodes.add(channelNode);

                        if (channelNode.expanded) {
                            // 添加该频段下的所有 P2P
                            List<P2PInfo> p2ps = channelInfo.p2pInfoList();
                            if (p2ps != null) {
                                p2ps.sort((a, b) -> Boolean.compare(a.isOutput(), b.isOutput()));

                                for (P2PInfo p2pInfo : p2ps) {
                                    TreeNode p2pNode = new TreeNode();
                                    p2pNode.type = NodeType.P2P;
                                    p2pNode.p2pInfo = p2pInfo;
                                    p2pNode.displayComponent = formatP2PDisplayName(p2pInfo);
                                    p2pNode.parent = channelNode;
                                    p2pNode.frequency = p2pInfo.frequency();
                                    flatNodes.add(p2pNode);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 恢复选中状态
        restoreSelectedNode(selectedNodeId);
    }

    /**
     * 保存当前展开状态
     */
    private void saveExpandedState(Set<String> expandedNodeIds) {
        for (TreeNode node : flatNodes) {
            if (node.expanded) {
                expandedNodeIds.add(node.getNodeId());
            }
        }
    }

    public int getScrollOffset() {
        return currentScrollOffset;
    }

    // 添加新的方法供外部设置滚动偏移
    public void setScrollOffset(int scrollOffset) {
        this.currentScrollOffset = scrollOffset;
    }

    // 添加获取总内容高度的方法
    public int getTotalContentHeight() {
        return flatNodes.size() * ROW_HEIGHT;
    }

    /**
     * 获取当前选中节点的唯一标识
     */
    private String getSelectedNodeId() {
        if (selectedNode == null) return null;
        return selectedNode.getNodeId();
    }

    /**
     * 恢复选中的节点
     */
    private void restoreSelectedNode(String selectedNodeId) {
        if (selectedNodeId == null) {
            selectedNode = null;
            selectedP2PInfo = null;
            return;
        }

        for (TreeNode node : flatNodes) {
            if (selectedNodeId.equals(node.getNodeId())) {
                selectedNode = node;
                selectedP2PInfo = node.type == NodeType.P2P ? node.p2pInfo : null;
                if (selectionListener != null && selectedP2PInfo == null) {
                    selectionListener.accept(null);
                }
                break;
            }
        }

        if (selectedNode == null) {
            selectedP2PInfo = null;
            if (selectionListener != null) {
                selectionListener.accept(null);
            }
        }
    }

    /**
     * 切换节点的展开/收起状态
     */
    private void toggleNode(TreeNode node) {
        if (node.type == NodeType.P2P) return; // P2P 不可展开

        node.expanded = !node.expanded;
        rebuildTree();
    }

    /**
     * 格式化类型显示名称（返回 Component）
     */
    private Component formatTypeDisplayName(String typeName, P2PTypeInfo typeInfo) {
        int p2pCount = typeInfo.p2pCount();
        int channelCount = typeInfo.channelCount();

        return Component.literal("📁 ")
                .append(Component.literal(typeName).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(String.format(" (%d P2P, %d Freq)", p2pCount, channelCount))
                        .withStyle(ChatFormatting.GRAY));
    }

    /**
     * 格式化频段显示名称
     */
    private Component formatChannelDisplayName(ChannelInfo channelInfo) {
        String alias = channelInfo.alias();
        short freq = channelInfo.frequency();
        String hexFreq = Integer.toHexString(freq & 0xFFFF);
        int channelRemaining = channelInfo.channelRemaining();
        int p2pCount = channelInfo.p2pCount();

        MutableComponent freqComponent;

        if (alias != null && !alias.equals(String.valueOf(freq))) {
            freqComponent = Component.literal(alias).withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(String.format(" (%s)", hexFreq)).withStyle(ChatFormatting.DARK_AQUA));
        } else {
            freqComponent = Component.literal(hexFreq).withStyle(ChatFormatting.AQUA);
        }

        // 剩余频道数量颜色
        ChatFormatting channelColor = channelRemaining > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;

        return Component.literal("📡 ")
                .append(freqComponent)
                .append(Component.literal(String.format(" [%d P2P | ⚡%d]", p2pCount, channelRemaining))
                        .withStyle(channelColor));
    }

    /**
     * 格式化 P2P 显示名称
     */
    private Component formatP2PDisplayName(P2PInfo p2pInfo) {
        String name = p2pInfo.name().isEmpty() ? p2pInfo.toShortString() : p2pInfo.name() + "-" + p2pInfo.toShortString();
        boolean isOutput = p2pInfo.isOutput();
        boolean isActive = p2pInfo.isActive();
        boolean isConnected = p2pInfo.isConnected();
        boolean isPendingBind = p2pInfo.isPendingBind();

        // 状态图标和颜色
        MutableComponent statusComponent;
        if (!isActive) {
            statusComponent = Component.literal("⭕").withStyle(ChatFormatting.GRAY);
        } else if (isConnected) {
            statusComponent = Component.literal("🟢").withStyle(ChatFormatting.GREEN);
        } else {
            statusComponent = Component.literal("🔴").withStyle(ChatFormatting.RED);
        }

        // 方向图标
        MutableComponent directionComponent = isOutput ?
                Component.literal("📤").withStyle(ChatFormatting.YELLOW) :
                Component.literal("📥").withStyle(ChatFormatting.BLUE);

        // 名称颜色（待绑定的特殊标记）
        MutableComponent nameComponent;
        if (isPendingBind) {
            nameComponent = Component.literal(truncateString(name, 20))
                    .withStyle(ChatFormatting.LIGHT_PURPLE);
        } else if (isActive && isConnected) {
            nameComponent = Component.literal(truncateString(name, 20))
                    .withStyle(ChatFormatting.WHITE);
        } else {
            nameComponent = Component.literal(truncateString(name, 20))
                    .withStyle(ChatFormatting.YELLOW);
        }

        MutableComponent result = Component.literal("")
                .append(statusComponent)
                .append(directionComponent)
                .append(Component.literal(" ").withStyle(ChatFormatting.RESET))
                .append(nameComponent);

        if (isPendingBind) {
            result.append(Component.literal(" ◆ ").withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        return result;
    }

    /**
     * 截断字符串
     */
    private String truncateString(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }

    /**
     * 绘制截断的 Component 文本
     */
    private void drawTruncatedComponent(GuiGraphics guiGraphics, Component component, int x, int y, int maxWidth) {
        Font font = Minecraft.getInstance().font;
        int textWidth = font.width(component);

        if (textWidth <= maxWidth) {
            // 不需要截断，直接绘制
            guiGraphics.drawString(font, component, x, y, -1, false);
        } else {
            // 使用 Font.getSplitter() 来截断
            FormattedText truncated = font.getSplitter().headByWidth(component, maxWidth - font.width("..."), Style.EMPTY);
            guiGraphics.drawString(font, truncated.getString(), x, y,
                    component.getStyle().getColor() != null ? component.getStyle().getColor().getValue() : 0xFFFFFF,
                    false);
            guiGraphics.drawString(font, "...", x + font.width(truncated.getString()), y,
                    component.getStyle().getColor() != null ? component.getStyle().getColor().getValue() : 0xFFFFFF,
                    false);
        }
    }

    // 修改 getNodeAt 方法，移除滚动条检测
    private TreeNode getNodeAt(double mouseX, double mouseY) {
        if (mouseX < getX() || mouseX > getX() + width ||
                mouseY < getY() || mouseY > getY() + height) {
            return null;
        }

        int relativeY = (int) (mouseY - getY() + currentScrollOffset);
        int index = relativeY / ROW_HEIGHT;

        if (index >= 0 && index < flatNodes.size()) {
            return flatNodes.get(index);
        }
        return null;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        TreeNode clicked = getNodeAt(mouseX, mouseY);
        if (clicked != null) {
            // 可展开的节点类型
            toggleNode(clicked);
            selectNode(clicked);
        }
    }

    /**
     * 选中节点
     */
    private void selectNode(TreeNode node) {
        this.selectedNode = node;

        if (node.type == NodeType.P2P) {
            this.selectedP2PInfo = node.p2pInfo;
            if (selectionListener != null) {
                selectionListener.accept(node.p2pInfo);
            }
        } else {
            this.selectedP2PInfo = null;
            if (selectionListener != null) {
                selectionListener.accept(null);
            }
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        // 更新悬停项
        hoveredNode = getNodeAt(mouseX, mouseY);

        // 绘制背景
        renderBackground(guiGraphics);

        // 启用 scissor 裁剪
        guiGraphics.enableScissor(getX(), getY(), getX() + width, getY() + height);

        // 绘制树内容
        renderTreeContent(guiGraphics, mouseX, mouseY);

        // 禁用 scissor
        guiGraphics.disableScissor();
    }

    /**
     * 绘制背景
     */
    private void renderBackground(GuiGraphics guiGraphics) {
        // 使用 GUI 默认背景色（透明或半透明，让 JSON 定义的背景透出）
        // 这里绘制半透明深色作为基础背景
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, COLOR_BG);
    }

    /**
     * 绘制树内容（修改后，排除滚动条区域）
     */
    private void renderTreeContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (flatNodes.isEmpty()) {
            Component emptyText = Component.translatable("gui.advancedmemorycard.p2p_tree.empty")
                    .withStyle(ChatFormatting.BLACK);
            int textWidth = Minecraft.getInstance().font.width(emptyText);
            guiGraphics.drawString(Minecraft.getInstance().font, emptyText,
                    getX() + (width - textWidth) / 2,
                    getY() + height / 2 - 4,
                    -1, false);
            return;
        }

        int startIndex = currentScrollOffset / ROW_HEIGHT;
        int endIndex = Math.min(flatNodes.size(), startIndex + (height + ROW_HEIGHT - 1) / ROW_HEIGHT + 1);
        int yOffset = getY() - currentScrollOffset + startIndex * ROW_HEIGHT;

        int clickableWidth = width;

        for (int i = startIndex; i < endIndex; i++) {
            TreeNode node = flatNodes.get(i);
            int rowY = yOffset + (i - startIndex) * ROW_HEIGHT;

            if (rowY + ROW_HEIGHT < getY() || rowY > getY() + height) {
                continue;
            }

            int indent = getIndent(node);
            int iconX = getX() + indent;
            int textX = getX() + indent + EXPAND_ICON_WIDTH + ICON_TEXT_SPACING;
            int textY = rowY + (ROW_HEIGHT - 8) / 2;

            // 绘制选中背景
            if (node == selectedNode) {
                guiGraphics.fill(getX(), rowY, getX() + clickableWidth, rowY + ROW_HEIGHT, COLOR_SELECTED);
            } else if (node == hoveredNode) {
                guiGraphics.fill(getX(), rowY, getX() + clickableWidth, rowY + ROW_HEIGHT, COLOR_HOVER);
            }

            // 绘制展开/收起图标（对于可展开的节点）
            if (node.type == NodeType.P2P_TYPE || node.type == NodeType.CHANNEL) {
                String iconSymbol = node.expanded ? "▼" : "▶";
                MutableComponent iconComponent = Component.literal(iconSymbol)
                        .withStyle(ChatFormatting.GOLD);
                guiGraphics.drawString(Minecraft.getInstance().font, iconComponent,
                        iconX, rowY + (ROW_HEIGHT - 8) / 2, -1, false);
            }

            // 绘制文本
            Component displayComponent = getDisplayComponent(node);
            drawTruncatedComponent(guiGraphics, displayComponent, textX, textY,
                    clickableWidth - indent - EXPAND_ICON_WIDTH - ICON_TEXT_SPACING);
        }
    }

    private Component getDisplayComponent(TreeNode node) {
        return switch (node.type) {
            case P2P_TYPE -> {
                if (node.data instanceof P2PTypeInfo typeInfo) {
                    yield formatTypeDisplayName(node.typeName, typeInfo);
                }
                yield Component.literal(node.typeName);
            }
            case CHANNEL -> {
                if (node.data instanceof ChannelInfo) {
                    yield formatChannelDisplayName((ChannelInfo) node.data);
                }
                yield Component.literal(String.valueOf(node.frequency));
            }
            case P2P -> {
                if (node.p2pInfo != null) {
                    yield formatP2PDisplayName(node.p2pInfo);
                }
                yield Component.literal("Unknown P2P");
            }
        };
    }

    /**
     * 获取缩进级别（基于父链深度）
     */
    private int getIndent(TreeNode node) {
        int depth = 0;
        TreeNode current = node.parent;
        while (current != null) {
            depth++;
            current = current.parent;
        }
        return depth * INDENT_WIDTH;
    }

    /**
     * 绘制截断文本
     */
    private void drawTruncatedText(GuiGraphics guiGraphics, String text, int x, int y, int maxWidth) {
        var font = Minecraft.getInstance().font;
        String displayText = font.width(text) > maxWidth ?
                font.plainSubstrByWidth(text, maxWidth - font.width("...")) + "..." : text;
        guiGraphics.drawString(font, displayText, x, y, COLOR_TEXT, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationOutput) {
        this.defaultButtonNarrationText(narrationOutput);
    }
}