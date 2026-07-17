package com.suntide_20210418.advancedmemorycard.utils;

import net.minecraft.client.resources.I18n;

/**
 * 翻译助手工具类 提供统一的翻译、格式化与本地化功能（1.12.2 适配：返回 String）
 */
public class TranslateHelper {

    private static final String BASE_KEY = "advanced_memory_card";

    public static class Keys {
        public static final String ITEM_PREFIX = "item." + BASE_KEY + ".";
        public static final String ITEM_ADVANCED_MEMORY_CARD = ITEM_PREFIX + "advanced_memory_card";

        public static final String ITEM_GROUP_PREFIX = "itemGroup." + BASE_KEY + ".";
        public static final String ITEM_GROUP_TAB = ITEM_GROUP_PREFIX + "tab";

        public static final String GUI_PREFIX = "gui." + BASE_KEY + ".";
        public static final String SCREEN_PREFIX = "screen." + BASE_KEY + ".";

        public static final String COPY_MODE_PREFIX = GUI_PREFIX + "advanced_memory_card.player.copy.";
        public static final String COPY_MODE_SHOW = COPY_MODE_PREFIX + "show";
        public static final String COPY_MODE_FIRST_POS_MARKED = COPY_MODE_PREFIX + "first_pos_marked";
        public static final String COPY_MODE_SECOND_POS_MARKED = COPY_MODE_PREFIX + "second_pos_marked";
        public static final String COPY_MODE_TOO_LARGE = COPY_MODE_PREFIX + "too_large";
        public static final String COPY_MODE_COMPLETED = COPY_MODE_PREFIX + "completed";
        public static final String COPY_MODE_ALREADY_MARKED = COPY_MODE_PREFIX + "already_marked";
        public static final String COPY_MODE_FAILED = COPY_MODE_PREFIX + "failed";

        public static final String COPY_MODE_SCREEN_PREFIX = SCREEN_PREFIX + "advanced_memory_card.player.copy.";
        public static final String COPY_MODE_SCREEN_START_POS = COPY_MODE_SCREEN_PREFIX + "start_pos";
        public static final String COPY_MODE_SCREEN_END_POS = COPY_MODE_SCREEN_PREFIX + "end_pos";
        public static final String COPY_MODE_SCREEN_CLEAR_TOOLTIP = COPY_MODE_SCREEN_PREFIX + "clear_tooltip";
        public static final String COPY_MODE_SCREEN_TITLE = COPY_MODE_SCREEN_PREFIX + "title";

        public static final String CONFIG_MODE_SCREEN_PREFIX = SCREEN_PREFIX + "advanced_memory_card.player.config.";
        public static final String CONFIG_MODE_SCREEN_TITLE = CONFIG_MODE_SCREEN_PREFIX + "title";
        public static final String CONFIG_MODE_SCREEN_SEARCH_PLACEHOLDER = CONFIG_MODE_SCREEN_PREFIX + "search_placeholder";
        public static final String CONFIG_MODE_SCREEN_INFO = CONFIG_MODE_SCREEN_PREFIX + "info";

        public static final String P2P_TREE_PREFIX = GUI_PREFIX + "advanced_memory_card.p2p_tree.";
        public static final String P2P_TREE_EMPTY = P2P_TREE_PREFIX + "empty";
        public static final String P2P_TREE_UNKNOWN_P2P = P2P_TREE_PREFIX + "unknown_p2p";
        public static final String P2P_TREE_SEARCH_RESULTS = P2P_TREE_PREFIX + "search_results";
        public static final String P2P_TREE_SEARCH_RESULT_COUNT = P2P_TREE_PREFIX + "search_result_count";

