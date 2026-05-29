package com.suntide_20210418.advancedmemorycard.item.custom;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.core.localization.Tooltips;
import appeng.items.tools.MemoryCardItem;
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
                    return InteractionResult.PASS;
                } else {
                    return InteractionResult.CONSUME;
                }
            }
            return CardMode.of(stack).onItemUseFirst(stack, context);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }



    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack handStack = player.getItemInHand(hand);
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

    @Override
    public int getColor(ItemStack stack) {
        return 0xFF0000;
    }

    public static int getTintColor(ItemStack stack, int index) {
        return 0xFFFFFF;
    }
}
