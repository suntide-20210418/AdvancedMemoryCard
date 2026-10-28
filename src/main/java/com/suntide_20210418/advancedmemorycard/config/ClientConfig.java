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
 *
 * <p>注意：Minecraft 的默认字体只有一种字号（8px 高），不存在 CSS 意义上的
 * font-size 层级。因此本模组的 GUI 用<b>颜色与留白</b>表达层级，所有文本统一
 * 8px，不要试图靠“小一号字体”压行高。
 */
public class ClientConfig {
    // ========== 高亮行为 ==========
    public final ModConfigSpec.ConfigValue<Boolean> rotateHeadOnHighlight;
    public final ModConfigSpec.IntValue p2pHighlightDurationSec;

    // ========== P2P 树控件布局 ==========
    public final ModConfigSpec.IntValue treeIndentWidth;
    public final ModConfigSpec.IntValue treeRowHeight;
    public final ModConfigSpec.IntValue treeChannelWarningThreshold;
    public final ModConfigSpec.IntValue treeP2PNameMaxLength;

    // ========== GUI 语义色 ==========
    public final ModConfigSpec.IntValue paletteTextPrimary;
    public final ModConfigSpec.IntValue paletteTextSecondary;
    public final ModConfigSpec.IntValue paletteTextTertiary;
    public final ModConfigSpec.IntValue paletteAccent;
    public final ModConfigSpec.IntValue paletteAccentText;
    public final ModConfigSpec.IntValue paletteRowSelected;
    public final ModConfigSpec.IntValue paletteRowHover;
    public final ModConfigSpec.IntValue paletteSurface;
    public final ModConfigSpec.IntValue paletteSurfaceHover;
    public final ModConfigSpec.IntValue paletteDivider;
    public final ModConfigSpec.IntValue paletteStatusOk;
    public final ModConfigSpec.IntValue paletteStatusWarn;
    public final ModConfigSpec.IntValue paletteStatusDanger;
    public final ModConfigSpec.IntValue paletteChipOkBg;
    public final ModConfigSpec.IntValue paletteChipAccentBg;
    public final ModConfigSpec.IntValue paletteChipWarnBg;
    public final ModConfigSpec.IntValue paletteChipDangerBg;
    public final ModConfigSpec.IntValue paletteOverlayBg;
    public final ModConfigSpec.IntValue paletteOverlayBorder;
    public final ModConfigSpec.IntValue paletteOverlayShadow;

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

        // ---- 树布局 ----
        builder.comment("P2P 树控件布局设置").push("treeLayout");

        treeRowHeight = builder
                .comment("每行高度（像素）。8px 字体 + 上下各 3px 呼吸 = 14")
                .defineInRange("rowHeight", 14, 10, 32);
        treeIndentWidth = builder
                .comment("每级缩进像素。10px/级 才能让三级层级肉眼可分")
                .defineInRange("indentWidth", 10, 0, 40);
        treeChannelWarningThreshold = builder
                .comment("频道剩余数低于此值时汇总列转警告色")
                .defineInRange("channelWarningThreshold", 8, 0, 128);
        treeP2PNameMaxLength = builder
                .comment("P2P 设备名称最大显示长度（超出截断）")
                .defineInRange("nameMaxLength", 14, 4, 64);

        builder.pop(); // treeLayout

        // ---- 语义色板 ----
        builder.comment("GUI 语义色（ARGB）。默认值已在 SINK #ADB0C4 / CARD #CBCCD4 上验证对比度 >= 4.5:1")
                .push("palette");

        paletteTextPrimary = builder
                .comment("主文本（名称、取值）")
                .defineInRange("textPrimary", 0xFF16191F, Integer.MIN_VALUE, Integer.MAX_VALUE);
        paletteTextSecondary = builder
                .comment("次要文本（频段、计数、表头）")
                .defineInRange("textSecondary", 0xFF3B4152, Integer.MIN_VALUE, Integer.MAX_VALUE);
        paletteTextTertiary = builder
                .comment("辅助文本（属性标签、占位提示）")
                .defineInRange("textTertiary", 0xFF4A5265, Integer.MIN_VALUE, Integer.MAX_VALUE);
        paletteAccent = builder
                .comment("强调色（主按钮底、当前可编辑字段）")
                .defineInRange("accent", 0xFF1B4483, Integer.MIN_VALUE, Integer.MAX_VALUE);
        paletteAccentText = builder
                .comment("强调色上的文字")
                .defineInRange("accentText", 0xFFFFFFFF, Integer.MIN_VALUE, Integer.MAX_VALUE);
        paletteRowSelected = builder
                .comment("选中行底色")
                .defineInRange("rowSelected", 0xFF2E4F86, Integer.MIN_VALUE, Integer.MAX_VALUE);
        paletteRowHover = builder
                .comment("悬停行底色")
                .defineInRange("rowHover", 0xFFBEC3D4, Integer.MIN_VALUE, Integer.MAX_VALUE);
        paletteSurface = builder
                .comment("输入框 / 槽位底色")
                .defineInRange("surface", 0xFFDDE1EA, Integer.MIN_VALUE, Integer.MAX_VALUE);
        paletteSurfaceHover = builder
                .comment("输入框 / 槽位悬停底色")
                .defineInRange("surfaceHover", 0xFFE8EBF2, Integer.MIN_VALUE, Integer.MAX_VALUE);
        paletteDivider = builder
                .comment("分隔线 / 细描边")
                .defineInRange("divider", 0xFF8C93A6, Integer.MIN_VALUE, Integer.MAX_VALUE);
        paletteStatusOk = builder
                .comment("状态：连通")
                .defineInRange("statusOk", 0xFF084D1C, Integer.MIN_VALUE, Integer.MAX_VALUE);
        paletteStatusWarn = builder
                .comment("状态：降级 / 未连通")
                .defineInRange("statusWarn", 0xFF5C3800, Integer.MIN_VALUE, Integer.MAX_VALUE);
        paletteStatusDanger = builder
                .comment("状态：异常 / 破坏性操作")
                .defineInRange("statusDanger", 0xFF7A0F0F, Integer.MIN_VALUE, Integer.MAX_VALUE);
        paletteChipOkBg = builder
                .comment("chip 底色：正常")
                .defineInRange("chipOkBg", 0xFFD6E8DA, Integer.MIN_VALUE, Integer.MAX_VALUE);
        paletteChipAccentBg = builder
                .comment("chip 底色：强调")
                .defineInRange("chipAccentBg", 0xFFD5E0F2, Integer.MIN_VALUE, Integer.MAX_VALUE);
        paletteChipWarnBg = builder
                .comment("chip 底色：警告")
                .defineInRange("chipWarnBg", 0xFFEDE2CC, Integer.MIN_VALUE, Integer.MAX_VALUE);
        paletteChipDangerBg = builder
                .comment("chip 底色：危险")
                .defineInRange("chipDangerBg", 0xFFF2DCDC, Integer.MIN_VALUE, Integer.MAX_VALUE);
        paletteOverlayBg = builder
                .comment("浮层底色（频段下拉）")
                .defineInRange("overlayBg", 0xFFE7EBF3, Integer.MIN_VALUE, Integer.MAX_VALUE);
        paletteOverlayBorder = builder
                .comment("浮层描边")
                .defineInRange("overlayBorder", 0xFF5E6678, Integer.MIN_VALUE, Integer.MAX_VALUE);
        paletteOverlayShadow = builder
                .comment("浮层投影（含透明度）")
                .defineInRange("overlayShadow", 0x40000000, Integer.MIN_VALUE, Integer.MAX_VALUE);

        builder.pop(); // palette

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