        public static final String DETAIL_PANEL_PREFIX = GUI_PREFIX + "advanced_memory_card.detail_panel.";
        public static final String DETAIL_PANEL_PLACEHOLDER = DETAIL_PANEL_PREFIX + "placeholder";
        public static final String DETAIL_PANEL_NAME_LABEL = DETAIL_PANEL_PREFIX + "name";
        public static final String DETAIL_PANEL_FREQUENCY_LABEL = DETAIL_PANEL_PREFIX + "frequency";
        public static final String DETAIL_PANEL_TYPE_LABEL = DETAIL_PANEL_PREFIX + "type";
        public static final String DETAIL_PANEL_DIMENSION_LABEL = DETAIL_PANEL_PREFIX + "dimension";
        public static final String DETAIL_PANEL_POSITION_LABEL = DETAIL_PANEL_PREFIX + "position";
        public static final String DETAIL_PANEL_DIRECTION_LABEL = DETAIL_PANEL_PREFIX + "direction";
        public static final String DETAIL_PANEL_STATUS_LABEL = DETAIL_PANEL_PREFIX + "status";
        public static final String DETAIL_PANEL_CHANNEL_INFO_LABEL = DETAIL_PANEL_PREFIX + "channel_info";
        public static final String DETAIL_PANEL_P2P_COUNT_LABEL = DETAIL_PANEL_PREFIX + "p2p_count";
        public static final String DETAIL_PANEL_INPUT_COUNT_LABEL = DETAIL_PANEL_PREFIX + "input_count";
        public static final String DETAIL_PANEL_OUTPUT_COUNT_LABEL = DETAIL_PANEL_PREFIX + "output_count";
        public static final String DETAIL_PANEL_TOTAL_COUNT_LABEL = DETAIL_PANEL_PREFIX + "total_count";
        public static final String DETAIL_PANEL_CHANNEL_COUNT_LABEL = DETAIL_PANEL_PREFIX + "channel_count";
        public static final String DETAIL_PANEL_INPUT_NAME_LABEL = DETAIL_PANEL_PREFIX + "input_name";

        public static final String BUTTON_PREFIX = GUI_PREFIX + "advanced_memory_card.button.";
        public static final String BUTTON_RENAME = BUTTON_PREFIX + "rename";
        public static final String BUTTON_SELECT = BUTTON_PREFIX + "select";
        public static final String BUTTON_HIGHLIGHT = BUTTON_PREFIX + "highlight";
        public static final String BUTTON_PENDING_BIND = BUTTON_PREFIX + "pending_bind";
        public static final String BUTTON_REFRESH = BUTTON_PREFIX + "refresh";
        public static final String BUTTON_CANCEL = BUTTON_PREFIX + "cancel";
        public static final String BUTTON_BIND = BUTTON_PREFIX + "bind";
        public static final String BUTTON_LOCATE = BUTTON_PREFIX + "locate";
        public static final String BUTTON_INIT_P2P = BUTTON_PREFIX + "init_p2p";
        public static final String BUTTON_ASSIGN_FREQ = BUTTON_PREFIX + "assign_freq";

        public static final String STATUS_PREFIX = GUI_PREFIX + "advanced_memory_card.status.";
        public static final String STATUS_NOT_ACTIVE = STATUS_PREFIX + "not_active";
        public static final String STATUS_NOT_CONNECTED = STATUS_PREFIX + "not_connected";
        public static final String STATUS_CONNECTED = STATUS_PREFIX + "connected";
        public static final String STATUS_OUTPUT = STATUS_PREFIX + "output";
        public static final String STATUS_INPUT = STATUS_PREFIX + "input";
        public static final String STATUS_CURRENT_SELECTED = STATUS_PREFIX + "current_selected";

        public static final String CHAT_PREFIX = GUI_PREFIX + "advanced_memory_card.chat.";
        public static final String CHAT_LOCATION_INFO = CHAT_PREFIX + "location_info";
        public static final String CHAT_CLICK_TO_TELEPORT = CHAT_PREFIX + "click_to_teleport";
        public static final String CHAT_TELEPORT_HOVER = CHAT_PREFIX + "teleport_hover";
        public static final String CHAT_FREQ_HIGHLIGHT_HEADER = CHAT_PREFIX + "freq_highlight_header";
        public static final String CHAT_TELEPORT_BUTTON = CHAT_PREFIX + "teleport_button";
        public static final String CHAT_TELEPORT_USAGE = CHAT_PREFIX + "teleport_usage";
        public static final String CHAT_TELEPORT_ONLY_PLAYER = CHAT_PREFIX + "teleport_only_player";
        public static final String CHAT_TELEPORT_DIM_FAILED = CHAT_PREFIX + "teleport_dim_failed";
        public static final String CHAT_TELEPORT_SUCCESS = CHAT_PREFIX + "teleport_success";

