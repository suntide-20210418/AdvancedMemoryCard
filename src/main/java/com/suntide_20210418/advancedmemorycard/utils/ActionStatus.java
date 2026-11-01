package com.suntide_20210418.advancedmemorycard.utils;

/**
 * 服务端 → 客户端的操作结果码。
 *
 * <p>与 1.21.1 的同名类同码表、同 severity。差异只有一个：这个版本返回
 * {@code String}（1.12.2 的 {@code TranslateHelper.translate} 就是 String），
 * 码表与语义不许各版本自行增删。
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

    private ActionStatus() {
    }

    public static int severity(int code) {
        switch (code) {
            case BIND_OK:
            case ASSIGNED:
            case RENAMED:
            case REFRESHED:
            case SOURCE_SELECTED:
            case BIND_BATCH_OK:
            case SWITCHED_TO_INPUT:
                return SEV_OK;
            case BIND_NO_TARGET:
            case BIND_INVALID_FREQ:
            case RENAME_FAILED:
            case BIND_BATCH_PARTIAL:
            case SWITCH_NO_CHANNEL:
            case SWITCH_ALREADY_INPUT:
                return SEV_WARN;
            case BIND_NO_GRID:
            case SWITCH_FAILED:
                return SEV_ERROR;
            default:
                return SEV_NEUTRAL;
        }
    }

    /** 客户端渲染用：把码翻成当前语言的一句话。 */
    public static String message(int code, String param) {
        String p = param == null ? "" : param;
        switch (code) {
            case BIND_OK:
                return TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_BIND_OK, p);
            case BIND_NO_TARGET:
                return TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_BIND_NO_TARGET);
            case BIND_INVALID_FREQ:
                return TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_BIND_INVALID_FREQ, p);
            case BIND_NO_GRID:
                return TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_BIND_NO_GRID);
            case SOURCE_SELECTED:
                return TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_SOURCE_SELECTED, p);
            case ASSIGNED:
                return TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_ASSIGNED, p);
            case RENAMED:
                return TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_RENAMED, p);
            case REFRESHED:
                return TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_REFRESHED);
            case RENAME_FAILED:
                return TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_RENAME_FAILED);
            case SWITCHED_TO_INPUT:
                return TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_SWITCHED_TO_INPUT, p);
            case SWITCH_NO_CHANNEL:
                return TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_SWITCH_NO_CHANNEL, p);
            case SWITCH_ALREADY_INPUT:
                return TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_SWITCH_ALREADY_INPUT);
            case SWITCH_FAILED:
                return TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_SWITCH_FAILED);
            case BIND_BATCH_OK: {
                String[] parts = p.split("\\|");
                return TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_BIND_BATCH_OK,
                        parts.length > 0 ? parts[0] : "0",
                        parts.length > 1 ? parts[1] : "");
            }
            case BIND_BATCH_PARTIAL: {
                String[] parts = p.split("\\|");
                return TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_BIND_BATCH_PARTIAL,
                        parts.length > 0 ? parts[0] : "0",
                        parts.length > 1 ? parts[1] : "0",
                        parts.length > 2 ? parts[2] : "");
            }
            default:
                return TranslateHelper.translate(TranslateHelper.Keys.STATUS_LINE_IDLE);
        }
    }
}
