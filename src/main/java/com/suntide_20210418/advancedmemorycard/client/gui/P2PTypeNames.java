package com.suntide_20210418.advancedmemorycard.client.gui;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * P2P 类型 → 显示名。
 *
 * <p>协议里传的是 {@code IPartItem.getId(...).getPath()}（例如 {@code me_p2p_tunnel}），
 * 直接画到界面上就是一句资源路径。这里把它换回对应部件的物品名：
 * {@code ae2:me_p2p_tunnel} → {@code item.ae2.me_p2p_tunnel.name} → “ME P2P 隧道”。
 *
 * <p>用物品的 translation key 而不是把中文写死，这样语言切换、以及 AE2 自己改译名
 * 都能自动跟上。查不到的（第三方模组的 P2P 部件只给了 path，没有 namespace）
 * 退回原始字符串 —— 至少不会显示成空白。
 *
 * <p>1.12.2 的 {@code I18n} 没有 canTranslate/exists：未注册的 key 经 {@code format}
 * 会原样返回，所以用「翻译结果等于 key 本身」判定译名是否存在，缓存里直接存译文。
 */
public final class P2PTypeNames {

    /** typePath → 已解析的显示文本；空串表示解析失败（显示原始 path）。 */
    private static final Map<String, String> CACHE = new HashMap<>();

    private P2PTypeNames() {}

    public static String string(String typePath) {
        if (typePath == null || typePath.isEmpty()) {
            return "—";
        }
        String resolved = CACHE.get(typePath);
        if (resolved == null) {
            resolved = resolve(typePath);
            CACHE.put(typePath, resolved);
        }
        return resolved.isEmpty() ? typePath : resolved;
    }

    /** 解析显示文本；查不到物品或译名时返回空串。 */
    private static String resolve(String typePath) {
        ResourceLocation id = new ResourceLocation("ae2", typePath);
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null) {
            return "";
        }
        String key = item.getUnlocalizedName() + ".name";
        String translated = I18n.format(key);
        // 有物品但没译名时，I18n 会把 key 原样返回 —— 那宁可显示原始 path，
        // 也不要把 "item.ae2.xxx" 这种键名画上去
        return translated.equals(key) ? "" : translated;
    }
}