        public static final String CONFIG_MODE_PREFIX = GUI_PREFIX + "advanced_memory_card.player.config.";
        public static final String CONFIG_MODE_SHOW = CONFIG_MODE_PREFIX + "show";

        public static final String TOOLTIP_PREFIX = GUI_PREFIX + "advanced_memory_card.tooltip.";
        public static final String TOOLTIP_COPY_INFO = TOOLTIP_PREFIX + "copy.info";
        public static final String TOOLTIP_COPY_FIRST_POS = TOOLTIP_PREFIX + "copy.first_pos";
        public static final String TOOLTIP_COPY_SECOND_POS = TOOLTIP_PREFIX + "copy.second_pos";
        public static final String TOOLTIP_COPY_READY = TOOLTIP_PREFIX + "copy.ready";
        public static final String TOOLTIP_CONFIG_INFO = TOOLTIP_PREFIX + "config.info";
    }

    public static String translate(String key, Object... args) {
        return I18n.format(key, args);
    }

    public static String formatCoordinates(int x, int y, int z) {
        return String.format("[%d, %d, %d]", x, y, z);
    }

    public static String formatAreaSize(int width, int height, int depth) {
        return String.format("[%d×%d×%d]", width, height, depth);
    }

    public static class CopyMode {
        public static String show() { return translate(Keys.COPY_MODE_SHOW); }
        public static String firstPosMarked(String pos) { return translate(Keys.COPY_MODE_FIRST_POS_MARKED, pos); }
        public static String secondPosMarked(String pos, int w, int h, int d) { return translate(Keys.COPY_MODE_SECOND_POS_MARKED, pos, w, h, d); }
        public static String tooLarge(long cur, long max) { return translate(Keys.COPY_MODE_TOO_LARGE, cur, max); }
        public static String completed(long count) { return translate(Keys.COPY_MODE_COMPLETED, count); }
        public static String alreadyMarked() { return translate(Keys.COPY_MODE_ALREADY_MARKED); }
        public static String failed() { return translate(Keys.COPY_MODE_FAILED); }
        public static String screenStartPos() { return translate(Keys.COPY_MODE_SCREEN_START_POS); }
        public static String screenEndPos() { return translate(Keys.COPY_MODE_SCREEN_END_POS); }
    }

    public static class ConfigMode {
        public static String show() { return translate(Keys.CONFIG_MODE_SHOW); }
    }

    public static class Tooltip {
        public static String copyInfo() { return translate(Keys.TOOLTIP_COPY_INFO); }
        public static String copyFirstPos(String pos) { return translate(Keys.TOOLTIP_COPY_FIRST_POS, pos); }
        public static String copySecondPos(String pos) { return translate(Keys.TOOLTIP_COPY_SECOND_POS, pos); }
        public static String copyReady() { return translate(Keys.TOOLTIP_COPY_READY); }
        public static String configInfo() { return translate(Keys.TOOLTIP_CONFIG_INFO); }
    }

    public static class Common {
        public static String itemName() { return translate(Keys.ITEM_ADVANCED_MEMORY_CARD); }
        public static String itemGroupName() { return translate(Keys.ITEM_GROUP_TAB); }
    }

    public static class P2PTree {
        public static String empty() { return translate(Keys.P2P_TREE_EMPTY); }
        public static String unknownP2P() { return translate(Keys.P2P_TREE_UNKNOWN_P2P); }
        public static String searchResults() { return translate(Keys.P2P_TREE_SEARCH_RESULTS); }
        public static String searchResultCount(int count) { return translate(Keys.P2P_TREE_SEARCH_RESULT_COUNT, count); }
    }

