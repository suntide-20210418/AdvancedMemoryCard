package com.suntide_20210418.advancedmemorycard.item.custom;

import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;
import appeng.parts.p2p.PartP2PTunnel;
import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.client.gui.ModGuiHandler;
import com.suntide_20210418.advancedmemorycard.p2p.P2PManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.ConfigMode.show;
import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.Tooltip.configInfo;

public class ConfigMode extends CardMode {
    private P2PManager currentP2PManager;

    @Override
    public String getType() {
        return AdvancedMemoryCardMod.MOD_ID + ":config";
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world,
            int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (player != null) {
            TileEntity blockEntity = world.getTileEntity(x, y, z);
            if (blockEntity instanceof IPartHost) {
                IPartHost partHost = (IPartHost) blockEntity;
                // rv3: selectPart expects LOCAL (block-relative) coordinates
                SelectedPart selectedPart = partHost.selectPart(Vec3.createVectorHelper(hitX, hitY, hitZ));
                if (selectedPart.part instanceof PartP2PTunnel) {
                    PartP2PTunnel hitP2P = (PartP2PTunnel) selectedPart.part;
                    currentP2PManager = new P2PManager(hitP2P, player);
                    ModGuiHandler.putPendingManager(player, currentP2PManager);
                    player.openGui(AdvancedMemoryCardMod.INSTANCE, ModGuiHandler.CONFIG_GUI_ID, world, 0, 0, 0);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public ItemStack onItemUse(World world, EntityPlayer player) {
        return player.getHeldItem();
    }

    @Override
    public String getName() {
        return show();
    }

    @Override
    protected String getDescription() {
        return configInfo();
    }

    public P2PManager getCurrentP2PManager() {
        return currentP2PManager;
    }
}
