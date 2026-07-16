package com.suntide_20210418.advancedmemorycard.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 客户端配置
 * <ul>
 *   <li>GUI 界面颜色设置</li>
 *   <li>GUI 布局尺寸设置</li>
 *   <li>渲染颜色设置</li>
 *   <li>高亮行为设置</li>
 * </ul>
 */
public class ClientConfig {
    // ========== 高亮行为 ==========
    public final ModConfigSpec.ConfigValue<Boolean> rotateHeadOnHighlight;
    public final ModConfigSpec.IntValue p2pHighlightDurationSec;

    // ========== P2P 树控件颜色 ==========
    public final ModConfigSpec.IntValue treeColorBg;
    public final ModConfigSpec.IntValue treeColorSelected;
    public final ModConfigSpec.IntValue treeColorHover;

    // ========== P2P 树控件布局 ==========
    public final ModConfigSpec.IntValue treeIndentWidth;
    public final ModConfigSpec.IntValue treeRowHeight;
    public final ModConfigSpec.IntValue treeIconTextSpacing;
    public final ModConfigSpec.IntValue treeExpandIconWidth;
    public final ModConfigSpec.IntValue treeChannelWarningThreshold;
    public final ModConfigSpec.IntValue treeP2PNameMaxLength;

    // ========== 详情面板颜色 ==========
    public final ModConfigSpec.IntValue panelColorBg;
    public final ModConfigSpec.IntValue panelColorTitle;
    public final ModConfigSpec.IntValue panelColorLabel;
    public final ModConfigSpec.IntValue panelColorValue;
    public final ModConfigSpec.IntValue panelColorPlaceholder;
    public final ModConfigSpec.IntValue panelColorSeparator;
    public final ModConfigSpec.IntValue panelColorButtonAreaBg;
    public final ModConfigSpec.IntValue panelStatusNotActive;
    public final ModConfigSpec.IntValue panelStatusNotConnected;
    public final ModConfigSpec.IntValue panelStatusConnected;

    // ========== 详情面板布局 ==========
    public final ModConfigSpec.IntValue panelButtonWidth;
    public final ModConfigSpec.IntValue panelButtonHeight;
    public final ModConfigSpec.IntValue panelButtonSpacing;
    public final ModConfigSpec.IntValue panelInfoAreaHeightRatio;
    public final ModConfigSpec.IntValue panelTotalHeightRatio;
    public final ModConfigSpec.IntValue panelLineHeight;
    public final ModConfigSpec.IntValue panelPaddingLeft;
    public final ModConfigSpec.IntValue panelPaddingTop;
    public final ModConfigSpec.IntValue panelButtonCountMax;

    // ========== FlatButton 颜色 ==========
    public final ModConfigSpec.IntValue buttonColorBg;
    public final ModConfigSpec.IntValue buttonColorHover;
    public final ModConfigSpec.IntValue buttonColorClick;
    public final ModConfigSpec.IntValue buttonColorText;

    // ========== 复制模式选择框颜色 ==========
    public final ModConfigSpec.IntValue copyColorStartPos;
    public final ModConfigSpec.IntValue copyColorEndPos;
    public final ModConfigSpec.IntValue copyColorTargetedPos;
    public final ModConfigSpec.IntValue copyColorOverLimit;
    public final ModConfigSpec.IntValue copySelectionReady;
    public final ModConfigSpec.IntValue copySelectionFirst;
    public final ModConfigSpec.IntValue copySelectionSecond;

    // ========== P2P 高亮渲染颜色 ==========
    public final ModConfigSpec.IntValue highlightColorSelf;
    public final ModConfigSpec.IntValue highlightColorInput;
    public final ModConfigSpec.IntValue highlightColorOutput;

    // ========== 物品颜色 ==========
    public final ModConfigSpec.IntValue itemColor;
    public final ModConfigSpec.IntValue itemTintColor;

