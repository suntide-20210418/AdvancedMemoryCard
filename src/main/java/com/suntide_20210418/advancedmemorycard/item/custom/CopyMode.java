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
import net.minecraft.tileentity.TileEntity;
import com.suntide_20210418.advancedmemorycard.utils.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import static com.suntide_20210418.advancedmemorycard.utils.AreaHelper.Area;
import static com.suntide_20210418.advancedmemorycard.utils.AreaHelper.calculateVolume;
import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.CopyMode.*;
import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.Tooltip.*;

public class CopyMode extends CardMode {

    private static final String START_X = "start_x";
    private static final String START_Y = "start_y";
    private static final String START_Z = "start_z";
    private static final String END_X = "end_x";
    private static final String END_Y = "end_y";
    private static final String END_Z = "end_z";
    private static final String IS_COPYING = "is_copying";

    private BlockPos startPos;
    private BlockPos endPos;
    private boolean isCopying;

    public CopyMode() {}

    @Override
    protected CardMode load(NBTTagCompound tag) {
        if (tag.hasKey(START_X)) {
            startPos = new BlockPos(tag.getInteger(START_X), tag.getInteger(START_Y), tag.getInteger(START_Z));
        } else {
            startPos = null;
        }

        if (tag.hasKey(END_X)) {
            endPos = new BlockPos(tag.getInteger(END_X), tag.getInteger(END_Y), tag.getInteger(END_Z));
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
            data.setInteger(START_X, startPos.getX());
            data.setInteger(START_Y, startPos.getY());
            data.setInteger(START_Z, startPos.getZ());
        }
        if (endPos != null) {
            data.setInteger(END_X, endPos.getX());
            data.setInteger(END_Y, endPos.getY());
            data.setInteger(END_Z, endPos.getZ());
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
    public ItemStack onItemUse(World world, EntityPlayer player) {
        ItemStack stack = player.getHeldItem();
        if (this.isCopying) {
            ToolMemoryCard toolMemoryCard = (ToolMemoryCard) stack.getItem();
            NBTTagCompound data = toolMemoryCard.getData(stack);
            int count = 0;

            if (startPos != null && endPos != null) {
                int minX = Math.min(startPos.getX(), endPos.getX());
                int minY = Math.min(startPos.getY(), endPos.getY());
                int minZ = Math.min(startPos.getZ(), endPos.getZ());
                int maxX = Math.max(startPos.getX(), endPos.getX());
                int maxY = Math.max(startPos.getY(), endPos.getY());
                int maxZ = Math.max(startPos.getZ(), endPos.getZ());
                for (int xi = minX; xi <= maxX; xi++) {
                    for (int yi = minY; yi <= maxY; yi++) {
                        for (int zi = minZ; zi <= maxZ; zi++) {
                            TileEntity blockEntity = world.getTileEntity(xi, yi, zi);
                            if (blockEntity instanceof AEBaseTile) {
                                AEBaseTile aeBlockEntity = (AEBaseTile) blockEntity;
                                aeBlockEntity.uploadSettings(SettingsFrom.MEMORY_CARD, data);
                                count++;
                            }
                            if (blockEntity instanceof IPartHost) {
                                IPartHost partHost = (IPartHost) blockEntity;
                                for (ForgeDirection direction : ForgeDirection.values()) {
                                    IPart part = partHost.getPart(direction);
                                    if (part != null) {
                                        part.onActivate(player, Vec3.createVectorHelper(xi + 0.5, yi + 0.5, zi + 0.5));
                                        count++;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            player.addChatMessage(new ChatComponentText(completed(count)));
            this.isCopying = false;
            this.startPos = null;
            this.endPos = null;
            this.save(CardMode.getOrCreateRoot(stack));
        } else {
            player.addChatMessage(new ChatComponentText(failed()));
            return stack;
        }
        return stack;
    }

    @Override
    public String getType() {
        return AdvancedMemoryCardMod.MOD_ID + ":copy";
    }

    public int getSelectionColor() {
        if (isCopying) {
            return ModConfigs.getClientConfig().copySelectionReady;
        } else if (endPos == null && startPos != null) {
            return ModConfigs.getClientConfig().copySelectionSecond;
        } else {
            return ModConfigs.getClientConfig().copySelectionFirst;
        }
    }

    public BlockPos getTargetedBlockPos(EntityPlayer player) {
        if (player == null) return null;
        MovingObjectPosition hitResult = player.rayTrace(5.0D, 1.0F);
        if (hitResult != null && hitResult.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return new BlockPos(hitResult.blockX, hitResult.blockY, hitResult.blockZ);
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
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world,
            int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (player != null) {
            if (startPos == null) {
                startPos = new BlockPos(x, y, z);
                this.save(CardMode.getOrCreateRoot(stack));
                player.addChatMessage(new ChatComponentText(firstPosMarked(startPos.toString())));
            } else {
                endPos = new BlockPos(x, y, z);
                long currentVolume = calculateVolume(startPos, endPos);
                int maxVolume = getMaxVolume();
                if (currentVolume > maxVolume) {
                    player.addChatMessage(new ChatComponentText(tooLarge(currentVolume, maxVolume)));
                    this.startPos = null;
                    this.endPos = null;
                    this.save(CardMode.getOrCreateRoot(stack));
                    return false;
                }
                isCopying = true;
                this.save(CardMode.getOrCreateRoot(stack));
                int[] area = Area(startPos, endPos);
                player.addChatMessage(new ChatComponentText(
                        secondPosMarked(endPos.toString(), area[0], area[1], area[2])));
            }
        }
        return true;
    }
}
