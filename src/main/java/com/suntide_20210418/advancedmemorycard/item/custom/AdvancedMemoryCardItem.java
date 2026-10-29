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

    /**
     * 关掉 AE2 内存卡的「潜行绕过方块交互」。
     *
     * <p>父类返回 true，含义是「玩家潜行时仍然把点击交给方块」。AE2 方块看到手里的 IMemoryCard
     * 就会自己执行原生内存卡的复制/粘贴，结果就是我们的卡片表现成普通 AE 内存卡。
     */
    @Override
    public boolean doesSneakBypassUse(World world, int x, int y, int z, EntityPlayer player) {
        return false;
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            // 客户端必须返回 false：若在这里返回 true，PlayerControllerMP.onPlayerRightClick 会在
            // 发送方块交互包(C08) 之前直接 return，服务端根本收不到点击。
            return false;
        }

        CardMode cardMode = CardMode.of(stack);
        logClick(player, world, x, y, z, cardMode);

        if (cardMode instanceof CopyMode && player.isSneaking()) {
            boolean copied = copyFromTarget(player, world, x, y, z, ForgeDirection.getOrientation(side), stack);
            if (copied && CopyOptions.of(stack)
                .isAutoOpen()) {
                openCopyGui(player, world);
            }
            // 无论复制成功与否都吃掉这次点击。
            // 交回 AE2 会发生两件事：AEBaseTileBlock 的潜行分支把卡当普通内存卡再复制一次；
            // 以及方块交互失败后落到 ToolMemoryCard.onItemUse，那里在潜行时会
            // setTagCompound(null) 把 mode_data / copy_options 一起清掉。
            return true;
        }

        if (cardMode.onItemUseFirst(stack, player, world, x, y, z, side, hitX, hitY, hitZ)) {
            return true;
        }

        // 我们的卡不兼职 AE2 原生内存卡：点到 AE 方块/部件宿主时直接吃掉点击，
        // 否则 AE2 会拿它当普通内存卡去粘贴设置。
        return isAeTarget(world, x, y, z);
    }

    /**
     * 服务端方块右键的兜底分支。
     *
     * <p>这里刻意<b>不</b>调用 {@code super.onItemUse}：AE2 的 {@code ToolMemoryCard#onItemUse}
     * 在潜行时会 {@code stack.setTagCompound(null)}，把整张卡的数据（含模式与粘贴选项）清空。
     * 粘贴走的是右键空气（{@code onItemRightClick}），方块交互不需要它。
     */
    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        // 客户端同样返回 true 消费这次点击，避免再发一次 (-1,-1,-1) 空气包
        return true;
    }

    private static boolean isAeTarget(World world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        return tileEntity instanceof AEBaseTile || tileEntity instanceof IPartHost;
    }

    /**
     * 服务端每次方块右键打一行，用于区分「我们的 onItemUseFirst 没被调用」和
     * 「调用了但没命中目标」。定位完成后可以删掉。
     */
    private static void logClick(EntityPlayer player, World world, int x, int y, int z, CardMode mode) {
        org.apache.logging.log4j.Logger logger = AdvancedMemoryCardMod.getLogger();
        if (logger == null) {
            return;
        }
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        logger.info(
            "[amc] click at {},{},{} mode={} sneaking={} tile={}",
            x,
            y,
            z,
            mode.getType(),
            player.isSneaking(),
            tileEntity == null ? "null"
                : tileEntity.getClass()
                    .getSimpleName());
    }

    private static void openCopyGui(EntityPlayer player, World world) {
        logGuiOpen("copy", player);
        player.openGui(AdvancedMemoryCardMod.INSTANCE, AdvancedMemoryCardMod.COPY_GUI_ID, world, 0, 0, 0);
    }

    /**
     * 打开 GUI 只在服务端发生，日志用于排查「点了没反应」：如果这行出现但界面没出，
     * 问题在 GuiHandler / 客户端构造；这行不出现，问题在点击链路上游。
     */
    static void logGuiOpen(String mode, EntityPlayer player) {
        org.apache.logging.log4j.Logger logger = AdvancedMemoryCardMod.getLogger();
        if (logger != null) {
            logger.info("[amc] opening {} GUI for {}", mode, player.getCommandSenderName());
        }
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
