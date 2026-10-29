package com.suntide_20210418.advancedmemorycard.item.custom;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 粘贴选项与「复制后自动弹出」开关的存储。
 *
 * <p>刻意写在卡片根标签的 {@code copy_options} 下，而不是 {@code mode_data} 里：
 * {@link CardMode#save(NBTTagCompound)} 每次切换模式或重新复制都会重建 {@code mode_data}，
 * 放在里面的选项会被重置掉。
 *
 * <p>默认掩码为 0（全不选），与 1.12.2 / 1.20.1 的行为一致：未配置过的卡片粘贴时不改动
 * 任何可选项，需要玩家显式勾选。
 */
public final class CopyOptions {

    private static final String ROOT = "copy_options";
    private static final String MASK = "mask";
    private static final String AUTO_OPEN = "auto_open";

    private final int mask;
    private final boolean autoOpen;

    private CopyOptions(int mask, boolean autoOpen) {
        this.mask = mask & CopyOption.allBits();
        this.autoOpen = autoOpen;
    }

    /**
     * 读取卡片上的选项。1.7.10 侧没有 {@code ItemStack.EMPTY}，客户端菜单也可能拿不到
     * 权威物品（构造期物品被切走），所以这里必须容忍 null。
     */
    public static CopyOptions of(ItemStack stack) {
        if (stack == null) {
            return new CopyOptions(0, true);
        }
        NBTTagCompound root = stack.getTagCompound();
        if (root == null || !root.hasKey(ROOT)) {
            return new CopyOptions(0, true);
        }
        NBTTagCompound tag = root.getCompoundTag(ROOT);
        // 老卡片没有 auto_open 键时按默认开启处理
        boolean autoOpen = !tag.hasKey(AUTO_OPEN) || tag.getBoolean(AUTO_OPEN);
        return new CopyOptions(tag.getInteger(MASK), autoOpen);
    }

    public int mask() {
        return mask;
    }

    public boolean has(CopyOption option) {
        return (mask & option.bit()) != 0;
    }

    public boolean isAutoOpen() {
        return autoOpen;
    }

    public CopyOptions with(CopyOption option, boolean enabled) {
        int next = enabled ? (mask | option.bit()) : (mask & ~option.bit());
        return new CopyOptions(next, autoOpen);
    }

    public CopyOptions withMask(int newMask) {
        return new CopyOptions(newMask, autoOpen);
    }

    public CopyOptions withAutoOpen(boolean enabled) {
        return new CopyOptions(mask, enabled);
    }

    public void save(ItemStack stack) {
        if (stack == null) {
            return;
        }
        NBTTagCompound root = CardMode.getOrCreateRoot(stack);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(MASK, mask);
        tag.setBoolean(AUTO_OPEN, autoOpen);
        root.setTag(ROOT, tag);
    }

    @Override
    public String toString() {
        return "CopyOptions{mask=" + Integer.toBinaryString(mask) + ", autoOpen=" + autoOpen + '}';
    }
}
