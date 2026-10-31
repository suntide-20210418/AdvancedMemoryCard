package com.suntide_20210418.advancedmemorycard.menu;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyOption;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyOptions;
import com.suntide_20210418.advancedmemorycard.network.CopyModePacket;
import com.suntide_20210418.advancedmemorycard.network.CopyOptionsSyncPacket;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;
import com.suntide_20210418.advancedmemorycard.utils.BlockPos;

/**
 * 复制模式菜单（服务端容器）。
 *
 * <p>
 * 零槽位纯逻辑容器：客户端面板只做「勾选 → 提交整张掩码」，真正的落盘在这里。
 * 1.7.10 适配：移除 EnumHand，统一使用 player.getHeldItem() 获取主手物品。
 */
public class CopyModeMenu extends Container implements CopyModePacket.ICopyMenu {

    private static final String SET_OPTIONS = "set_options:";

    /** 背包槽位的屏幕外坐标：既让 getSlotFromInventory 能命中，又不会被 GuiContainer 绘制。 */
    private static final int OFFSCREEN_SLOT = -10000;

    private final InventoryPlayer playerInventory;

    /** 服务端持有的权威选项，客户端显示以同步包为准（物品 NBT 不会随空容器自动同步）。 */
    private int applyMask;
    private boolean autoOpen;

    public CopyModeMenu(InventoryPlayer playerInventory) {
        this.playerInventory = playerInventory;
        bindOffscreenPlayerSlots(playerInventory);
        readOptionsFromCard();
        syncOptionsToClient();
    }

    /**
     * 把玩家背包槽位挂到容器上，但坐标放在屏幕外（不会被绘制）。
     *
     * <p>
     * 1.7.10 的 {@code NetHandlerPlayServer#processPlayerBlockPlacement} 末尾会执行
     * {@code openContainer.getSlotFromInventory(player.inventory, currentItem).slotNumber}；
     * 零槽位容器在那里返回 null，随后直接 NPE 崩服——GUI 打开期间或容器未正确关闭时的
     * 任何方块右键都会踩到。
     */
    private void bindOffscreenPlayerSlots(InventoryPlayer inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(inventory, col + row * 9 + 9, OFFSCREEN_SLOT, OFFSCREEN_SLOT));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(inventory, col, OFFSCREEN_SLOT, OFFSCREEN_SLOT));
        }
    }

    private void readOptionsFromCard() {
        CopyOptions opts = CopyOptions.of(playerInventory.player.getHeldItem());
        this.applyMask = opts.mask();
        this.autoOpen = opts.isAutoOpen();
    }

    /**
     * 只由服务端的菜单实例发包：客户端的同名对象只是容器镜像，它没有权威状态
     * （{@code getClientGuiElement} 会 new 一个不带上下文的新实例）。
     */
    private void syncOptionsToClient() {
        if (playerInventory.player instanceof EntityPlayerMP) {
            NetworkHandler
                .sendToPlayer(new CopyOptionsSyncPacket(applyMask, autoOpen), (EntityPlayerMP) playerInventory.player);
        }
    }

    public int getApplyMask() {
        return applyMask;
    }

    public boolean isAutoOpen() {
        return autoOpen;
    }

    @Override
    public void handleCopyAction(String action, BlockPos pos) {
        if (action != null && action.startsWith(SET_OPTIONS)) {
            applyOptions(action.substring(SET_OPTIONS.length()));
        }
    }

    /**
     * 载荷形如 {@code <mask>:<autoOpen>}。
     * 选项写在卡片根标签上，所以这里直接改手持的物品，不经过菜单字段。
     */
    private void applyOptions(String payload) {
        String[] parts = payload.split(":");
        if (parts.length != 2) {
            return;
        }
        int mask;
        try {
            mask = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return;
        }
        boolean nextAutoOpen = "1".equals(parts[1]);

        ItemStack stack = playerInventory.player.getHeldItem();
        if (stack != null && stack.getItem() instanceof AdvancedMemoryCardItem) {
            this.applyMask = mask & CopyOption.allBits();
            this.autoOpen = nextAutoOpen;
            CopyOptions.of(stack)
                .withMask(this.applyMask)
                .withAutoOpen(this.autoOpen)
                .save(stack);
            // 空容器不会替我们把 NBT 变化推给客户端，必须显式同步，否则界面要等重开才刷新
            syncOptionsToClient();
        }
    }

    // 客户端 action 分发
    public void dispatchClientAction(String action) {
        NetworkHandler.sendToServer(new CopyModePacket(action));
    }

    public void dispatchClientAction(String action, BlockPos pos) {
        NetworkHandler.sendToServer(new CopyModePacket(action, pos));
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }
}
