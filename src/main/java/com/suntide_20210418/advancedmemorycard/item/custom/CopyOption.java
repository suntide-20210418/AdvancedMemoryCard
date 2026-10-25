package com.suntide_20210418.advancedmemorycard.item.custom;

import appeng.api.ids.AEComponents;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;
import java.util.function.Predicate;
import net.minecraft.core.component.DataComponentType;

/**
 * 复制模式粘贴时可选择「应用哪些内容」的选项。
 *
 * <p>每一项都直接对应 AE2 记忆卡在导出设置时写到卡片上的一个 {@link DataComponentType}
 * （见 {@code appeng.items.tools.MemoryCardItem#importGenericSettings}）：
 * 导入方只会从传入的 {@code DataComponentMap} 里取这几个组件，取不到就跳过对应步骤。
 * 因此「不应用某项」等价于「把该组件从交给导入方的组件表里剔掉」，
 * 不需要在 AE2 的导入流程里插任何钩子。
 *
 * <p>组件声明为 {@link DataComponentType}{@code <?>}：四者泛型互不相同
 * （{@code ExportedUpgrades} / {@code Map<String>} / {@code Integer} / {@code List<GenericStack>}），
 * 而调用点只用它做 {@code ItemStack#remove}，不关心具体类型。
 */
public enum CopyOption {
    UPGRADES(
            AEComponents.EXPORTED_UPGRADES,
            TranslateHelper.Keys.COPY_MODE_OPTION_UPGRADES,
            1 << 0),
    SETTINGS(
            AEComponents.EXPORTED_SETTINGS,
            TranslateHelper.Keys.COPY_MODE_OPTION_SETTINGS,
            1 << 1),
    PRIORITY(
            AEComponents.EXPORTED_PRIORITY,
            TranslateHelper.Keys.COPY_MODE_OPTION_PRIORITY,
            1 << 2),
    CONFIG_INV(
            AEComponents.EXPORTED_CONFIG_INV,
            TranslateHelper.Keys.COPY_MODE_OPTION_CONFIG_INV,
            1 << 3);

    /** 4 项全部选中时的掩码，也是新建卡片的默认值（与改造前行为一致）。 */
    public static final int ALL = 0xF;

    private final DataComponentType<?> component;
    private final String translationKey;
    private final int mask;

    CopyOption(DataComponentType<?> component, String translationKey, int mask) {
        this.component = component;
        this.translationKey = translationKey;
        this.mask = mask;
    }

    public DataComponentType<?> component() {
        return component;
    }

    public String translationKey() {
        return translationKey;
    }

    public int mask() {
        return mask;
    }

    /** 按选中状态汇总出掩码，用于从 UI 状态生成待提交的值。 */
    public static int maskOf(Predicate<CopyOption> enabled) {
        int mask = 0;
        for (CopyOption option : values()) {
            if (enabled.test(option)) {
                mask |= option.mask;
            }
        }
        return mask;
    }
}
