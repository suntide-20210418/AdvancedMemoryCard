package com.suntide_20210418.advancedmemorycard.client.gui;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.StatCollector;

/**
 * P2P 类型 → 显示名。
 *
 * <p>
 * 1.7.10 的协议里传的是<b>部件类名的后缀</b>（{@code PartP2PTunnelME} → {@code ME}、
 * {@code PartP2PRedstone} → {@code Redstone}，见 {@code P2PManager.getP2PType}），
 * 不是 1.20.1/1.21.1 那种注册表路径（{@code me_p2p_tunnel}）。AE2 rv3 的 P2P 隧道
 * 是同一个物品 {@code appeng.items.parts.ItemMultiPart} 的多个 damage 值，物品注册表里
 * 根本没有按变体区分的键，所以「用 ResourceLocation 查物品再取译名」这条路在 1.7.10
 * 上既查不到东西、还会因为注册表键是 String 而抛 ClassCastException。
 *
 * <p>
 * 这里改成查本模组自己的语言键：{@code gui.advanced_memory_card.p2p_type.<小写 token>}。
 * 查不到（第三方模组的 P2P 部件）就退回原始 token —— 至少不会显示成空白。
 */
public final class P2PTypeNames {

    private static final String LANG_PREFIX = "gui.advanced_memory_card.p2p_type.";

    /** typePath → 可翻译的 key；空串表示没有对应译名。 */
    private static final Map<String, String> LANG_KEY_CACHE = new HashMap<>();

    private P2PTypeNames() {}

    public static String string(String p2pType) {
        if (p2pType == null || p2pType.isEmpty()) {
            return "—";
        }
        String key = LANG_KEY_CACHE.get(p2pType);
        if (key == null) {
            key = resolveLangKey(p2pType);
            LANG_KEY_CACHE.put(p2pType, key);
        }
        return key.isEmpty() ? p2pType : I18n.format(key);
    }

    /** 返回可翻译的 key；不可翻译返回空串（调用方退回原始 token）。 */
    private static String resolveLangKey(String p2pType) {
        String key = LANG_PREFIX + p2pType.toLowerCase(Locale.ROOT);
        // 有键但没译名时，宁可显示原始 token，也不要把 "gui.xxx" 这种键名画上去
        return StatCollector.canTranslate(key) ? key : "";
    }
}
