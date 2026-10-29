package com.suntide_20210418.advancedmemorycard.utils;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.suntide_20210418.advancedmemorycard.item.custom.CopyOption;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyOptions;
import com.suntide_20210418.advancedmemorycard.p2p.P2PManager;

import appeng.api.implementations.tiles.ISegmentedInventory;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.PartItemStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.helpers.IPriorityHost;
import appeng.parts.AEBasePart;
import appeng.parts.p2p.PartP2PTunnel;
import appeng.tile.AEBaseTile;
import appeng.util.item.AEItemStack;

/**
 * 按 {@link CopyOptions} 把卡片上的配置写进目标方块/部件（AE2 rv3 / 1.7.10 版）。
 *
 * <p>
 * 这里刻意不走 {@code AEBaseTile#uploadSettings}：该方法把「缺少键」当成「清空目标」
 * （{@code compound.getInteger("priority")} 缺键即清零、{@code readFromNBT(compound,"config")}
 * 缺键即清空配置槽），而选择性粘贴需要「没勾选的项保持目标原值」。
 *
 * <p>
 * 与 1.12.2 版的四处硬差异（都由 rv3 的实现决定，不是风格选择）：
 * <ul>
 * <li>库存是 {@link IInventory} 而不是 {@code IItemHandlerModifiable}；</li>
 * <li>库存 NBT 是 compound（槽键 {@code "#0".."#n"}），不是 NBTTagList；</li>
 * <li>槽值由 {@code AEItemStack.writeToNBT} 写出，{@code Count} 恒为 0、真实数量在 {@code Cnt}，
 * 所以必须用 {@link AEItemStack#loadItemStackFromNBT} 反序列化；</li>
 * <li>频段是 long（`getShort` 会静默截断到 16 位），且没有 {@code removeTunnel}，只能用 {@code unbind}。</li>
 * </ul>
 */
public final class SettingsApplier {

    public enum Result {
        /** 至少写入了这一项。 */
        APPLIED,
        /** 目标不支持任何已勾选的项，未做修改。 */
        UNTOUCHED,
        /** 目标支持，但本版无法安全处理（目前只有 P2P 换型失败）。 */
        SKIPPED
    }

    private static final String NBT_DISPLAY = "display";
    private static final String NBT_NAME = "Name";
    private static final String NBT_PRIORITY = "priority";
    private static final String NBT_CONFIG = "config";
    private static final String NBT_PATTERNS = "patterns";
    private static final String NBT_FREQ = "freq";

    /** NBTTagCompound#hasKey 的类型参数，1.7.10 没有 Constants.NBT 的便捷常量导出。 */
    private static final int TAG_COMPOUND = 10;

    private SettingsApplier() {}

    public static Result applyTile(AEBaseTile tile, NBTTagCompound source, CopyOptions opts) {
        if (tile == null || source == null || opts == null) {
            return Result.UNTOUCHED;
        }
        boolean applied = false;

        // 没有 display 段就说明源设备本来没有自定义名（AE2 只在有名字时才写 display），
        // 此时保持目标原名，而不是把名字清空。
        if (opts.has(CopyOption.NAME) && source.hasKey(NBT_DISPLAY)) {
            tile.setCustomName(readName(source));
            applied = true;
        }

        if (opts.has(CopyOption.SETTINGS) && tile instanceof IConfigurableObject) {
            IConfigManager cm = ((IConfigurableObject) tile).getConfigManager();
            if (cm != null) {
                cm.readFromNBT(source);
                applied = true;
            }
        }

        if (opts.has(CopyOption.PRIORITY) && tile instanceof IPriorityHost) {
            ((IPriorityHost) tile).setPriority(source.getInteger(NBT_PRIORITY));
            applied = true;
        }

        if (tile instanceof ISegmentedInventory) {
            ISegmentedInventory segmented = (ISegmentedInventory) tile;
            if (opts.has(CopyOption.CONFIG_INV)
                && applyInventory(segmented.getInventoryByName(NBT_CONFIG), source, NBT_CONFIG)) {
                applied = true;
            }
            if (opts.has(CopyOption.PATTERNS)
                && applyInventory(segmented.getInventoryByName(NBT_PATTERNS), source, NBT_PATTERNS)) {
                applied = true;
            }
        }

        return applied ? Result.APPLIED : Result.UNTOUCHED;
    }

