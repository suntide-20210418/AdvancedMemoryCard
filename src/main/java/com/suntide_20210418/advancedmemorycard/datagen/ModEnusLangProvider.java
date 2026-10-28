package com.suntide_20210418.advancedmemorycard.datagen;

import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.Keys;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

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
        add(Keys.TOOLTIP_UNCONNECTED_NOT_BINDABLE, "\"Unconnected\" is not a bindable frequency");

        // 复制模式屏幕
        add(Keys.COPY_MODE_SCREEN_START_POS, "Start Pos");
        add(Keys.COPY_MODE_SCREEN_END_POS, "End Pos");
        add(Keys.COPY_MODE_SCREEN_CLEAR_TOOLTIP, "Clear positions");

        // 复制模式粘贴选项
        add(Keys.COPY_MODE_SCREEN_OPTIONS_TITLE, "Paste options");
        add(Keys.COPY_MODE_SCREEN_AUTO_OPEN, "Open these options after each copy");
        add(Keys.COPY_MODE_OPTION_UPGRADES, "Apply upgrades");
        add(Keys.COPY_MODE_OPTION_SETTINGS, "Apply settings");
        add(Keys.COPY_MODE_OPTION_PRIORITY, "Apply priority");
        add(Keys.COPY_MODE_OPTION_CONFIG_INV, "Apply config inventory");
        add(Keys.COPY_MODE_OPTION_PATTERNS, "Apply patterns");
        add(Keys.COPY_MODE_OPTION_P2P_TYPE, "Apply P2P type");
        add(Keys.COPY_MODE_OPTION_P2P_FREQUENCY, "Apply P2P frequency");
        add(Keys.COPY_MODE_OPTION_CUSTOM_NAME, "Apply name");

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
        add(Keys.BUTTON_TELEPORT, "Teleport");
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
        add(Keys.CHAT_FREQ_HIGHLIGHT_HEADER, "=== Frequency %s Highlight ===");

        // 按键绑定
        add("key.categories.advanced_memory_card", "Advanced Memory Card");
        add("key.advancedmemorycard.switch", "Switch Mode");
        add("key.advancedmemorycard.copy_options", "Copy Options");

        // ===== Pair bar =====
        add(Keys.PAIR_SOURCE, "FROM");
        add(Keys.PAIR_TARGET, "TO");
        add(Keys.PAIR_TARGET_PLACEHOLDER, "Click to pick a channel");
        add(Keys.PAIR_SOURCE_PLACEHOLDER, "Pick a device on the left");
        add(Keys.PAIR_CLEAR, "Clear");
        add(Keys.PAIR_NEW_FREQ, "New channel…");
        add(Keys.PAIR_NO_CHANNEL, "No channels available");
        add(Keys.PAIR_UNNAMED, "Unnamed");
        add(Keys.PAIR_SOURCE_MULTI, "%s selected");

        // ===== Status line =====
        add(Keys.STATUS_LINE_IDLE, "Pick a device as source, then a channel as target");
        add(Keys.STATUS_LINE_BIND_OK, "Bound to %s");
        add(Keys.STATUS_LINE_BIND_NO_TARGET, "No source device — pick a P2P on the left");
        add(Keys.STATUS_LINE_BIND_INVALID_FREQ, "%s is not a bindable channel");
        add(Keys.STATUS_LINE_BIND_NO_GRID, "Network not ready, bind skipped");
        add(Keys.STATUS_LINE_SOURCE_SELECTED, "Source: %s");
        add(Keys.STATUS_LINE_ASSIGNED, "Assigned new channel %s");
        add(Keys.STATUS_LINE_RENAMED, "Renamed to %s");
        add(Keys.STATUS_LINE_REFRESHED, "Refreshed");
        add(Keys.STATUS_LINE_RENAME_FAILED, "Rename failed: device is no longer on this network");
        add(Keys.STATUS_LINE_BIND_BATCH_OK, "Bound %s devices to %s");
        add(Keys.STATUS_LINE_BIND_BATCH_PARTIAL, "%s bound, %s failed (target %s)");

        // ===== Tree columns =====
        add(Keys.TREE_COL_NAME, "Name");
        add(Keys.TREE_COL_FREQ, "Channel");
        add(Keys.TREE_COL_SUMMARY, "Info");
        add(Keys.TREE_SUMMARY_CHANNEL, "%s dev, %s left");
        add(Keys.TREE_SUMMARY_CHANNEL_PLAIN, "%s dev");
        add(Keys.TREE_SUMMARY_TYPE, "%s dev, %s ch");
        add(Keys.TREE_ROLE_INPUT, "IN");
        add(Keys.TREE_ROLE_OUTPUT, "OUT");
        add(Keys.TREE_PREFIX_INPUT, "IN");
        add(Keys.TREE_PREFIX_OUTPUT, "OUT");

        // ===== Action bar =====
        add(Keys.ACTION_BAR_HINT, "Pick device → pick channel → bind");
        add(Keys.ACTION_BAR_HINT_READY, "Bind %s to %s, press Enter");
        add(Keys.ACTION_BAR_HINT_READY_MULTI, "Bind %s devices to %s, press Enter");
        add(Keys.ACTION_BAR_BIND, "Bind");
        add(Keys.ACTION_BAR_BATCH, "Batch select");
        add(Keys.ACTION_BAR_EXIT_BATCH, "Exit batch");
    }
}
