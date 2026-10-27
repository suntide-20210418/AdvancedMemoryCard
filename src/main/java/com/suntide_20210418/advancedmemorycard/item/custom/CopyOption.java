package com.suntide_20210418.advancedmemorycard.item.custom;

import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;

/**
 * 复制模式的可选项。对应 1.21.1 的 CopyOption，但 1.12.2 的 AE2 没有
 * MemoryCardMessages.ExportSettings 这套分段掩码，只能在粘贴侧自己按项落地。
 *
 * <p>顺序即界面顺序，位序即掩码位。当前含 7 项：AE2 1.12.2 的
 * {@code downloadSettings} 从不导出 upgrades（全仓没有对应的 writeToNBT），
 * 卡片里没有这段数据，因此「升级」不在其中——不做一个永远空转的开关。
 */
public enum CopyOption {
    SETTINGS("settings"),
    PRIORITY("priority"),
    CONFIG_INV("config_inv"),
    PATTERNS("patterns"),
    P2P_TYPE("p2p_type"),
    P2P_FREQUENCY("p2p_frequency"),
    NAME("name");

    private final String id;

    CopyOption(String id) {
        this.id = id;
    }

    public int bit() {
        return 1 << ordinal();
    }

    public String translationKey() {
        return TranslateHelper.Keys.COPY_OPTION_PREFIX + id;
    }

    public static final CopyOption[] VALUES = values();

    public static int allBits() {
        return (1 << VALUES.length) - 1;
    }
}
