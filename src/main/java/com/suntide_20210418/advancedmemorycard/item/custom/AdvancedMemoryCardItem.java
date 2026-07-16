package com.suntide_20210418.advancedmemorycard.item.custom;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.core.localization.Tooltips;
import appeng.items.tools.MemoryCardItem;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.util.InteractionUtil;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

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
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack handStack = player.getItemInHand(hand);
        if (InteractionUtil.isInAlternateUseMode(player)) {
            this.cycleMode(player, handStack);
            return InteractionResultHolder.consume(handStack);
        } else {
            return CardMode.of(handStack).onItemUse(level, player, hand);
        }
    }

    public void clearCard(Player player, Level level) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);

        IMemoryCard mem = (IMemoryCard) stack.getItem();
        mem.notifyUser(player, MemoryCardMessages.SETTINGS_CLEARED);

        // 仅清除 Data 根下的所有数据
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains("Data")) {
            tag.remove("Data");
            // 如果 Data 是唯一的数据，移除整个标签
            if (tag.isEmpty()) {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            }
        }
        // 清除copy模式下的数据
        CardMode cardMode = CopyMode.of(stack);
        if (cardMode instanceof CopyMode copyMode){
            copyMode.clearPos(stack);
        }
    }

    public void cycleMode(Player player, ItemStack cardStack) {
        CardMode nextMode = CardMode.cycleMode(CardMode.of(cardStack), true);
        // 1.21.1：模式数据存于 CUSTOM_DATA 组件，必须通过 saveToStack 写回物品栈才能持久化
        nextMode.saveToStack(cardStack);
        if (player != null) {
            player.displayClientMessage(nextMode.getName(), true);
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag advancedTooltips) {
        lines.add(Tooltips.of(CardMode.of(stack).getDescription()));
        super.appendHoverText(stack, context, lines, advancedTooltips);
    }

    @Override
    public int getColor(ItemStack stack) {
        return ModConfigs.getClientConfig().itemColor.get();
    }

    public static int getTintColor(ItemStack stack, int index) {
        return ModConfigs.getClientConfig().itemTintColor.get();
    }

    @Override
    public @Nullable ItemMenuHost<?> getMenuHost(Player player, ItemMenuHostLocator itemMenuHostLocator, @Nullable BlockHitResult blockHitResult) {
        return null;
    }
}