    public static Result applyPart(IPart part, NBTTagCompound source, CopyOptions opts, EntityPlayer player) {
        if (part == null || source == null || opts == null) {
            return Result.UNTOUCHED;
        }
        if (part instanceof PartP2PTunnel) {
            return applyP2P((PartP2PTunnel) part, source, opts, player);
        }
        if (!(part instanceof AEBasePart)) {
            return Result.UNTOUCHED;
        }

        AEBasePart base = (AEBasePart) part;
        boolean applied = false;

        if (opts.has(CopyOption.NAME) && source.hasKey(NBT_DISPLAY)) {
            base.setCustomName(readName(source));
            applied = true;
        }

        if (opts.has(CopyOption.SETTINGS)) {
            IConfigManager cm = base.getConfigManager();
            if (cm != null) {
                cm.readFromNBT(source);
                applied = true;
            }
        }

        if (opts.has(CopyOption.PRIORITY) && base instanceof IPriorityHost) {
            ((IPriorityHost) base).setPriority(source.getInteger(NBT_PRIORITY));
            applied = true;
        }

        if (opts.has(CopyOption.CONFIG_INV)
            && applyInventory(base.getInventoryByName(NBT_CONFIG), source, NBT_CONFIG)) {
            applied = true;
        }
        if (opts.has(CopyOption.PATTERNS)
            && applyInventory(base.getInventoryByName(NBT_PATTERNS), source, NBT_PATTERNS)) {
            applied = true;
        }

        return applied ? Result.APPLIED : Result.UNTOUCHED;
    }

    /**
     * P2P 部件单独处理：频段不在 {@code IConfigManager} 里，换频段必须经过
     * {@code P2PCache#updateFreq} 重新登记；{@code setFrequency} 只改字段，通道会挂在旧频段上。
     *
     * <p>
     * 换型在 rv3 里同样没有「就地改类型」的入口，AE2 自己的内存卡也是拆掉部件再 addPart 重建
     * （{@code PartP2PTunnel#replacePartInWorld}），这里复刻同一流程并保留输入/输出角色。
     */
    private static Result applyP2P(PartP2PTunnel tunnel, NBTTagCompound source, CopyOptions opts, EntityPlayer player) {
        ItemStack stored = readCardStack(source);
        if (stored == null) {
            return Result.UNTOUCHED;
        }

        // 卡上存的类型名来自 getMemoryCardData() -> getItemStack(Wrench)，这里用同一个口径比较，
        // 避免 Network/World 分支返回不同物品时把「同型」误判成「换型」。
        ItemStack current = tunnel.getItemStack(PartItemStack.Wrench);
        boolean sameType = current != null && current.getItem() == stored.getItem();
        if (!sameType) {
            if (!opts.has(CopyOption.P2P_TYPE)) {
                return Result.SKIPPED;
            }
            return replaceTunnel(tunnel, source, opts, player, stored);
        }

        if (!opts.has(CopyOption.P2P_FREQUENCY)) {
            return Result.UNTOUCHED;
        }

        long freq = source.getLong(NBT_FREQ);
        if (freq == 0 || freq == tunnel.getFrequency()) {
            return Result.APPLIED;
        }
        try {
            tunnel.getProxy()
                .getP2P()
                .updateFreq(tunnel, freq);
            tunnel.getHost()
                .markForSave();
            return Result.APPLIED;
        } catch (Exception e) {
            // 网络未就绪（GridAccessException）时没法改频段，宁可不动也不要留下半截状态
            return Result.UNTOUCHED;
        }
    }

