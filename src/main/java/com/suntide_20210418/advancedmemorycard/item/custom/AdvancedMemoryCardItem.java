package com.suntide_20210418.advancedmemorycard.item.custom;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.item.ModCreativeModeTabs;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.PartItemStack;
import appeng.block.networking.BlockCableBus;
import appeng.items.tools.ToolMemoryCard;
import appeng.parts.AEBasePart;
import appeng.parts.p2p.PartP2PTunnel;
import appeng.tile.AEBaseTile;
import appeng.util.SettingsFrom;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

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
                if (copyFromTarget(player, world, x, y, z, ForgeDirection.getOrientation(side), stack)) {
                    if (CopyOptions.of(stack)
                        .isAutoOpen()) {
                        player.openGui(
                            AdvancedMemoryCardMod.INSTANCE,
                            AdvancedMemoryCardMod.COPY_GUI_ID,
                            world,
                            0,
                            0,
                            0);
                    }
                    // 已经复制过，阻止方块交互再复制一次
                    return true;
                }
                // 目标不认识就交回 1.7.10 原有的处理（AE2 方块自己也会做潜行复制）
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

    /**
     * 复刻 AE2 的复制分支：方块走 {@code downloadSettings}，P2P 部件走
     * {@code getMemoryCardData}，其余部件走 {@code onActivate}（它对内存卡的潜行分支就是复制）。
     *
     * <p>为什么 P2P 不能像 1.12.2 那样走 {@code onActivate}：rv3 的
     * {@code PartP2PTunnel#useStandardMemoryCard()} 返回 false，而
     * {@code CableBusContainer#activate} 只调 {@code onActivate}，真正的
     * {@code onPartShiftActivate} 只有「手持 AE 部件物品」时才会被分派。走 onActivate
     * 只会打印连接信息，卡片数据一点不变。
     *
     * @return 是否真的复制到了东西；false 表示这不是我们能处理的目标
     */
    private boolean copyFromTarget(EntityPlayer player, World world, int x, int y, int z, ForgeDirection side,
        ItemStack stack) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity == null) {
            return false;
        }
        IMemoryCard card = (IMemoryCard) stack.getItem();

        if (tileEntity instanceof AEBaseTile && !(world.getBlock(x, y, z) instanceof BlockCableBus)) {
            NBTTagCompound data = ((AEBaseTile) tileEntity).downloadSettings(SettingsFrom.MEMORY_CARD);
            if (data == null) {
                // downloadSettings 返回 null = 没有可复制的内容，交回原版/AE2 的处理
                return false;
            }
            card.setMemoryCardContents(stack, world.getBlock(x, y, z)
                .getUnlocalizedName(), data);
            card.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED);
            return true;
        }

        if (tileEntity instanceof IPartHost) {
            IPart part = findClickedPart((IPartHost) tileEntity, side);
            if (part instanceof PartP2PTunnel) {
                PartP2PTunnel tunnel = (PartP2PTunnel) part;
                NBTTagCompound data = tunnel.getMemoryCardData();
                ItemStack p2pItem = tunnel.getItemStack(PartItemStack.Wrench);
                String type = p2pItem == null ? "P2P" : p2pItem.getUnlocalizedName();
                card.setMemoryCardContents(stack, type + ".name", data);
                card.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED);
                return true;
            }
            if (part instanceof AEBasePart) {
                NBTTagCompound before = copyOfData(stack);
                part.onActivate(player, Vec3.createVectorHelper(x + 0.5, y + 0.5, z + 0.5));
                return !sameData(before, copyOfData(stack));
            }
        }

        return false;
    }

    /**
     * 从方块坐标 + 点击面反推玩家点到的部件。线缆总线上的部件在面上，覆盖板/中心部件在
     * {@code ForgeDirection.UNKNOWN} 槽位，所以先试点击面，再试对面，最后退回中心。
     */
    private IPart findClickedPart(IPartHost host, ForgeDirection side) {
        IPart part = host.getPart(side);
        if (part == null) {
            part = host.getPart(side.getOpposite());
        }
        if (part == null) {
            part = host.getPart(ForgeDirection.UNKNOWN);
        }
        return part;
    }

    private static NBTTagCompound copyOfData(ItemStack stack) {
        NBTTagCompound data = ((ToolMemoryCard) stack.getItem()).getData(stack);
        return data == null ? new NBTTagCompound() : (NBTTagCompound) data.copy();
    }

    private static boolean sameData(NBTTagCompound a, NBTTagCompound b) {
        return a.equals(b);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
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
            return CardMode.of(stack)
                .onItemUse(world, player);
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
    public void addCheckedInformation(ItemStack stack, EntityPlayer player, List<String> lines,
        boolean advancedTooltips) {
        String desc = CardMode.of(stack)
            .getDescription();
        if (desc != null) {
            for (String line : desc.split("\n")) {
                lines.add(line);
            }
        }
        super.addCheckedInformation(stack, player, lines, advancedTooltips);
    }
}
