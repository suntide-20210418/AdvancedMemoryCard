package com.suntide_20210418.advancedmemorycard.item.custom;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.items.tools.ToolMemoryCard;
import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.item.ModCreativeModeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraft.client.renderer.texture.IIconRegister;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.List;

public class AdvancedMemoryCardItem extends ToolMemoryCard {

    public AdvancedMemoryCardItem() {
        super();
        setUnlocalizedName(AdvancedMemoryCardMod.MOD_ID + ".advanced_memory_card");
        setCreativeTab(ModCreativeModeTabs.ADVANCED_MEMORY_CARD_TAB);
        setMaxStackSize(1);
    }

    @Override
    public void registerIcons(IIconRegister iconRegister) {
        this.itemIcon = iconRegister.registerIcon(AdvancedMemoryCardMod.MOD_ID + ":advanced_memory_card");
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
        CardMode cardMode = CardMode.of(stack);
        if (!world.isRemote) {
            if (cardMode instanceof CopyMode && player.isSneaking()) {
                return false;
            }
            return cardMode.onItemUseFirst(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
        }
        // 客户端必须返回 false：在 1.7.10 中，若客户端 onItemUseFirst 返回 true，
        // PlayerControllerMP.onPlayerRightClick 会在发送方块交互包(C08)之前直接 return，
        // 导致服务端根本收不到点击，服务端逻辑(上面的分支)永远不会执行 -> 不按 shift 右键无反应。
        // 返回 false 让客户端继续把交互包发往服务端，由服务端真正处理各模式逻辑。
        return false;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world,
            int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            // 在客户端消费这次点击：让 PlayerControllerMP.onPlayerRightClick 返回 true，
            // 从而 rightClickMouse 不再调用 sendUseItem 发送 (-1,-1,-1) 空气包，
            // 避免服务端重复触发 onItemRightClick -> onItemUse（点击空气逻辑）。
            return true;
        }
        return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            if (player.isSneaking() && CardMode.of(stack) instanceof CopyMode) {
                return stack;
            }
            return CardMode.of(stack).onItemUse(world, player);
        }
        return stack;
    }

    public void clearCard(EntityPlayer player, World world) {
        ItemStack stack = player.getHeldItem();
        IMemoryCard mem = (IMemoryCard) stack.getItem();
        mem.notifyUser(player, MemoryCardMessages.SETTINGS_CLEARED);
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("Data")) {
            tag.removeTag("Data");
            if (tag.hasNoTags()) {
                stack.setTagCompound(null);
            }
        }
        CardMode cardMode = CardMode.of(stack);
        if (cardMode instanceof CopyMode) {
            ((CopyMode) cardMode).clearPos(stack);
        }
    }

    public void cycleMode(EntityPlayer player, ItemStack cardStack) {
        CardMode nextMode = CardMode.cycleMode(CardMode.of(cardStack));
        nextMode.save(CardMode.getOrCreateRoot(cardStack));
        if (player != null) {
            player.addChatMessage(new ChatComponentText(nextMode.getName()));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addCheckedInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean advancedTooltips) {
        String desc = CardMode.of(stack).getDescription();
        if (desc != null) {
            for (String line : desc.split("\n")) {
                lines.add(line);
            }
        }
        super.addCheckedInformation(stack, player, lines, advancedTooltips);
    }
}
