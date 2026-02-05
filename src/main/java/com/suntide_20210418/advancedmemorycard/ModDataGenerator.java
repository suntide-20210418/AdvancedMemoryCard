package com.suntide_20210418.advancedmemorycard;

import com.suntide_20210418.advancedmemorycard.datagen.ModEnusLangProvider;
import com.suntide_20210418.advancedmemorycard.datagen.ModItemModelsProvider;
import com.suntide_20210418.advancedmemorycard.datagen.ModRecipesProvider;
import com.suntide_20210418.advancedmemorycard.datagen.ModZhcnLangProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AdvancedMemoryCardMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModDataGenerator {
  @SubscribeEvent
  public static void gatherData(GatherDataEvent event) {
    DataGenerator generator = event.getGenerator();
    PackOutput packOutput = generator.getPackOutput();
    ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

    generator.addProvider(event.includeServer(), new ModRecipesProvider(packOutput));

    generator.addProvider(
        event.includeClient(), new ModItemModelsProvider(packOutput, existingFileHelper));
    generator.addProvider(event.includeClient(), new ModEnusLangProvider(packOutput));
    generator.addProvider(event.includeClient(), new ModZhcnLangProvider(packOutput));
  }
}
