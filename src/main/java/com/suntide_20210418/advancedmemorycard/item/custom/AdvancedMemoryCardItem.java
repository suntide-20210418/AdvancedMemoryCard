package com.suntide_20210418.advancedmemorycard.item.custom;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.parts.IPartHost;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.core.localization.Tooltips;
import appeng.items.tools.MemoryCardItem;
import appeng.util.InteractionUtil;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.menu.CopyModeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AdvancedMemoryCardItem extends MemoryCardItem implements IMenuItem {

    public AdvancedMemoryCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        CardMode cardMode = CardMode.of(stack);
        Level level = context.getLevel();

        if (!level.isClientSide()) {
            if (context.isSecondaryUseActive()) {
                if (cardMode instanceof CopyMode) {
                    // shift+右键 = 把目标设置「复制」进卡片。真正的导出在 AE2 那边
                    // （AEBaseBlockEntity#useItemOn / AEBasePart#useMemoryCard 里 exportSettings），
                    // 所以这里先按卡片设置弹出选项界面，再返回 PASS 让原版继续走方块交互，
                    // 导出照常发生；界面只展示粘贴选项，不依赖刚导出的内容，先后顺序无影响。
                    tryOpenOptionsAfterCopy(stack, context);
                    return InteractionResult.PASS;
                } else {
                    return InteractionResult.CONSUME;
                }
            }
            return cardMode.onItemUseFirst(stack, context);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * 复制（导出到卡片）后自动弹出选项界面。
     *
     * <p>只有目标确实是 AE 的设置源（AE 方块实体或线缆部件宿主）时才弹，
     * 否则对着普通方块 shift+右键也会弹出一个空界面 —— 那种情况下根本没有复制发生。
     */
    private static void tryOpenOptionsAfterCopy(ItemStack stack, UseOnContext context) {
        if (!CopyMode.isAutoOpenOptions(stack)) {
            return;
        }

        BlockEntity target = context.getLevel().getBlockEntity(context.getClickedPos());
        if (!(target instanceof AEBaseBlockEntity) && !(target instanceof IPartHost)) {
            return;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return;
        }

        player.openMenu(new SimpleMenuProvider(
                (wnd, inv, p) -> new CopyModeMenu(wnd, inv, context.getHand()),
                stack.getHoverName()));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack handStack = player.getItemInHand(hand);
        if (InteractionUtil.isInAlternateUseMode(player)) {
            // shift+右键空气：切换模式（与 1.21.1 一致，粘贴只走非潜行右键）
            this.cycleMode(player, handStack);
            return InteractionResultHolder.consume(handStack);
        }
        return CardMode.of(handStack).onItemUse(level, player, hand);
    }

    public void clearCard(Player player, Level level) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);

        IMemoryCard mem = (IMemoryCard) stack.getItem();
        mem.notifyUser(player, MemoryCardMessages.SETTINGS_CLEARED);
        // 仅清除 Data 根下的所有数据
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Data")) {
            tag.remove("Data");
            // 如果 Data 是唯一的数据，移除整个标签
            if (tag.isEmpty()) {
                stack.setTag(null);
            }
        }
        // 清除copy模式下的数据
        CardMode cardMode = CopyMode.of(stack);
        if (cardMode instanceof CopyMode copyMode){
            copyMode.clearPos(stack);
        }
    }

    public void cycleMode(Player player, ItemStack cardStack) {
        CardMode nextMode = CardMode.cycleMode(CardMode.of(cardStack));
        nextMode.save(cardStack.getOrCreateTag());
        if (player != null) {
            player.displayClientMessage(nextMode.getName(), true);
        }
    }

    @Override
    public @Nullable ItemMenuHost getMenuHost(
            Player player, int i, ItemStack stack, @Nullable BlockPos blockPos) {
        return null;
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> lines, TooltipFlag advancedTooltips) {
        lines.add(Tooltips.of(CardMode.of(stack).getDescription()));
        super.appendHoverText(stack, level, lines, advancedTooltips);
    }

    public static int getTintColor(ItemStack stack, int index) {
        return ModConfigs.getClientConfig().itemTintColor.get();
    }

    @Override
    public int getColor(ItemStack stack) {
        return ModConfigs.getClientConfig().itemColor.get();
    }
}
