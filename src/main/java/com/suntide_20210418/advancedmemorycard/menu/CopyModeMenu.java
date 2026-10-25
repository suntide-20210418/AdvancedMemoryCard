package com.suntide_20210418.advancedmemorycard.menu;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyMode;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyOption;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 复制模式选项界面的服务端 menu。
 *
 * <p>菜单只做一件事：把「粘贴时要应用哪些内容」的位掩码写进卡片根标签，并把它同步回客户端。
 * 掩码是卡片级偏好而不是当前模式的状态，所以切换模式不会把它覆盖成默认值。
 * 卡片持有 {@code InteractionHand}，所有动作都重新从手上取栈 —— 不能缓存构造时的
 * {@link ItemStack}，因为玩家可能在界面打开期间切换手持、切模式或丢掉卡片。
 *
 * <p>{@code AEBaseMenu} 只接受 {@code BlockEntity / IPart / ItemMenuHost} 作为 host，
 * 这里没有实际宿主，因此显式传 {@code null}（与 {@code ConfigModeMenu} 一致）。
 */
public class CopyModeMenu extends AEBaseMenu {

    private static final String SET_APPLY_MASK = "set_apply_mask";
    private static final String SET_AUTO_OPEN = "set_auto_open";

    private InteractionHand hand;

    /**
     * 当前的粘贴选项掩码（见 {@link CopyOption}）。
     * 服务端写入、客户端只读；菜单打开时 AE2 会全量下发一次，之后靠 tick 增量同步。
     */
    @GuiSync(0)
    private int applyMask;

    /**
     * 手持序数，同步给客户端用于「卡被换走后自动关界面」的判断。
     * 客户端构造菜单时还拿不到手，默认 -1 表示未知。
     */
    @GuiSync(1)
    private int handOrdinal = -1;

    /** 复制完成后是否自动弹出本界面（左侧 COG 按钮切换，存在卡片根标签）。 */
    @GuiSync(2)
    private boolean autoOpen = true;

    /** 供 {@code IMenuTypeExtension} 使用的工厂构造器（客户端建菜单时走这里，data 可能为 null）。 */
    public CopyModeMenu(int id, Inventory playerInventory, FriendlyByteBuf data) {
        super(ModMenu.COPY_MODE_MENU.get(), id, playerInventory, (Object) null);

        // action 注册必须在这个构造器里完成，子构造器会先调到这里
        registerClientAction(SET_APPLY_MASK, Integer.class, this::handleSetApplyMask);
        registerClientAction(SET_AUTO_OPEN, Boolean.class, this::handleSetAutoOpen);
    }

    /** 服务端由 {@code OpenGuiPacket} / 复制完成自动弹出时使用。 */
    public CopyModeMenu(int id, Inventory playerInventory, InteractionHand hand) {
        this(id, playerInventory, (FriendlyByteBuf) null);

        this.hand = hand;
        this.handOrdinal = hand.ordinal();
        this.applyMask = readMaskFromHand();
        this.autoOpen = CopyMode.isAutoOpenOptions(getPlayer().getItemInHand(hand));
    }

    private int readMaskFromHand() {
        if (hand == null) {
            return 0;
        }
        return CopyMode.getApplyMask(getPlayer().getItemInHand(hand));
    }

    /**
     * 客户端点击复选框后提交新的完整掩码。
     *
     * <p>用整张掩码而不是 delta：天然幂等，重复包/丢包都不会把状态写歪。
     */
    public void dispatchSetApplyMask(int mask) {
        sendClientAction(SET_APPLY_MASK, mask);
    }

    private void handleSetApplyMask(Integer mask) {
        if (mask == null || hand == null) {
            return;
        }

        // 界面打开期间玩家可能换手/换卡，这里必须重新取栈
        ItemStack stack = getPlayer().getItemInHand(hand);
        if (!(stack.getItem() instanceof AdvancedMemoryCardItem)) {
            return;
        }

        // 掩码是卡级偏好（存在根标签），不依赖当前模式，因此这里只校验物品
        CopyMode.setApplyMask(stack, mask);
        // 回读真实值：掩码被 clamp 时客户端能靠同步自愈
        this.applyMask = CopyMode.getApplyMask(stack);
    }

    public int getApplyMask() {
        return applyMask;
    }

    /** 左侧 COG 按钮切换「复制完成后是否自动弹本界面」。 */
    public void dispatchSetAutoOpen(boolean enabled) {
        sendClientAction(SET_AUTO_OPEN, enabled);
    }

    public boolean isAutoOpen() {
        return autoOpen;
    }

    private void handleSetAutoOpen(Boolean enabled) {
        if (enabled == null || hand == null) {
            return;
        }

        ItemStack stack = getPlayer().getItemInHand(hand);
        if (!(stack.getItem() instanceof AdvancedMemoryCardItem)) {
            return;
        }

        CopyMode.setAutoOpenOptions(stack, enabled);
        // 回读真实值，保证图标与服务端一致
        this.autoOpen = CopyMode.isAutoOpenOptions(stack);
    }

    /**
     * 当前菜单绑定的手。服务端直接用构造时传入的手；客户端靠 {@link #handOrdinal} 同步得到，
     * 尚未同步时返回 {@code null}。
     */
    public InteractionHand getHand() {
        if (hand != null) {
            return hand;
        }
        InteractionHand[] hands = InteractionHand.values();
        return handOrdinal >= 0 && handOrdinal < hands.length ? hands[handOrdinal] : null;
    }
}