    public static class DetailPanel {
        public static String placeholder() { return translate(Keys.DETAIL_PANEL_PLACEHOLDER); }
        public static String nameLabel() { return translate(Keys.DETAIL_PANEL_NAME_LABEL); }
        public static String frequencyLabel() { return translate(Keys.DETAIL_PANEL_FREQUENCY_LABEL); }
        public static String typeLabel() { return translate(Keys.DETAIL_PANEL_TYPE_LABEL); }
        public static String dimensionLabel() { return translate(Keys.DETAIL_PANEL_DIMENSION_LABEL); }
        public static String positionLabel() { return translate(Keys.DETAIL_PANEL_POSITION_LABEL); }
        public static String directionLabel() { return translate(Keys.DETAIL_PANEL_DIRECTION_LABEL); }
        public static String statusLabel() { return translate(Keys.DETAIL_PANEL_STATUS_LABEL); }
        public static String channelInfoLabel() { return translate(Keys.DETAIL_PANEL_CHANNEL_INFO_LABEL); }
        public static String p2pCountLabel() { return translate(Keys.DETAIL_PANEL_P2P_COUNT_LABEL); }
        public static String inputCountLabel() { return translate(Keys.DETAIL_PANEL_INPUT_COUNT_LABEL); }
        public static String outputCountLabel() { return translate(Keys.DETAIL_PANEL_OUTPUT_COUNT_LABEL); }
        public static String totalCountLabel() { return translate(Keys.DETAIL_PANEL_TOTAL_COUNT_LABEL); }
        public static String channelCountLabel() { return translate(Keys.DETAIL_PANEL_CHANNEL_COUNT_LABEL); }
        public static String inputNameLabel() { return translate(Keys.DETAIL_PANEL_INPUT_NAME_LABEL); }
    }

    public static class Button {
        public static String rename() { return translate(Keys.BUTTON_RENAME); }
        public static String select() { return translate(Keys.BUTTON_SELECT); }
        public static String highlight() { return translate(Keys.BUTTON_HIGHLIGHT); }
        public static String pendingBind() { return translate(Keys.BUTTON_PENDING_BIND); }
        public static String refresh() { return translate(Keys.BUTTON_REFRESH); }
        public static String cancel() { return translate(Keys.BUTTON_CANCEL); }
        public static String bind() { return translate(Keys.BUTTON_BIND); }
        public static String locate() { return translate(Keys.BUTTON_LOCATE); }
        public static String initP2P() { return translate(Keys.BUTTON_INIT_P2P); }
        public static String assignFreq() { return translate(Keys.BUTTON_ASSIGN_FREQ); }
    }

    public static class Status {
        public static String notActive() { return translate(Keys.STATUS_NOT_ACTIVE); }
        public static String notConnected() { return translate(Keys.STATUS_NOT_CONNECTED); }
        public static String connected() { return translate(Keys.STATUS_CONNECTED); }
        public static String output() { return translate(Keys.STATUS_OUTPUT); }
        public static String input() { return translate(Keys.STATUS_INPUT); }
        public static String currentSelected() { return translate(Keys.STATUS_CURRENT_SELECTED); }
    }

    public static class Chat {
        public static String locationInfo(Object... args) { return translate(Keys.CHAT_LOCATION_INFO, args); }
        public static String clickToTeleport() { return translate(Keys.CHAT_CLICK_TO_TELEPORT); }
        public static String teleportHover(Object... args) { return translate(Keys.CHAT_TELEPORT_HOVER, args); }
        public static String freqHighlightHeader(Object... args) { return translate(Keys.CHAT_FREQ_HIGHLIGHT_HEADER, args); }
        public static String teleportButton() { return translate(Keys.CHAT_TELEPORT_BUTTON); }
        public static String teleportUsage() { return translate(Keys.CHAT_TELEPORT_USAGE); }
        public static String teleportOnlyPlayer() { return translate(Keys.CHAT_TELEPORT_ONLY_PLAYER); }
        public static String teleportDimFailed() { return translate(Keys.CHAT_TELEPORT_DIM_FAILED); }
        public static String teleportSuccess(Object... args) { return translate(Keys.CHAT_TELEPORT_SUCCESS, args); }
    }
}
