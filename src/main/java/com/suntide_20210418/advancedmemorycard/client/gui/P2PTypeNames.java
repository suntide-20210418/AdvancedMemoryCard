package com.suntide_20210418.advancedmemorycard.client.gui;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * P2P 类型 → 显示名。
 *
 * <p>协议里传的是 {@code IPartItem.getId(...).getPath()}（例如 {@code me_p2p_tunnel}），
 * 直接画到界面上就是一句资源路径。这里把它换回对应部件的物品名：
 * {@code ae2:me_p2p_tunnel} → {@code item.ae2.me_p2p_tunnel} → “ME P2P 隧道”。
 *
 * <p>用物品的 translation key 而不是把中文写死，这样语言切换、以及 AE2 自己改译名
 * 都能自动跟上。查不到的（第三方模组的 P2P 部件只给了 path，没有 namespace）
 * 退回原始字符串 —— 至少不会显示成空白。
 */
public final class P2PTypeNames {

    private static final Map<String, String> LANG_KEY_CACHE = new HashMap<>();

    private P2PTypeNames() {}

    public static Component of(String typePath) {
        if (typePath == null || typePath.isEmpty()) {
            return Component.literal("—");
        }
        String key = LANG_KEY_CACHE.computeIfAbsent(typePath, P2PTypeNames::resolveLangKey);
        return key.isEmpty() ? Component.literal(typePath) : Component.translatable(key);
    }

    public static String string(String typePath) {
        return of(typePath).getString();
    }

    private static String resolveLangKey(String typePath) {
        ResourceLocation id = ResourceLocation.tryBuild("ae2", typePath);
        if (id == null) {
            return "";
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) {
            return "";
        }
        String key = item.getDescriptionId();
        // 有物品但没译名时，宁可显示原始 path，也不要把 "item.ae2.xxx" 这种键名画上去
        return I18n.exists(key) ? key : "";
    }
}
