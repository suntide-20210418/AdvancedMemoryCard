package com.suntide_20210418.advancedmemorycard.item.custom;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.items.tools.ToolMemoryCard;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import appeng.block.networking.BlockCableBus;
import appeng.parts.AEBasePart;
import appeng.tile.AEBaseTile;
import appeng.util.SettingsFrom;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.Vec3d;
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

    /**
     * 潜行 + 右键 = 复制。1.12.2 里这条路径**不在** {@code Item#onItemUse} 上：
     * 方块交互（{@code AEBaseTileBlock#onBlockActivated}）看到手里的内存卡就直接
     * {@code downloadSettings} 并 return true，根本没有 {@code onItemUse} 的机会。
     * 之前把「复制后自动弹出」挂在 {@code onItemUse} 里，所以永远不触发。
     *
     * <p>现在在这里自己做复制：成功则按卡片开关打开面板并 return SUCCESS（阻止方块交互
     * 再复制一次）；目标不认识就 return PASS，交回原版/AE2 的原有行为。
     */
    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
            float hitX, float hitY, float hitZ, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        CardMode cardMode = CardMode.of(stack);

        if (cardMode instanceof CopyMode && player.isSneaking()) {
            if (world.isRemote) {
                // 复制是服务端行为，客户端不预测方块交互
                return EnumActionResult.SUCCESS;
            }
            if (copyFromTarget(player, world, pos, side, hand, stack)) {
                if (CopyOptions.of(stack).isAutoOpen()) {
                    player.openGui(AdvancedMemoryCardMod.INSTANCE, ModGuiHandler.COPY_GUI_ID,
                            world, hand.ordinal(), 0, 0);
                }
                return EnumActionResult.SUCCESS;
            }
            return EnumActionResult.PASS;
        }

        if (!world.isRemote) {
            return cardMode.onItemUseFirst(stack, player, world, pos, side, hitX, hitY, hitZ, hand);
        }
        return EnumActionResult.SUCCESS;
    }

    /**
     * 复刻 AE2 1.12.2 的复制分支：方块走 {@code downloadSettings}，部件走
     * {@code AEBasePart#onActivate}（它对内存卡的潜行分支就是复制）。
     *
     * @return 是否真的复制到了东西；false 表示这不是我们能处理的目标
     */
    private boolean copyFromTarget(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
            EnumHand hand, ItemStack stack) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity == null) {
            return false;
        }
        IMemoryCard card = (IMemoryCard) stack.getItem();

        if (tileEntity instanceof AEBaseTile
                && !(world.getBlockState(pos).getBlock() instanceof BlockCableBus)) {
            NBTTagCompound data = ((AEBaseTile) tileEntity).downloadSettings(SettingsFrom.MEMORY_CARD);
            if (data == null) {
                // downloadSettings 返回 null = 没有可复制的内容，交回原版给玩家"无设置"的提示
                return false;
            }
            card.setMemoryCardContents(stack,
                    world.getBlockState(pos).getBlock().getUnlocalizedName(), data);
            card.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED);
            return true;
        }

        if (tileEntity instanceof IPartHost) {
            IPart part = findClickedPart((IPartHost) tileEntity, side);
            if (!(part instanceof AEBasePart)) {
                return false;
            }
            NBTTagCompound before = copyOfData(stack);
            ((AEBasePart) part).onActivate(player, hand,
                    new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            return !sameData(before, copyOfData(stack));
        }

        return false;
    }

    /**
     * 从方块坐标 + 点击面反推玩家点到的部件。线缆总线上的部件在面上，覆盖板/中心部件在
     * {@code null} 槽位，所以先试点击面，再试对面，最后退回中心。
     */
    private IPart findClickedPart(IPartHost host, EnumFacing side) {
        AEPartLocation clicked = AEPartLocation.fromFacing(side);
        IPart part = host.getPart(clicked);
        if (part == null) {
            part = host.getPart(clicked.getOpposite());
        }
        if (part == null) {
            part = host.getPart(AEPartLocation.INTERNAL);
        }
        return part;
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
