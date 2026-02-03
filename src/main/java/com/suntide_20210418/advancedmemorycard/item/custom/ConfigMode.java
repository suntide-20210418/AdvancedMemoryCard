package com.suntide_20210418.advancedmemorycard.item.custom;


import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public class ConfigMode extends CardMode{

    @Override
    public ResourceLocation getType() {
        return ResourceLocation.fromNamespaceAndPath(AdvancedMemoryCardMod.MOD_ID, "config");
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    protected Component getName() {
        return Component.translatable(
                "gui.advanced_memory_card.advanced_memory_card.player.config.show"
        );
    }

    @Override
    protected Component getDescription() {
        return Component.translatable(
                "gui.advanced_memory_card.advanced_memory_card.tooltip.config.info"
        );
    }
}
