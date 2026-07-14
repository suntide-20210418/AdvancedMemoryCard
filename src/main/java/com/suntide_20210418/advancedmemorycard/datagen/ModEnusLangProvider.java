package com.suntide_20210418.advancedmemorycard.datagen;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.Keys;

public class ModEnusLangProvider extends LanguageProvider {

    public ModEnusLangProvider(PackOutput pOutput) {
        super(pOutput, AdvancedMemoryCardMod.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // 物品相关
        add(Keys.ITEM_ADVANCED_MEMORY_CARD, "Advanced Memory Card");

        // 物品组相关
        add(Keys.ITEM_GROUP_TAB, "Advanced Memory Card");

        // 复制模式工具提示
        add(Keys.TOOLTIP_COPY_INFO, "Copy Mode\n");
        add(Keys.TOOLTIP_COPY_FIRST_POS, "First position:%s\n");
        add(Keys.TOOLTIP_COPY_SECOND_POS, "Second position:%s\n");
        add(Keys.TOOLTIP_COPY_READY, "Ready to paste");

        // 配置模式显示
        add(Keys.CONFIG_MODE_SHOW, "P2P Tunnel Config Mode");

        // 复制模式显示
        add(Keys.COPY_MODE_SHOW, "Copy Mode");

        // 复制模式玩家消息
        add(Keys.COPY_MODE_FIRST_POS_MARKED, "First Position[%s] Marked");
        add(
                Keys.COPY_MODE_SECOND_POS_MARKED,
                "Second Position[%s] Marked, Current area:[%s,%s,%s]");
        add(Keys.COPY_MODE_TOO_LARGE, "Too large area,Max area is:%s,current area is:%s");
        add(Keys.COPY_MODE_COMPLETED, "Pasted %s blocks");
        add(
                Keys.COPY_MODE_ALREADY_MARKED,
                "This position has already been marked, please remark it");
        add(
                Keys.COPY_MODE_FAILED,
                "You have not marked first or second position, please mark first");

        // 配置模式工具提示
        add(Keys.TOOLTIP_CONFIG_INFO, "Config Mode\n");

        // 复制模式屏幕
        add(Keys.COPY_MODE_SCREEN_START_POS, "Start Pos");
        add(Keys.COPY_MODE_SCREEN_END_POS, "End Pos");
        add(Keys.COPY_MODE_SCREEN_CLEAR_TOOLTIP, "Clear positions");

        // 配置模式屏幕
        add(Keys.CONFIG_MODE_SCREEN_SEARCH_PLACEHOLDER, "Search...");
        add(Keys.CONFIG_MODE_SCREEN_TITLE, "P2P Config Mode");
        add(Keys.CONFIG_MODE_SCREEN_INFO, "Select a P2P node to view details");

        // P2P 树控件
        add(Keys.P2P_TREE_EMPTY, "No P2P devices found");
        add(Keys.P2P_TREE_UNKNOWN_P2P, "Unknown P2P");
        add(Keys.P2P_TREE_SEARCH_RESULTS, "Search Results");
        add(Keys.P2P_TREE_SEARCH_RESULT_COUNT, "Found %d result(s)");

        // 详情面板
        add(Keys.DETAIL_PANEL_PLACEHOLDER, "Select a P2P device");
        add(Keys.DETAIL_PANEL_NAME_LABEL, "Name: ");
        add(Keys.DETAIL_PANEL_FREQUENCY_LABEL, "Frequency: ");
        add(Keys.DETAIL_PANEL_TYPE_LABEL, "Type: ");
        add(Keys.DETAIL_PANEL_DIMENSION_LABEL, "Dimension: ");
        add(Keys.DETAIL_PANEL_POSITION_LABEL, "Position: ");
        add(Keys.DETAIL_PANEL_DIRECTION_LABEL, "Direction: ");
        add(Keys.DETAIL_PANEL_STATUS_LABEL, "Status: ");
        add(Keys.DETAIL_PANEL_CHANNEL_INFO_LABEL, "Used/Remaining/Total: ");
        add(Keys.DETAIL_PANEL_P2P_COUNT_LABEL, "P2P Count: ");
        add(Keys.DETAIL_PANEL_INPUT_COUNT_LABEL, "Input: ");
        add(Keys.DETAIL_PANEL_OUTPUT_COUNT_LABEL, "Output: ");
        add(Keys.DETAIL_PANEL_TOTAL_COUNT_LABEL, "P2P Total: ");
        add(Keys.DETAIL_PANEL_CHANNEL_COUNT_LABEL, "Frequency Count: ");
        add(Keys.DETAIL_PANEL_INPUT_NAME_LABEL, "Input Name: ");

        // 按钮
        add(Keys.BUTTON_RENAME, "Rename");
        add(Keys.BUTTON_SELECT, "Select");
        add(Keys.BUTTON_HIGHLIGHT, "Highlight");
        add(Keys.BUTTON_PENDING_BIND, "Pending Bind");
        add(Keys.BUTTON_REFRESH, "Refresh");
        add(Keys.BUTTON_CANCEL, "Cancel");
        add(Keys.BUTTON_BIND, "Bind");
        add(Keys.BUTTON_LOCATE, "Locate");
        add(Keys.BUTTON_INIT_P2P, "Init P2P");
        add(Keys.BUTTON_ASSIGN_FREQ, "Assign New Frequency");

        // 状态文本
        add(Keys.STATUS_NOT_ACTIVE, "Inactive");
        add(Keys.STATUS_NOT_CONNECTED, "Not Connected");
        add(Keys.STATUS_CONNECTED, "Connected");
        add(Keys.STATUS_OUTPUT, " (Output)");
        add(Keys.STATUS_INPUT, " (Input)");
        add(Keys.STATUS_CURRENT_SELECTED, " (Currently Selected)");

        // 聊天消息
        add(Keys.CHAT_LOCATION_INFO, "%s at %s - (%s) ");
        add(Keys.CHAT_CLICK_TO_TELEPORT, "[Click to Teleport]");
        add(Keys.CHAT_TELEPORT_HOVER, "Click to teleport to %s - (%s)\nRequires cheats or OP permissions");
        add(Keys.CHAT_FREQ_HIGHLIGHT_HEADER, "=== Frequency %s Highlight ===");
        add(Keys.CHAT_TELEPORT_BUTTON, "[Teleport]");
    }
}
