package com.suntide_20210418.advancedmemorycard.utils;

import appeng.api.implementations.tiles.ISegmentedInventory;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.PartItemStack;
import appeng.api.util.AEPartLocation;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.helpers.IPriorityHost;
import appeng.parts.AEBasePart;
import appeng.parts.p2p.PartP2PTunnel;
import appeng.tile.AEBaseTile;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyOption;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyOptions;
import com.suntide_20210418.advancedmemorycard.p2p.P2PManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * 按 {@link CopyOptions} 把卡片上的配置写进目标方块/部件。
 *
 * <p>这里刻意不走 {@code AEBaseTile#uploadSettings}：该方法把「缺少键」当成「清空目标」
 * （例如 {@code compound.getInteger("priority")} 缺键即清零、{@code readFromNBT(compound,"config")}
 * 缺键即清空配置槽），而选择性粘贴需要「没勾选的项保持目标原值」。另外
 * {@code TileInterface#uploadSettings} 会把接口里的样板退还成空白样板、并且只在你背包里有
 * 空白样板时才恢复样板 —— 批量粘贴时这种物品交互不可接受。
 *
 * <p>因此这里按项直接写目标对象的字段：配置管理器、优先级、名称、库存。
 * 每一段的读取口径都对照 rv6 的 {@code downloadSettings}/{@code uploadSettings} 写成。
 */
public final class SettingsApplier {

    public enum Result {
        /** 至少写入了这一项。 */
        APPLIED,
        /** 目标不支持任何已勾选的项，未做修改。 */
        UNTOUCHED,
        /** 目标支持，但本版无法安全处理（目前只有 P2P 换型）。 */
        SKIPPED
    }

    private static final String NBT_DISPLAY = "display";
    private static final String NBT_NAME = "Name";
    private static final String NBT_PRIORITY = "priority";
    private static final String NBT_CONFIG = "config";
    private static final String NBT_PATTERNS = "patterns";
    private static final String NBT_FREQ = "freq";

    private SettingsApplier() {
    }

    public static Result applyTile(AEBaseTile tile, NBTTagCompound source, CopyOptions opts) {
        if (source == null) {
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

    public static Result applyPart(IPart part, NBTTagCompound source, CopyOptions opts,
            EntityPlayer player, EnumHand hand) {
        if (source == null) {
            return Result.UNTOUCHED;
        }
        if (part instanceof PartP2PTunnel) {
            return applyP2P((PartP2PTunnel) part, source, opts, player, hand);
        }
        if (!(part instanceof AEBasePart)) {
            return Result.UNTOUCHED;
        }

        AEBasePart base = (AEBasePart) part;
        boolean applied = false;

        // 同上：部件侧 AE2 的 downloadSettings 完全不写 display，所以「名称」一项
        // 对部件实际无效（不会清空，也不会写入），保持目标原值即可。
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
     * <p>换型在 rv6 里没有「就地改类型」的入口，AE2 自己的内存卡也是拆掉部件再
     * {@code addPart} 重建（{@code PartP2PTunnel#onPartActivate}）。这里复刻同一流程，
     * 并保留原来的输入/输出角色；频段按卡片决定（勾了「P2P 频率」才用卡上的值）。
     */
    private static Result applyP2P(PartP2PTunnel tunnel, NBTTagCompound source, CopyOptions opts,
            EntityPlayer player, EnumHand hand) {
        ItemStack stored = readCardStack(source);
        if (stored.isEmpty()) {
            return Result.UNTOUCHED;
        }

        boolean sameType = stored.getItem() == tunnel.getItemStack(PartItemStack.NETWORK).getItem();
        if (!sameType) {
            if (!opts.has(CopyOption.P2P_TYPE)) {
                return Result.SKIPPED;
            }
            return replaceTunnel(tunnel, source, opts, player, hand, stored);
        }

        if (!opts.has(CopyOption.P2P_FREQUENCY)) {
            return Result.UNTOUCHED;
        }

        short freq = source.getShort(NBT_FREQ);
        if (freq == 0 || freq == tunnel.getFrequency()) {
            return Result.APPLIED;
        }
        try {
            tunnel.getProxy().getP2P().updateFreq(tunnel, freq);
            tunnel.getHost().markForSave();
            return Result.APPLIED;
        } catch (Exception e) {
            // 网络未就绪（GridAccessException）时没法改频段，宁可不动也不要留下半截状态
            return Result.UNTOUCHED;
        }
    }

    private static Result replaceTunnel(PartP2PTunnel tunnel, NBTTagCompound source, CopyOptions opts,
            EntityPlayer player, EnumHand hand, ItemStack stored) {
        IPartHost host = tunnel.getHost();
        if (host == null || player == null) {
            return Result.SKIPPED;
        }

        boolean keepOutput = tunnel.isOutput();
        short targetFreq = opts.has(CopyOption.P2P_FREQUENCY)
                ? source.getShort(NBT_FREQ)
                : tunnel.getFrequency();
        AEPartLocation side = tunnel.getSide();

        try {
            tunnel.getProxy().getP2P().removeTunnel(tunnel, tunnel.getFrequency());
        } catch (Exception ignored) {
            // 网络已断开时映射本来就不存在，继续拆部件即可
        }

        // suppressUpdate = false：让宿主立刻把这次改动同步给客户端，
        // 否则要等下一次方块更新，玩家会看到旧部件还挂在墙上。
        host.removePart(side, false);
        AEPartLocation placed = host.addPart(stored.copy(), side, player, hand);
        IPart newPart = host.getPart(placed);
        if (!(newPart instanceof PartP2PTunnel)) {
            // addPart 失败（位置已被占/权限不足）时部件已经拆掉了，如实报告，别假装成功
            return Result.SKIPPED;
        }

        PartP2PTunnel fresh = (PartP2PTunnel) newPart;
        P2PManager.setP2POutput(fresh, keepOutput);
        if (targetFreq != 0) {
            try {
                fresh.getProxy().getP2P().updateFreq(fresh, targetFreq);
            } catch (Exception ignored) {
                // 同上：网络未就绪时保持新部件的默认频段
            }
        }
        fresh.onTunnelNetworkChange();
        host.markForSave();
        return Result.APPLIED;
    }

    private static ItemStack readCardStack(NBTTagCompound source) {
        if (!source.hasKey("id")) {
            return ItemStack.EMPTY;
        }
        try {
            return new ItemStack(source);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    /**
     * 把一个库存整段覆盖成存档里的内容。源里没有这一段就说明源设备本来就没有该库存，
     * 此时不动目标（而不是清空）——「没这个功能」和「有这个功能但是空的」要区分开。
     */
    private static boolean applyInventory(IItemHandler inv, NBTTagCompound source, String key) {
        // 1.12.2 的 IItemHandler 只有读与插入/抽取，可写入口在 IItemHandlerModifiable 上
        if (!(inv instanceof IItemHandlerModifiable) || !source.hasKey(key, 9)) {
            return false;
        }
        IItemHandlerModifiable target = (IItemHandlerModifiable) inv;

        NBTTagList list = source.getTagList(key, 10);
        int slots = target.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = ItemStack.EMPTY;
            if (i < list.tagCount()) {
                NBTTagCompound entry = list.getCompoundTagAt(i);
                if (entry.hasKey("id")) {
                    stack = new ItemStack(entry);
                }
            }
            target.setStackInSlot(i, stack);
        }
        return true;
    }

    private static String readName(NBTTagCompound source) {
        if (!source.hasKey(NBT_DISPLAY)) {
            return "";
        }
        return source.getCompoundTag(NBT_DISPLAY).getString(NBT_NAME);
    }
}
