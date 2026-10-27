package com.suntide_20210418.advancedmemorycard.utils;

import appeng.api.implementations.tiles.ISegmentedInventory;
import appeng.api.parts.IPart;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.helpers.IPriorityHost;
import appeng.parts.AEBasePart;
import appeng.parts.p2p.PartP2PTunnel;
import appeng.tile.AEBaseTile;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyOption;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyOptions;
import net.minecraft.item.ItemStack;
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
    private static final String NBT_UPGRADES = "upgrades";
    private static final String NBT_PATTERNS = "patterns";
    private static final String NBT_FREQ = "freq";

    private SettingsApplier() {
    }

    public static Result applyTile(AEBaseTile tile, NBTTagCompound source, CopyOptions opts) {
        if (source == null) {
            return Result.UNTOUCHED;
        }
        boolean applied = false;

        if (opts.has(CopyOption.NAME)) {
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
            if (opts.has(CopyOption.UPGRADES)
                    && applyInventory(segmented.getInventoryByName(NBT_UPGRADES), source, NBT_UPGRADES)) {
                applied = true;
            }
            if (opts.has(CopyOption.PATTERNS)
                    && applyInventory(segmented.getInventoryByName(NBT_PATTERNS), source, NBT_PATTERNS)) {
                applied = true;
            }
        }

        return applied ? Result.APPLIED : Result.UNTOUCHED;
    }

    public static Result applyPart(IPart part, NBTTagCompound source, CopyOptions opts) {
        if (source == null) {
            return Result.UNTOUCHED;
        }
        if (part instanceof PartP2PTunnel) {
            return applyP2P((PartP2PTunnel) part, source, opts);
        }
        if (!(part instanceof AEBasePart)) {
            return Result.UNTOUCHED;
        }

        AEBasePart base = (AEBasePart) part;
        boolean applied = false;

        if (opts.has(CopyOption.NAME)) {
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
        if (opts.has(CopyOption.UPGRADES)
                && applyInventory(base.getInventoryByName(NBT_UPGRADES), source, NBT_UPGRADES)) {
            applied = true;
        }
        if (opts.has(CopyOption.PATTERNS)
                && applyInventory(base.getInventoryByName(NBT_PATTERNS), source, NBT_PATTERNS)) {
            applied = true;
        }

        return applied ? Result.APPLIED : Result.UNTOUCHED;
    }

    /**
     * P2P 部件单独处理：它的频段不在 {@code IConfigManager} 里，改频段必须经过
     * {@code P2PCache#updateFreq} 重新登记，否则整条通道会一直挂在旧频段上。
     *
     * <p>换型（卡上是另一种 P2P）在 rv6 里要拆掉部件再 {@code addPart} 重建，
     * 属于会丢升级卡/掉物品的破坏性操作，本版不做，直接标记 SKIPPED 让调用方提示玩家。
     */
    private static Result applyP2P(PartP2PTunnel tunnel, NBTTagCompound source, CopyOptions opts) {
        if (opts.has(CopyOption.P2P_TYPE)) {
            return Result.SKIPPED;
        }
        if (!opts.has(CopyOption.P2P_FREQUENCY)) {
            return Result.UNTOUCHED;
        }

        short freq = source.getShort(NBT_FREQ);
        if (freq == tunnel.getFrequency()) {
            return Result.APPLIED;
        }
        try {
            tunnel.getProxy().getP2P().updateFreq(tunnel, freq);
            return Result.APPLIED;
        } catch (Exception e) {
            // 网络未就绪（GridAccessException）时没法改频段，宁可不动也不要留下半截状态
            return Result.UNTOUCHED;
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