    private static Result replaceTunnel(PartP2PTunnel tunnel, NBTTagCompound source, CopyOptions opts,
        EntityPlayer player, ItemStack stored) {
        IPartHost host = tunnel.getHost();
        if (host == null || player == null) {
            return Result.SKIPPED;
        }

        boolean keepOutput = tunnel.isOutput();
        long targetFreq = opts.has(CopyOption.P2P_FREQUENCY) ? source.getLong(NBT_FREQ) : tunnel.getFrequency();
        ForgeDirection side = tunnel.getSide();

        // suppressUpdate = true：rv3 自己的 replacePartInWorld 也这样用，避免客户端先看到空档
        host.removePart(side, true);
        try {
            tunnel.getProxy()
                .getP2P()
                .unbind(tunnel);
        } catch (Exception ignored) {
            // 网络已断开时映射本来就不存在，继续拆部件即可
        }

        // rv3 的 addPart 返回实际放置的朝向，失败时返回 null（不是抛异常）
        ForgeDirection placed = host.addPart(stored, side, player);
        if (placed == null) {
            return Result.SKIPPED;
        }
        IPart newPart = host.getPart(placed == ForgeDirection.UNKNOWN ? side : placed);
        if (!(newPart instanceof PartP2PTunnel)) {
            // addPart 失败（位置已被占/权限不足）时部件已经拆掉了，如实报告，别假装成功
            return Result.SKIPPED;
        }

        PartP2PTunnel fresh = (PartP2PTunnel) newPart;
        P2PManager.setP2POutput(fresh, keepOutput);
        if (targetFreq != 0) {
            try {
                fresh.getProxy()
                    .getP2P()
                    .updateFreq(fresh, targetFreq);
            } catch (Exception ignored) {
                // 同上：网络未就绪时保持新部件的默认频段
            }
        }
        fresh.onTunnelNetworkChange();
        host.markForSave();
        return Result.APPLIED;
    }

    /**
     * 从卡片数据里取出 P2P 部件物品。
     *
     * <p>
     * rv3 的 {@code PartP2PTunnel#getMemoryCardData} 把 {@code getItemStack(Wrench)} 直接写在
     * 根标签上，而 {@code AEItemStack.writeToNBT} 写的是 {@code Count=0} + {@code Cnt=<真实数量>}，
     * 所以 {@code new ItemStack(tag)} 会得到 stackSize 为 0 的非法堆栈，必须走 AE 的反序列化。
     */
    private static ItemStack readCardStack(NBTTagCompound source) {
        if (source == null || !source.hasKey("id")) {
            return null;
        }
        try {
            IAEItemStack stack = AEItemStack.loadItemStackFromNBT(source);
            return stack == null ? null : stack.getItemStack();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 把一个库存整段覆盖成存档里的内容。源里没有这一段就说明源设备本来就没有该库存，
     * 此时不动目标（而不是清空）——「没这个功能」和「有这个功能但是空的」要区分开。
     *
     * <p>
     * rv3 的库存 NBT 是 {@code { "#0": {...}, "#1": {...} }} 形式的 compound，不是列表；
     * 每个槽值是 {@code AEItemStack.writeToNBT} 的产物（{@code Count=0}、数量在 {@code Cnt}）。
     */
    private static boolean applyInventory(IInventory inv, NBTTagCompound source, String key) {
        if (inv == null || source == null || !source.hasKey(key, TAG_COMPOUND)) {
            return false;
        }
        NBTTagCompound segment = source.getCompoundTag(key);
        int slots = inv.getSizeInventory();
        for (int i = 0; i < slots; i++) {
            String slotKey = "#" + i;
            ItemStack stack = null;
            if (segment.hasKey(slotKey, TAG_COMPOUND)) {
                try {
                    IAEItemStack ae = AEItemStack.loadItemStackFromNBT(segment.getCompoundTag(slotKey));
                    stack = ae == null ? null : ae.getItemStack();
                } catch (Exception ignored) {
                    stack = null;
                }
            }
            inv.setInventorySlotContents(i, stack);
        }
        inv.markDirty();
        return true;
    }

    private static String readName(NBTTagCompound source) {
        if (!source.hasKey(NBT_DISPLAY)) {
            return "";
        }
        return source.getCompoundTag(NBT_DISPLAY)
            .getString(NBT_NAME);
    }
}
