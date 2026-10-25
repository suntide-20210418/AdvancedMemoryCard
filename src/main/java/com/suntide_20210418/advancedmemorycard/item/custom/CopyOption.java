package com.suntide_20210418.advancedmemorycard.item.custom;

import appeng.api.ids.AEComponents;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;
import java.util.function.Predicate;
import net.minecraft.core.component.DataComponentType;

/**
 * 复制模式粘贴时可选择「应用哪些内容」的选项。
 *
 * <p>每一项都直接对应 AE2 记忆卡在导出设置时写到卡片上的一个 {@link DataComponentType}：
 * 导入方从传入的 {@code DataComponentMap} 里取这些组件，取不到时按各自实现决定是「跳过」
 * 还是「清空」—— 这正是 {@link #preserveTargetValue} 存在的原因：
 *
 * <ul>
 *   <li>跳过型（列表里为 {@code false}）：组件缺失 → AE2 直接不处理，目标保持原值。
 *       因此「不应用」= 从交给导入方的组件表里剔掉即可。</li>
 *   <li>清空型（列表里为 {@code true}）：组件缺失 → AE2 会把目标写成「空值」
 *       （名称被置 null、样板被清空、P2P 类型分支整段不执行）。这类选项不能靠移除表达
 *       「不应用」，必须在粘贴时用目标自己导出的值回填。</li>
 * </ul>
 *
 * <p>组件声明为 {@link DataComponentType}{@code <?>}：各项泛型互不相同
 * （{@code ExportedUpgrades} / {@code Map<String>} / {@code Integer} / {@code List<GenericStack>} …），
 * 而调用点只用它做 {@code ItemStack#remove/set}，不关心具体类型。
 */
public enum CopyOption {
    /** 升级卡：{@code MemoryCardItem#restoreUpgrades}，缺失即跳过。 */
    UPGRADES(
            AEComponents.EXPORTED_UPGRADES,
            TranslateHelper.Keys.COPY_MODE_OPTION_UPGRADES,
            1 << 0,
            false),
    /** 通用配置（{@code IConfigurableObject}），缺失即跳过。 */
    SETTINGS(
            AEComponents.EXPORTED_SETTINGS,
            TranslateHelper.Keys.COPY_MODE_OPTION_SETTINGS,
            1 << 1,
            false),
    /** 优先级，缺失即跳过。 */
    PRIORITY(
            AEComponents.EXPORTED_PRIORITY,
            TranslateHelper.Keys.COPY_MODE_OPTION_PRIORITY,
            1 << 2,
            false),
    /** 配置槽（过滤/标记物品等），缺失即跳过。 */
    CONFIG_INV(
            AEComponents.EXPORTED_CONFIG_INV,
            TranslateHelper.Keys.COPY_MODE_OPTION_CONFIG_INV,
            1 << 3,
            false),
    /**
     * 样板：{@code PatternProviderLogic#importSettings} 用
     * {@code getOrDefault(EXPORTED_PATTERNS, EMPTY)} → 缺失会先清空目标样板再按空内容回填。
     */
    PATTERNS(
            AEComponents.EXPORTED_PATTERNS,
            TranslateHelper.Keys.COPY_MODE_OPTION_PATTERNS,
            1 << 4,
            true),
    /**
     * P2P 类型：{@code P2PTunnelPart#onUseItemOn} 必须读到该组件才会进入「换型 + 导入」分支，
     * 缺失会导致对部件整个粘贴动作都不执行（连升级/设置一起失效）。
     */
    P2P_TYPE(
            AEComponents.EXPORTED_P2P_TYPE,
            TranslateHelper.Keys.COPY_MODE_OPTION_P2P_TYPE,
            1 << 5,
            true),
    /** P2P 频率：{@code P2PTunnelPart#importSettings} 里 null 会跳过，属于跳过型。 */
    P2P_FREQUENCY(
            AEComponents.EXPORTED_P2P_FREQUENCY,
            TranslateHelper.Keys.COPY_MODE_OPTION_P2P_FREQUENCY,
            1 << 6,
            false),
    /** 名称：{@code AEBaseBlockEntity/AEBasePart#importSettings} 直接把组件值赋给 customName，缺失即清空名称。 */
    CUSTOM_NAME(
            AEComponents.EXPORTED_CUSTOM_NAME,
            TranslateHelper.Keys.COPY_MODE_OPTION_CUSTOM_NAME,
            1 << 7,
            true);

    /** 全部选中时的掩码，也是从未配置过的卡片的默认值。 */
    public static final int ALL = 0xFF;

    private final DataComponentType<?> component;
    private final String translationKey;
    private final int mask;
    private final boolean preserveTargetValue;

    CopyOption(DataComponentType<?> component, String translationKey, int mask, boolean preserveTargetValue) {
        this.component = component;
        this.translationKey = translationKey;
        this.mask = mask;
        this.preserveTargetValue = preserveTargetValue;
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

    /** 未勾选时是否必须用目标自身的当前值回填（缺失会把目标写成空值）。 */
    public boolean preserveTargetValue() {
        return preserveTargetValue;
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

    /** 是否存在「未勾选时必须回填目标值」的选项，用于跳过整段回填开销。 */
    public static boolean needsTargetValues(int mask) {
        for (CopyOption option : values()) {
            if (option.preserveTargetValue && (mask & option.mask) == 0) {
                return true;
            }
        }
        return false;
    }
}
