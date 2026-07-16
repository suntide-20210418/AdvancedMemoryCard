package com.suntide_20210418.advancedmemorycard.datagen;

import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.Keys;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModZhcnLangProvider extends LanguageProvider {

    public ModZhcnLangProvider(PackOutput pOutput) {
        super(pOutput, AdvancedMemoryCardMod.MOD_ID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        // 物品相关
        add(Keys.ITEM_ADVANCED_MEMORY_CARD, "高级内存卡");

        // 物品组相关
        add(Keys.ITEM_GROUP_TAB, "高级内存卡");

        // 复制模式工具提示
        add(Keys.TOOLTIP_COPY_INFO, "复制模式\n");
        add(Keys.TOOLTIP_COPY_FIRST_POS, "第一个位置：%s\n");
        add(Keys.TOOLTIP_COPY_SECOND_POS, "第二个位置：%s\n");
        add(Keys.TOOLTIP_COPY_READY, "准备粘贴");

        // 配置模式显示
        add(Keys.CONFIG_MODE_SHOW, "P2P通道 配置模式");

        // 复制模式显示
        add(Keys.COPY_MODE_SHOW, "复制模式");

        // 复制模式玩家消息
        add(Keys.COPY_MODE_FIRST_POS_MARKED, "第一个位置[%s]已标记");
        add(Keys.COPY_MODE_SECOND_POS_MARKED, "第二个位置[%s]已标记，待复制区域大小:[%s,%s,%s]");
        add(Keys.COPY_MODE_TOO_LARGE, "范围过大(%s>%s)");
        add(Keys.COPY_MODE_COMPLETED, "已粘贴%s个方块的配置");
        add(Keys.COPY_MODE_ALREADY_MARKED, "区域已标记");
        add(Keys.COPY_MODE_FAILED, "你还没有标记第一个或第二个方块的位置，请先标记位置");

        // 配置模式工具提示
        add(Keys.TOOLTIP_CONFIG_INFO, "配置模式\n");

        // 复制模式屏幕
        add(Keys.COPY_MODE_SCREEN_START_POS, "开始坐标");
        add(Keys.COPY_MODE_SCREEN_END_POS, "结束坐标");
        add(Keys.COPY_MODE_SCREEN_CLEAR_TOOLTIP, "清除坐标");

        // 配置模式屏幕
        add(Keys.CONFIG_MODE_SCREEN_SEARCH_PLACEHOLDER, "搜索...");
        add(Keys.CONFIG_MODE_SCREEN_TITLE, "P2P 配置模式");
        add(Keys.CONFIG_MODE_SCREEN_INFO, "选择一个 P2P 节点查看详情");

        // P2P 树控件
        add(Keys.P2P_TREE_EMPTY, "未找到 P2P 设备");
        add(Keys.P2P_TREE_UNKNOWN_P2P, "未知 P2P");
        add(Keys.P2P_TREE_SEARCH_RESULTS, "搜索结果");
        add(Keys.P2P_TREE_SEARCH_RESULT_COUNT, "共找到%d个结果");

        // 详情面板
        add(Keys.DETAIL_PANEL_PLACEHOLDER, "请选择P2P");
        add(Keys.DETAIL_PANEL_NAME_LABEL, "名称: ");
        add(Keys.DETAIL_PANEL_FREQUENCY_LABEL, "频段: ");
        add(Keys.DETAIL_PANEL_TYPE_LABEL, "类型: ");
        add(Keys.DETAIL_PANEL_DIMENSION_LABEL, "维度: ");
        add(Keys.DETAIL_PANEL_POSITION_LABEL, "位置: ");
        add(Keys.DETAIL_PANEL_DIRECTION_LABEL, "方向: ");
        add(Keys.DETAIL_PANEL_STATUS_LABEL, "状态: ");
        add(Keys.DETAIL_PANEL_CHANNEL_INFO_LABEL, "已用/剩余/总数: ");
        add(Keys.DETAIL_PANEL_P2P_COUNT_LABEL, "P2P数量: ");
        add(Keys.DETAIL_PANEL_INPUT_COUNT_LABEL, "输入端: ");
        add(Keys.DETAIL_PANEL_OUTPUT_COUNT_LABEL, "输出端: ");
        add(Keys.DETAIL_PANEL_TOTAL_COUNT_LABEL, "P2P总数: ");
        add(Keys.DETAIL_PANEL_CHANNEL_COUNT_LABEL, "频段数: ");
        add(Keys.DETAIL_PANEL_INPUT_NAME_LABEL, "输入端名称: ");

        // 按钮
        add(Keys.BUTTON_RENAME, "改名");
        add(Keys.BUTTON_SELECT, "选择");
        add(Keys.BUTTON_HIGHLIGHT, "高亮");
        add(Keys.BUTTON_PENDING_BIND, "待绑定");
        add(Keys.BUTTON_REFRESH, "刷新");
        add(Keys.BUTTON_CANCEL, "取消");
        add(Keys.BUTTON_BIND, "绑定");
        add(Keys.BUTTON_LOCATE, "定位");
        add(Keys.BUTTON_INIT_P2P, "初始化P2P");
        add(Keys.BUTTON_ASSIGN_FREQ, "分配新频段");

        // 状态文本
        add(Keys.STATUS_NOT_ACTIVE, "未激活");
        add(Keys.STATUS_NOT_CONNECTED, "未连接");
        add(Keys.STATUS_CONNECTED, "已连接");
        add(Keys.STATUS_OUTPUT, "（输出端）");
        add(Keys.STATUS_INPUT, "（输入端）");
        add(Keys.STATUS_CURRENT_SELECTED, "（当前选中）");

        // 聊天消息
        add(Keys.CHAT_LOCATION_INFO, "%s 在 %s - (%s) ");
        add(Keys.CHAT_CLICK_TO_TELEPORT, "【点击此处来传送】");
        add(Keys.CHAT_TELEPORT_HOVER, "点击传送到 %s - (%s)\n需要开启作弊模式或拥有OP权限");
        add(Keys.CHAT_FREQ_HIGHLIGHT_HEADER, "=== 频段 %s 高亮 ===");
        add(Keys.CHAT_TELEPORT_BUTTON, "【传送】");

        // 按键绑定
        add("key.categories.advanced_memory_card", "高级内存卡");
        add("key.advancedmemorycard.switch", "切换模式");
    }
}