    public ClientConfig(ModConfigSpec.Builder builder) {
        builder.comment("Advanced Memory Card - 客户端配置").push("client");

        // ---- 高亮行为 ----
        builder.comment("高亮行为设置").push("highlight");

        rotateHeadOnHighlight = builder
                .comment("高亮 P2P 设备时是否自动转头面向该设备")
                .define("rotateHeadOnHighlight", true);

        p2pHighlightDurationSec = builder
                .comment("P2P 设备高亮渲染的持续时间（秒），默认 5")
                .defineInRange("p2pHighlightDurationSec", 5, 1, 60);

        builder.pop(); // highlight

        // ---- P2P 树控件颜色 ----
        builder.comment("P2P 树控件颜色设置 (ARGB)").push("treeColors");

        treeColorBg = builder
                .comment("树控件背景色")
                .defineInRange("bg", 0xFFADB0C4, Integer.MIN_VALUE, Integer.MAX_VALUE);
        treeColorSelected = builder
                .comment("选中行背景色")
                .defineInRange("selected", 0xFF4A6EA9, Integer.MIN_VALUE, Integer.MAX_VALUE);
        treeColorHover = builder
                .comment("悬停行背景色")
                .defineInRange("hover", 0xFF3A5A8A, Integer.MIN_VALUE, Integer.MAX_VALUE);

        builder.pop(); // treeColors

        // ---- P2P 树控件布局 ----
        builder.comment("P2P 树控件布局设置").push("treeLayout");

        treeIndentWidth = builder
                .comment("每级缩进像素")
                .defineInRange("indentWidth", 2, 0, 100);
        treeRowHeight = builder
                .comment("每行高度（像素）")
                .defineInRange("rowHeight", 12, 4, 64);
        treeIconTextSpacing = builder
                .comment("图标与文字间距（像素）")
                .defineInRange("iconTextSpacing", 4, 0, 32);
        treeExpandIconWidth = builder
                .comment("展开/收起图标宽度（像素）")
                .defineInRange("expandIconWidth", 2, 0, 16);
        treeChannelWarningThreshold = builder
                .comment("频道剩余数低于此值显示黄色警告")
                .defineInRange("channelWarningThreshold", 8, 0, 128);
        treeP2PNameMaxLength = builder
                .comment("P2P 设备名称最大显示长度（超出截断）")
                .defineInRange("p2pNameMaxLength", 20, 5, 100);

        builder.pop(); // treeLayout

        // ---- 详情面板颜色 ----
        builder.comment("详情面板颜色设置 (ARGB)").push("panelColors");

        panelColorBg = builder
                .comment("面板背景色")
                .defineInRange("bg", 0xFF9A9FB4, Integer.MIN_VALUE, Integer.MAX_VALUE);
        panelColorTitle = builder
                .comment("标题文字颜色")
                .defineInRange("title", 0xFFFFFFFF, Integer.MIN_VALUE, Integer.MAX_VALUE);
        panelColorLabel = builder
                .comment("标签文字颜色")
                .defineInRange("label", 0xFF000000, Integer.MIN_VALUE, Integer.MAX_VALUE);
        panelColorValue = builder
                .comment("值文字颜色")
                .defineInRange("value", 0xFFFFFFFF, Integer.MIN_VALUE, Integer.MAX_VALUE);
        panelColorPlaceholder = builder
                .comment("占位符文字颜色")
                .defineInRange("placeholder", 0xFF888888, Integer.MIN_VALUE, Integer.MAX_VALUE);
        panelColorSeparator = builder
                .comment("分隔线颜色")
                .defineInRange("separator", 0xFF888888, Integer.MIN_VALUE, Integer.MAX_VALUE);
        panelColorButtonAreaBg = builder
                .comment("按钮区域背景色")
                .defineInRange("buttonAreaBg", 0xFF9A9FB4, Integer.MIN_VALUE, Integer.MAX_VALUE);

        builder.comment("详情面板状态颜色").push("status");
        panelStatusNotActive = builder
                .comment("未激活状态颜色")
                .defineInRange("notActive", 0xFF000000, Integer.MIN_VALUE, Integer.MAX_VALUE);
        panelStatusNotConnected = builder
                .comment("未连接状态颜色")
                .defineInRange("notConnected", 0xFFFC5454, Integer.MIN_VALUE, Integer.MAX_VALUE);
        panelStatusConnected = builder
                .comment("已连接状态颜色")
                .defineInRange("connected", 0xFF54FC54, Integer.MIN_VALUE, Integer.MAX_VALUE);
        builder.pop(); // status

        builder.pop(); // panelColors

        // ---- 详情面板布局 ----
        builder.comment("详情面板布局设置").push("panelLayout");

        panelButtonWidth = builder
                .comment("按钮宽度")
                .defineInRange("buttonWidth", 46, 10, 200);
        panelButtonHeight = builder
                .comment("按钮高度")
                .defineInRange("buttonHeight", 14, 6, 64);
        panelButtonSpacing = builder
                .comment("按钮间距")
                .defineInRange("buttonSpacing", 4, 0, 32);
        panelInfoAreaHeightRatio = builder
                .comment("信息区高度比例分子（3/4）")
                .defineInRange("infoAreaHeightRatio", 3, 1, 10);
        panelTotalHeightRatio = builder
                .comment("总高度比例分母（3/4）")
                .defineInRange("totalHeightRatio", 4, 1, 10);
        panelLineHeight = builder
                .comment("行高")
                .defineInRange("lineHeight", 12, 6, 64);
        panelPaddingLeft = builder
                .comment("左侧内边距")
                .defineInRange("paddingLeft", 4, 0, 64);
        panelPaddingTop = builder
                .comment("顶部内边距")
                .defineInRange("paddingTop", 4, 0, 64);
        panelButtonCountMax = builder
                .comment("最大按钮数量")
                .defineInRange("buttonCountMax", 6, 1, 12);

        builder.pop(); // panelLayout

        // ---- FlatButton 颜色 ----
        builder.comment("通用按钮颜色设置 (ARGB)").push("buttonColors");

        buttonColorBg = builder
                .comment("按钮默认背景色")
                .defineInRange("bg", 0xFF7E8299, Integer.MIN_VALUE, Integer.MAX_VALUE);
        buttonColorHover = builder
                .comment("按钮悬停背景色")
                .defineInRange("hover", 0xFF3A5A8A, Integer.MIN_VALUE, Integer.MAX_VALUE);
        buttonColorClick = builder
                .comment("按钮点击按下背景色")
                .defineInRange("click", 0xFF2A4A7A, Integer.MIN_VALUE, Integer.MAX_VALUE);
        buttonColorText = builder
                .comment("按钮文字颜色")
                .defineInRange("text", 0xFFFFFFFF, Integer.MIN_VALUE, Integer.MAX_VALUE);

        builder.pop(); // buttonColors

        // ---- 复制模式选择框颜色 ----
        builder.comment("复制模式选择框颜色 (RGB)").push("copyColors");

        copyColorStartPos = builder
                .comment("起始位置方块渲染颜色")
                .defineInRange("startPos", 0xFF0000, 0, 0xFFFFFF);
        copyColorEndPos = builder
                .comment("结束位置方块渲染颜色")
                .defineInRange("endPos", 0xFFFF00, 0, 0xFFFFFF);
        copyColorTargetedPos = builder
                .comment("目标位置方块渲染颜色")
                .defineInRange("targetedPos", 0x00FF00, 0, 0xFFFFFF);
        copyColorOverLimit = builder
                .comment("超过体积限制时渲染颜色")
                .defineInRange("overLimit", 0xFF0000, 0, 0xFFFFFF);

        builder.comment("选择框状态颜色").push("selection");
        copySelectionReady = builder
                .comment("准备粘贴状态颜色")
                .defineInRange("ready", 0x00FF00, 0, 0xFFFFFF);
        copySelectionFirst = builder
                .comment("选择第一个点状态颜色")
                .defineInRange("first", 0xFF0000, 0, 0xFFFFFF);
        copySelectionSecond = builder
                .comment("选择第二个点状态颜色")
                .defineInRange("second", 0xFFFFFF, 0, 0xFFFFFF);
        builder.pop(); // selection

        builder.pop(); // copyColors

        // ---- P2P 高亮渲染颜色 ----
        builder.comment("P2P 高亮渲染颜色 (RGB)").push("highlightColors");

        highlightColorSelf = builder
                .comment("自身 P2P 渲染颜色")
                .defineInRange("self", 0xFF0000, 0, 0xFFFFFF);
        highlightColorInput = builder
                .comment("输入端 P2P 渲染颜色")
                .defineInRange("input", 0x00FF00, 0, 0xFFFFFF);
        highlightColorOutput = builder
                .comment("输出端 P2P 渲染颜色")
                .defineInRange("output", 0x0000FF, 0, 0xFFFFFF);

        builder.pop(); // highlightColors

        // ---- 物品颜色 ----
        builder.comment("物品颜色设置 (RGB)").push("itemColors");

        itemColor = builder
                .comment("物品主颜色")
                .defineInRange("color", 0xFF0000, 0, 0xFFFFFF);
        itemTintColor = builder
                .comment("物品着色颜色")
                .defineInRange("tintColor", 0xFFFFFF, 0, 0xFFFFFF);

        builder.pop(); // itemColors

        builder.pop(); // client
    }
}
