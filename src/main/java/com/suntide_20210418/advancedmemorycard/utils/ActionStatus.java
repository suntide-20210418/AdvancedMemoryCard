package com.suntide_20210418.advancedmemorycard.utils;

import net.minecraft.network.chat.Component;

/**
 * 服务端 → 客户端的操作结果码。
 *
 * <p>为什么要这么一层：原来的所有失败路径都是裸 {@code return}，界面拿不到任何
 * “为什么没成功”的信息，玩家只能靠重新读树去猜。现在服务端只回传一个码 + 一个
 * 参数字符串，文案在客户端按语言文件渲染，避免在包里塞 Component。
 */
public final class ActionStatus {

    public static final int NONE = 0;
    /** 参数：频段 hex */
    public static final int BIND_OK = 1;
    public static final int BIND_NO_TARGET = 2;
    /** 参数：频段 hex */
    public static final int BIND_INVALID_FREQ = 3;
    public static final int BIND_NO_GRID = 4;
    /** 参数：设备名 */
    public static final int SOURCE_SELECTED = 5;
    /** 参数：频段 hex */
    public static final int ASSIGNED = 6;
    /** 参数：新名称 */
    public static final int RENAMED = 7;
    public static final int REFRESHED = 8;
    public static final int RENAME_FAILED = 9;
    /** 参数："成功台数|频段" */
    public static final int BIND_BATCH_OK = 10;
    /** 参数："成功台数|失败台数|频段" */
    public static final int BIND_BATCH_PARTIAL = 11;
    /** 参数：频段 hex */
    public static final int SWITCHED_TO_INPUT = 12;
    /** 参数：频段 hex */
    public static final int SWITCH_NO_CHANNEL = 13;
    public static final int SWITCH_ALREADY_INPUT = 14;
    public static final int SWITCH_FAILED = 15;

    // 语义等级：0 中性 / 1 成功 / 2 警告 / 3 错误
    public static final int SEV_NEUTRAL = 0;
    public static final int SEV_OK = 1;
    public static final int SEV_WARN = 2;
    public static final int SEV_ERROR = 3;

    private ActionStatus() {}

    public static int severity(int code) {
        return switch (code) {
            case BIND_OK, ASSIGNED, RENAMED, REFRESHED, SOURCE_SELECTED -> SEV_OK;
            case BIND_NO_TARGET, BIND_INVALID_FREQ, RENAME_FAILED -> SEV_WARN;
            case BIND_NO_GRID -> SEV_ERROR;
            case SWITCHED_TO_INPUT -> SEV_OK;
            case SWITCH_NO_CHANNEL, SWITCH_ALREADY_INPUT -> SEV_WARN;
            case SWITCH_FAILED -> SEV_ERROR;
            default -> SEV_NEUTRAL;
        };
    }

    /** 客户端渲染用：把码翻成当前语言的一句话。 */
    public static Component message(int code, String param) {
        String p = param == null ? "" : param;
        return switch (code) {
            case BIND_OK -> TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_BIND_OK, p);
            case BIND_NO_TARGET -> TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_BIND_NO_TARGET);
            case BIND_INVALID_FREQ -> TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_BIND_INVALID_FREQ, p);
            case BIND_NO_GRID -> TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_BIND_NO_GRID);
            case SOURCE_SELECTED -> TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_SOURCE_SELECTED, p);
            case ASSIGNED -> TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_ASSIGNED, p);
            case RENAMED -> TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_RENAMED, p);
            case REFRESHED -> TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_REFRESHED);
            case RENAME_FAILED -> TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_RENAME_FAILED);
            case SWITCHED_TO_INPUT -> TranslateHelper.translate(
                    TranslateHelper.Keys.STATUS_LINE_SWITCHED_TO_INPUT, p);
            case SWITCH_NO_CHANNEL -> TranslateHelper.translate(
                    TranslateHelper.Keys.STATUS_LINE_SWITCH_NO_CHANNEL, p);
            case SWITCH_ALREADY_INPUT -> TranslateHelper.translate(
                    TranslateHelper.Keys.STATUS_LINE_SWITCH_ALREADY_INPUT);
            case SWITCH_FAILED -> TranslateHelper.translate(
                    TranslateHelper.Keys.STATUS_LINE_SWITCH_FAILED);
            case BIND_BATCH_OK -> {
                String[] parts = p.split("\\|");
                yield TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_BIND_BATCH_OK,
                        parts.length > 0 ? parts[0] : "0",
                        parts.length > 1 ? parts[1] : "");
            }
            case BIND_BATCH_PARTIAL -> {
                String[] parts = p.split("\\|");
                yield TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_BIND_BATCH_PARTIAL,
                        parts.length > 0 ? parts[0] : "0",
                        parts.length > 1 ? parts[1] : "0",
                        parts.length > 2 ? parts[2] : "");
            }
            default -> TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_IDLE);
        };
    }
}
