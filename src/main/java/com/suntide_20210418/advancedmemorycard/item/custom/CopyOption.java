package com.suntide_20210418.advancedmemorycard.item.custom;

import appeng.api.config.Setting;
import appeng.api.util.IConfigManager;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * 复制模式粘贴时可选择「应用哪些内容」的选项（1.20.1 / NBT 版）。
 *
 * <p>每一项对应 AE2 记忆卡 {@code Data} 标签下的一个键。与 1.21.1 的 DataComponent 版语义
 * 一一对应，但 NBT 体系里有两个比组件体系更麻烦的地方：
 *
 * <ul>
 *   <li><b>SETTINGS 不是单键</b>：{@code IConfigManager#writeToNBT} 把每个设置写成
 *       「设置名 → 字符串」平铺在顶层，键名取决于目标方块的类型。因此它无法在「卡片层」
 *       统一剔除，只能在粘贴时按<b>目标自身的设置集合</b>逐个删除。</li>
 *   <li><b>自定义名是嵌套键</b>：{@code CustomNameUtil} 写在 {@code display.Name}，
 *       不是顶层的独立键。</li>
 * </ul>
 *
 * <p>其余语义与 1.21.1 完全一致：跳过型（{@code false}）未勾选时移除键即可；清空型
 * （{@code true}）未勾选时必须用目标自身的导出值回填，因为 AE2 把「键缺失」当成「清空目标」
 * ——样板会被清、名称会被置 null、P2P 类型分支整段不执行。
 */
public enum CopyOption {
    /** 升级卡：{@code MemoryCardItem#restoreUpgrades}，缺 {@code upgrades} 即跳过。 */
    UPGRADES(TranslateHelper.Keys.COPY_MODE_OPTION_UPGRADES, 1 << 0, false, "upgrades"),
    /** 通用配置：键名随目标类型变化，见类注释，需要按目标设置集合剔除。 */
    SETTINGS(TranslateHelper.Keys.COPY_MODE_OPTION_SETTINGS, 1 << 1, false, null),
    /** 优先级，缺 {@code priority} 即跳过。 */
    PRIORITY(TranslateHelper.Keys.COPY_MODE_OPTION_PRIORITY, 1 << 2, false, "priority"),
    /** 配置槽（过滤/标记物品），缺 {@code config} 即跳过。 */
    CONFIG_INV(TranslateHelper.Keys.COPY_MODE_OPTION_CONFIG_INV, 1 << 3, false, "config"),
    /**
     * 样板：{@code PatternProviderLogic#importSettings} 用
     * {@code getOrDefault(NBT_MEMORY_CARD_PATTERNS, EMPTY)} → 缺失会先清空目标样板。
     */
    PATTERNS(TranslateHelper.Keys.COPY_MODE_OPTION_PATTERNS, 1 << 4, true, "patterns"),
    /**
     * P2P 类型：{@code PartP2PTunnel#onPartActivate} 必须读到 {@code p2pType} 才会进入
     * 「换型 + 导入」分支，缺失时整个粘贴动作不执行（连升级/设置一起失效）。
     */
    P2P_TYPE(TranslateHelper.Keys.COPY_MODE_OPTION_P2P_TYPE, 1 << 5, true, "p2pType"),
    /** P2P 频率：{@code importSettings} 里 {@code contains} 判断，缺失即跳过。 */
    P2P_FREQUENCY(TranslateHelper.Keys.COPY_MODE_OPTION_P2P_FREQUENCY, 1 << 6, false, "p2pFreq"),
    /** 名称：{@code AEBaseBlockEntity/AEBasePart#importSettings} 直接把值赋给 customName，缺失即清空。 */
    CUSTOM_NAME(TranslateHelper.Keys.COPY_MODE_OPTION_CUSTOM_NAME, 1 << 7, true, null);

    /** 全部选中时的掩码。 */
    public static final int ALL = 0xFF;

    private static final String DISPLAY_TAG = "display";
    private static final String NAME_TAG = "Name";

    private final String translationKey;
    private final int mask;
    private final boolean preserveTargetValue;
    private final String nbtKey;

    CopyOption(String translationKey, int mask, boolean preserveTargetValue, String nbtKey) {
        this.translationKey = translationKey;
        this.mask = mask;
        this.preserveTargetValue = preserveTargetValue;
        this.nbtKey = nbtKey;
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

    public boolean enabled(int mask) {
        return (mask & this.mask) != 0;
    }

    /** 按选中状态汇总出掩码。 */
    public static int maskOf(Predicate<CopyOption> enabled) {
        int mask = 0;
        for (CopyOption option : values()) {
            if (enabled.test(option)) {
                mask |= option.mask;
            }
        }
        return mask;
    }

    /** 是否存在「未勾选时必须回填目标值」的选项，用于跳过整段导出开销。 */
    public static boolean needsTargetValues(int mask) {
        for (CopyOption option : values()) {
            if (option.preserveTargetValue && !option.enabled(mask)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 卡片数据副本：把所有未勾选的选项从标签里剔掉。
     *
     * <p>SETTINGS 在这里不动（没有固定键名），由调用方按目标自身设置集合再删。
     */
    public static CompoundTag createFiltered(CompoundTag source, int mask) {
        CompoundTag filtered = source.copy();
        for (CopyOption option : values()) {
            if (option.enabled(mask)) {
                continue;
            }
            option.removeFrom(filtered);
        }
        return filtered;
    }

    /**
     * 用目标自身导出的值回填「未勾选但不能靠缺失表达」的选项。
     */
    public static void restoreTargetValues(CompoundTag filtered, CompoundTag targetExport, int mask) {
        for (CopyOption option : values()) {
            if (!option.preserveTargetValue || option.enabled(mask)) {
                continue;
            }
            option.copyFrom(filtered, targetExport);
        }
    }

    /**
     * 按目标的设置集合删除未勾选时为「跳过型」的配置键。
     *
     * <p>只在 SETTINGS 未勾选时调用；{@code getSettings()} 返回的是目标自己注册过的键名，
     * 因此不会误删同名的其它数据（例如 P2P 的类型/频段）。
     */
    public static void removeTargetSettings(CompoundTag tag, IConfigManager configManager) {
        if (configManager == null) {
            return;
        }
        for (Setting<?> setting : configManager.getSettings()) {
            tag.remove(setting.getName());
        }
    }

    private void removeFrom(CompoundTag tag) {
        if (nbtKey != null) {
            tag.remove(nbtKey);
            return;
        }
        if (this == CUSTOM_NAME) {
            CompoundTag display = tag.getCompound(DISPLAY_TAG);
            display.remove(NAME_TAG);
            if (display.isEmpty()) {
                tag.remove(DISPLAY_TAG);
            } else {
                tag.put(DISPLAY_TAG, display);
            }
        }
        // SETTINGS：无固定键名，由 CopyMode 按目标设置集合处理
    }

    private void copyFrom(CompoundTag filtered, CompoundTag targetExport) {
        if (nbtKey != null) {
            Tag value = targetExport.get(nbtKey);
            if (value != null) {
                filtered.put(nbtKey, value.copy());
            }
            return;
        }
        if (this == CUSTOM_NAME) {
            CompoundTag targetDisplay = targetExport.getCompound(DISPLAY_TAG);
            if (targetDisplay.contains(NAME_TAG, Tag.TAG_STRING)) {
                CompoundTag display = filtered.getCompound(DISPLAY_TAG);
                display.putString(NAME_TAG, targetDisplay.getString(NAME_TAG));
                filtered.put(DISPLAY_TAG, display);
            }
        }
    }
}
