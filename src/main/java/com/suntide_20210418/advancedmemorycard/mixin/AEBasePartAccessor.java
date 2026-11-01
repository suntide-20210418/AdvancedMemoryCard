package com.suntide_20210418.advancedmemorycard.mixin;

import appeng.parts.AEBasePart;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 直写 {@link AEBasePart#customName} 字段。
 *
 * <p>AE2 没有公开的 setName，唯一的官方写入口是
 * {@code importSettings(SettingsFrom.MEMORY_CARD, ...)}，而它会顺带执行
 * {@code MemoryCardItem.importGenericSettings}（升级卡 / 优先级 / IConfigurableObject 配置），
 * 并且 {@code P2PTunnelPart#importSettings} 在数据里带 EXPORTED_P2P_FREQUENCY 时还会
 * 调用 {@code setOutput(true)} + {@code updateFreq} —— 对"只改名字"这件事来说副作用太大。
 *
 * <p>因此改名改走字段访问器：只动 customName，其余状态一概不碰。
 * 写完之后由调用方负责 {@code markForSave()}（{@code writeToNBT} 只在 customName 非 null
 * 时写这个键，静默改名失败时存档里连键都不会有）与 {@code markForUpdate()}。
 */
@Mixin(AEBasePart.class)
public interface AEBasePartAccessor {

    @Accessor(value = "customName", remap = false)
    void setCustomName(Component customName);
}
