package com.suntide_20210418.advancedmemorycard.client.gui;

/**
 * 配置模式界面的布局常量（440×312）。
 *
 * <p>
 * 1.20.1 / 1.21.1 由 AE2 的 ScreenStyle JSON 定槽位，旧版没有那套机制，
 * 用这一个类承担同样职责：所有控件矩形只在这里定义，界面与控件都从这里读。
 * 与 JSON 的槽位数值保持一致，迁移过的美术资产可以直接复用。
 *
 * <p>
 * 树行高等度量来自 {@code client.treeLayout.*} 配置，不在此处。
 */
public final class ConfigUiLayout {

    /** 面板尺寸（贴图 srcRect，也是整体内容区）。 */
    public static final int PANEL_W = 440;
    public static final int PANEL_H = 312;
    /** 底图图集尺寸：贴图是 512×512，绘制时取左上 440×312。 */
    public static final int PANEL_TEX_SIZE = 512;

    /** 标题文字左上角（对应 JSON 的 {@code text.title.position}）。 */
    public static final Rect TITLE = new Rect(7, 7, 0, 0);

    /** 顶部「源 → 目标」配对条。 */
    public static final Rect PAIR_BAR = new Rect(10, 20, 420, 52);
    /** 搜索框。 */
    public static final Rect SEARCH = new Rect(18, 76, 120, 13);
    /** 列表树。 */
    public static final Rect TREE = new Rect(18, 102, 231, 175);
    /** 树滚动条（贴在树右缘）。 */
    public static final Rect SCROLLBAR = new Rect(236, 102, 14, 175);
    /** 右侧检查器。 */
    public static final Rect DETAIL = new Rect(250, 75, 179, 207);
    /** 底部操作条。 */
    public static final Rect ACTION_BAR = new Rect(10, 288, 420, 17);

    /** 列头画在树矩形上方的偏移量。 */
    public static final int COLUMN_HEADER_OFFSET = 10;

    // 树列宽（与基准 P2PTreeWidget 的常量一致）
    public static final int COL_STATUS_W = 12;
    public static final int COL_FREQ_W = 40;
    public static final int COL_SUMMARY_W = 52;
    public static final int SCROLLBAR_GUTTER = 14;
    public static final int CHEVRON_W = 9;
    public static final int RIGHT_PAD = 4;

    private ConfigUiLayout() {}

    /** 面板内的矩形（相对坐标），界面渲染时加上 left/top 平移。 */
    public static final class Rect {

        public final int x;
        public final int y;
        public final int w;
        public final int h;

        public Rect(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public boolean contains(int px, int py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }

        public boolean contains(double px, double py) {
            return contains((int) px, (int) py);
        }
    }
}
