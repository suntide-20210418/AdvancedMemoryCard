package com.suntide_20210418.advancedmemorycard.item.custom;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.items.tools.ToolMemoryCard;
import appeng.tile.AEBaseTile;
import appeng.util.SettingsFrom;
import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import static com.suntide_20210418.advancedmemorycard.utils.AreaHelper.Area;
import static com.suntide_20210418.advancedmemorycard.utils.AreaHelper.calculateVolume;
import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.CopyMode.*;
import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.Tooltip.*;

public class CopyMode extends CardMode {

    private static final String START_POS = "start_pos";
    private static final String END_POS = "end_pos";
    private static final String IS_COPYING = "is_copying";

    private BlockPos startPos;
    private BlockPos endPos;
    private boolean isCopying;

    public CopyMode() {}

    @Override
    protected CardMode load(NBTTagCompound tag) {
        if (tag.hasKey(START_POS)) {
            startPos = BlockPos.fromLong(tag.getLong(START_POS));
        } else {
            startPos = null;
        }

        if (tag.hasKey(END_POS)) {
            endPos = BlockPos.fromLong(tag.getLong(END_POS));
        } else {
            endPos = null;
        }

        isCopying = tag.getBoolean(IS_COPYING);
        return this;
    }

    @Override
    public NBTTagCompound save(NBTTagCompound tag) {
        NBTTagCompound data = super.save(tag);

        if (startPos != null) {
            data.setLong(START_POS, startPos.toLong());
        }
        if (endPos != null) {
            data.setLong(END_POS, endPos.toLong());
        }
        data.setBoolean(IS_COPYING, isCopying);
        return data;
    }

    public static int getMaxVolume() {
        return ModConfigs.getServerConfig().maxCopyVolume;
    }

    public BlockPos getStartPos() {
        return startPos;
    }

    public BlockPos getEndPos() {
        return endPos;
    }

    public void setEndPos(ItemStack stack, BlockPos endPos) {
        this.endPos = endPos;
        this.save(CardMode.getOrCreateRoot(stack));
    }

    public void setStartPos(ItemStack stack, BlockPos startPos) {
        this.startPos = startPos;
        this.save(CardMode.getOrCreateRoot(stack));
    }

    @Override
    public ActionResult<ItemStack> onItemUse(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (this.isCopying) {
            ToolMemoryCard ToolMemoryCard = (ToolMemoryCard) stack.getItem();
            NBTTagCompound data = ToolMemoryCard.getData(stack);
            int count = 0;

            if (startPos != null && endPos != null) {
                Iterable<BlockPos> positions = BlockPos.getAllInBox(startPos, endPos);
                for (BlockPos pos : positions) {
                    net.minecraft.tileentity.TileEntity blockEntity = world.getTileEntity(pos);
                    if (blockEntity instanceof AEBaseTile) {
                        AEBaseTile aeBlockEntity = (AEBaseTile) blockEntity;
                        aeBlockEntity.uploadSettings(SettingsFrom.MEMORY_CARD, data, player);
                        count++;
                    }
                    if (blockEntity instanceof IPartHost) {
                        IPartHost partHost = (IPartHost) blockEntity;
                        for (EnumFacing direction : EnumFacing.values()) {
                            IPart part = partHost.getPart(direction);
                            if (part != null) {
                                part.onActivate(player, EnumHand.MAIN_HAND,
                                        new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
                                count++;
                            }
                        }
                    }
                }
            }

            player.sendStatusMessage(new TextComponentString(completed(count)), true);
            this.isCopying = false;
            this.startPos = null;
            this.endPos = null;
            this.save(CardMode.getOrCreateRoot(stack));
        } else {
            player.sendStatusMessage(new TextComponentString(failed()), true);
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public String getType() {
        return AdvancedMemoryCardMod.MOD_ID + ":copy";
    }

    public boolean isCopying() {
        return isCopying;
    }

    public BlockPos getTargetedBlockPos(EntityPlayer player) {
        if (player == null) return null;
        RayTraceResult hitResult = player.rayTrace(5.0D, 1.0F);
        if (hitResult != null && hitResult.typeOfHit == RayTraceResult.Type.BLOCK) {
            return hitResult.getBlockPos();
        }
        return null;
    }

    @Override
    public String getName() {
        return show();
    }

    @Override
    protected String getDescription() {
        if (startPos == null) {
            return copyInfo();
        } else if (endPos == null) {
            return copyInfo() + copyFirstPos(startPos.toString());
        } else {
            return copyInfo() + copyFirstPos(startPos.toString())
                    + copySecondPos(endPos.toString()) + copyReady();
        }
    }

    public void clearPos(ItemStack stack) {
        startPos = null;
        endPos = null;
        isCopying = false;
        this.save(CardMode.getOrCreateRoot(stack));
    }

    @Override
    public EnumActionResult onItemUseFirst(ItemStack stack, EntityPlayer player, World world,
            BlockPos clickedPos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        if (player != null) {
            if (startPos == null) {
                startPos = clickedPos;
                this.save(CardMode.getOrCreateRoot(stack));
                player.sendStatusMessage(new TextComponentString(firstPosMarked(startPos.toString())), true);
            } else {
                endPos = clickedPos;
                long currentVolume = calculateVolume(startPos, endPos);
                int maxVolume = getMaxVolume();
                if (currentVolume > maxVolume) {
                    player.sendStatusMessage(new TextComponentString(tooLarge(currentVolume, maxVolume)), true);
                    this.startPos = null;
                    this.endPos = null;
                    this.save(CardMode.getOrCreateRoot(stack));
                    return EnumActionResult.FAIL;
                }
                isCopying = true;
                this.save(CardMode.getOrCreateRoot(stack));
                int[] area = Area(startPos, endPos);
                player.sendStatusMessage(new TextComponentString(
                        secondPosMarked(endPos.toString(), area[0], area[1], area[2])), true);
            }
        }
        return EnumActionResult.SUCCESS;
    }
}
