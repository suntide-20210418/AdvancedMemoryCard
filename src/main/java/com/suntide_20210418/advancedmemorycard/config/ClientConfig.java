package com.suntide_20210418.advancedmemorycard.config;

import net.minecraftforge.common.config.Configuration;

public class ClientConfig {
    // 高亮行为
    public final boolean rotateHeadOnHighlight;
    public final int p2pHighlightDurationSec;

    // P2P 树控件颜色
    public final int treeColorBg;
    public final int treeColorSelected;
    public final int treeColorHover;

    // P2P 树控件布局
    public final int treeIndentWidth;
    public final int treeRowHeight;
    public final int treeIconTextSpacing;
    public final int treeExpandIconWidth;
    public final int treeChannelWarningThreshold;
    public final int treeP2PNameMaxLength;

    // 详情面板颜色
    public final int panelColorBg;
    public final int panelColorTitle;
    public final int panelColorLabel;
    public final int panelColorValue;
    public final int panelColorPlaceholder;
    public final int panelColorSeparator;
    public final int panelColorButtonAreaBg;
    public final int panelStatusNotActive;
    public final int panelStatusNotConnected;
    public final int panelStatusConnected;

    // 详情面板布局
    public final int panelButtonWidth;
    public final int panelButtonHeight;
    public final int panelButtonSpacing;
    public final int panelInfoAreaHeightRatio;
    public final int panelTotalHeightRatio;
    public final int panelLineHeight;
    public final int panelPaddingLeft;
    public final int panelPaddingTop;
    public final int panelButtonCountMax;

    // FlatButton 颜色
    public final int buttonColorBg;
    public final int buttonColorHover;
    public final int buttonColorClick;
    public final int buttonColorText;

    // 复制模式选择框颜色
    public final int copyColorStartPos;
    public final int copyColorEndPos;
    public final int copyColorTargetedPos;
    public final int copyColorOverLimit;
    public final int copySelectionReady;
    public final int copySelectionFirst;
    public final int copySelectionSecond;

    // P2P 高亮渲染颜色
    public final int highlightColorSelf;
    public final int highlightColorInput;
    public final int highlightColorOutput;

    // 物品颜色
    public final int itemColor;
    public final int itemTintColor;

