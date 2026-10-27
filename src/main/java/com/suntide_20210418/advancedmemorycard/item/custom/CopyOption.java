package com.suntide_20210418.advancedmemorycard.item.custom;

import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;

/**
 * 复制模式的可选项。对应 1.21.1 的 CopyOption，但 1.12.2 的 AE2 没有
 * MemoryCardMessages.ExportSettings 这套分段掩码，只能在粘贴侧自己按项落地。
 *
 * <p>顺序即界面顺序，位序即掩码位，改动会破坏已存档卡片上的掩码，不要调整。
 */
public enum CopyOption {
    UPGRADES("upgrades"),
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
