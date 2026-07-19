package com.suntide_20210418.advancedmemorycard.item.custom;

import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;
import appeng.parts.p2p.P2PTunnelPart;
import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.menu.ConfigModeMenu;
import com.suntide_20210418.advancedmemorycard.p2p.P2PManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.ConfigMode.show;
import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.Tooltip.configInfo;


public class ConfigMode extends CardMode {
    private P2PManager currentP2PManager;

    @Override
    public ResourceLocation getType() {
        return ResourceLocation.fromNamespaceAndPath(AdvancedMemoryCardMod.MOD_ID, "config");
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        InteractionHand hand = context.getHand();

        if (player != null) {
            BlockEntity blockEntity = level.getBlockEntity(clickedPos);
            if (blockEntity instanceof IPartHost partHost) {
                SelectedPart selectedPart = partHost.selectPartWorld(context.getClickLocation());

                // 检查选中的是否是 P2P 部件
                if (selectedPart.part instanceof P2PTunnelPart<?> hitP2P) {
                    // 先创建 P2PManager 并分配所有 P2P 频段
                    currentP2PManager = new P2PManager(hitP2P, player);
                    player.openMenu(new SimpleMenuProvider((id, inv, p) -> {
                        ConfigModeMenu menu = new ConfigModeMenu(id, inv, hand);
                        menu.setP2PManager(currentP2PManager);
                        return menu;
                    }, getName()));
                    return InteractionResult.CONSUME;
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> onItemUse(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public Component getName() {
        return show();
    }

    @Override
    public Component getDescription() {
        return configInfo();
    }

    public P2PManager getCurrentP2PManager() {
        return currentP2PManager;
    }
}
