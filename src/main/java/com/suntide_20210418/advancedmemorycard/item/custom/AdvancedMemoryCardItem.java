package com.suntide_20210418.advancedmemorycard.item.custom;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.items.tools.ToolMemoryCard;
import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.client.gui.ModGuiHandler;
import com.suntide_20210418.advancedmemorycard.item.ModCreativeModeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class AdvancedMemoryCardItem extends ToolMemoryCard {

    public AdvancedMemoryCardItem() {
        super();
        setRegistryName(new ResourceLocation(AdvancedMemoryCardMod.MOD_ID, "advanced_memory_card"));
        setUnlocalizedName(AdvancedMemoryCardMod.MOD_ID + ".advanced_memory_card");
        setCreativeTab(ModCreativeModeTabs.ADVANCED_MEMORY_CARD_TAB);
        setMaxStackSize(1);
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
            float hitX, float hitY, float hitZ, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        CardMode cardMode = CardMode.of(stack);
        if (!world.isRemote) {
            if (cardMode instanceof CopyMode && player.isSneaking()) {
                return EnumActionResult.PASS;
            }
            return cardMode.onItemUseFirst(stack, player, world, pos, side, hitX, hitY, hitZ, hand);
        }
        return EnumActionResult.SUCCESS;
    }

    /**
     * 复制走 AE2 自己的 {@code ToolMemoryCard#onItemUse}（我们只在潜行时放行），
     * 因此要判断"这次是否真的复制成功"只能比对卡片 Data 标签的前后差异。
     * 复制成功后按卡片上的开关决定是否弹出粘贴选项面板。
     */
    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
            EnumFacing side, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        NBTTagCompound before = copyOfData(stack);

        EnumActionResult result = super.onItemUse(player, world, pos, hand, side, hitX, hitY, hitZ);

        if (!world.isRemote
                && result == EnumActionResult.SUCCESS
                && CardMode.of(stack) instanceof CopyMode
                && CopyOptions.of(stack).isAutoOpen()
                && !sameData(before, copyOfData(stack))) {
            player.openGui(AdvancedMemoryCardMod.INSTANCE, ModGuiHandler.COPY_GUI_ID,
                    world, hand.ordinal(), 0, 0);
        }
        return result;
    }

    private static NBTTagCompound copyOfData(ItemStack stack) {
        NBTTagCompound data = ((ToolMemoryCard) stack.getItem()).getData(stack);
        return data == null ? new NBTTagCompound() : data.copy();
    }

    private static boolean sameData(NBTTagCompound a, NBTTagCompound b) {
        return a.equals(b);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack handStack = player.getHeldItem(hand);
        if (!world.isRemote){
            if (player.isSneaking() && CardMode.of(handStack) instanceof CopyMode) {
                return new ActionResult<>(EnumActionResult.PASS, handStack);
            }
            return CardMode.of(handStack).onItemUse(world, player, hand);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, handStack);
    }

    public void clearCard(EntityPlayer player, World world) {
        ItemStack stack = player.getHeldItem(EnumHand.MAIN_HAND);
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
            player.sendStatusMessage(new TextComponentString(nextMode.getName()), true);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addCheckedInformation(ItemStack stack, World world, List<String> lines, net.minecraft.client.util.ITooltipFlag advancedTooltips) {
        String desc = CardMode.of(stack).getDescription();
        if (desc != null) {
            for (String line : desc.split("\n")) {
                lines.add(line);
            }
        }
        super.addCheckedInformation(stack, world, lines, advancedTooltips);
    }
}