    public ClientConfig(Configuration config) {
        config.load();
        config.addCustomCategoryComment("client", "Advanced Memory Card - 客户端配置");

        config.addCustomCategoryComment("client.highlight", "高亮行为设置");
        rotateHeadOnHighlight = config.getBoolean("rotateHeadOnHighlight", "client.highlight", true,
                "高亮 P2P 设备时是否自动转头面向该设备");
        p2pHighlightDurationSec = config.getInt("p2pHighlightDurationSec", "client.highlight", 5, 1, 60,
                "P2P 设备高亮渲染的持续时间（秒），默认 5");

        config.addCustomCategoryComment("client.treeColors", "P2P 树控件颜色设置 (ARGB)");
        treeColorBg = config.getInt("bg", "client.treeColors", 0xFFADB0C4, Integer.MIN_VALUE, Integer.MAX_VALUE, "树控件背景色");
        treeColorSelected = config.getInt("selected", "client.treeColors", 0xFF4A6EA9, Integer.MIN_VALUE, Integer.MAX_VALUE, "选中行背景色");
        treeColorHover = config.getInt("hover", "client.treeColors", 0xFF3A5A8A, Integer.MIN_VALUE, Integer.MAX_VALUE, "悬停行背景色");

        config.addCustomCategoryComment("client.treeLayout", "P2P 树控件布局设置");
        treeIndentWidth = config.getInt("indentWidth", "client.treeLayout", 2, 0, 100, "每级缩进像素");
        treeRowHeight = config.getInt("rowHeight", "client.treeLayout", 12, 4, 64, "每行高度（像素）");
        treeIconTextSpacing = config.getInt("iconTextSpacing", "client.treeLayout", 4, 0, 32, "图标与文字间距（像素）");
        treeExpandIconWidth = config.getInt("expandIconWidth", "client.treeLayout", 2, 0, 16, "展开/收起图标宽度（像素）");
        treeChannelWarningThreshold = config.getInt("channelWarningThreshold", "client.treeLayout", 8, 0, 128, "频道剩余数低于此值显示黄色警告");
        treeP2PNameMaxLength = config.getInt("p2pNameMaxLength", "client.treeLayout", 20, 5, 100, "P2P 设备名称最大显示长度（超出截断）");

        config.addCustomCategoryComment("client.panelColors", "详情面板颜色设置 (ARGB)");
        panelColorBg = config.getInt("bg", "client.panelColors", 0xFF9A9FB4, Integer.MIN_VALUE, Integer.MAX_VALUE, "面板背景色");
        panelColorTitle = config.getInt("title", "client.panelColors", 0xFFFFFFFF, Integer.MIN_VALUE, Integer.MAX_VALUE, "标题文字颜色");
        panelColorLabel = config.getInt("label", "client.panelColors", 0xFF000000, Integer.MIN_VALUE, Integer.MAX_VALUE, "标签文字颜色");
        panelColorValue = config.getInt("value", "client.panelColors", 0xFFFFFFFF, Integer.MIN_VALUE, Integer.MAX_VALUE, "值文字颜色");
        panelColorPlaceholder = config.getInt("placeholder", "client.panelColors", 0xFF888888, Integer.MIN_VALUE, Integer.MAX_VALUE, "占位符文字颜色");
        panelColorSeparator = config.getInt("separator", "client.panelColors", 0xFF888888, Integer.MIN_VALUE, Integer.MAX_VALUE, "分隔线颜色");
        panelColorButtonAreaBg = config.getInt("buttonAreaBg", "client.panelColors", 0xFF9A9FB4, Integer.MIN_VALUE, Integer.MAX_VALUE, "按钮区域背景色");
        config.addCustomCategoryComment("client.panelColors.status", "详情面板状态颜色");
        panelStatusNotActive = config.getInt("notActive", "client.panelColors.status", 0xFF000000, Integer.MIN_VALUE, Integer.MAX_VALUE, "未激活状态颜色");
        panelStatusNotConnected = config.getInt("notConnected", "client.panelColors.status", 0xFFFC5454, Integer.MIN_VALUE, Integer.MAX_VALUE, "未连接状态颜色");
        panelStatusConnected = config.getInt("connected", "client.panelColors.status", 0xFF54FC54, Integer.MIN_VALUE, Integer.MAX_VALUE, "已连接状态颜色");

        config.addCustomCategoryComment("client.panelLayout", "详情面板布局设置");
        panelButtonWidth = config.getInt("buttonWidth", "client.panelLayout", 46, 10, 200, "按钮宽度");
        panelButtonHeight = config.getInt("buttonHeight", "client.panelLayout", 14, 6, 64, "按钮高度");
        panelButtonSpacing = config.getInt("buttonSpacing", "client.panelLayout", 4, 0, 32, "按钮间距");
        panelInfoAreaHeightRatio = config.getInt("infoAreaHeightRatio", "client.panelLayout", 3, 1, 10, "信息区高度比例分子（3/4）");
        panelTotalHeightRatio = config.getInt("totalHeightRatio", "client.panelLayout", 4, 1, 10, "总高度比例分母（3/4）");
        panelLineHeight = config.getInt("lineHeight", "client.panelLayout", 12, 6, 64, "行高");
        panelPaddingLeft = config.getInt("paddingLeft", "client.panelLayout", 4, 0, 64, "左侧内边距");
        panelPaddingTop = config.getInt("paddingTop", "client.panelLayout", 4, 0, 64, "顶部内边距");
        panelButtonCountMax = config.getInt("buttonCountMax", "client.panelLayout", 6, 1, 12, "最大按钮数量");

        config.addCustomCategoryComment("client.buttonColors", "通用按钮颜色设置 (ARGB)");
        buttonColorBg = config.getInt("bg", "client.buttonColors", 0xFF7E8299, Integer.MIN_VALUE, Integer.MAX_VALUE, "按钮默认背景色");
        buttonColorHover = config.getInt("hover", "client.buttonColors", 0xFF3A5A8A, Integer.MIN_VALUE, Integer.MAX_VALUE, "按钮悬停背景色");
        buttonColorClick = config.getInt("click", "client.buttonColors", 0xFF2A4A7A, Integer.MIN_VALUE, Integer.MAX_VALUE, "按钮点击按下背景色");
        buttonColorText = config.getInt("text", "client.buttonColors", 0xFFFFFFFF, Integer.MIN_VALUE, Integer.MAX_VALUE, "按钮文字颜色");

        config.addCustomCategoryComment("client.copyColors", "复制模式选择框颜色 (RGB)");
        copyColorStartPos = config.getInt("startPos", "client.copyColors", 0xFF0000, 0, 0xFFFFFF, "起始位置方块渲染颜色");
        copyColorEndPos = config.getInt("endPos", "client.copyColors", 0xFFFF00, 0, 0xFFFFFF, "结束位置方块渲染颜色");
        copyColorTargetedPos = config.getInt("targetedPos", "client.copyColors", 0x00FF00, 0, 0xFFFFFF, "目标位置方块渲染颜色");
        copyColorOverLimit = config.getInt("overLimit", "client.copyColors", 0xFF0000, 0, 0xFFFFFF, "超过体积限制时渲染颜色");
        config.addCustomCategoryComment("client.copyColors.selection", "选择框状态颜色");
        copySelectionReady = config.getInt("ready", "client.copyColors.selection", 0x00FF00, 0, 0xFFFFFF, "准备粘贴状态颜色");
        copySelectionFirst = config.getInt("first", "client.copyColors.selection", 0xFF0000, 0, 0xFFFFFF, "选择第一个点状态颜色");
        copySelectionSecond = config.getInt("second", "client.copyColors.selection", 0xFFFFFF, 0, 0xFFFFFF, "选择第二个点状态颜色");

        config.addCustomCategoryComment("client.highlightColors", "P2P 高亮渲染颜色 (RGB)");
        highlightColorSelf = config.getInt("self", "client.highlightColors", 0xFF0000, 0, 0xFFFFFF, "自身 P2P 渲染颜色");
        highlightColorInput = config.getInt("input", "client.highlightColors", 0x00FF00, 0, 0xFFFFFF, "输入端 P2P 渲染颜色");
        highlightColorOutput = config.getInt("output", "client.highlightColors", 0x0000FF, 0, 0xFFFFFF, "输出端 P2P 渲染颜色");

        config.addCustomCategoryComment("client.itemColors", "物品颜色设置 (RGB)");
        itemColor = config.getInt("color", "client.itemColors", 0xFF0000, 0, 0xFFFFFF, "物品主颜色");
        itemTintColor = config.getInt("tintColor", "client.itemColors", 0xFFFFFF, 0, 0xFFFFFF, "物品着色颜色");
    }
}
