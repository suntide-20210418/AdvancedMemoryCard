package com.suntide_20210418.advancedmemorycard.item.custom;

import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;

/**
 * 复制模式的可选项。
 *
 * <p>顺序即界面顺序，位序即掩码位。当前含 7 项：AE2 的 {@code downloadSettings} 从不导出
 * upgrades，卡片里没有这段数据，因此「升级」不在其中——不做一个永远空转的开关。
 *
 * <p>1.7.10 适配说明：rv3 的 {@code downloadSettings} 只写 display / configManager / priority /
 * config 四段，样板（patterns）属于存档路径（{@code DualityInterface#writeToNBT}），不会进入内存卡。
 * 于是 {@link #PATTERNS} 在 1.7.10 上恒为 UNTOUCHED（{@code SettingsApplier} 找不到该段就跳过）。
 * 这里仍然保留该项与它的位序：掩码位由 {@code ordinal()} 决定，删项会让后续位号整体前移，
 * 使同一张卡片在不同版本间读出的 mask 语义错位。
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
