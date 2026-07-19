package com.suntide_20210418.advancedmemorycard.client;

/**
 * 客户端模型注册器（1.7.10 适配）。
 *
 * <p>
 * 1.7.10 不使用 1.8+ 的 JSON 模型与 {@code ItemModelMesher}，物品贴图通过
 * {@code Item.registerIcons(IIconRegister)} 在贴图缝合阶段自动注册（见
 * {@code AdvancedMemoryCardItem#registerIcons}）。因此此处无需任何运行时注册，保留空实现以兼容调用点。
 * </p>
 */
public class ClientModelRegistry {

    public static void registerModels() {
        // 1.7.10: 物品贴图由 AdvancedMemoryCardItem.registerIcons(IIconRegister) 处理，无需此处注册。
    }
}
