package com.suntide_20210418.advancedmemorycard.item.custom;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.core.localization.Tooltips;
import appeng.items.tools.MemoryCardItem;
import appeng.util.InteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;


/*
    1.shift+右键复制单个机器配置或p2p配置后右键选择一片长方体区域的两个角（用方框框起待选区域）批量粘贴
    2.shift+滚轮可以调节模式（复制模式、配置模式）
    3.任何模式下对着空气右击可以打开GUI来配置复制模式的复制功能
*/

public class AdvancedMemoryCardItem extends MemoryCardItem implements IMenuItem {

    public AdvancedMemoryCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        // copying/special tool use
        if (context.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        if (!level.isClientSide()) {
            return CardMode.of(stack).onItemUseFirst(stack, context);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack handStack = player.getItemInHand(hand);
        if (InteractionUtil.isInAlternateUseMode(player)) {
            this.cycleMode(player, handStack, true);
            return InteractionResultHolder.consume(handStack);
        } else {
            return CardMode.of(handStack).onItemUse(level, player, hand);
        }
    }

    private void clearCard(Player player, Level level, InteractionHand hand){
        ItemStack stack = player.getItemInHand(hand);
        IMemoryCard mem = (IMemoryCard)stack.getItem();
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
    }

    private void cycleMode(Player player, ItemStack cardStack, boolean cycleForward) {
        CardMode nextMode = CardMode.cycleMode(CardMode.of(cardStack), cycleForward);
        nextMode.save(cardStack.getOrCreateTag());
        if (player != null) {
            player.displayClientMessage(nextMode.getName(), true);
        }
    }

    @Override
    public @Nullable ItemMenuHost getMenuHost(Player player, int i, ItemStack itemStack, @Nullable BlockPos blockPos) {
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag advancedTooltips) {
        lines.add(Tooltips.of(CardMode.of(stack).getDescription()));
        super.appendHoverText(stack, level, lines, advancedTooltips);
    }

    @Override
    public int getColor(ItemStack stack) {
        return 0xFF0000;
    }

    public static int getTintColor(ItemStack stack, int index) {
        return 0xFFFFFF;
    }
}
